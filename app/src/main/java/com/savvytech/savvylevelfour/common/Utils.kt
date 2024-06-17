package com.savvytech.savvylevelfour.common

import android.app.ActionBar
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.savvytech.savvylevelfour.R
import com.savvytech.savvylevelfour.ui.interfaces.SelectionInterfaces
import com.savvytech.savvylevelfour.ui.interfaces.SendStringInterfaces
import com.savvytech.savvylevelfour.viewmodels.CommonViewModel

object Utils {

    var dialog: Dialog? = null

    fun showImageInStatusBar(context: Context) {
        val window = (context as AppCompatActivity).window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//        window.statusBarColor = this.resources.getColor(R.color.colorPrimaryDark)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)//  set status text dark
            window.setStatusBarColor(
                ContextCompat.getColor(
                    context,
                    R.color.white
                )
            )// set status background white
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    fun showDialog(
        commonViewModel: CommonViewModel,
        activity: FragmentActivity,
        context: Context,
        layout: Int,
        type: Int,
        selectionInterfaces: SelectionInterfaces
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(layout)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)

        val preference = Preference(context)

        when (type) {
            0 -> {
                dialog.findViewById<View>(R.id.btnDpsOk).setOnClickListener {
                    selectionInterfaces.clickOnView()
                    dialog.dismiss()
//                    commonViewModel.isDialogShow = false
                    preference.setIsShowDialogData(Preference.isDialogShow, false)
                }
            }
            1 -> {
                dialog.findViewById<View>(R.id.btnWTJOk).setOnClickListener {
                    selectionInterfaces.clickOnView()
                    dialog.dismiss()
//                    commonViewModel.isDialogShow = false
                    preference.setIsShowDialogData(Preference.isDialogShow, false)
                }
            }
            2 -> {
                dialog.findViewById<View>(R.id.btnLROk).setOnClickListener {
                    selectionInterfaces.clickOnView()
                    dialog.dismiss()
//                    commonViewModel.isDialogShow = false
                    preference.setIsShowDialogData(Preference.isDialogShow, false)
                }
            }
            3 -> {

                val textView: AppCompatTextView = dialog.findViewById(R.id.tvErrorCode)

                commonViewModel.errorCode.observe(activity, Observer {
                    textView.text = it
                    dialog.findViewById<View>(R.id.pbLoaderSettings).visibility = View.GONE
                    dialog.findViewById<View>(R.id.relRequestErrorCode).background = ContextCompat.getDrawable(context, R.drawable.request_error_code_bg)
//                    textView.setTextColor(ContextCompat.getColor(context, R.color.black))
                    textView.setTextColor(ContextCompat.getColor(context, R.color.gray_img_color))
                })

                dialog.findViewById<View>(R.id.tvRequestErrorCode).setOnClickListener {
                    dialog.findViewById<View>(R.id.pbLoaderSettings).visibility = View.GONE
                    dialog.findViewById<View>(R.id.relRequestErrorCode).background = ContextCompat.getDrawable(context, R.drawable.request_error_code_bg_disabled)
                    textView.setTextColor(ContextCompat.getColor(context, R.color.gray_img_color))
                    selectionInterfaces.clickOnView()

                    Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                        override fun run() {
                            if (textView.text.toString().equals("")) {
//                                textView.setTextColor(ContextCompat.getColor(context, R.color.black))
                                textView.setTextColor(ContextCompat.getColor(context, R.color.gray_img_color))
                                dialog.findViewById<View>(R.id.relRequestErrorCode).background = ContextCompat.getDrawable(context, R.drawable.request_error_code_bg)
                            }
                        }
                    }, 4000)
                }

                dialog.findViewById<View>(R.id.btnECCancelCode).setOnClickListener {
                    if (textView.text.trim().toString() == "" || textView.text.trim().toString() == "0x00") {
                        Toast.makeText(context, "There is no code available for clear!", Toast.LENGTH_SHORT).show()
                    } else {
                        dialog.findViewById<View>(R.id.linMainErrorCodePopup).visibility = View.GONE
                        dialog.findViewById<View>(R.id.linMainClearErrorCode).visibility = View.VISIBLE
                    }
                }

                dialog.findViewById<View>(R.id.txtECCancel).setOnClickListener {
                    dialog.dismiss()
//                    commonViewModel.isDialogShow = false
                    preference.setIsShowDialogData(Preference.isDialogShow, false)
                }

                dialog.findViewById<View>(R.id.btnNoClearCode).setOnClickListener {
                    dialog.findViewById<View>(R.id.linMainClearErrorCode).visibility = View.GONE
                    dialog.findViewById<View>(R.id.linMainErrorCodePopup).visibility = View.VISIBLE
                }

                dialog.findViewById<View>(R.id.btnYesClearCode).setOnClickListener {
                    selectionInterfaces.clickOnClearErrorCode()
                    dialog.dismiss()
//                    commonViewModel.isDialogShow = false
                    preference.setIsShowDialogData(Preference.isDialogShow, false)
                }
            }
            4 -> {
                dialog.findViewById<View>(R.id.btnMUok).setOnClickListener {
                    selectionInterfaces.clickOnView()
                    dialog.dismiss()
//                    commonViewModel.isDialogShow = false
                    preference.setIsShowDialogData(Preference.isDialogShow, false)
                }
            }
            5 -> {
                dialog.findViewById<View>(R.id.btnZCOk).setOnClickListener {
                    selectionInterfaces.clickOnView()
                    dialog.dismiss()
//                    commonViewModel.isDialogShow = false
                    preference.setIsShowDialogData(Preference.isDialogShow, false)
                }
                dialog.findViewById<View>(R.id.txtZcCancel).setOnClickListener {
//                    selectionInterfaces.clickOnView()
                    dialog.dismiss()
//                    commonViewModel.isDialogShow = false
                    preference.setIsShowDialogData(Preference.isDialogShow, false)
                }
            }
            6 -> {
                dialog.findViewById<View>(R.id.btnCHOk).setOnClickListener {
                    selectionInterfaces.clickOnView()
                    dialog.dismiss()
//                    commonViewModel.isDialogShow = false
                    preference.setIsShowDialogData(Preference.isDialogShow, false)
                }
                dialog.findViewById<View>(R.id.txtCHCancel).setOnClickListener {
//                    selectionInterfaces.clickOnView()
                    dialog.dismiss()
//                    commonViewModel.isDialogShow = false
                    preference.setIsShowDialogData(Preference.isDialogShow, false)
                }
            }
            7 -> {
                dialog.findViewById<AppCompatTextView>(R.id.tvPrivacyPolicyDialog).text = HtmlCompat.fromHtml(
                    Common.readRawTextFile(context, R.raw.privacy_policy),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )

                dialog.findViewById<View>(R.id.btnDpsOk).setOnClickListener {
                    selectionInterfaces.clickOnView()
                    dialog.dismiss()
//                    commonViewModel.isDialogShow = false
                    preference.setIsShowDialogData(Preference.isDialogShow, false)
                }
            }
            8 -> {
                dialog.findViewById<AppCompatTextView>(R.id.tvCreditsDialog).text = HtmlCompat.fromHtml(
                    Common.readRawTextFile(
                        context,
                        R.raw.credits
                    ), HtmlCompat.FROM_HTML_MODE_LEGACY
                )

                dialog.findViewById<View>(R.id.btnDpsOk).setOnClickListener {
                    selectionInterfaces.clickOnView()
                    dialog.dismiss()
//                    commonViewModel.isDialogShow = false
                    preference.setIsShowDialogData(Preference.isDialogShow, false)
                }
            }
        }

        val width = (context.resources.displayMetrics.widthPixels * 0.90).toInt()
