package com.example.tscrcpydroid

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.util.Base64
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tscrcpydroid.data.entities.BitRate
import com.example.tscrcpydroid.util.ConnectionTools.startRemoteServer
import com.example.tscrcpydroid.util.toInt
import com.example.tscrcpydroid.util.toLong
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.Socket

@RunWith(AndroidJUnit4::class)
class ConnectionPackageTest {
    val localIP = "192.168.1.142"
    val targetIP = "192.168.1.239"
    val targetPort = 13432


    @Test
    fun startRemoteServerTest() {
        val context: Context = ApplicationProvider.getApplicationContext()//利用这个在测试类中获取context
        val assetManager = context.assets
        val inputStream = assetManager.open("scrcpy-server.jar")
        val buffer = ByteArray(inputStream.available())
        inputStream.read(buffer)
        val fileBase64 = Base64.encode(buffer, Base64.NO_WRAP)
        val result = startRemoteServer(
            context = context,
            localIP = localIP,
            targetIP = targetIP,
            targetPort = 13432,
            fileBase64 = fileBase64,
            bitRate = BitRate(1024 * 1024),
            size = 1280,
            timeOut = 100000
        )
        Assert.assertTrue(result)
        while (true){
        }
        //已经测试通过，可以启动server.jar
    }

    /**
     * 测试视频流解包
     */
    @Test
    fun videoSocketTest(){
        while(true) {
            try {
                //本来可以只用一个Socket，因为是全双工的，但是Server端非要开两个Socket
                val videoSocket = Socket(targetIP, targetPort)
                val controlSocket = Socket(targetIP, targetPort)
                val dataInputStream = videoSocket.getInputStream()
                val dataOutputStream = controlSocket.getOutputStream()
                var ptsOrigin = 0L
                while (true){
                    if(dataInputStream.available()>0){
                        //获取pts以及包主体长度
                        val ptsBuffer = ByteArray(8)
                        var r = dataInputStream.read(ptsBuffer)
                        if(r<8) {
                            Log.e("ZLT", "broken pts")
                        }
                        val lenBuffer = ByteArray(4)
                        r = dataInputStream.read(lenBuffer)
                        if(r<8){
                            Log.e("ZLT", "broken video package size")
                        }
                        val pts = ptsBuffer.toLong()
                        val len = lenBuffer.toInt()

                        //获取包的主体部分
                        val dataBuffer = ByteArray(len)
                        r = dataInputStream.read(dataBuffer)
                        if(r<len){
                            Log.e("ZLT", "broken video data package")
                        }

                        //根据PTS判断是关键帧还是配置包
                        //关键帧:01开头
                        //配置包:10开头
                        //剩余62位是pts
                        val PACKET_FLAG_CONFIG = 1L shl 63
                        val PACKET_FLAG_KEY_FRAME = 1L shl 62
                        val LOW_62_MASK = (1L shl 62) - 1L
                        val bufferInfo = BufferInfo()
                        if((pts and PACKET_FLAG_CONFIG)!=0L){
                            bufferInfo.flags = bufferInfo.flags or MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                        }
                        else if((pts and PACKET_FLAG_KEY_FRAME) != 0L) {
                            bufferInfo.flags = bufferInfo.flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                            if (ptsOrigin == 0L) {
                                ptsOrigin = bufferInfo.presentationTimeUs
                            }
                            bufferInfo.presentationTimeUs = (pts and LOW_62_MASK) + ptsOrigin
                        }

                        //不知道怎么测试
                    }
                }
            } catch (e: IOException){
                e.printStackTrace()
            }
        }
    }

}