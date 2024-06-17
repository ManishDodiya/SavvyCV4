package com.savvytech.savvylevelfour.ui.fragments

import android.bluetooth.BluetoothAdapter
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.joanfuentes.hintcase.HintCase
import com.joanfuentes.hintcaseassets.contentholderanimators.FadeInContentHolderAnimator
import com.joanfuentes.hintcaseassets.hintcontentholders.SimpleHintContentHolder
import com.joanfuentes.hintcaseassets.shapeanimators.RevealCircleShapeAnimator
import com.joanfuentes.hintcaseassets.shapeanimators.RevealRectangularShapeAnimator
import com.joanfuentes.hintcaseassets.shapeanimators.UnrevealCircleShapeAnimator
import com.joanfuentes.hintcaseassets.shapeanimators.UnrevealRectangularShapeAnimator
import com.joanfuentes.hintcaseassets.shapes.CircularShape
import com.savvytech.savvylevelfour.R
import com.savvytech.savvylevelfour.common.*
import com.savvytech.savvylevelfour.databinding.FragmentHomeBinding
import com.savvytech.savvylevelfour.viewmodels.CommonViewModel
import com.savvytech.savvylevelfour.viewmodels.HomeFragmentViewModel

class HomeFragment : Fragment() {

    companion object {
        var currentTutorial = ""
        var screenNumber = 0
    }

    lateinit var fragmentHomeBinding: FragmentHomeBinding
    lateinit var homeFragmentViewModel: HomeFragmentViewModel

    lateinit var commonViewModel: CommonViewModel
    lateinit var preference: Preference

    var mBluetoothLeService: BluetoothLeService? = null
    lateinit var bluetoothAdapter: BluetoothAdapter

    private var doubleBackToExitPressedOnce = false

    var currentPosPitchForZeroCal = 0f
    var currentPosPitch = 0f
    var currentPosRollForZeroCal = 0f
    var currentPosRoll = 0f

    var isConnectedDevice = true

    private var isZoom = false

    var mServiceBound = false

    var accTimeStart: Long = 0
    var accIntervalCtr = 0
    var accTimeStartPitch: Long = 0
    var accIntervalCtrPitch = 0
    var isAnimateRoll = true
    var isAnimatePitch = true

    lateinit var relFHLock: RelativeLayout
    lateinit var relSetHitch: RelativeLayout
    lateinit var relSetHitchHome: RelativeLayout
    lateinit var relTopHome: RelativeLayout
    var isDeviceListVisible = false
    lateinit var ivTenXHome: AppCompatImageView
    lateinit var ivSetHitch: AppCompatImageView
    lateinit var txtFHHitch: AppCompatTextView

    lateinit var parentView: View

    var isWelcomeHintClosed = false
    var isTenXHintClosed = false
    var isHitchHintClosed = false
    var isBatteryHintClosed = false
    var isBluetoothHintClosed = false
    var isWelcomeHintShown = false
    var isTenXHintShown = false
    var isHitchHintShown = false
    var isBatteryHintShown = false
    var isBluetoothHintShown = false
    lateinit var welcomeHintCase: HintCase
    lateinit var tenXHintCase: HintCase
    lateinit var hitchHintCase: HintCase
    lateinit var batteryHintCase: HintCase
    lateinit var bluetoothHintCase: HintCase

    private val zoom = 0.toChar()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        retainInstance = true

        fragmentHomeBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        if (!fragmentHomeBinding.root.isFocused) {
            fragmentHomeBinding.root.requestLayout()
        }

        findViewByID()
        init()
        setListeners()
        observer()
        onBackPresser()

        currentPosRoll = preference.getFloatData(EXTRA_CURRENT_POS_ROLL)
        fragmentHomeBinding.viewRoundTop.setAnimationSpeed(0)
        if (currentPosRoll != 0.0f) {
            isAnimateRoll = false
        }

        Log.d(EXTRA_CURRENT_POS_ROLL, currentPosRoll.toString())

