package com.example.tscrcpydroid.util

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.example.tscrcpydroid.data.entities.BitRate
import com.example.tscrcpydroid.data.entities.Resolution
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Arrays
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * 包括两个线程
 * 一个线程接受网络连接、解析数据然后喂给解码器
 * 一个线程接受解码器输出并渲染到surface
 * 但是这两个线程要从service启动
 */
class ScreenDecoder(
    private val surface: Surface,
    private val inputStream: DataInputStream,
    private val bitRate: BitRate
)  {
    private val DEFAULT_I_FRAME_INTERVAL = 10 // seconds
    private val REPEAT_FRAME_DELAY_US = 100_000L // repeat after 100ms
    private val KEY_MAX_FPS_TO_ENCODER = "max-fps-to-encoder"

    private val PACKET_FLAG_CONFIG = 1L shl 63
    private val PACKET_FLAG_KEY_FRAME = 1L shl 62
    private val LOW_62_MASK = (1L shl 62) - 1L
    private val DEVICE_NAME_FIELD_LENGTH = 64

    private var targetDeviceName = "homo"
    private var targetWidth = -114514
    private var targetHeight = -114514

    private val ptsBuffer = ByteArray(8)
    private val lenBuffer = ByteArray(4)

    private val encoderName = "" //暂时没用
    private val maxFps = 0 //暂时没加设置

    private lateinit var mCodec: MediaCodec
    private lateinit var mFormat: MediaFormat

    private var running = AtomicBoolean(false)
    private val isConfigured = AtomicBoolean(false) // 是否已经处理好sps和pps

    /**
     * 处理dummyBuffer以及DeviceMetaData
     */
    fun parseDeviceMetaData() {
        // 有一个dummyByte在最开始
        val dummyBuffer = ByteArray(1)
        var r = inputStream.readFully(dummyBuffer) //读取到的值应该是114才对
        // 从metadata读取分辨率
        readDeviceMetaData(inputStream)
        //设置刚刚读取到的分辨率
        mFormat = createFormat()
        mFormat.setInteger(MediaFormat.KEY_WIDTH, targetWidth)
        mFormat.setInteger(MediaFormat.KEY_HEIGHT, targetHeight)
    }

    /**
     * 没有codecOption
     */
    private fun createFormat(): MediaFormat {
        val format = MediaFormat()
        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate.value)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 60)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, DEFAULT_I_FRAME_INTERVAL)
        format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, REPEAT_FRAME_DELAY_US); // µs
        if (maxFps > 0) {
            format.setFloat(KEY_MAX_FPS_TO_ENCODER, maxFps.toFloat())
        }
        return format
    }

    private fun createCodec(): MediaCodec {
        if (encoderName.isNotBlank()) {
            Log.i("ZLT", "Creating encoder by name: '${encoderName}'");
            try {
                return MediaCodec.createByCodecName(encoderName);
            } catch (e: IllegalArgumentException) {
                Log.e("ZLT", "There is no encoder having name '${encoderName}'")
            }
        }
        val codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        Log.d("ZLT", "Using decoder: '${codec.name}'")
        return codec
    }

    /**
     * 列出可用的编码器，暂时不用
     */
    private fun listEncoders(): Array<MediaCodecInfo>? {
        val result: MutableList<MediaCodecInfo> = ArrayList()
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (codecInfo in list.codecInfos) {
            if (codecInfo.isEncoder && Arrays.asList(*codecInfo.supportedTypes)
                    .contains(MediaFormat.MIMETYPE_VIDEO_AVC)
            ) {
                result.add(codecInfo)
            }
        }
        return result.toTypedArray()
    }

    /**
     * 获取包解析并喂给解码器
     * 目前没有考虑屏幕旋转
     */
    fun feedIntoDecocder() {
        while (running.get()) {
            if (inputStream.available() > 0) {
                //获取pts以及包主体长度
                var r = inputStream.read(ptsBuffer)
                if (r < 8) {
                    Log.e("ZLT", "broken pts")
                }
                r = inputStream.read(lenBuffer)
                if (r < 4) {
                    Log.e("ZLT", "broken video package size")
                    Log.e("ZLT", "buffer read:${r}bytes")
                }
                val pts = byteToLong(ptsBuffer)
                val len = byteToInt(lenBuffer)

                Log.d("ZLT", "pts:${pts}\nlen:${len}")

                //获取包的主体部分
                val dataBuffer = ByteArray(len)
                try {
                    inputStream.readFully(dataBuffer)
                }
                catch (e:IOException){
                    Log.e("ZLT", e.stackTraceToString())
                    Log.e("ZLT", "broken video data package")
                    Log.e("ZLT", "buffer read:${r}bytes")
                }

                //64位的PTS头两位用来标识是否关键帧或配置包
                //一般帧:00开头
                //关键帧:01开头
                //配置包:10开头
                //剩余62位是pts
                val bufferInfo = MediaCodec.BufferInfo()
                var spsBuffer: ByteBuffer
                var ppsBuffer: ByteBuffer
                if ((pts and PACKET_FLAG_CONFIG) != 0L) {//是配置包，需要拼接起来，最后肯定要算出来sps和pps
                    //获取sps和pps
                    val configBuffer = ByteBuffer.wrap(dataBuffer)
                    if (configBuffer.getInt() == 0x00000001) {//获取NALU start code
                        var ppsIndex = 0
                        while (!(configBuffer.get().toInt() == 0x00
                                    && configBuffer.get().toInt() == 0x00
                                    && configBuffer.get().toInt() == 0x00
                                    && configBuffer.get().toInt() == 0x01
                                    )
                        ) {
                        }
                        ppsIndex = configBuffer.position()
                        val sps = ByteArray(ppsIndex - 4)
                        System.arraycopy(dataBuffer, 0, sps, 0, sps.size)
                        ppsIndex -= 4
                        val pps = ByteArray(dataBuffer.size - ppsIndex)
                        System.arraycopy(dataBuffer, ppsIndex, pps, 0, pps.size)
                        spsBuffer = ByteBuffer.wrap(sps)
                        ppsBuffer = ByteBuffer.wrap(pps)
                        mFormat.setByteBuffer("csd-0", spsBuffer)
                        mFormat.setByteBuffer("csd-1", ppsBuffer)
                        mCodec = createCodec()
                        mCodec.configure(mFormat, surface, null, 0)
                        mCodec.start()
                        isConfigured.set(true)
                    } else {
                        Log.e("ZLT", "Parsing NALU start code failed")
                    }
                } else {//不是配置包，那么可以喂给解码器
                    val realPts = (pts and LOW_62_MASK)
                    var flag = 0 // 默认为普通帧
                    if ((pts and PACKET_FLAG_KEY_FRAME) != 0L) {
                        flag = MediaCodec.BUFFER_FLAG_KEY_FRAME
                    }
                    if (isConfigured.get()) {//已经配置好了，可以开始解码
                        val inputBufferIndex = mCodec.dequeueInputBuffer(-1)
                        if (inputBufferIndex >= 0) {
                            val inputBuffer = mCodec.getInputBuffer(inputBufferIndex)
                            inputBuffer?.clear()
                            inputBuffer?.put(dataBuffer)
                            mCodec.queueInputBuffer(inputBufferIndex,0, dataBuffer.size, realPts, flag)
                        }
                    }
                }
            }
        }
    }

    fun readDeviceMetaData(inputStream: DataInputStream) {
        val deviceNameBuffer = ByteArray(DEVICE_NAME_FIELD_LENGTH)
        try {
            inputStream.readFully(deviceNameBuffer)
        }catch (e:IOException){
            Log.e("ZLT", e.stackTraceToString())
            Log.i("ZLT", "readDeviceMetaData read device name failed")
        }
        targetDeviceName = String(deviceNameBuffer, Charsets.UTF_8)

        val widthBuffer = ByteArray(4)
        try {
            inputStream.readFully(widthBuffer, 2, 2)
        } catch (e: IOException){
            Log.e("ZLT", e.stackTraceToString())
            Log.i("ZLT", "readDeviceMetaData read width failed")
        }
        targetWidth = byteToInt(widthBuffer)

        val heightBuffer = ByteArray(4)
        try {
            inputStream.read(heightBuffer, 2, 2)
        } catch (e: IOException){
            Log.e("ZLT", e.stackTraceToString())
            Log.i("ZLT", "readDeviceMetaData read height failed")

        }
        targetHeight = byteToInt(heightBuffer)

        Log.i(
            "ZLT", "readDeviceMetaData:\n" +
                    "targetDeviceName:${targetDeviceName}\n" +
                    "targetWidth:${targetWidth}\n" +
                    "targetHeight:${targetHeight}"
        )
    }

    /**
     * 解码器输出渲染到屏幕的循环
     */
    fun decodeLoop() {
        val bufferInfo = BufferInfo()
        while (running.get()) {
            if (isConfigured.get()) {
                val outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, -1)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = mCodec.getOutputBuffer(outputBufferIndex)
                    //
                    //如果需要某一帧进行字节上的处理，可在此对outputBuffer进行处理
                    //
                    mCodec.releaseOutputBuffer(outputBufferIndex, true)
                    val eof = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    if (eof) {
                        this.stop()
                        break
                    }
                }
            } else {//等待配置
                Thread.sleep(10)
            }
        }
    }

    fun start() {
        if (!running.get()) {
            running.set(true)
        }
    }

    fun stop() {
        running.set(false)
    }
}