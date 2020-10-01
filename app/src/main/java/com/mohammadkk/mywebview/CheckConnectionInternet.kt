package com.mohammadkk.mywebview

import android.content.Context
import android.net.ConnectivityManager

class CheckConnectionInternet(private var context: Context) {

    final fun checkedConnectionInternet():Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = manager.activeNetworkInfo
        return info != null && info.isConnected
    }
}