        currentPosPitch = preference.getFloatData(EXTRA_CURRENT_POS_PITCH)
        fragmentHomeBinding.viewRoundBottom.setAnimationSpeed(0)
        if (currentPosPitch != 0.0f)
            isAnimatePitch = false
        return fragmentHomeBinding.root
    }

    private fun findViewByID() {
        relFHLock = fragmentHomeBinding.root.findViewById(R.id.relFHLock)
        relSetHitch = fragmentHomeBinding.root.findViewById(R.id.relSetHitch)
        relSetHitchHome = fragmentHomeBinding.root.findViewById(R.id.relSetHitchHome)
        ivTenXHome = fragmentHomeBinding.root.findViewById(R.id.ivTenXHome)
        relTopHome = fragmentHomeBinding.root.findViewById(R.id.relTopHome)
        ivSetHitch = fragmentHomeBinding.root.findViewById(R.id.ivSetHitch)
        txtFHHitch = fragmentHomeBinding.root.findViewById(R.id.txtFHHitch)
    }

    override fun onStop() {
        super.onStop()
        if (mServiceBound) {
//            requireActivity().unbindService(mServiceConnection)
            mServiceBound = false
        }
    }

    private fun onBackPresser() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(),
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (doubleBackToExitPressedOnce) {
                        preference.setIntData(Preference.processId, 1)
                        requireActivity().finish()
                        return
                    }

                    requireActivity().toast(getString(R.string.pleaseClickBackPress))

                    doubleBackToExitPressedOnce = true
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        doubleBackToExitPressedOnce = false
                    }, 2000)
                }
            })
    }

    private fun init() {
        homeFragmentViewModel = ViewModelProvider(this).get(HomeFragmentViewModel::class.java)
        fragmentHomeBinding.homeFragmentViewModel = homeFragmentViewModel
        fragmentHomeBinding.lifecycleOwner = requireActivity()

        preference = Preference(requireContext())

        screenNumber = arguments?.getInt(EXTRA_SCREEN_NUMBER)!!

        commonViewModel = activity?.run {
            ViewModelProvider(this).get(CommonViewModel::class.java)
        }!!

        commonViewModel.isHomeFragment = true

        if (screenNumber == 10) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (fragmentHomeBinding.lnrFHNoDataFound.visibility == View.GONE) {
                    isWelcomeHintShown = true
                    if (fragmentHomeBinding.lnrFHNoDataFound.visibility == View.GONE) {
                        fragmentHomeBinding.relSetHitchHome.performClick()
                    }
//                    showHint(ivTenXHome)
                }
            }, 200)
        } else {

            commonViewModel.currentFragment.value = EXTRA_HOME

            if (!preference.getBooleanData(Preference.isToolTipCompleted)) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (fragmentHomeBinding.lnrFHNoDataFound.visibility == View.GONE) {
//                        showHint(ivTenXHome)
                        if (!preference.getBattData(Preference.batteryStatus)) {
                            commonViewModel.isBattHideBeforeTutorial = true
                            preference.setBattData(Preference.batteryStatus, true)
                            homeFragmentViewModel.txtFHBatteryStatus.value =
                                if (preference.getBattData(Preference.batteryStatus)) View.VISIBLE else View.GONE
                        }
                        showWelcomeHint()
                    }
                }, 200)
            }
        }

        if (preference.getBooleanData(Preference.tiltHeight)) {
            fragmentHomeBinding.viewRoundTop.setSecondaryTextVisibility(
                true,
                preference.getSpanType(Preference.spanType),
                fragmentHomeBinding.tvTiltHeightTop,
                fragmentHomeBinding.txtFHBlockTitle,
                requireContext()
            )

            fragmentHomeBinding.viewRoundBottom.setSecondaryTextVisibility(
                true,
                preference.getSpanType(Preference.spanType),
                fragmentHomeBinding.tvTiltHeightBottom,
                AppCompatTextView(requireContext()),
                requireContext()
            )
        } else {
            fragmentHomeBinding.viewRoundTop.setSecondaryTextVisibility(
                false,
                preference.getSpanType(Preference.spanType),
                fragmentHomeBinding.tvTiltHeightTop,
                fragmentHomeBinding.txtFHBlockTitle,
                requireContext()
            )

            fragmentHomeBinding.viewRoundBottom.setSecondaryTextVisibility(
                false,
                preference.getSpanType(Preference.spanType),
                fragmentHomeBinding.tvTiltHeightBottom,
                AppCompatTextView(requireContext()),
                requireContext()
            )
        }

        setWheelSpan((preference.getWheelToWheelData(Preference.wheelToWheel) ?: "0").toFloat().toInt())
        setTowSpan((preference.getWheelToJockeyWheelData(Preference.wheelToJockeyWheel) ?: "0").toFloat().toInt())

        try {
            if (fragmentHomeBinding != null && fragmentHomeBinding.viewRoundTop != null) {

                if (isZoom) {
                    currentPosRoll *= 10
                    currentPosPitch *= 10
                }

                fragmentHomeBinding.viewRoundTop.setValue(currentPosRoll)
                fragmentHomeBinding.viewRoundBottom.setValue(currentPosPitch)
            }
        } catch (ex: Exception) {
            Log.e("EXCEPTION", ex.message.toString())
        }
        val rollResId: Int = resources.getIdentifier(
            EXTRA_CARAVAN_HOME_TOP,
            EXTRA_DRAWABLE,
            requireActivity().getPackageName()
        )
        val pitchResId: Int = resources.getIdentifier(
            EXTRA_CARAVAN_HOME_BOTTOM,
            EXTRA_DRAWABLE,
            requireActivity().getPackageName()
        )

        fragmentHomeBinding.viewRoundTop.setImage(rollResId, requireContext())
        fragmentHomeBinding.viewRoundBottom.setImage(pitchResId, requireContext())

        if (preference.getBooleanData(Preference.hitchEnabled)) {
            fragmentHomeBinding.viewRoundBottom.setHitchEnabled(true)
            Log.e("HITCH_VALUE", preference.getFloatData(Preference.hitchValue).toString())
            fragmentHomeBinding.viewRoundBottom.setHitchAngle(preference.getFloatData(Preference.hitchValue))
            relSetHitch.visibility = View.GONE
            relFHLock.visibility = View.VISIBLE
            homeFragmentViewModel.relSetHitch.value = View.GONE
            homeFragmentViewModel.relFHLock.value = View.VISIBLE
        } else {
            homeFragmentViewModel.relSetHitch.value = View.VISIBLE
            homeFragmentViewModel.relFHLock.value = View.GONE
            relFHLock.visibility = View.GONE
            relSetHitch.visibility = View.VISIBLE
        }
    }

    private fun setListeners() {

        ivTenXHome.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (v != null) {
                    if (!isTenXHintShown) {
                        homeFragmentViewModel.clickOnZoom(ivTenXHome)
                    }
                }
            }
        })

