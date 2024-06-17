package com

import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication

class MyApplication : MultiDexApplication() {
//    lateinit var preference: Preference


    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    /*override fun onCreate() {
        preference = Preference(this)
        super.onCreate()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, p1: Bundle?) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR)
                activity.setRequestedOrientation(
                    preference.getIntData(Preference.SCREEN_ORIENTATION)!!);
            }

            override fun onActivityStarted(activity: Activity) {
                *//*activity.setRequestedOrientation(
                    preference.getIntData(Preference.SCREEN_ORIENTATION)!!)*//*
            }

            override fun onActivityResumed(activity: Activity) {
                *//*activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR)

                activity.setRequestedOrientation(
                    preference.getIntData(Preference.SCREEN_ORIENTATION)!!)*//*
            }

            override fun onActivityPaused(activity: Activity) {
                *//*activity.setRequestedOrientation(
                    preference.getIntData(Preference.SCREEN_ORIENTATION)!!)*//*
            }

            override fun onActivityStopped(activity: Activity) {
                *//*activity.setRequestedOrientation(
                    preference.getIntData(Preference.SCREEN_ORIENTATION)!!)*//*
            }

            override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {
                *//*activity.setRequestedOrientation(
                    preference.getIntData(Preference.SCREEN_ORIENTATION)!!)*//*
            }

            override fun onActivityDestroyed(activity: Activity) {
                *//*activity.setRequestedOrientation(
                    preference.getIntData(Preference.SCREEN_ORIENTATION)!!)*//*
            }

        })
    }*/
}