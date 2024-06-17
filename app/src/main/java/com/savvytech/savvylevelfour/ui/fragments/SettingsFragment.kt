package com.savvytech.savvylevelfour.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.*
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.joanfuentes.hintcase.HintCase
import com.joanfuentes.hintcaseassets.contentholderanimators.FadeInContentHolderAnimator
import com.joanfuentes.hintcaseassets.hintcontentholders.SimpleHintContentHolder
import com.joanfuentes.hintcaseassets.shapeanimators.RevealRectangularShapeAnimator
import com.joanfuentes.hintcaseassets.shapeanimators.UnrevealRectangularShapeAnimator
import com.savvytech.savvylevelfour.R
import com.savvytech.savvylevelfour.common.*
import com.savvytech.savvylevelfour.databinding.FragmentSettingsBinding
import com.savvytech.savvylevelfour.ui.fragments.HomeFragment.Companion.currentTutorial
import com.savvytech.savvylevelfour.ui.fragments.HomeFragment.Companion.screenNumber
import com.savvytech.savvylevelfour.viewmodels.CommonViewModel
import com.savvytech.savvylevelfour.viewmodels.SettingViewModel
import java.util.*

class SettingsFragment : Fragment() {

    lateinit var binding: FragmentSettingsBinding
    lateinit var settingVM: SettingViewModel
    lateinit var svSettings: ScrollView

    val commonViewModel: CommonViewModel by activityViewModels()
    lateinit var preference: Preference
    var mBluetoothLeService: BluetoothLeService? = null
    var deviceName = ""
    lateinit var linMainSettings: LinearLayout
    var isConnectedDevice = true

    var isCalibrationHintClosed = false
    var isTiltHintClosed = false
    var isBatteryHintClosed = false
    var isCalibrationHintShown = false
    var isTiltHintShown = false
    var isBatteryHintShown = false
    lateinit var parentView: View
    lateinit var calibrationHintCase: HintCase
    lateinit var tiltHintCase: HintCase
    lateinit var batteryHintCase: HintCase
    var isViewStateSaved = false

    private var mLastClickTime: Long = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        retainInstance = true

        preference = Preference(requireActivity())
        deviceName = preference.getData(Preference.DEVICE_NAME).toString()

        binding = DataBindingUtil.inflate(LayoutInflater.from(requireActivity()), R.layout.fragment_settings, container, false)

        commonViewModel.isHomeFragment = false

