package com.example.tscrcpydroid.viewmodels

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.SurfaceView
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.tscrcpydroid.ListenService
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
    private lateinit var _surfaceView: SurfaceView
    private val _surfaceViewState = MutableStateFlow(_surfaceView)
    val surfaceViewState: StateFlow<SurfaceView>
        get() = _surfaceViewState
    val firstTime = true //第一次连接，否则说明服务应该已经启动了
    lateinit var resolution: Resolution
    lateinit var targetIP: String
    var targetPort = 13432//给个默认值

    private val serviceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                service as ListenService.ListenBinder
                val listenService = service.getService()
                if (firstTime) {
                    listenService.start(_surfaceView.holder.surface, targetIP, targetPort, resolution)
                } else {
                    listenService.updateServiceParam(_surfaceView.holder.surface, targetIP, targetPort,resolution)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                TODO("Not yet implemented")
            }
        }
    }

    init {
        // 这些值是传参传过来的绝对不应该为null
        targetIP = savedStateHandle.get<String>("targetIP")!!
        targetPort = savedStateHandle.get<Int>("targetPort")!!
        resolution = Resolution(
            savedStateHandle.get<Int>("width")!!,
            savedStateHandle.get<Int>("height")!!
        )
        _surfaceView = SurfaceView(applicationContext)
        val intent = Intent(applicationContext, ListenService::class.java)
        applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
}
