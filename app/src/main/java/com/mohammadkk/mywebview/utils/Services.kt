package com.mohammadkk.mywebview.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.view.View
import com.google.android.material.snackbar.Snackbar

object Services {
    @Suppress("DEPRECATION")
    fun isInternetConnected(context: Context):Boolean{
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = cm.activeNetworkInfo
        return info!= null && info.isConnected
    }
    fun rootSnackBar(activity: Activity,message:String){
        val rootView:View = activity.window.decorView.findViewById(android.R.id.content)
        Snackbar.make(rootView,message,Snackbar.LENGTH_LONG).show()
    }
    var isTargetList = false
}