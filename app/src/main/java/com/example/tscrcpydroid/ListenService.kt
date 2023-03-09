package com.example.tscrcpydroid

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.view.Surface
import com.example.tscrcpydroid.data.entities.Resolution
import com.example.tscrcpydroid.util.VideoDecoder
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
    private lateinit var surface:Surface
    private lateinit var resolution: Resolution
    private lateinit var videoDecoder: VideoDecoder
    private var running = AtomicBoolean(true)
    private var rotationCallback: (()->Unit)? = null //先不管这个callback了
    private var serviceRunning = AtomicBoolean(true)

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    inner class ListenBinder : Binder(){
        fun getService(): ListenService{
            return this@ListenService
        }
    }


    /**
     * 设置旋转回调
     */
    fun setRotationCallback(callback: (()->Unit)?){
        this.rotationCallback = callback
    }

    /**
     * 首次运行此服务
     */
    fun start(surface: Surface, targetIP:String, targetPort:Int, resolution: Resolution){
        this.surface = surface
        this.targetIP = targetIP
        this.targetPort = targetPort
        this.resolution = resolution
        videoDecoder = VideoDecoder()//内部有解码线程
        videoDecoder.start()
        thread{
            handleConnection()
        }
    }

    /**
     * 并非首次运行此服务，只是想改变一下参数
     */
    fun updateServiceParam(surface: Surface, targetIP:String, targetPort: Int, resolution: Resolution){
        this.surface = surface
        this.targetIP = targetIP
        this.targetPort = targetPort
        this.resolution = resolution
        videoDecoder.stop()
        videoDecoder.start()
    }

    /**
     * 处理网络连接的线程
     */
    fun handleConnection(){
        var cnt=50
        while(0<cnt) {
            try {
                val socket = Socket(targetIP, targetPort)
                val dataInputStream = socket.getInputStream()
                val dataOutputStream = socket.getOutputStream()
                while (serviceRunning.get()){
                    TODO()
                }
                TODO()
            } catch (e: IOException){
                e.printStackTrace()
            }
            finally {
                cnt--
            }
        }
    }

    fun stopService(){
        TODO()
    }

    override fun onDestroy() {
        super.onDestroy()
        TODO()
    }
}