//        txtFHHitch.setOnClickListener {
//            Log.e("ON_CLICK", "ON_CLICK_TEXT")
//        }
//
//        relSetHitch.setOnClickListener {
//            Log.e("ON_CLICK", relSetHitch.tag.toString() + "    *****   ")
//        }
//
//        ivSetHitch.setOnClickListener {
//            Log.e("ON_CLICK", "ON_CLICK_IMAGE")
//        }

        fragmentHomeBinding.relSetHitchHome.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (v != null) {
                    if (!isHitchHintShown) {
                        if (screenNumber == 10) {
                            showHitchHint(v)
                        } else {
                            if (!preference.getBooleanData(Preference.isToolTipCompleted)) {
                                showHitchHint(v)
                            }
                        }
                    }
                }
            }
        })

        fragmentHomeBinding.ivBluetoothStrength.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (v != null) {
                    if (screenNumber == 10) {
                        showBluetoothHint(v)
                    } else {
                        if (!preference.getBooleanData(Preference.isToolTipCompleted)) {
                            showBluetoothHint(v)
                        }
                    }
                }
            }
        })
    }

    private fun showWelcomeHint() {
        val blockInfo = SimpleHintContentHolder.Builder(context)
            .setContentTitle(R.string.welcome_to_savvylevel)
            .setContentText(R.string.quick_tutorial)
            .setTitleStyle(R.style.title)
            .setContentStyle(R.style.content).build()

        parentView = requireActivity().window.decorView

        welcomeHintCase = HintCase(parentView)
//        tenXHintCase.setTarget(view, CircularShape(), HintCase.TARGET_IS_CLICKABLE)
//        welcomeHintCase.setShapeAnimators(RevealCircleShapeAnimator(), UnrevealCircleShapeAnimator())
        welcomeHintCase.setHintBlock(blockInfo)
        welcomeHintCase.setOnClosedListener {
            if (!isWelcomeHintClosed) {
                isWelcomeHintClosed = true
                if (fragmentHomeBinding.lnrFHNoDataFound.visibility == View.GONE) {
                    fragmentHomeBinding.relSetHitchHome.performClick()
//                    showHint(ivTenXHome)
                }
            }
        }
        welcomeHintCase.show()

        Log.e("IS_SHOWN", isWelcomeHintShown.toString())

        isWelcomeHintShown = true
        currentTutorial = EXTRA_WELCOME
    }

    private fun showHint(view: View) {
        val blockInfo = SimpleHintContentHolder.Builder(view.context)
            .setContentTitle(R.string.ten_x_zoom)
            .setContentText(R.string.ten_x_zoom_description)
            .setTitleStyle(R.style.title)
            .setContentStyle(R.style.content)
            .setGravity(Gravity.LEFT)
            .setMarginByResourcesId(
                R.dimen.activity_vertical_margin,
                R.dimen.activity_horizontal_margin,
                R.dimen.activity_vertical_margin,
                R.dimen.activity_horizontal_margin
            )
            .build()

        parentView = requireActivity().window.decorView

        if (zoom == TiltGauge.NORMAL) {
            ivTenXHome.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ten_x
                )
            )
        }

        tenXHintCase = HintCase(parentView)
        tenXHintCase.setTarget(view, CircularShape(), HintCase.TARGET_IS_CLICKABLE)
        tenXHintCase.setShapeAnimators(RevealCircleShapeAnimator(), UnrevealCircleShapeAnimator())
        tenXHintCase.setHintBlock(blockInfo)
        tenXHintCase.setOnClosedListener {
            if (!isTenXHintClosed) {
                isTenXHintClosed = true
//                if (fragmentHomeBinding.lnrFHNoDataFound.visibility == View.GONE) {
//                    fragmentHomeBinding.relSetHitchHome.performClick()
//                }
                if (commonViewModel.isBattHideBeforeTutorial) {
                    preference.setBattData(Preference.batteryStatus, false)
                }

                if (zoom == TiltGauge.ZOOM) {
                    ivTenXHome.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.one_x
                        )
                    )
                }

                if (isConnectedDevice) {
                    val bundle = bundleOf()
                    bundle.putString(EXTRA_FROM, EXTRA_CALIBRATION)
                    findNavController().navigate(R.id.homeToSetting, bundle)
                }
            }
        }
        tenXHintCase.show()
        isTenXHintShown = true
        currentTutorial = EXTRA_TEN_X
    }

    private fun showHitchHint(v: View) {

        val blockInfo = SimpleHintContentHolder.Builder(v.context)
            .setContentTitle(R.string.hitch_setup)
            .setContentText(R.string.hitch_setup_description)
            .setTitleStyle(R.style.title)
            .setContentStyle(R.style.content)
            .setGravity(Gravity.CENTER)
            .setMarginByResourcesId(
                R.dimen.activity_vertical_margin,
                R.dimen.activity_horizontal_margin,
                R.dimen.activity_vertical_margin,
                R.dimen.activity_horizontal_margin
            )
            .build()

        parentView = requireActivity().window.decorView
//        val view: View
//
//        if (preference.getBooleanData(Preference.hitchEnabled)) {
//            view = fragmentHomeBinding.relFHLock
//        } else {
//            view = fragmentHomeBinding.relSetHitch
//        }

        if (preference.getBooleanData(Preference.hitchEnabled)) {
            homeFragmentViewModel.relSetHitch.value = View.VISIBLE
            homeFragmentViewModel.relFHLock.value = View.GONE
            relFHLock.visibility = View.GONE
            relSetHitch.visibility = View.VISIBLE
        }

        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                hitchHintCase = HintCase(parentView)
                hitchHintCase.setTarget(
                    fragmentHomeBinding.relSetHitch,
                    HintCase.TARGET_IS_CLICKABLE
                )
                hitchHintCase.setShapeAnimators(
                    RevealRectangularShapeAnimator(),
                    UnrevealRectangularShapeAnimator()
                )
                hitchHintCase.setHintBlock(blockInfo, FadeInContentHolderAnimator())
                hitchHintCase.setOnClosedListener {
                    if (!isHitchHintClosed) {
                        isHitchHintClosed = true

                        if (preference.getBooleanData(Preference.hitchEnabled)) {
                            fragmentHomeBinding.viewRoundBottom.setHitchEnabled(true)
                            fragmentHomeBinding.viewRoundBottom.setHitchAngle(
                                preference.getFloatData(
                                    Preference.hitchValue
                                )
                            )
                            relSetHitch.visibility = View.GONE
                            relFHLock.visibility = View.VISIBLE
                            homeFragmentViewModel.relSetHitch.value = View.GONE
                            homeFragmentViewModel.relFHLock.value = View.VISIBLE
                        } else {
                            homeFragmentViewModel.relSetHitch.value = View.VISIBLE
                            homeFragmentViewModel.relFHLock.value = View.GONE
                            relFHLock.visibility = View.GONE
                            relSetHitch.visibility = View.VISIBLE
                        }

                        if (isConnectedDevice) {
                            fragmentHomeBinding.ivBluetoothStrength.performClick()
                        }
                    }
                }
                hitchHintCase.show()
                isHitchHintShown = true
                currentTutorial = EXTRA_HITCH
            }
        }, 100)
    }

    private fun showBatteryHint(v: View) {

        val blockInfo = SimpleHintContentHolder.Builder(v.context)
            .setContentTitle(R.string.battery)
            .setContentText(R.string.battery_description)
            .setTitleStyle(R.style.title)
            .setContentStyle(R.style.content)
            .setGravity(Gravity.CENTER)
            .setMarginByResourcesId(
                R.dimen.activity_vertical_margin,
                R.dimen.activity_horizontal_margin,
                R.dimen.activity_vertical_margin,
                R.dimen.activity_horizontal_margin
            )
            .build()

        parentView = requireActivity().window.decorView

        batteryHintCase = HintCase(parentView)
        batteryHintCase.setTarget(fragmentHomeBinding.clParentBattery, HintCase.TARGET_IS_CLICKABLE)
        batteryHintCase.setShapeAnimators(
            RevealRectangularShapeAnimator(),
            UnrevealRectangularShapeAnimator()
        )
        batteryHintCase.setHintBlock(blockInfo, FadeInContentHolderAnimator())
        batteryHintCase.setOnClosedListener {
            if (!isBatteryHintClosed) {
                isBatteryHintClosed = true
                if (isConnectedDevice) {
                    fragmentHomeBinding.ivBluetoothStrength.performClick()
                }
            }
//                commonViewModel.isActivityToolTipCompleted.value = true
        }
        batteryHintCase.show()
        isBatteryHintShown = true
        currentTutorial = EXTRA_BATTERY
    }

    private fun showBluetoothHint(v: View) {

        val blockInfo = SimpleHintContentHolder.Builder(v.context)
            .setContentTitle(R.string.bluetooth_strength)
            .setContentText(R.string.bluetooth_strength_description)
            .setTitleStyle(R.style.title)
            .setContentStyle(R.style.content)
            .setGravity(Gravity.CENTER)
            .setMarginByResourcesId(
                R.dimen.activity_vertical_margin,
                R.dimen.activity_horizontal_margin,
                R.dimen.activity_vertical_margin,
                R.dimen.activity_horizontal_margin
            )
            .build()

        parentView = requireActivity().window.decorView

        bluetoothHintCase = HintCase(parentView)
        bluetoothHintCase.setTarget(
            fragmentHomeBinding.ivBluetoothStrength,
            CircularShape(),
            HintCase.TARGET_IS_CLICKABLE
        )
        bluetoothHintCase.setShapeAnimators(
            RevealCircleShapeAnimator(),
            UnrevealCircleShapeAnimator()
        )
        bluetoothHintCase.setHintBlock(blockInfo, FadeInContentHolderAnimator())
        bluetoothHintCase.setOnClosedListener {
            if (!isBluetoothHintClosed) {
                isBluetoothHintClosed = true
                showHint(ivTenXHome)
            }
        }
        bluetoothHintCase.show()
        isBluetoothHintShown = true
        currentTutorial = EXTRA_BLUETOOTH
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.e("==>HOME", "CONFIG_CHANGE")
    }

    fun setWheelSpan(length: Int) {
        fragmentHomeBinding.viewRoundTop.setSecondaryTextSpan(length)
    }

    fun setTowSpan(length: Int) {
        fragmentHomeBinding.viewRoundBottom.setSecondaryTextSpan(length)
    }

    private fun observer() {

        commonViewModel.isDeviceListVisible.observe(viewLifecycleOwner, Observer {
            isDeviceListVisible = it
        })

        homeFragmentViewModel.enableHitch.observe(viewLifecycleOwner, object : Observer<Boolean> {
            override fun onChanged(t: Boolean?) {
                if (t!!) {
                    if (isZoom) {
                        currentPosPitch /= 10
                    }
                    Log.e("CURRENT_POS_PITCH", currentPosPitch.toString())
                    fragmentHomeBinding.viewRoundBottom.setHitchAngle(currentPosPitch)
                    fragmentHomeBinding.viewRoundBottom.setHitchEnabled(t)
                    preference.setFloatData(Preference.hitchValue, currentPosPitch)
                    Log.e("FLOAT_DATA", preference.getFloatData(Preference.hitchValue).toString())
                    Toast.makeText(requireContext(), R.string.hitch_value_saved, Toast.LENGTH_LONG).show()
                } else {
                    var hitchValue = preference.getFloatData(Preference.hitchValue)
                    if (isZoom) {
                        hitchValue /= 10
                    }
                    Log.e("CURRENT_POS_PITCH", hitchValue.toString())
                    fragmentHomeBinding.viewRoundBottom.setHitchAngle(hitchValue)
                    fragmentHomeBinding.viewRoundBottom.setHitchEnabled(t)
                    preference.setFloatData(Preference.hitchValue, hitchValue)
                }
            }
        })

        homeFragmentViewModel.isZoom.observe(viewLifecycleOwner, Observer {
//            if (it != preference.getBooleanData(Preference.isZoom)) {
            isZoom = it
            if (it) {
                fragmentHomeBinding.viewRoundBottom.setZoomInOut(TiltGauge.ZOOM)
                fragmentHomeBinding.viewRoundTop.setZoomInOut(TiltGauge.ZOOM)
                ivTenXHome.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.one_x))
                preference.setBoolenData(Preference.isZoom, true)
            } else {
                fragmentHomeBinding.viewRoundBottom.setZoomInOut(TiltGauge.NORMAL)
                fragmentHomeBinding.viewRoundTop.setZoomInOut(TiltGauge.NORMAL)
                ivTenXHome.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ten_x))
                preference.setBoolenData(Preference.isZoom, false)
            }
