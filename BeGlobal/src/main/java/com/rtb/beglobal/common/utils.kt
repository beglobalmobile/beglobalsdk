package com.rtb.beglobal.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

fun Context.connectionAvailable(): Boolean? {
    return try {
        val internetAvailable: Boolean
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifi: NetworkInfo? = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        val network: NetworkInfo? = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
        internetAvailable = wifi != null && wifi.isConnected || network != null && network.isConnected
        internetAvailable
    } catch (e: Exception) {
        null
    }
}