        findViewByID()
        init()
        setListener()
        observer()
        return binding.root
    }

    private fun findViewByID() {
        svSettings = binding.root.findViewById(R.id.svSettings)
        linMainSettings = binding.root.findViewById(R.id.linMainSettings)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun init() {
        settingVM = ViewModelProvider(this).get(SettingViewModel::class.java)
        settingVM.showFirstTimeView(requireActivity(), commonViewModel)

        binding.settingVM = settingVM
        binding.lifecycleOwner = this

//        binding.tvDeviceNameItemConnectDevice.text = preference.getData(Preference.DEVICE_NAME)

//        commonViewModel.batt.observe(viewLifecycleOwner, Observer {
//            val number2digits = String.format("%.1f", it.toDouble()).toDouble()
//            binding.txtFSBatteryStatus.text = "BATT: " + number2digits.toString() + " V"
//        })

        if (preference.getBloackTypeName(Preference.blockTypeName).equals(EXTRA_OZI)) {
            settingVM.manageRadioButton.value = 0
            settingVM.linCustomBlockType.value = false
            settingVM.editTextColor.value = ContextCompat.getColor(requireContext(), R.color.gray_797)
        } else if (preference.getBloackTypeName(Preference.blockTypeName).equals(EXTRA_LYNX)) {
            settingVM.manageRadioButton.value = 1
            settingVM.linCustomBlockType.value = false
            settingVM.editTextColor.value = ContextCompat.getColor(requireContext(), R.color.gray_797)
        } else if (preference.getBloackTypeName(Preference.blockTypeName).equals(EXTRA_CUSTOM)) {
            settingVM.manageRadioButton.value = 2
            settingVM.linCustomBlockType.value = true
            settingVM.editTextColor.value = ContextCompat.getColor(requireContext(), R.color.black)
        }

//        if (preference.getSpanType(Preference.spanType).equals(EXTRA_INCH)) {
//            binding.etWheelToWheelSpan.inputType = InputType.TYPE_CLASS_NUMBER
//            binding.etWheelToWheelSpan.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, true)
//
//            binding.etWheelToJockeyWheelSpan.inputType = InputType.TYPE_CLASS_NUMBER
//            binding.etWheelToJockeyWheelSpan.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, true)
//
//            binding.etBlockTypeCustom.inputType = InputType.TYPE_CLASS_NUMBER
//            binding.etBlockTypeCustom.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, true)
//        } else {
//            binding.etWheelToWheelSpan.inputType = InputType.TYPE_CLASS_NUMBER
//            binding.etWheelToWheelSpan.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, false)
//
//            binding.etWheelToJockeyWheelSpan.inputType = InputType.TYPE_CLASS_NUMBER
//            binding.etWheelToJockeyWheelSpan.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, false)
//
//            binding.etBlockTypeCustom.inputType = InputType.TYPE_CLASS_NUMBER
//            binding.etBlockTypeCustom.keyListener = DigitsKeyListener.getInstance(Locale.ENGLISH, false, false)
//        }

        if (preference.getSpanType(Preference.spanType).equals(EXTRA_INCH)) {
            setInputType(binding.etWheelToWheelSpan,false,true)
            setInputType(binding.etWheelToJockeyWheelSpan,false,true)
            setInputType(binding.etBlockTypeCustom,false,true)
        }else{
            setInputType(binding.etWheelToWheelSpan,false,false)
            setInputType(binding.etWheelToJockeyWheelSpan,false,false)
            setInputType(binding.etBlockTypeCustom,false,false)
        }

        binding.etWheelToWheelSpan.setOnFocusChangeListener(object : View.OnFocusChangeListener {
            override fun onFocusChange(p0: View?, p1: Boolean) {
                if (binding.etWheelToWheelSpan.text.toString().isEmpty() || binding.etWheelToWheelSpan.text.toString().equals("")) {
                    preference.setWheelToWheelData(Preference.wheelToWheel, "0")
                } else {
                    preference.setWheelToWheelData(Preference.wheelToWheel, binding.etWheelToWheelSpan.text.toString())
                }
            }
        })

        binding.etWheelToJockeyWheelSpan.setOnFocusChangeListener(object :
            View.OnFocusChangeListener {
            override fun onFocusChange(p0: View?, p1: Boolean) {
                if (binding.etWheelToJockeyWheelSpan.text.toString().isEmpty() || binding.etWheelToJockeyWheelSpan.text.toString().equals("")) {
                    preference.setWheelToJockeyWheelData(Preference.wheelToJockeyWheel, "0")
                } else {
                    preference.setWheelToJockeyWheelData(Preference.wheelToJockeyWheel, binding.etWheelToJockeyWheelSpan.text.toString())
                }
            }
        })

        binding.etBlockTypeCustom.setOnFocusChangeListener(object : View.OnFocusChangeListener {
            override fun onFocusChange(p0: View?, p1: Boolean) {
                if (preference.getBloackTypeName(Preference.blockTypeName).equals(EXTRA_CUSTOM)) {
                    if (binding.etBlockTypeCustom.text.toString().isEmpty() || binding.etBlockTypeCustom.text.toString().equals("")) {
                        preference.setCustomeBlockValue(Preference.customBlockTypeValue, "1")
                    } else if (binding.etBlockTypeCustom.text.toString().toDouble() <= 0) {
//                        Toast.makeText(p0?.context, "Please enter at-least 1 mm", Toast.LENGTH_SHORT).show()
//                        binding.etBlockTypeCustom.isFocusable = true
//                        binding.etBlockTypeCustom.isFocusableInTouchMode = true
                        preference.setCustomeBlockValue(Preference.customBlockTypeValue, "1")
                    } else {
                        preference.setCustomeBlockValue(Preference.customBlockTypeValue, binding.etBlockTypeCustom.text.toString())
                    }
                }
            }
        })

        if (!isViewStateSaved) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!preference.getBooleanData(Preference.isToolTipCompleted) || screenNumber == 10) {
                    if (arguments?.getString(EXTRA_FROM).toString().equals(EXTRA_CALIBRATION)) {
                        showCalibrationTutorial(binding.linZeroCalibrationSettings)
                    } else if (arguments?.getString(EXTRA_FROM).toString().equals(EXTRA_TILT)) {
                        showTiltTutorial(binding.inSwitchTillHeight.root)
                    } else if (arguments?.getString(EXTRA_FROM).toString().equals(EXTRA_BATTERY)) {
                        showBatteryTutorial(binding.inSwitchBattery.root)
                    }
                }
            }, 200)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        isViewStateSaved = true
    }

    private fun setInputType(edittext: AppCompatEditText, sign:Boolean, decimal:Boolean) {
        edittext.inputType = InputType.TYPE_CLASS_NUMBER
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            edittext.keyListener =
                DigitsKeyListener.getInstance(Locale.ENGLISH, sign, decimal)
        else
            edittext.keyListener = DigitsKeyListener.getInstance(sign, decimal)
    }

    private fun showCalibrationTutorial(view: View) {

        val blockInfo = SimpleHintContentHolder.Builder(view.context)
            .setContentTitle(R.string.zero_calibration)
//            .setContentText("Initially level your caravan to your preferred level. Go to settings and press the ‘Zero Calibration’ button to record this settings and press the ‘Zero Calibration’ button to record this even when powered off.")
            .setContentText(R.string.zero_calibration_description)
            .setTitleStyle(R.style.title)
            .setContentStyle(R.style.content)
            .setMarginByResourcesId(R.dimen.activity_vertical_margin, R.dimen.activity_horizontal_margin, R.dimen.activity_vertical_margin, R.dimen.activity_horizontal_margin)
            .build()

        parentView = requireActivity().window.decorView

        calibrationHintCase = HintCase(parentView)
        calibrationHintCase.setTarget(view, HintCase.TARGET_IS_CLICKABLE)
        calibrationHintCase.setShapeAnimators(RevealRectangularShapeAnimator(), UnrevealRectangularShapeAnimator())
        calibrationHintCase.setHintBlock(blockInfo)
        calibrationHintCase.setOnClosedListener {
            Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                override fun run() {
                    if (!isCalibrationHintClosed) {
                        Log.e("setOnClosedListener", "setOnClosedListener")
                        isCalibrationHintClosed = true
                        calibrationHintCase.setCloseOnTouchView(false)

                        if (preference.getBooleanData(Preference.blockType)) {
                            val lastY = binding.svSettings.pivotY
                            var currentY = 0
                            if (preference.getIntData(Preference.SCREEN_ORIENTATION) == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                                currentY = (lastY / 2.5).toInt()
                            } else {
                                if (linMainSettings.tag.equals(EXTRA_PHONE_PORTRAIT) || linMainSettings.tag.equals(EXTRA_PHONE_LANDSCAPE)) {
                                    currentY = lastY.toInt() + 650
                                } else {
                                    currentY = lastY.toInt() + 150
                                }
                            }
                            binding.svSettings.scrollTo(0, currentY)
                        } else {
                            val lastY = binding.svSettings.pivotY
                            var currentY = 0
                            if (preference.getIntData(Preference.SCREEN_ORIENTATION) == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                                currentY = (lastY / 2.5).toInt()
                            } else {
                                if (linMainSettings.tag.equals(EXTRA_PHONE_PORTRAIT) || linMainSettings.tag.equals(EXTRA_PHONE_LANDSCAPE)) {
                                    currentY = lastY.toInt() + 650
                                } else {
                                    currentY = lastY.toInt() + 150
                                }
                            }

                            binding.svSettings.scrollTo(0, currentY)
                        }

                        Log.e("ELAPSEDREALTIME", SystemClock.elapsedRealtime().toString())
                        Log.e("MLASTCLICKTIME", mLastClickTime.toString())

                        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                            return
                        }
                        mLastClickTime = SystemClock.elapsedRealtime()

                        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                            override fun run() {
                                if (isConnectedDevice) {
                                    showTiltTutorial(binding.inSwitchTillHeight.root)
                                }
                            }
                        }, 200)
                    }
                }
            }, 200)
        }
        calibrationHintCase.show()
        isCalibrationHintShown = true
        settingVM.isCalibrationHintShown = true
        currentTutorial = EXTRA_CALIBRATION
    }

    private fun showTiltTutorial(view: View) {
        val blockInfo = SimpleHintContentHolder.Builder(view.context)
            .setContentTitle(R.string.tilt_height)
//            .setContentText("Go to settings and press the ‘Enable Tilt Height’ button to provide correction height values required to achieve perfect level - in your chosen units.")
            .setContentText(R.string.tilt_height_description)
            .setTitleStyle(R.style.title)
            .setContentStyle(R.style.content)
            .setMarginByResourcesId(
                R.dimen.activity_vertical_margin,
                R.dimen.activity_horizontal_margin,
                R.dimen.activity_vertical_margin,
                R.dimen.activity_horizontal_margin
            ).build()

        parentView = requireActivity().window.decorView

        tiltHintCase = HintCase(parentView)
        tiltHintCase.setTarget(view, HintCase.TARGET_IS_CLICKABLE)
        tiltHintCase.setShapeAnimators(RevealRectangularShapeAnimator(), UnrevealRectangularShapeAnimator())
        tiltHintCase.setHintBlock(blockInfo, FadeInContentHolderAnimator())
        tiltHintCase.setOnClosedListener {
            if (!isTiltHintClosed) {
                isTiltHintClosed = true
                if (isConnectedDevice) {
                    showBatteryTutorial(binding.inSwitchBattery.root)
                }
            }
        }
        tiltHintCase.show()
        isTiltHintShown = true
        settingVM.isTiltHintShown = true
        currentTutorial = EXTRA_TILT
    }

    private fun showBatteryTutorial(view: View) {
        val blockInfo = SimpleHintContentHolder.Builder(view.context)
            .setContentTitle(R.string.battery_indicator)
//            .setContentText("Go to settings and press the ‘Enable Battery Indicator’ button to provide the voltage powering the SavvyLevel unit.")
            .setContentText(R.string.battery_indicator_description)
            .setTitleStyle(R.style.title)
            .setContentStyle(R.style.content)
            .setMarginByResourcesId(R.dimen.activity_vertical_margin, R.dimen.activity_horizontal_margin, R.dimen.activity_vertical_margin, R.dimen.activity_horizontal_margin)
            .build()

        parentView = requireActivity().window.decorView

        batteryHintCase = HintCase(parentView)
        batteryHintCase.setTarget(view, HintCase.TARGET_IS_CLICKABLE)
        batteryHintCase.setShapeAnimators(RevealRectangularShapeAnimator(), UnrevealRectangularShapeAnimator())
        batteryHintCase.setHintBlock(blockInfo, FadeInContentHolderAnimator())
        batteryHintCase.setOnClosedListener {
            if (!isBatteryHintClosed) {
                isBatteryHintClosed = true
                if (isConnectedDevice) {
                    preference.setBoolenData(Preference.isToolTipCompleted, true)
                    commonViewModel.showTutorial.value = ""
                    currentTutorial = ""

                    if (!commonViewModel.currentFragment.value.equals(EXTRA_SETTING)) {
                        val bundle = bundleOf()
                        bundle.putInt(EXTRA_SCREEN_NUMBER, 1)
                        findNavController().navigate(R.id.settingToHome, bundle)
                    }
                }
            }
        }
        batteryHintCase.show()
        isBatteryHintShown = true
        settingVM.isBatteryHintShown = true
        currentTutorial = EXTRA_BATTERY_SETTINGS
    }

    private fun observer() {
        settingVM.zeroCallobation.observe(requireActivity(), object : Observer<Boolean> {
            override fun onChanged(t: Boolean?) {
                commonViewModel.zeroCallibration.value = t ?: false
                Log.e("ZERO_CALI_DEBUG", "zeroCallibration_Observe")

                if (t == true) {
                    val bundle = bundleOf()
                    bundle.putInt(EXTRA_SCREEN_NUMBER, 1)
                    findNavController().navigate(R.id.settingToHome, bundle)
                }

//                commonViewModel.zeroCallibration.value = false
            }
        })

        commonViewModel.lossBlutoothConenction.observe(viewLifecycleOwner, Observer { it ->
            Log.e("it", it)
            if (it.equals(EXTRA_DISCONNECT)) {
                isConnectedDevice = false

                if (preference.getBooleanData(Preference.isDialogShow)) {
                    if (Utils.dialog != null) {
                        if (Utils.dialog!!.isShowing) {
                            Utils.dialog!!.dismiss()
                            preference.setIsShowDialogData(Preference.isDialogShow, false)
                        }
                    }
                }

                when (currentTutorial) {
                    EXTRA_CALIBRATION -> {
                        if (::calibrationHintCase.isInitialized) {
                            calibrationHintCase.hide()
                            isCalibrationHintShown = false
                            settingVM.isCalibrationHintShown = false
                        }
                    }
                    EXTRA_TILT -> {
                        if (::tiltHintCase.isInitialized) {
                            tiltHintCase.hide()
                            isTiltHintShown = false
                            settingVM.isTiltHintShown = false
                        }
                    }
                    EXTRA_BATTERY_SETTINGS -> {
                        if (::batteryHintCase.isInitialized) {
                            batteryHintCase.hide()
                            isBatteryHintShown = false
                            settingVM.isBatteryHintShown = false
                        }
                    }
                }

//                if (commonViewModel.currentFragment.equals(EXTRA_SETTING)) {
                if (!commonViewModel.isHomeFragment && commonViewModel.fromChangeDevice.value.toString().equals("")) {
                    commonViewModel.isHomeFragment = true
                    val bundle = bundleOf()
                    bundle.putInt(EXTRA_SCREEN_NUMBER, 10)
                    findNavController().navigate(R.id.settingToHome, bundle)
                }
//                    commonViewModel.showTutorial.value = ""
//                }
            } else {
                isConnectedDevice = true
            }
        })

        commonViewModel.showTutorial.observe(viewLifecycleOwner, Observer {
            if (it.equals(EXTRA_SETTINGS)) {
                preference.setBoolenData(Preference.isToolTipCompleted, false)
                val bundle = bundleOf()
                bundle.putInt(EXTRA_SCREEN_NUMBER, 10)
                findNavController().navigate(R.id.settingToHome, bundle)
                commonViewModel.showTutorial.value = ""
            }
        })

        settingVM.changeDevice.observe(viewLifecycleOwner, Observer {
            if (it.isNotEmpty()) {
                commonViewModel.fromChangeDevice.value = EXTRA_CHANGE_DEVICE
//                Handler(Looper.getMainLooper()).postDelayed({
                    val bundle = bundleOf()
                    bundle.putInt(EXTRA_SCREEN_NUMBER, 1)
                    if(mBluetoothLeService!!.mBluetoothGatt.connect())
                    mBluetoothLeService!!.disconnect()
                    findNavController().navigate(R.id.settingToSplash, bundle)
//                }, 1000)
            }
        })

        commonViewModel.bluetoothService.observe(viewLifecycleOwner, Observer {
            mBluetoothLeService = it
        })

        commonViewModel.errorCode.observe(viewLifecycleOwner, Observer {
//            binding.tvErrorCodeSettings.text = it
        })

        settingVM.errorOptions.observe(viewLifecycleOwner, Observer {
            if (it.equals(EXTRA_REQUEST)) {
                val messageBytes = byteArrayOf(0x0B.toByte())

                commonViewModel.hitCommandFor.value = EXTRA_REQUEST_ERROR

                mBluetoothLeService?.writeCharacteristic(messageBytes, Attributes.UUID_HM11_CHARACTERISTIC_CMD)

                Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                    override fun run() {
                        mBluetoothLeService?.readCMDCharacteristic(Attributes.UUID_HM11_CHARACTERISTIC_CMD)
                    }
                }, 500)
            } else {
                val messageBytes = byteArrayOf(0x10.toByte())

                commonViewModel.hitCommandFor.value = EXTRA_CLEAR_ERROR

                mBluetoothLeService?.writeCharacteristic(messageBytes, Attributes.UUID_HM11_CHARACTERISTIC_CMD)

                Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                    override fun run() {
                        mBluetoothLeService?.readCMDCharacteristic(Attributes.UUID_HM11_CHARACTERISTIC_CMD)
                    }
                }, 500)
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListener() {
        svSettings.setOnTouchListener({ view, motionEvent ->
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(svSettings.getWindowToken(), 0)
            false
        })

        binding.etWheelToWheelSpan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val text: String = p0.toString()
                if (text.contains(".") && text.split(".").size > 1) {
                    val splitArray = text.split(".")
                    if (splitArray[1].length > 1) {
                        binding.etWheelToWheelSpan.setText(text.substring(0, text.length - 1))
                        binding.etWheelToWheelSpan.setSelection(binding.etWheelToWheelSpan.text.toString().length)
                    }
                }
            }

            override fun afterTextChanged(p0: Editable?) {
                if (p0?.length!! > 0) {
                    if (p0.toString() == ".") {
                        binding.etWheelToWheelSpan.setText("")
                    }

                    if (p0.length == 5) {
                        if (p0.get(4).toString().equals(".")) {
                            binding.etWheelToWheelSpan.setText(p0.toString().substring(0, p0.toString().length - 1))
                            binding.etWheelToWheelSpan.setSelection(binding.etWheelToWheelSpan.text.toString().length)
                        }
                    }
                }
            }
        })

        binding.etWheelToJockeyWheelSpan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val text: String = p0.toString()
                if (text.contains(".") && text.split(".").size > 1) {
                    val splitArray = text.split(".")
                    if (splitArray[1].length > 1) {
                        binding.etWheelToJockeyWheelSpan.setText(text.substring(0, text.length - 1))
                        binding.etWheelToJockeyWheelSpan.setSelection(binding.etWheelToJockeyWheelSpan.text.toString().length)
                    }
                }
            }

            override fun afterTextChanged(p0: Editable?) {
                if (p0?.length!! > 0) {
                    if (p0.toString() == ".") {
                        binding.etWheelToJockeyWheelSpan.setText("")
                    }

                    if (p0.length == 5) {
                        if (p0.get(4).toString().equals(".")) {
                            binding.etWheelToJockeyWheelSpan.setText(p0.toString().substring(0, p0.toString().length - 1))
                            binding.etWheelToJockeyWheelSpan.setSelection(binding.etWheelToJockeyWheelSpan.text.toString().length)
                        }
                    }
                }
            }
        })

        binding.etBlockTypeCustom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val text: String = p0.toString()
                if (text.contains(".") && text.split(".").size > 1) {
                    val splitArray = text.split(".")
                    if (splitArray[1].length > 1) {
                        binding.etBlockTypeCustom.setText(text.substring(0, text.length - 1))
                        binding.etBlockTypeCustom.setSelection(binding.etBlockTypeCustom.text.toString().length)
                    }
                }
            }

            override fun afterTextChanged(p0: Editable?) {
                if (p0?.length!! > 0) {
                    if (p0.toString() == ".") {
                        binding.etBlockTypeCustom.setText("")
                    }

                    if (p0.length == 5) {
                        if (p0.get(4).toString().equals(".")) {
                            binding.etBlockTypeCustom.setText(p0.toString().substring(0, p0.toString().length - 1))
                            binding.etBlockTypeCustom.setSelection(binding.etBlockTypeCustom.text.toString().length)
                        }
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        settingVM.onResumeInFragment(
            binding.inSwitch.motionLayout,
            binding.lnrBlockTypeOption,
            binding.inSwitchBattery.motionLayout,
            binding.inSwitchTillHeight.motionLayout,
            binding.etWheelToWheelSpan,
            binding.etWheelToJockeyWheelSpan,
            binding.etBlockTypeCustom,
            binding.tvDeviceNameItemConnectDevice,
            commonViewModel,
            requireActivity()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
//        binding.unbind()
    }
}