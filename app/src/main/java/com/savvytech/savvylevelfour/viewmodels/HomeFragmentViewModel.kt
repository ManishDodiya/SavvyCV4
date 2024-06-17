package com.savvytech.savvylevelfour.viewmodels

import android.app.Application
import android.content.Context
import android.text.Spanned
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.savvytech.savvylevelfour.R
import com.savvytech.savvylevelfour.common.Common
import com.savvytech.savvylevelfour.common.Preference
import com.savvytech.savvylevelfour.common.TiltGauge
import com.savvytech.savvylevelfour.common.Utils
import com.savvytech.savvylevelfour.ui.interfaces.SelectionInterfaces

class HomeFragmentViewModel(application: Application) : AndroidViewModel(application) {

    var relSetHitch: MutableLiveData<Int> = MutableLiveData()
    var relFHLock: MutableLiveData<Int> = MutableLiveData()
    var enableHitch: MutableLiveData<Boolean> = MutableLiveData()
    var txtFHBatteryStatus: MutableLiveData<Int> = MutableLiveData()
    var tvTiltHeightTop: MutableLiveData<Int> = MutableLiveData()
    var tvTiltHeightBottom: MutableLiveData<Int> = MutableLiveData()
    var txtFHBlockTitle: MutableLiveData<Int> = MutableLiveData()
    var viewRoundTop: MutableLiveData<Char> = MutableLiveData()
    var isZoom: MutableLiveData<Boolean> = MutableLiveData()
    protected var zoom = false

    var batt: MutableLiveData<Float> = MutableLiveData()

    var spannableString: MutableLiveData<Spanned> = MutableLiveData()

    lateinit var preference: Preference

    init {
        relSetHitch.value = View.VISIBLE
        relFHLock.value = View.GONE
    }

    fun clickOnSetHitch(v: View) {
//        Log.e("TAG", v.tag.toString())
        Toast.makeText(v.context, v.context.getString(R.string.please_press_seconds), Toast.LENGTH_LONG).show()
    }

    fun longClickOnSetHitch(): Boolean {
        relSetHitch.value = View.GONE
        relFHLock.value = View.VISIBLE
        enableHitch.value = true
        preference.setBoolenData(Preference.hitchEnabled, true)
        return true
    }

    fun clickOnClearHitch(v: View) {
        Toast.makeText(v.context, v.context.getString(R.string.please_press_seconds), Toast.LENGTH_LONG).show()
    }

    fun clickOnLongClearHitch(v: View): Boolean {
        Utils.showDialog(CommonViewModel(), FragmentActivity(), v.context, R.layout.dailog_clear_hitch, 6, object : SelectionInterfaces {
            override fun clickOnView() {
                relSetHitch.value = View.VISIBLE
                relFHLock.value = View.GONE
                enableHitch.value = false
                preference.setBoolenData(Preference.hitchEnabled, false)
                preference.setFloatData(Preference.hitchValue, 0f)
                Toast.makeText(v.context, R.string.hitch_value_cleared, Toast.LENGTH_LONG).show()
            }

            override fun clickOnClearErrorCode() {

            }
        })
        return true
    }

    fun clickOnZoom(imageView: AppCompatImageView) {
//         zoom = !zoom
         zoom = !preference.getBooleanData(Preference.isZoom)
        if (zoom) {
            viewRoundTop.value = TiltGauge.ZOOM
            imageView.setImageDrawable(ContextCompat.getDrawable(imageView.context, R.drawable.one_x))
        } else {
            viewRoundTop.value = TiltGauge.NORMAL
            imageView.setImageDrawable(ContextCompat.getDrawable(imageView.context, R.drawable.ten_x))
        }
        isZoom.value = zoom
    }

    fun setAllHideAndShowView(context: Context) {
        preference = Preference(context)

        txtFHBatteryStatus.value = if (preference.getBattData(Preference.batteryStatus)) View.VISIBLE else View.GONE
        tvTiltHeightTop.value = if (preference.getBooleanData(Preference.tiltHeight)) View.VISIBLE else View.GONE
        tvTiltHeightBottom.value = if (preference.getBooleanData(Preference.tiltHeight)) View.VISIBLE else View.GONE
        txtFHBlockTitle.value = if (preference.getBooleanData(Preference.blockType)) View.VISIBLE else View.GONE
        isZoom.value = preference.getBooleanData(Preference.isZoom)

        spannableString.value = HtmlCompat.fromHtml(Common.readRawTextFile(context, R.raw.blank_2), HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}