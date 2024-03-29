package com.example.tscrcpydroid.viewmodels

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.SurfaceView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.tscrcpydroid.ListenService
import com.example.tscrcpydroid.data.entities.BitRate
import com.example.tscrcpydroid.data.entities.Resolution
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


@HiltViewModel
class ScreenCopyScreenViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _surfaceView = SurfaceView(applicationContext)
    private val _surfaceViewState = MutableStateFlow(_surfaceView)
    val surfaceViewState: StateFlow<SurfaceView>
        get() = _surfaceViewState
    val firstTime = true //第一次连接，否则说明服务应该已经启动了
    var resolution: Resolution
    var bitRate: BitRate
    var targetIP: String
    var targetPort = 13432//给个默认值

    init {
        // 这些值是传参传过来的绝对不应该为null
        targetIP = savedStateHandle.get<String>("targetIP")!!
        targetPort = savedStateHandle.get<Int>("targetPort")!!
        resolution = Resolution(
            savedStateHandle.get<Int>("width")!!,
            savedStateHandle.get<Int>("height")!!
        )
        bitRate = BitRate(savedStateHandle.get<Int>("bitRate")!!)
        val serviceConnection =
            object : ServiceConnection {
                @SuppressLint("ClickableViewAccessibility")
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    service as ListenService.ListenBinder
                    val listenService = service.getService()
                    if (firstTime) {
                        listenService.start(
                            _surfaceView, targetIP, targetPort,
                            resolution, bitRate)
                        //完全就是在破坏MVVM
                        _surfaceView.setOnTouchListener { v, event ->
                            listenService.onTouchEvent(event)
                            //v.performClick()//其实没必要click
                        }
                    } else {
                        TODO()
                        //listenService.updateServiceParam(_surfaceView.holder.surface, targetIP, targetPort,resolution)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    TODO("Not yet implemented")
                }
            }
        val intent = Intent(applicationContext, ListenService::class.java)
        applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
}
