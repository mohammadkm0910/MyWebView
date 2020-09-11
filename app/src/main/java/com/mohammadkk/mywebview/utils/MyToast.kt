package com.mohammadkk.mywebview.utils

import android.content.Context
import es.dmoral.toasty.Toasty

class MyToast(private var context:Context, private var msg:String, private var mode:Int) {
    fun show() {
        when(mode) {
            1-> Toasty.warning(context,msg,Toasty.LENGTH_LONG).show()
            2-> Toasty.info(context,msg,Toasty.LENGTH_LONG).show()
            3-> Toasty.success(context,msg,Toasty.LENGTH_LONG).show()
        }
    }
}