//            }
        })

//        commonViewModel.aniamtionLive.observe(viewLifecycleOwner, Observer {
//            Log.e("ON_VALUE_CHANGE", it.toString() + "   ******   ")
//            fragmentHomeBinding.viewRoundBottom.setAnimationSpeed(it.toInt())
//        })

        commonViewModel.lossBlutoothConenction.observe(viewLifecycleOwner, Observer { it ->
            Log.e("CONNECTION", it)
            if (it.equals(EXTRA_CONNECT)) {
                if (screenNumber != 0) {
                    if (isConnectedDevice) {
                        fragmentHomeBinding.lnrFHNoDataFound.visibility = View.GONE
//                        fragmentHomeBinding.lnrFHNoDataFound.alpha = 0f
                        fragmentHomeBinding.lnrFHNoDataFound.isClickable = true
                        fragmentHomeBinding.lnrFHNoDataFound.isFocusable = true
                    } else {
                        fragmentHomeBinding.lnrFHNoDataFound.visibility = View.GONE
//                        fragmentHomeBinding.lnrFHNoDataFound.animate()?.alpha(0.0f)?.setDuration(1000)?.setListener(null)
                        fragmentHomeBinding.lnrFHNoDataFound.isClickable = false
                        fragmentHomeBinding.lnrFHNoDataFound.isFocusable = false
                    }
                } else {
                    fragmentHomeBinding.lnrFHNoDataFound.visibility = View.GONE
//                    fragmentHomeBinding.lnrFHNoDataFound.animate()?.alpha(0.0f)?.setDuration(1000)?.setListener(null)
                    fragmentHomeBinding.lnrFHNoDataFound.isClickable = false
                    fragmentHomeBinding.lnrFHNoDataFound.isFocusable = false
                }
                isConnectedDevice = true
                fragmentHomeBinding.ivBluetoothStrength.setImageResource(R.drawable.bluetooth_high)

                when (currentTutorial) {
                    EXTRA_WELCOME -> {
                        if (!isWelcomeHintShown) {
                            isWelcomeHintShown = true
                            showWelcomeHint()
                        }
                    }
                    EXTRA_TEN_X -> {
                        if (!isTenXHintShown) {
                            isTenXHintShown = true
                            showHint(ivTenXHome)
                        }
                    }
                    EXTRA_HITCH -> {
                        if (!isHitchHintShown) {
                            isHitchHintShown = true
                            showHitchHint(fragmentHomeBinding.relSetHitchHome.rootView)
                        }
                    }
                    EXTRA_BATTERY -> {
                        if (!isBatteryHintShown) {
                            isBatteryHintShown = true
                            showBatteryHint(fragmentHomeBinding.clParentBattery.rootView)
                        }
                    }
                    EXTRA_BLUETOOTH -> {
                        if (!isBluetoothHintShown) {
                            isBluetoothHintShown = true
                            fragmentHomeBinding.ivBluetoothStrength.performClick()
                        }
                    }
                    EXTRA_CALIBRATION -> {
                        val bundle = bundleOf()
                        bundle.putString(EXTRA_FROM, EXTRA_CALIBRATION)
                        findNavController().navigate(R.id.homeToSetting, bundle)
                    }
                    EXTRA_TILT -> {
                        val bundle = bundleOf()
                        bundle.putString(EXTRA_FROM, EXTRA_TILT)
                        findNavController().navigate(R.id.homeToSetting, bundle)
                    }
                    EXTRA_BATTERY_SETTINGS -> {
                        val bundle = bundleOf()
                        bundle.putString(EXTRA_FROM, EXTRA_BATTERY)
                        findNavController().navigate(R.id.homeToSetting, bundle)
                    }
                }
            } else {

                if (preference.getIsShowDialog(Preference.isDialogShow)) {
                    if (Utils.dialog != null) {
                        if (Utils.dialog!!.isShowing) {
                            Utils.dialog!!.dismiss()
                            preference.setIsShowDialogData(Preference.isDialogShow, false)
                        }
                    }
                }

                isConnectedDevice = false
                fragmentHomeBinding.lnrFHNoDataFound.visibility = View.VISIBLE
//                fragmentHomeBinding.lnrFHNoDataFound.animate()?.alpha(1.0f)?.setDuration(1000)?.setListener(null)
//                fragmentHomeBinding.lnrFHNoDataFound.visibility = View.GONE
                fragmentHomeBinding.lnrFHNoDataFound.isClickable = true
                fragmentHomeBinding.lnrFHNoDataFound.isFocusable = true
                fragmentHomeBinding.ivBluetoothStrength.setImageResource(R.drawable.blutooth_disconnect)
                fragmentHomeBinding.txtFHBatteryStatus.text = "0.0 V"

//                if (commonViewModel.currentFragment.equals(EXTRA_HOME)) {
                when (currentTutorial) {
                    EXTRA_WELCOME -> {
                        if (::welcomeHintCase.isInitialized) {
                            welcomeHintCase.hide()
                            isWelcomeHintShown = false
                        }
                    }
                    EXTRA_TEN_X -> {
                        if (::tenXHintCase.isInitialized) {
                            tenXHintCase.hide()
                            isTenXHintShown = false
                        }
                    }
                    EXTRA_HITCH -> {
                        if (::hitchHintCase.isInitialized) {
                            hitchHintCase.hide()
                            isHitchHintShown = false
                        }
                    }
                    EXTRA_BATTERY -> {
                        if (::batteryHintCase.isInitialized) {
                            batteryHintCase.hide()
                            isBatteryHintShown = false
                        }
                    }
                    EXTRA_BLUETOOTH -> {
                        if (::bluetoothHintCase.isInitialized) {
                            bluetoothHintCase.hide()
                            isBluetoothHintShown = false
                        }
                    }
//                    }
                }
            }
        })

        commonViewModel.batt.observe(viewLifecycleOwner, Observer {
            if (fragmentHomeBinding.lnrFHNoDataFound.visibility == View.GONE) {
                val number2digits = String.format("%.1f", it.toDouble()).toDouble()
                fragmentHomeBinding.txtFHBatteryStatus.text = number2digits.toString() + " V"
            }
        })

        commonViewModel.pitch.observe(viewLifecycleOwner, Observer { it ->
            Log.e("it", it.toString())
            currentPosPitch = it

            var animationSpeed: Long = 0
            accIntervalCtrPitch++
            val tEnd = System.currentTimeMillis()
            val tDelta: Long = tEnd - accTimeStartPitch
//            if (tDelta > 1000) {
            animationSpeed = tDelta / accIntervalCtrPitch
            if (animationSpeed > 500) animationSpeed = 500
            if (isAnimatePitch) {
                if (!isZoom && it <= 40.0 && it >= -40.0) {
                    animationSpeed = 600
                } else if (isZoom && it <= 4.0 && it >= -4.0) {
                    animationSpeed = 600
                } else {
                    animationSpeed = 200
                }
                fragmentHomeBinding.viewRoundBottom.setAnimationSpeed(animationSpeed.toInt())
//                fragmentHomeBinding.viewRoundBottom.setAnimationSpeed(900)
                fragmentHomeBinding.viewRoundBottom.setValue(it)
            } else {
                isAnimatePitch = true
                fragmentHomeBinding.viewRoundBottom.setValue(preference.getFloatData(EXTRA_CURRENT_POS_PITCH))
            }
            accTimeStartPitch = tEnd
            accIntervalCtrPitch = 0
//            }

//            fragmentHomeBinding.viewRoundBottom.setValue(it)

            if (isZoom) {
                currentPosPitch *= 10
            }
            preference.setFloatData(EXTRA_CURRENT_POS_PITCH, currentPosPitch)

            if (isZoom) {
                if (it <= 4.5 && it >= -4.5) {
                    fragmentHomeBinding.txtFHPitch.clearAnimation()
                    fragmentHomeBinding.tvTiltHeightBottom.clearAnimation()
//                fragmentHomeBinding.imgFHPitch.rotation = currentPosPitch
                    fragmentHomeBinding.txtFHPitch.setText(Math.abs(it).toString() + "\u00B0")
                    fragmentHomeBinding.tvTiltHeightBottom.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    fragmentHomeBinding.txtFHPitch.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                } else {
                    val anim: Animation = AlphaAnimation(0.0f, 1.0f)
                    anim.duration = 50 //You can manage the blinking time with this parameter
                    anim.startOffset = 20
                    anim.repeatMode = Animation.REVERSE
                    anim.repeatCount = Animation.INFINITE

                    fragmentHomeBinding.txtFHPitch.text = "...."
                    fragmentHomeBinding.txtFHPitch.animation = anim
                    fragmentHomeBinding.tvTiltHeightBottom.animation = anim
                    fragmentHomeBinding.tvTiltHeightBottom.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    fragmentHomeBinding.txtFHPitch.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
//                if (it > 0) {
//                    fragmentHomeBinding.imgFHPitch?.rotation = 45.0f
//                } else {
//                    fragmentHomeBinding.imgFHPitch?.rotation = -45.0f
//                }
                }
            } else {
                if (it <= 45.0 && it >= -45.0) {
                    fragmentHomeBinding.txtFHPitch.clearAnimation()
                    fragmentHomeBinding.tvTiltHeightBottom.clearAnimation()
//                fragmentHomeBinding.imgFHPitch.rotation = currentPosPitch
                    fragmentHomeBinding.txtFHPitch.setText(Math.abs(it).toString() + "\u00B0")
                    fragmentHomeBinding.tvTiltHeightBottom.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    fragmentHomeBinding.txtFHPitch.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                } else {
                    val anim: Animation = AlphaAnimation(0.0f, 1.0f)
                    anim.duration = 50 //You can manage the blinking time with this parameter
                    anim.startOffset = 20
                    anim.repeatMode = Animation.REVERSE
                    anim.repeatCount = Animation.INFINITE

                    fragmentHomeBinding.txtFHPitch.text = "...."
                    fragmentHomeBinding.txtFHPitch.animation = anim
                    fragmentHomeBinding.tvTiltHeightBottom.animation = anim
                    fragmentHomeBinding.tvTiltHeightBottom.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    fragmentHomeBinding.txtFHPitch.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
//                if (it > 0) {
//                    fragmentHomeBinding.imgFHPitch?.rotation = 45.0f
//                } else {
//                    fragmentHomeBinding.imgFHPitch?.rotation = -45.0f
//                }
                }
            }
        })

        commonViewModel.roll.observe(viewLifecycleOwner, Observer { it ->
            currentPosRoll = it

            var animationSpeed: Long = 0
            accIntervalCtr++
            val tEnd = System.currentTimeMillis()
            val tDelta: Long = tEnd - accTimeStart
//            if (tDelta > 1000) {
            animationSpeed = tDelta / accIntervalCtr
            if (animationSpeed > 500) animationSpeed = 500
            if (isAnimateRoll) {

                if (!isZoom && it <= 40.0 && it >= -40.0) {
                    animationSpeed = 600
                } else if (isZoom && it <= 4.0 && it >= -4.0) {
                    animationSpeed = 600
                } else {
                    animationSpeed = 200
                }

                fragmentHomeBinding.viewRoundTop.setAnimationSpeed(animationSpeed.toInt())
//                fragmentHomeBinding.viewRoundTop.setAnimationSpeed(900)
                fragmentHomeBinding.viewRoundTop.setValue(it)
            } else {
                isAnimateRoll = true
                fragmentHomeBinding.viewRoundTop.setValue(preference.getFloatData(EXTRA_CURRENT_POS_ROLL))
            }
            accTimeStart = tEnd
            accIntervalCtr = 0
//            }

            if (isZoom) {
                currentPosRoll *= 10
            }
            preference.setFloatData(EXTRA_CURRENT_POS_ROLL, currentPosRoll)
            if (isZoom) {
                if (it <= 4.5 && it >= -4.5) {
                    fragmentHomeBinding.txtFHRoll.clearAnimation()
                    fragmentHomeBinding.tvTiltHeightTop.clearAnimation()
                    fragmentHomeBinding.txtFHRoll.setText(Math.abs(it).toString() + "\u00B0")
                    fragmentHomeBinding.tvTiltHeightTop.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    fragmentHomeBinding.txtFHRoll.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                } else {
                    val anim: Animation = AlphaAnimation(0.0f, 1.0f)
                    anim.duration = 50 //You can manage the blinking time with this parameter
                    anim.startOffset = 20
                    anim.repeatMode = Animation.REVERSE
                    anim.repeatCount = Animation.INFINITE

                    fragmentHomeBinding.txtFHRoll.text = "...."
                    fragmentHomeBinding.txtFHRoll.animation = anim
                    fragmentHomeBinding.tvTiltHeightTop.animation = anim
                    fragmentHomeBinding.tvTiltHeightTop.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    fragmentHomeBinding.txtFHRoll.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                }
            } else {
                if (it <= 45.0 && it >= -45.0) {
                    fragmentHomeBinding.txtFHRoll.clearAnimation()
                    fragmentHomeBinding.tvTiltHeightTop.clearAnimation()
                    fragmentHomeBinding.txtFHRoll.setText(Math.abs(it).toString() + "\u00B0")
                    fragmentHomeBinding.tvTiltHeightTop.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    fragmentHomeBinding.txtFHRoll.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                } else {
                    val anim: Animation = AlphaAnimation(0.0f, 1.0f)
                    anim.duration = 50 //You can manage the blinking time with this parameter
                    anim.startOffset = 20
                    anim.repeatMode = Animation.REVERSE
                    anim.repeatCount = Animation.INFINITE

                    fragmentHomeBinding.txtFHRoll.text = "...."
                    fragmentHomeBinding.txtFHRoll.animation = anim
                    fragmentHomeBinding.tvTiltHeightTop.animation = anim
                    fragmentHomeBinding.tvTiltHeightTop.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    fragmentHomeBinding.txtFHRoll.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                }
            }
        })

        commonViewModel.zeroCallibration.observe(requireActivity(), object : Observer<Boolean> {
            override fun onChanged(t: Boolean?) {
                if (t == true) {

                    currentPosPitchForZeroCal = currentPosPitch
                    currentPosRollForZeroCal = currentPosRoll

                    val messageBytes = byteArrayOf(0x02.toByte())

                    commonViewModel.hitCommandFor.value = EXTRA_ZERO_CALIBRATION
                    Log.e("ZERO_CALI_DEBUG", "hitCommandFor_value_passed")

                    mBluetoothLeService?.writeCharacteristic(
                        messageBytes,
                        Attributes.UUID_HM11_CHARACTERISTIC_CMD
                    )

                    commonViewModel.zeroCallibration.value = false

                    Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                        override fun run() {
                            mBluetoothLeService?.readCMDCharacteristic(Attributes.UUID_HM11_CHARACTERISTIC_CMD)
                        }
                    }, 500)
                }
            }
        })

        commonViewModel.zeroCallibrationSuccess.observe(
            requireActivity(),
            object : Observer<Boolean> {
                override fun onChanged(t: Boolean?) {
                    if (t!!) {
                        commonViewModel.zeroCalibCounter++
                        Log.e("zeroCalibCounter", commonViewModel.zeroCalibCounter.toString())
                        if (commonViewModel.zeroCalibCounter == 1) {
                            var toast: Toast? = null
                            if (toast == null || toast.view!!.windowVisibility != View.VISIBLE || !toast.view!!.isShown) {
                                toast = Toast.makeText(requireContext(), R.string.zero_calibration_success, Toast.LENGTH_SHORT)
                                toast.show()
                                // Make toast null after show
                            }
                        }

//                        Toast.makeText(
//                            requireContext(),
//                            R.string.zero_calibration_success,
//                            Toast.LENGTH_LONG
//                        ).show()
                        Log.e("ZERO_CALI_DEBUG", "TOAST_SHOWED")
                        commonViewModel.zeroCallibrationSuccess.value = false
                    }
                }
            })

        commonViewModel.bluetoothService.observe(viewLifecycleOwner, Observer {
            mBluetoothLeService = it
        })

        commonViewModel.rssiValue.observe(viewLifecycleOwner, Observer {
            when (it) {
                0 -> {
                    fragmentHomeBinding.ivBluetoothStrength.setImageResource(R.drawable.blutooth_disconnect)
                }
                in 1 downTo -65 -> {
                    if (fragmentHomeBinding.lnrFHNoDataFound.visibility != View.VISIBLE)
                        fragmentHomeBinding.ivBluetoothStrength.setImageResource(R.drawable.bluetooth_high)
                }
                in -66 downTo -80 -> {
                    if (fragmentHomeBinding.lnrFHNoDataFound.visibility != View.VISIBLE)
                        fragmentHomeBinding.ivBluetoothStrength.setImageResource(R.drawable.bluetooth_medium)
                }
                in -81 downTo -200 -> {
                    if (fragmentHomeBinding.lnrFHNoDataFound.visibility != View.VISIBLE)
                        fragmentHomeBinding.ivBluetoothStrength.setImageResource(R.drawable.bluetooth_low)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
//        requireContext().registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        homeFragmentViewModel.setAllHideAndShowView(requireActivity())
    }

    override fun onPause() {
        super.onPause()
//        requireContext().unregisterReceiver(receiver)
    }

    override fun onDestroy() {
        homeFragmentViewModel.enableHitch.removeObservers(this)
        super.onDestroy()
        preference = Preference(requireContext())
        preference.setFloatData(EXTRA_CURRENT_POS_ROLL, 0f)
        preference.setFloatData(EXTRA_CURRENT_POS_PITCH, 0f)
        Log.e("destroycall", "destroy")
//        fragmentHomeBinding.unbind()
    }
}