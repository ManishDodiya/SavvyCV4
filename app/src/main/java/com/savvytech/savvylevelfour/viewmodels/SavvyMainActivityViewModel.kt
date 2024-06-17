package com.savvytech.savvylevelfour.viewmodels

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.savvytech.savvylevelfour.R
import com.savvytech.savvylevelfour.common.EXTRA_ABOUT_US
import com.savvytech.savvylevelfour.common.EXTRA_HOME
import com.savvytech.savvylevelfour.common.EXTRA_SCREEN_NUMBER
import com.savvytech.savvylevelfour.common.EXTRA_SETTING

class SavvyMainActivityViewModel : ViewModel() {

    lateinit var navController: NavController
    lateinit var navHomeFrag: NavHostFragment

    var batt: MutableLiveData<Float> = MutableLiveData()

    var aboutUsImage: MutableLiveData<Int> = MutableLiveData()
    var aboutUsText: MutableLiveData<Int> = MutableLiveData()
    var homeImage: MutableLiveData<Int> = MutableLiveData()
    var homeText: MutableLiveData<Int> = MutableLiveData()
    var settingImage: MutableLiveData<Int> = MutableLiveData()
    var settingText: MutableLiveData<Int> = MutableLiveData()

    var isShowNotConnectedDialog: MutableLiveData<Boolean> = MutableLiveData()
    lateinit var preference: com.savvytech.savvylevelfour.common.Preference
    lateinit var commonViewModel: CommonViewModel

    fun setCommonViewModels(commonViewModel: CommonViewModel) {
        this.commonViewModel = commonViewModel
    }

    fun sendNavGraph(navContainer: FragmentContainerView, conAsBottomView: ConstraintLayout, context: Context) {
        preference = com.savvytech.savvylevelfour.common.Preference(context)
        navHomeFrag = (navContainer.context as AppCompatActivity).supportFragmentManager.findFragmentById(navContainer.id) as NavHostFragment
        navController = navHomeFrag.navController

        allRefresh(navContainer.context)
        changeHomeButton(navContainer.context)
        manageCurrentFragment(conAsBottomView)
    }

    fun goToHome() {
        commonViewModel.currentFragment.value = EXTRA_HOME
        if (navController.currentDestination?.id != R.id.fragHome) {
            val bundle = bundleOf()
            bundle.putInt(EXTRA_SCREEN_NUMBER, 1)
            navController.navigate(R.id.fragHome, bundle)
        }
    }

    fun goToAboutUs() {
        commonViewModel.currentFragment.value = EXTRA_ABOUT_US
        setNavigation(R.id.fragAboutUs)
    }

    fun goToSetting() {
        if (commonViewModel.isDeviceConnected) {
            commonViewModel.currentFragment.value = EXTRA_SETTING
            setNavigation(R.id.fragSetting)
        } else {
            isShowNotConnectedDialog.value = true
        }
    }

    fun changeDevice() {
        val bundle = bundleOf()
        bundle.putInt(EXTRA_SCREEN_NUMBER, 1)
        setNavigationWithBundle(R.id.fragSplash, bundle)
//        setNavigationWithBundle(R.id.fragSearchingDevice, bundle)
    }

    fun allRefresh(context: Context) {
        aboutUsImage.value = ContextCompat.getColor(context, R.color.gray_img_color)
        aboutUsText.value = ContextCompat.getColor(context, R.color.gray_img_color)

        settingImage.value = ContextCompat.getColor(context, R.color.gray_img_color)
        settingText.value = ContextCompat.getColor(context, R.color.gray_img_color)

        homeImage.value = ContextCompat.getColor(context, R.color.gray_999)
        homeText.value = ContextCompat.getColor(context, R.color.gray_img_color)
    }

    fun changeHomeButton(v: Context) {
        homeImage.value = ContextCompat.getColor(v, R.color.blue_0b4)
        homeText.value = ContextCompat.getColor(v, R.color.black)
    }

    fun changeAboutButton(v: Context) {
        aboutUsImage.value = ContextCompat.getColor(v, R.color.black)
        aboutUsText.value = ContextCompat.getColor(v, R.color.black)
    }

    fun changeSettingButton(v: Context) {
        settingImage.value = ContextCompat.getColor(v, R.color.black)
        settingText.value = ContextCompat.getColor(v, R.color.black)
    }

    fun setNavigation(fragHome: Int) {
        if (navController.currentDestination?.id != fragHome) {
            navController.navigate(fragHome)
        }
    }

    fun setNavigationWithBundle(fragId: Int, bundle: Bundle) {
        if (navController.currentDestination?.id != fragId) {
            navController.navigate(fragId, bundle)
        }
    }

    fun manageCurrentFragment(conAsBottomView: ConstraintLayout) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.fragHome -> {
                    allRefresh(conAsBottomView.context)

                    conAsBottomView.visibility = View.VISIBLE
                    changeHomeButton(conAsBottomView.context)
                }
                R.id.fragAboutUs -> {
                    allRefresh(conAsBottomView.context)

                    conAsBottomView.visibility = View.VISIBLE
                    changeAboutButton(conAsBottomView.context)
                }
                R.id.fragSetting -> {
                    allRefresh(conAsBottomView.context)

                    conAsBottomView.visibility = View.VISIBLE
                    changeSettingButton(conAsBottomView.context)
                }
                else -> {
                    conAsBottomView.visibility = View.GONE
                }
            }
        }
    }
}