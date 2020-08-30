package com.mohammadkk.mywebview

import android.net.Uri
import android.webkit.WebView
import java.text.SimpleDateFormat
import java.util.*

class HelperUnit {
    fun fileName(url: String): String{
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH)
        val currentTime = sdf.format(Date())
        val domain = Uri.parse(url).host!!.replace("www.","").trim()
        return domain.replace(".","_").trim() + "_" + currentTime.trim()
    }
}