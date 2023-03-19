package com.example.tscrcpydroid.viewmodels

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tscrcpydroid.util.getMyWifiIPAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject

@HiltViewModel
class ShowIPScreenViewModel@Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    savedStateHandle: SavedStateHandle
): ViewModel(){
    private val _currentIpAddress:SharedFlow<String?>
    val currentIpAddress: SharedFlow<String?>
        get() = _currentIpAddress

    init {
        _currentIpAddress = callbackFlow {
            val receiver = object : BroadcastReceiver(){
                override fun onReceive(p0: Context?, p1: Intent?) {
                    trySendBlocking(getMyWifiIPAddress(applicationContext))
                }
            }
            applicationContext.registerReceiver(receiver, IntentFilter().apply {
                addAction("android.net.wifi.STATE_CHANGE")
                addAction("android.net.wifi.WIFI_STATE_CHANGE")
            })
            awaitClose{
                applicationContext.unregisterReceiver(receiver)
            }
        }.shareIn(viewModelScope, started = SharingStarted.WhileSubscribed())
    }
}

data class ShowIPScreenState (
    val currentIpAddress: String? = null
)
