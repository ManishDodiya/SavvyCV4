package com.savvytech.savvylevelfour.common

import android.content.Context
import android.content.SharedPreferences

class Preference(context: Context) {

    var sharedPreferences: SharedPreferences? = null
    var editor: SharedPreferences.Editor? = null
    var context: Context? = null

    companion object {
        val MAIN_PREF_INT: Int = 0
        val MAIN_PREF: String = "main_pref"
        val IS_LOGIN: String = "is_login"
        val DEVICE_NAME: String = "DEVICE_NAME"
        val DEVICE_ADDRESS: String = "DEVICE_ADDRESS"
        val DEVICE_ADDRESS_TEMP: String = "DEVICE_ADDRESS_TEMP"
        val SCREEN_ORIENTATION = "SCREEN_ORIENTATION"
        val batteryStatus = "batteryStatus"
        val tiltHeight = "tiltheight"
        val blockType = "blockType"
        val spanType = "spanType"
        val wheelToWheel = "wheelToWheel"
        val wheelToJockeyWheel = "wheelToJockeyWheel"
        val blockTypeName = "blockTypeName"
        val customBlockTypeValue = "customBlockTypeValue"
        val hitchEnabled = "hitchEnabled"
        val isZoom = "isZoom"
        val hitchValue = "hitchValue"
        val isToolTipCompleted = "isToolTipCompleted"
        val isDialogShow = "isDialogShow"
        val isFirstFragmentLoaded = "isFirstFragmentLoaded"
        val processId = "processID"
    }

    init {
        this.context = context
        sharedPreferences = context.getSharedPreferences(
            com.savvytech.savvylevelfour.common.Preference.Companion.MAIN_PREF,
            com.savvytech.savvylevelfour.common.Preference.Companion.MAIN_PREF_INT
        )
        editor = sharedPreferences?.edit()
    }

    fun setData(key: String, value: String) {
        editor?.putString(key, value)
        editor?.apply()
    }

    fun setWheelToWheelData(key: String, value: String) {
        editor?.putString(key, value)
        editor?.apply()
    }

    fun setSpanType(key: String, value: String) {
        editor?.putString(key, value)
        editor?.apply()
    }

    fun setWheelToJockeyWheelData(key: String, value: String) {
        editor?.putString(key, value)
        editor?.apply()
    }

    fun setCustomeBlockValue(key: String, value: String) {
        editor?.putString(key, value)
        editor?.apply()
    }

    fun setBlockTypeName(key: String, value: String) {
        editor?.putString(key, value)
        editor?.apply()
    }

    fun setDeviceAddressTemp(key: String, value: String) {
        editor?.putString(key, value)
        editor?.apply()
    }

    fun setFloatData(key: String, value: Float) {
        editor?.putFloat(key, value)
        editor?.apply()
    }

    fun setLogin(key: String, flag: Boolean) {
        editor?.putBoolean(key, flag)
        editor?.apply()
    }

    fun setBattData(key: String, flag: Boolean) {
        editor?.putBoolean(key, flag)
        editor?.apply()
    }

    fun setBoolenData(key: String, flag: Boolean) {
        editor?.putBoolean(key, flag)
        editor?.apply()
    }

    fun setIsShowDialogData(key: String, flag: Boolean) {
        editor?.putBoolean(key, flag)
        editor?.apply()
    }

    fun setIntData(key: String, flag: Int) {
        editor?.putInt(key, flag)
        editor?.apply()
    }

    fun getBattData(key: String): Boolean {
        return sharedPreferences?.getBoolean(key, false)!!
    }

    fun getIsShowDialog(key: String): Boolean {
        return sharedPreferences?.getBoolean(key, false)!!
    }

    fun getBooleanData(key: String): Boolean {
        return sharedPreferences?.getBoolean(key, false)!!
    }

    fun getFloatData(key: String): Float {
        return sharedPreferences?.getFloat(key, 0f)!!
    }

    fun getLogin(key: String): Boolean? {
        return sharedPreferences?.getBoolean(key, false)
    }

    fun getData(key: String): String? {
        return sharedPreferences?.getString(key, "")
    }

    fun getCustomBloackValue(key: String): String? {
        return sharedPreferences?.getString(key, "25")
    }

    fun getBloackTypeName(key: String): String? {
        return sharedPreferences?.getString(key, EXTRA_OZI)
    }

    fun getSpanType(key: String): String? {
        return sharedPreferences?.getString(key, EXTRA_MM)
    }

    fun getWheelToWheelData(key: String): String? {
        return sharedPreferences?.getString(key, "2100")
    }

    fun getWheelToJockeyWheelData(key: String): String? {
        return sharedPreferences?.getString(key, "4500")
    }

    fun getDeviceAddressTemp(key: String): String? {
        return sharedPreferences?.getString(key, "")
    }

    fun getIntData(key: String): Int? {
        return sharedPreferences?.getInt(key, 1)
    }

    fun clearAllValue() {
        setLogin(com.savvytech.savvylevelfour.common.Preference.Companion.IS_LOGIN, false)
        editor?.apply()
        editor?.clear()
    }

    fun getLongData(key: String): Long {
        return sharedPreferences?.getLong(key, 0)!!
    }

    fun setLongData(key: String, data: Long) {
        editor?.putLong(key, data)
        editor?.apply()
    }
}