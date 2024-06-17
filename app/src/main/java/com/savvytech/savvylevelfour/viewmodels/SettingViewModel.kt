package com.savvytech.savvylevelfour.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.savvytech.savvylevelfour.R
import com.savvytech.savvylevelfour.common.*
import com.savvytech.savvylevelfour.ui.activity.SavvyMainActivity
import com.savvytech.savvylevelfour.ui.interfaces.SelectionInterfaces
import java.text.DecimalFormat
import java.util.*

class SettingViewModel(application: Application) : AndroidViewModel(application) {

    var manageRadioButton: MutableLiveData<Int> = MutableLiveData()
    var linCustomBlockType: MutableLiveData<Boolean> = MutableLiveData()
    var editTextColor: MutableLiveData<Int> = MutableLiveData()

    var tvUnitInch: MutableLiveData<Boolean> = MutableLiveData()
    var tvUnitMM: MutableLiveData<Boolean> = MutableLiveData()

    var manageScreenOptions: MutableLiveData<Int> = MutableLiveData()
    var blockTypeText: MutableLiveData<String> = MutableLiveData()

    var listOfBlockType: MutableLiveData<Int> = MutableLiveData()
    var manageBatteryView: MutableLiveData<Int> = MutableLiveData()
    var tiltHeightTop: MutableLiveData<Int> = MutableLiveData()
    var tiltHeightBottom: MutableLiveData<Int> = MutableLiveData()
    var zeroCallobation: MutableLiveData<Boolean> = MutableLiveData(false)
    var selectedDeviceName: MutableLiveData<String> = MutableLiveData()
    var errorOptions: MutableLiveData<String> = MutableLiveData()
    var changeDevice: MutableLiveData<String> = MutableLiveData("")
    var customLabelText: MutableLiveData<String> = MutableLiveData("Custom (use mm only)")
    lateinit var commonViewModel: CommonViewModel
    lateinit var activity: FragmentActivity
    lateinit var preference: Preference

    var blockTypeFlag = false
    var tillHeightFlag = false
    var batteryFlag = true
    var isCalibrationHintShown = false
    var isCalibrationHintClicked = false
    var isTiltHintShown = false
    var isTiltHintClicked = false
    var isBatteryHintShown = false
    var isBatteryHintClicked = false

    val portrait = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    //    val landscapeLeft = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//    val landscapeRight = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
    val landscapeLeft = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
    val landscapeRight = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

    lateinit var etWheelToWheelSpan: AppCompatEditText
    lateinit var etWheelToJockeyWheelSpan: AppCompatEditText
    lateinit var etCustomBlockValue: AppCompatEditText

    fun showFirstTimeView(context: Context, commonViewModel: CommonViewModel) {
        preference = Preference(context)
        this.commonViewModel = commonViewModel

        linCustomBlockType.value = false
        editTextColor.value = ContextCompat.getColor(context, R.color.gray_797)

        selectedDeviceName.value = preference.getData(Preference.DEVICE_NAME)

        manageScreenOptions.value = preference.getIntData(Preference.SCREEN_ORIENTATION)

        if (preference.getSpanType(Preference.spanType).equals(EXTRA_INCH)) {
            customLabelText.value = "Custom (use inches only)"
        }
    }

    fun clickOnWheel(v: View) {
        Utils.showDialog(
            commonViewModel,
            activity,
            v.context,
            R.layout.dailog_wheel_to_jockey,
            1,
            object : SelectionInterfaces {
                override fun clickOnView() {

                }

                override fun clickOnClearErrorCode() {

                }
            })
    }

    fun clickOnLeftRightWheet(v: View) {
        Utils.showDialog(
            commonViewModel,
            activity,
            v.context,
            R.layout.dailog_left_to_right_wheel,
            2,
            object : SelectionInterfaces {
                override fun clickOnView() {

                }

                override fun clickOnClearErrorCode() {

                }
            })
    }

