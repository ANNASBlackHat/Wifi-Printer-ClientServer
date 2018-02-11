package com.example.annasblackhat.printwificlientserver

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Created by annasblackhat on 11/02/18.
 */

fun Context.toast(msg: String, duration: Int = Toast.LENGTH_SHORT){
    if(Looper.myLooper() == Looper.getMainLooper()){
        Toast.makeText(this, msg, duration).show()
    }else{
        Handler(Looper.getMainLooper()).post { Toast.makeText(this, msg, duration).show() }
    }
}