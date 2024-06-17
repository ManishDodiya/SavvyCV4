package com.savvytech.savvylevelfour.common

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter

fun Activity.makeStatusBarTransparent() {
    window.apply {
//        clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        statusBarColor = ContextCompat.getColor(this.context, android.R.color.transparent)
        addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
//        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        statusBarColor = Color.TRANSPARENT
    }
}

fun View.setMarginTop(marginTop: Int) {
    val menuLayoutParams = this.layoutParams as ViewGroup.MarginLayoutParams
    menuLayoutParams.setMargins(0, marginTop, 0, 0)
    this.layoutParams = menuLayoutParams
}

@BindingAdapter("changeTintColor")
fun ImageView.changeTintColor(color: Int) {
    this.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
}

fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

@BindingAdapter("setZoomInOut")
fun com.savvytech.savvylevelfour.common.TiltGauge.setZoomInOut(inOrOut: Char) {
    this.zoom = inOrOut
}

fun TextView.showHtmlText(message: String) {
    this.setText(message)
}