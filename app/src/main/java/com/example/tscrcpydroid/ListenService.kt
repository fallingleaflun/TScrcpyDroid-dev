package com.example.tscrcpydroid

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import com.example.tscrcpydroid.data.entities.BitRate
import com.example.tscrcpydroid.data.entities.Resolution
import com.example.tscrcpydroid.util.ScreenDecoder
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.properties.Delegates

class ListenService : Service() {
    private val mBinder = ListenBinder()
    private lateinit var targetIP: String
    private var targetPort by Delegates.notNull<Int>()
    private lateinit var surface: Surface
    private lateinit var resolution: Resolution
    private lateinit var bitRate: BitRate
    private var videoDecoder: ScreenDecoder? = null
    private var running = AtomicBoolean(true)
    private var rotationCallback: (() -> Unit)? = null //先不管这个callback了

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    inner class ListenBinder : Binder() {
        fun getService(): ListenService {
            return this@ListenService
        }
    }


    /**
     * 设置旋转回调
     */
    fun setRotationCallback(callback: (() -> Unit)?) {
        this.rotationCallback = callback
    }

    /**
     * 首次运行此服务
     */
    fun start(
        surface: Surface,
        targetIP: String,
        targetPort: Int,
        resolution: Resolution,
        bitRate: BitRate
    ) {
        this.surface = surface
        this.targetIP = targetIP
        this.targetPort = targetPort
        this.resolution = resolution
        this.bitRate = bitRate
        thread {
            workLoop()
        }
    }

    /**
     * 并非首次运行此服务，只是想改变一下参数
     */
    //fun updateServiceParam(surface: Surface, targetIP:String, targetPort: Int, resolution: Resolution){
    //    this.surface = surface
    //    this.targetIP = targetIP
    //    this.targetPort = targetPort
    //    this.resolution = resolution
    //}


    fun workLoop() {
        var videoSocket: Socket? = null
        var controlSocket: Socket? = null
        var videoSocketAccepted = false
        var controlSocketAccepted = false

        //尝试连接videoSocket
        var cnt = 50
        while (0 < cnt && !videoSocketAccepted) {
            try {
                videoSocket = Socket(targetIP, targetPort)
                videoSocketAccepted = true
            } catch (e: IOException){
                e.printStackTrace()
            }
            finally {
                cnt--;
                Thread.sleep(100)
            }
        }
        //尝试连接controlSocket
        cnt = 50
        while (0 < cnt && !controlSocketAccepted) {
            try {
                controlSocket = Socket(targetIP, targetPort)
                controlSocketAccepted = true
            } catch (e: IOException){
                e.printStackTrace()
            }
            finally {
                cnt--;
                Thread.sleep(100)
            }
        }
        if (videoSocketAccepted && controlSocketAccepted && videoSocket!=null && controlSocket!=null) {//这个不是always true，因为可能会因为抛异常而跳过
            val dataInputStream = DataInputStream(videoSocket.getInputStream())
            val dataOutputStream = DataOutputStream(controlSocket.getOutputStream())
            videoDecoder = ScreenDecoder(surface, dataInputStream, bitRate)
            // 启动视频解码的两个线程
            videoDecoder?.start()
            videoDecoder?.parseDeviceMetaData()
            thread { videoDecoder?.feedIntoDecocder() }
            thread { videoDecoder?.decodeLoop() }

            // TODO：启动指令执行的线程
        } else {
            Log.e("ZLT", "Socket connect failed")
            Log.e("ZLT", "videoSocketAccepted:${videoSocketAccepted}")
            Log.e("ZLT", "controlSocketAccepted:${controlSocketAccepted}")
        }
    }

    fun onTouchEvent(m: MotionEvent): Boolean{
        TODO("实现一个接受surfaceView的MotionEvent的handler")
    }
}