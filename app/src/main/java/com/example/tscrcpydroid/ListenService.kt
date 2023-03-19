package com.example.tscrcpydroid

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Insets
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import com.example.tscrcpydroid.data.entities.BitRate
import com.example.tscrcpydroid.data.entities.Resolution
import com.example.tscrcpydroid.util.ControlMessageSender
import com.example.tscrcpydroid.util.ControlMessageWriter
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
    private lateinit var surfaceView: SurfaceView //需要在获取到对面的设备大小之后再重新设置View的大小，我直接塞进来了，耦合度爆表
    private lateinit var resolution: Resolution
    var screenWidth = -114514
    var screenHeight = -114514
    private lateinit var bitRate: BitRate
    private lateinit var videoDecoder: ScreenDecoder
    private var running = AtomicBoolean(true)
    private var rotationCallback: (() -> Unit)? = null //先不管这个callback了
    private lateinit var controlMessageWriter: ControlMessageWriter
    private lateinit var dataInputStream: DataInputStream
    private lateinit var dataOutputStream: DataOutputStream

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
        surfaceView: SurfaceView,
        targetIP: String,
        targetPort: Int,
        resolution: Resolution,
        bitRate: BitRate
    ) {
        this.surfaceView = surfaceView
        this.surface = surfaceView.holder.surface
        this.targetIP = targetIP
        this.targetPort = targetPort
        this.resolution = resolution
        this.bitRate = bitRate
        //获取窗口大小，不知道Service能否正常获取
        val windowManager: WindowManager = this.baseContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = windowManager.getCurrentWindowMetrics()
            val insets: Insets = windowMetrics.getWindowInsets()
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            this.screenWidth = windowMetrics.getBounds().width() - insets.left - insets.right
            this.screenHeight = windowMetrics.getBounds().height() - insets.top - insets.bottom
        } else {
            val displayMetrics = DisplayMetrics()
            windowManager.getDefaultDisplay().getMetrics(displayMetrics)
            this.screenWidth = displayMetrics.widthPixels
            this.screenHeight = displayMetrics.heightPixels
        }
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
            dataInputStream = DataInputStream(videoSocket.getInputStream())
            dataOutputStream = DataOutputStream(controlSocket.getOutputStream())
            videoDecoder = ScreenDecoder(surface, dataInputStream, bitRate)
            // 启动视频解码的两个线程
            videoDecoder.start()
            videoDecoder.parseDeviceMetaData()
            surfaceView.holder.setFixedSize(videoDecoder.targetWidth, videoDecoder.targetHeight)//需要重新设置surfaceView的大小
            thread { videoDecoder.feedIntoDecocder() }
            thread { videoDecoder.decodeLoop() }
            // 启动发送控制信息的线程
            val controlMessageSender = ControlMessageSender(dataOutputStream)
            controlMessageWriter = ControlMessageWriter(controlMessageSender)
            thread {controlMessageSender.loop()}
        } else {
            Log.e("ZLT", "Socket connect failed")
            Log.e("ZLT", "videoSocketAccepted:${videoSocketAccepted}")
            Log.e("ZLT", "controlSocketAccepted:${controlSocketAccepted}")
        }
    }


    fun onTouchEvent(m: MotionEvent): Boolean{
        controlMessageWriter.writeMotionEvent(m, surfaceView.width, surfaceView.height, dataOutputStream)
        return true
    }
}