    fun clickOnError(v: View) {
        Utils.showDialog(
            commonViewModel,
            activity,
            v.context,
            R.layout.dialog_error_code,
            3,
            object : SelectionInterfaces {
                override fun clickOnView() {
                    errorOptions.value = EXTRA_REQUEST
                }

                override fun clickOnClearErrorCode() {
                    errorOptions.value = "Clear"
                }
            })
    }

    fun onClickShowTutorial(v: View) {
        commonViewModel.showTutorial.value = EXTRA_SETTINGS

        if (!preference.getBattData(Preference.batteryStatus)) {
            commonViewModel.isBattHideBeforeTutorial = true
            preference.setBattData(Preference.batteryStatus, true)
        }
    }

    fun changeDevice(v: View) {
        changeDevice.value = preference.getData(Preference.DEVICE_ADDRESS)
    }

    fun clickOnMeassure(v: View) {
        Utils.showDialog(
            commonViewModel,
            activity,
            v.context,
            R.layout.dailog_measurements_units,
            4,
            object : SelectionInterfaces {
                override fun clickOnView() {

                }

                override fun clickOnClearErrorCode() {

                }
            })
    }

    fun clickOnZeroScale(v: View) {

        isCalibrationHintClicked = true

        if (!isCalibrationHintShown) {
            Utils.showDialog(
                commonViewModel,
                activity,
                v.context,
                R.layout.dailog_zero_scale,
                5,
                object : SelectionInterfaces {
                    override fun clickOnView() {
                        commonViewModel.zeroCalibCounter = 0
                        zeroCallobation.value = true
                        Log.e("ZERO_CALI_DEBUG", "ON_CLICK_VIEWMODEL")
                    }

                    override fun clickOnClearErrorCode() {

                    }
                })
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun clickOnInch() {
        if (!preference.getSpanType(Preference.spanType).equals(EXTRA_INCH)) {
            tvUnitInch.value = true
            tvUnitMM.value = false
//        preference.setIntData(Preference.spanType, 1)
            preference.setSpanType(Preference.spanType, EXTRA_INCH)
//            etWheelToWheelSpan.inputType = InputType.TYPE_CLASS_NUMBER
//            etWheelToWheelSpan.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, true)
//
//            etWheelToJockeyWheelSpan.inputType = InputType.TYPE_CLASS_NUMBER
//            etWheelToJockeyWheelSpan.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, true)
//
//            etCustomBlockValue.inputType = InputType.TYPE_CLASS_NUMBER
//            etCustomBlockValue.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, true)

            setInputType(etWheelToWheelSpan, false, true)
            setInputType(etWheelToJockeyWheelSpan, false, true)
            setInputType(etCustomBlockValue, false, true)

            customLabelText.value = "Custom (use inches only)"

            val calculatedWheelToWheelInch = preference.getWheelToWheelData(Preference.wheelToWheel)?.toDouble()?.div(25.4)
            val calculatedWheelToJockeyWheelInch = preference.getWheelToJockeyWheelData(Preference.wheelToJockeyWheel)?.toDouble()?.div(25.4)

            val df = DecimalFormat("#.#")
            etWheelToWheelSpan.setText(df.format(calculatedWheelToWheelInch).toString())
            etWheelToJockeyWheelSpan.setText(df.format(calculatedWheelToJockeyWheelInch).toString())

            preference.setWheelToWheelData(Preference.wheelToWheel, etWheelToWheelSpan.text.toString())
            preference.setWheelToJockeyWheelData(Preference.wheelToJockeyWheel, etWheelToJockeyWheelSpan.text.toString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun clickOnMM() {
        if (!preference.getSpanType(Preference.spanType).equals(EXTRA_MM)) {
            tvUnitInch.value = false
            tvUnitMM.value = true
//        preference.setIntData(Preference.spanType, 0)
            preference.setSpanType(Preference.spanType, EXTRA_MM)
//            etWheelToWheelSpan.inputType = InputType.TYPE_CLASS_NUMBER
//            etWheelToWheelSpan.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, false)
//
//            etWheelToJockeyWheelSpan.inputType = InputType.TYPE_CLASS_NUMBER
//            etWheelToJockeyWheelSpan.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, false)
//
//            etCustomBlockValue.inputType = InputType.TYPE_CLASS_NUMBER
//            etCustomBlockValue.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, false)

            setInputType(etWheelToWheelSpan, false, false)
            setInputType(etWheelToJockeyWheelSpan, false, false)
            setInputType(etCustomBlockValue, false, false)

            customLabelText.value = "Custom (use mm only)"

            val calculatedWheelToWheelInch = preference.getWheelToWheelData(Preference.wheelToWheel)?.toDouble()?.times(25.4)
            val calculatedWheelToJockeyWheelInch = preference.getWheelToJockeyWheelData(Preference.wheelToJockeyWheel)?.toDouble()?.times(25.4)

            val df = DecimalFormat("#.#")
            etWheelToWheelSpan.setText(df.format(calculatedWheelToWheelInch).toString())
            etWheelToJockeyWheelSpan.setText(df.format(calculatedWheelToJockeyWheelInch).toString())

            preference.setWheelToWheelData(Preference.wheelToWheel, etWheelToWheelSpan.text.toString())
            preference.setWheelToJockeyWheelData(Preference.wheelToJockeyWheel, etWheelToJockeyWheelSpan.text.toString())
        }
    }

    private fun setInputType(edittext: AppCompatEditText, sign: Boolean, decimal: Boolean) {
        edittext.inputType = InputType.TYPE_CLASS_NUMBER
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            edittext.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, sign, decimal)
        else
            edittext.keyListener = DigitsKeyListener.getInstance(sign, decimal)
    }

    fun onCheckedChangeListener(v: View, value: Int) {
        manageRadioButton.value = value
        linCustomBlockType.value = value == 2
        editTextColor.value = if (value == 2) ContextCompat.getColor(
            v.context,
            R.color.black
        ) else ContextCompat.getColor(v.context, R.color.gray_797)

        if (value == 0) {
            preference.setBlockTypeName(Preference.blockTypeName, EXTRA_OZI)
        } else if (value == 1) {
            preference.setBlockTypeName(Preference.blockTypeName, EXTRA_LYNX)
        } else if (value == 2) {
            preference.setBlockTypeName(Preference.blockTypeName, EXTRA_CUSTOM)
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    fun onChangeOrientation(v: View, orientation: Int) {
        preference.setIntData(Preference.SCREEN_ORIENTATION, orientation)
        manageScreenOptions.value = orientation
        (v.context as SavvyMainActivity).requestedOrientation = orientation
        commonViewModel.onChangeOrientation.value = true
    }

    fun clickOnBlockType(v: View) {
        val motion: MotionLayout = (v as MotionLayout)
        if (blockTypeFlag) {
            startBlockTypeAnim(motion, R.id.startSwitchAnim, R.id.endSwitchAnim)
            blockTypeFlag = false

            listOfBlockType.value = View.GONE
            blockTypeText.value = motion.context.getString(R.string.disable)
        } else {
            startBlockTypeAnim(
                motion, R.id.endSwitchAnim, R.id.startSwitchAnim
            )
            blockTypeFlag = true

            listOfBlockType.value = View.VISIBLE
            blockTypeText.value = motion.context.getString(R.string.enable)
        }

        preference.setBoolenData(Preference.blockType, blockTypeFlag)
    }

    fun clickOnBatteryButton(v: View) {
        if (!isBatteryHintShown) {
            val motion: MotionLayout = (v as MotionLayout)

            if (batteryFlag) {
                motion.setTransition(R.id.startSwitchAnim, R.id.endSwitchAnim)
                manageBatteryView.value = View.GONE
                batteryFlag = false
            } else {
                motion.setTransition(R.id.endSwitchAnim, R.id.startSwitchAnim)
                manageBatteryView.value = View.VISIBLE
                batteryFlag = true
                commonViewModel.isBattHideBeforeTutorial = false
            }
            preference.setBattData(Preference.batteryStatus, batteryFlag)

            motion.setTransitionDuration(150)
            motion.transitionToEnd()
        }
    }

    fun clickOnTillHeight(v: View) {
        if (!isTiltHintShown) {
            val motion: MotionLayout = v as MotionLayout

            if (tillHeightFlag) {
                motion.setTransition(R.id.startSwitchAnim, R.id.endSwitchAnim)

                tiltHeightTop.value = View.GONE
                tiltHeightBottom.value = View.GONE
                tillHeightFlag = false
            } else {
                motion.setTransition(R.id.endSwitchAnim, R.id.startSwitchAnim)
                tiltHeightTop.value = View.VISIBLE
                tiltHeightBottom.value = View.VISIBLE
                tillHeightFlag = true
            }

            preference.setBoolenData(Preference.tiltHeight, tillHeightFlag)

            motion.setTransitionDuration(150)
            motion.transitionToEnd()
        }
    }

    fun onResumeInFragment(
        inBlockSwitch: MotionLayout, lnrBlockTypeOption: LinearLayout, inSwitchBattery: MotionLayout, inSwitchTiltHeight: MotionLayout,
        etWheelToWheelSpan: AppCompatEditText, etWheelToJockeyWheelSpan: AppCompatEditText, etCustomBlockValue: AppCompatEditText,
        tvDeviceNameItemConnectDevice: AppCompatTextView, commonViewModel: CommonViewModel, activity: FragmentActivity
    ) {
        this.commonViewModel = commonViewModel
        this.activity = activity
        this.etWheelToWheelSpan = etWheelToWheelSpan
        this.etWheelToJockeyWheelSpan = etWheelToJockeyWheelSpan
        this.etCustomBlockValue = etCustomBlockValue
        blockTypeFlag = preference.getBooleanData(Preference.blockType)
        batteryFlag = preference.getBattData(Preference.batteryStatus)
        tillHeightFlag = preference.getBooleanData(Preference.tiltHeight)

        preference = Preference(activity)
        tvDeviceNameItemConnectDevice.text = preference.getData(Preference.DEVICE_NAME)

        if (!blockTypeFlag) {
            /*startBlockTypeAnim(
                inBlockSwitch, R.id.startSwitchAnim, R.id.endSwitchAnim
            )*/

            listOfBlockType.value = View.GONE
            blockTypeText.value = inBlockSwitch.context.getString(R.string.disable)
            manageBatteryView.value = View.GONE
        } else {
            startBlockTypeAnim(inBlockSwitch, R.id.endSwitchAnim, R.id.startSwitchAnim)

            listOfBlockType.value = View.VISIBLE
            blockTypeText.value = inBlockSwitch.context.getString(R.string.enable)
            manageBatteryView.value = View.VISIBLE
        }

        if (tillHeightFlag)
            startBlockTypeAnim(inSwitchTiltHeight, R.id.endSwitchAnim, R.id.startSwitchAnim)

        if (batteryFlag)
            startBatteryAnim(inSwitchBattery, R.id.endSwitchAnim, R.id.startSwitchAnim, View.VISIBLE)

        etWheelToWheelSpan.setText(preference.getWheelToWheelData(Preference.wheelToWheel))
        etWheelToJockeyWheelSpan.setText(preference.getWheelToJockeyWheelData(Preference.wheelToJockeyWheel))
        etCustomBlockValue.setText(preference.getCustomBloackValue(Preference.customBlockTypeValue))

        if (preference.getSpanType(Preference.spanType).equals(EXTRA_INCH)) {
            tvUnitInch.value = true
            tvUnitMM.value = false
        } else {
            tvUnitInch.value = false
            tvUnitMM.value = true
        }
    }

    private fun startBatteryAnim(batteryMotion: MotionLayout, start: Int, end: Int, hideOrShow: Int) {
        batteryMotion.setTransition(start, end)
        manageBatteryView.value = hideOrShow
        batteryMotion.setTransitionDuration(150)
        batteryMotion.transitionToEnd()
    }

    private fun startBlockTypeAnim(motion: MotionLayout, startAnim: Int, endAnim: Int) {
        motion.setTransition(startAnim, endAnim)
        motion.setTransitionDuration(150)
        motion.transitionToEnd()
    }
}