//        val height = (context.resources.displayMetrics.heightPixels * 0.80).toInt()
        val height = ViewGroup.LayoutParams.WRAP_CONTENT

        dialog.window?.decorView?.setBackgroundResource(android.R.color.transparent)
        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

        dialog.show()
//        commonViewModel.isDialogShow = true
        preference.setIsShowDialogData(Preference.isDialogShow, true)
        this.dialog = dialog
    }

    fun showInstructionsDialog(
        context: Context,
        layout: Int,
        commonViewModel: CommonViewModel,
        selectionInterfaces: SelectionInterfaces
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(layout)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)

        val preference = Preference(context)

        dialog.findViewById<AppCompatTextView>(R.id.tvInstructionsDialog).text = HtmlCompat.fromHtml(
            Common.readRawTextFile(context, R.raw.blank_2),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        dialog.findViewById<View>(R.id.btnOkInstructionsDialog).setOnClickListener {
            selectionInterfaces.clickOnView()
            dialog.dismiss()
//            commonViewModel.isDialogShow = false
            preference.setIsShowDialogData(Preference.isDialogShow, false)
        }

        val width = (context.resources.displayMetrics.widthPixels * 0.70).toInt()
//        val height = (context.resources.displayMetrics.heightPixels * 0.80).toInt()
        val height = ViewGroup.LayoutParams.WRAP_CONTENT

        dialog.window?.decorView?.setBackgroundResource(android.R.color.transparent)
        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

        dialog.show()
//        commonViewModel.isDialogShow = true
        preference.setIsShowDialogData(Preference.isDialogShow, true)
        this.dialog = dialog
    }

    fun showAlertPermision(context: Context, dialog: Dialog) {
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setContentView(R.layout.custome_alert_dialog)

        val txtCADSetting: TextView = dialog.findViewById(R.id.txtCADSetting)
        txtCADSetting.setOnClickListener {
            val intent = Intent()
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", context.packageName, null)
            intent.setData(uri)
            context.startActivity(intent)
            dialog.dismiss()
        }

        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.90).toInt(),
            ActionBar.LayoutParams.WRAP_CONTENT
        )
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    fun showLocationAlertPermision(context: Context, dialog: Dialog) {
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setContentView(R.layout.custome_alert_dialog)

        val txtCADSetting: TextView = dialog.findViewById(R.id.txtCADSetting)
        val txtMsg: TextView = dialog.findViewById(R.id.txtview_customalert_msg)
        txtMsg.setText(context.getString(R.string.msg_permission_location))

        val txtTitle: TextView = dialog.findViewById(R.id.txtview_customalert_title)
        txtTitle.setText(context.getString(R.string.needLocationPermission))

        txtCADSetting.setOnClickListener {
            val intent = Intent()
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", context.packageName, null)
            intent.setData(uri)
            context.startActivity(intent)
            dialog.dismiss()
        }

        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.90).toInt(),
            ActionBar.LayoutParams.WRAP_CONTENT
        )
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    fun showBlutoothAlertPermision(context: Context, dialog: Dialog) {
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setContentView(R.layout.custome_alert_dialog)

        val txtCADSetting: TextView = dialog.findViewById(R.id.txtCADSetting)
        val txtMsg: TextView = dialog.findViewById(R.id.txtview_customalert_msg)
        txtMsg.setText(context.getString(R.string.msg_permission_blutooth))

        val txtTitle: TextView = dialog.findViewById(R.id.txtview_customalert_title)
        txtTitle.setText(context.getString(R.string.needBlePermission))

        txtCADSetting.setOnClickListener {
            val intent = Intent()
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", context.packageName, null)
            intent.setData(uri)
            context.startActivity(intent)
            dialog.dismiss()
        }

        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.90).toInt(),
            ActionBar.LayoutParams.WRAP_CONTENT
        )
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    fun customeDialog(
        dialog: Dialog,
        context: Context,
        title: String,
        message: String,
        buttonText: String,
        commonViewModel: CommonViewModel,
        selectionInterfaces: SelectionInterfaces
    ) {
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setContentView(R.layout.dialog_custome_alert)

        val preference = Preference(context)

//        dialog.findViewById<TextView>(R.id.txtDCATitle).setText(title)

        dialog.findViewById<TextView>(R.id.txtDCATitle).setText(title)
        dialog.findViewById<TextView>(R.id.txtDCAMessage).setText(message)
        dialog.findViewById<Button>(R.id.btnDCAAllow).setText(buttonText)

        dialog.findViewById<TextView>(R.id.btnDCAAllow).setOnClickListener {
            selectionInterfaces.clickOnView()
            dialog.dismiss()
//            commonViewModel.isDialogShow = false
            preference.setIsShowDialogData(Preference.isDialogShow, false)
        }

        dialog.window?.setLayout((context.resources.displayMetrics.widthPixels * 0.90).toInt(), ActionBar.LayoutParams.WRAP_CONTENT)
        if (!dialog.isShowing) {
            dialog.show()
//            commonViewModel.isDialogShow = true
            preference.setIsShowDialogData(Preference.isDialogShow, true)
        }

        this.dialog = dialog
    }

    fun setStatusBarTextColor(context: Context) {
        (context as AppCompatActivity).window.getDecorView()
            .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inc())
    }

    fun showAlertDialog(
        context: Context,
        title: String,
        message: String,
        sendStringInterfaces: SendStringInterfaces
    ) {
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setPositiveButton(context.getString(R.string.ok),
            object : DialogInterface.OnClickListener {
                override fun onClick(p0: DialogInterface?, p1: Int) {
                    sendStringInterfaces.sendString("Done", "")
                }
            })
        alertDialog.setNegativeButton(context.getString(R.string.cancel),
            object : DialogInterface.OnClickListener {
                override fun onClick(p0: DialogInterface?, p1: Int) {
                    sendStringInterfaces.sendString("Cancel", "")
                }
            })
        alertDialog.show()
    }
}