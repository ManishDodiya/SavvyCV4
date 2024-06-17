package com.savvytech.savvylevelfour.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.savvytech.savvylevelfour.common.EXTRA_HOME

class CommonViewModel : ViewModel() {

    var animationSpeed: MutableLiveData<Float> = MutableLiveData(0f)
    var pitch: MutableLiveData<Float> = MutableLiveData()
    var roll: MutableLiveData<Float> = MutableLiveData()
    var batt: MutableLiveData<Float> = MutableLiveData()
    var errorCode: MutableLiveData<String> = MutableLiveData()
    var hitCommandFor: MutableLiveData<String> = MutableLiveData("")
    var bluetoothService: MutableLiveData<com.savvytech.savvylevelfour.common.BluetoothLeService> = MutableLiveData()
    var zeroCallibration: MutableLiveData<Boolean> = MutableLiveData(false)
    var zeroCallibrationSuccess: MutableLiveData<Boolean> = MutableLiveData(false)
    var lossBlutoothConenction: MutableLiveData<String> = MutableLiveData("")
    var firmwareVersion: MutableLiveData<String> = MutableLiveData("")
    var onChangeOrientation: MutableLiveData<Boolean> = MutableLiveData()
    var viewRoundTop: MutableLiveData<Char> = MutableLiveData()
    var isDeviceListVisible: MutableLiveData<Boolean> = MutableLiveData()
    var rssiValue: MutableLiveData<Int> = MutableLiveData()
    var isHomeFragment: Boolean = false
    var isDeviceConnected: Boolean = false
    var currentFragment: MutableLiveData<String> = MutableLiveData("")
    var aniamtionLive: LiveData<Float> = animationSpeed
    var showTutorial: MutableLiveData<String> = MutableLiveData("")
    var fromChangeDevice: MutableLiveData<String> = MutableLiveData("")
    var isBattHideBeforeTutorial = false

    //    var isDialogShow = false
    var zeroCalibCounter = 0

    fun storeAnimated(animValue: Float) {
        animationSpeed.value = animValue
//        Log.e("viewmodel", animValue.toString() + " ***")
    }

    fun passBluetoothService(bluetoothLeService: com.savvytech.savvylevelfour.common.BluetoothLeService) {
        bluetoothService.value = bluetoothLeService
    }

    fun getAnimationSpeed(): LiveData<Float> {
        return animationSpeed
    }

    override fun onCleared() {
        currentFragment.value = EXTRA_HOME
        super.onCleared()
    }
}