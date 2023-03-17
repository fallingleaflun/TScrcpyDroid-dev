package com.example.tscrcpydroid.util

import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaFormat
import android.view.Surface
import com.example.tscrcpydroid.data.entities.Resolution
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 包装了个解码线程
 */
class AnotherVideoDecoder {
    private lateinit var mMediaCodec: MediaCodec
    private var mWorker: Worker? = null
    private var configured = AtomicBoolean(false)
    private var running = AtomicBoolean(false)


    /**
     * surface绑定到codec上, 并且设定好csd0与csd1缓冲
     */
    fun configure(surface: Surface, resolution: Resolution, csd0: ByteBuffer, csd1: ByteBuffer) {
        mWorker?.configure(surface, resolution, csd0, csd1)
    }


    fun start() {
        if(!running.get() && mWorker==null){
            mWorker = Worker()
            mWorker?.start()
            running.set(true)
        }
    }

    fun stop() {
        if(running.get() && mWorker!=null){
            running.set(false)
            mWorker?.clear()
            mWorker = null
        }
    }


    inner class Worker : Thread() {

        fun configure(
            surface: Surface,
            resolution: Resolution,
            csd0: ByteBuffer,
            csd1: ByteBuffer
        ) {
            if (configured.get()) {
                configured.set(false)
                mMediaCodec.stop()
            }
            val format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                resolution.width,
                resolution.height
            )
            format.setByteBuffer("csd-0", csd0)
            format.setByteBuffer("csd-1", csd1)
            mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mMediaCodec.configure(format, surface, null, 0)
            mMediaCodec.start()
            configured.set(true)
        }

        fun disconfigure(){
            if(configured.get()){
                configured.set(false)
                mMediaCodec.stop()
            }
        }


        private fun decode() {
            val bufferInfo = BufferInfo()
            while (running.get()) {
                if (configured.get()) {
                    val index = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                    if (index >= 0) {
                        mMediaCodec.releaseOutputBuffer(
                            index,
                            true
                        ) // render=true表示在释放的同时会把渲染到codec所绑定的surface上
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM == MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                            break
                        }
                    }
                }
                else {
                    Thread.sleep(10)
                }
            }
        }

        override fun run() {
            decode()
        }

        fun clear(){
            disconfigure()
        }
    }
}
