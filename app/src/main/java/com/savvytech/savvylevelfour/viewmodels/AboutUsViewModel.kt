package com.savvytech.savvylevelfour.viewmodels

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.savvytech.savvylevelfour.BuildConfig
import com.savvytech.savvylevelfour.R
import com.savvytech.savvylevelfour.common.EXTRA_HTTPS
import com.savvytech.savvylevelfour.common.EXTRA_MAIL_TO
import com.savvytech.savvylevelfour.common.EXTRA_SEND_FEEDBACK
import com.savvytech.savvylevelfour.common.Utils
import com.savvytech.savvylevelfour.ui.interfaces.SelectionInterfaces

class AboutUsViewModel(application: Application) : AndroidViewModel(application) {

    fun setVersionCode(tvAppVersion: AppCompatTextView) {
        tvAppVersion.text = BuildConfig.VERSION_NAME
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

    fun clickOnCredits(v: View) {
        Utils.showDialog(CommonViewModel(), FragmentActivity(), v.context, R.layout.dailog_credits, 8, object : SelectionInterfaces {
            override fun clickOnView() {

            }

            override fun clickOnClearErrorCode() {

            }
        })
    }

    fun openWebSite(v: View) {
        val url = EXTRA_HTTPS + (v as TextView).text.toString()
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        v.context.startActivity(browserIntent)
    }

    fun openEmail(v: View) {
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            data = Uri.parse(EXTRA_MAIL_TO + (v as TextView).text.toString())
        }
        v.context.startActivity(Intent.createChooser(emailIntent, EXTRA_SEND_FEEDBACK))
    }
}