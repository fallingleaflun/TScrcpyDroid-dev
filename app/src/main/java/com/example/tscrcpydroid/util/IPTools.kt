package com.example.tscrcpydroid.util

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.util.Log
import java.math.BigInteger
import java.net.InetAddress

fun getMyWifiIPAddress(context: Context): String?{
    try {
        val wm = context.getSystemService(WIFI_SERVICE) as WifiManager
        val longIp = wm.connectionInfo.ipAddress.toLong()
        val byteIP = BigInteger.valueOf(longIp).toByteArray().reversedArray()
        return InetAddress.getByAddress(byteIP).hostAddress
    }
    catch(e: Exception){
        Log.e("IPTools", "getMyWifiIPAddress failed")
    }
    return null
}