package com.example.tscrcpydroid.data.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tscrcpydroid.util.getMyWifiIPAddress
import kotlinx.coroutines.flow.MutableStateFlow

class WIFIStateChangeReceiver: BroadcastReceiver(){
    private val currentIPAddress: MutableStateFlow<String?> = MutableStateFlow("")

    fun getCurrentIPAddress(): MutableStateFlow<String?> {
        return currentIPAddress
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if(context!=null) {
            currentIPAddress.value = getMyWifiIPAddress(context)
        }
    }
}