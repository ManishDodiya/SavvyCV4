package com.savvytech.savvylevelfour.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.savvytech.savvylevelfour.common.Utils.setStatusBarTextColor(this)
    }
}