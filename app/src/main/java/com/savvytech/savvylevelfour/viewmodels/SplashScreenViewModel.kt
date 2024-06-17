package com.savvytech.savvylevelfour.viewmodels

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Spanned
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.savvytech.savvylevelfour.R
import com.savvytech.savvylevelfour.common.EXTRA_KOP
import com.savvytech.savvylevelfour.common.EXTRA_SCREEN_NUMBER
import com.savvytech.savvylevelfour.common.Utils
import com.savvytech.savvylevelfour.ui.activity.SavvyMainActivity
import com.savvytech.savvylevelfour.ui.adapters.NearbyDevicesAdapter
import com.savvytech.savvylevelfour.ui.interfaces.SelectionInterfaces

class SplashScreenViewModel(application: Application) : AndroidViewModel(application) {
    val _isFinish = MutableLiveData(false)
    val isSearchAgainClick = MutableLiveData(true)
    var isFinishedTime: LiveData<Boolean> = _isFinish

    var connectOrNot = MutableLiveData(false)
    var onClickOfSearchAgain: LiveData<Boolean> = isSearchAgainClick

    var mutableView: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
    var onCancelClick: MutableLiveData<String> = MutableLiveData<String>("")
    var tvQuestionsVisible: MutableLiveData<Int> = MutableLiveData(View.GONE)
    var isClickedOnSearchAgain = false
    var nearbyDevicesAdapter: NearbyDevicesAdapter = NearbyDevicesAdapter()
    var number = ""
    lateinit var prefernce: com.savvytech.savvylevelfour.common.Preference
    var getSelectedMsg = ""
    var spannableString: MutableLiveData<Spanned> = MutableLiveData()
    var batt: MutableLiveData<Float> = MutableLiveData()
    lateinit var commonViewModel: CommonViewModel

    fun init(context: Context, commonViewModel: CommonViewModel) {
        this.commonViewModel = commonViewModel
        prefernce = com.savvytech.savvylevelfour.common.Preference(context)
        spannableString.value = HtmlCompat.fromHtml(com.savvytech.savvylevelfour.common.Common.readRawTextFile(context, R.raw.blank_2), HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    fun splashDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            _isFinish.value = true
        }, 3000)
    }

    fun getView(): MutableLiveData<Boolean> {
        if (mutableView == null) {
            mutableView = MutableLiveData<Boolean>()
        }
        return mutableView
    }

    fun onClickSearchAgain(isSearch: Boolean) {
        isClickedOnSearchAgain = true
        mutableView.value = isSearch
    }

    fun seeListOfDevice(): NearbyDevicesAdapter {
        return nearbyDevicesAdapter
    }

    fun onClickOfConnect(v: View) {
        connectOrNot.value = true
        prefernce.setData(com.savvytech.savvylevelfour.common.Preference.DEVICE_NAME, getSelectedMsg)
    }

    fun clickOnCancel(v: View) {
        onCancelClick.value = EXTRA_KOP
    }

    fun showInstructionsDialog(v: View) {
        Utils.showInstructionsDialog(
            v.context,
            R.layout.dialog_instructions_for_connect,
            commonViewModel,
            object : SelectionInterfaces {
                override fun clickOnView() {

                }

                override fun clickOnClearErrorCode() {

                }
            })
    }

    fun clickOnPrivacyRead(v: View) {
        Utils.showDialog(CommonViewModel(), FragmentActivity(), v.context, R.layout.dailog_privacy_statement, 7, object : SelectionInterfaces {
            override fun clickOnView() {

            }

            override fun clickOnClearErrorCode() {

            }
        })
    }

    fun clickOnEnduserAgreement(v: View) {
        Utils.showDialog(CommonViewModel(), FragmentActivity(), v.context, R.layout.dailog_end_user_agreement, 0, object : SelectionInterfaces {
            override fun clickOnView() {

            }

            override fun clickOnClearErrorCode() {

            }
        })
    }
}