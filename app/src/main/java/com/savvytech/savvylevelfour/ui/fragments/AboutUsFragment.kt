package com.savvytech.savvylevelfour.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.savvytech.savvylevelfour.R
import com.savvytech.savvylevelfour.common.EXTRA_FIRMWARE_VERSION
import com.savvytech.savvylevelfour.databinding.FragmentAboutusBinding
import com.savvytech.savvylevelfour.viewmodels.AboutUsViewModel
import com.savvytech.savvylevelfour.viewmodels.CommonViewModel

class AboutUsFragment : Fragment() {

    lateinit var binding: FragmentAboutusBinding
    lateinit var aboutUsVM: AboutUsViewModel

    lateinit var commonViewModel: CommonViewModel
    var mBluetoothLeService: com.savvytech.savvylevelfour.common.BluetoothLeService? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_aboutus, container, false)
        init()
        setObserver()
        return binding.root
    }

    private fun setObserver() {
        commonViewModel.bluetoothService.observe(viewLifecycleOwner, Observer {
            mBluetoothLeService = it

            getFirmwareVersion()
        })

        commonViewModel.firmwareVersion.observe(viewLifecycleOwner, Observer {
            binding.tvDeviceVersionAboutUs.text = it
        })
    }

    private fun init() {
        aboutUsVM = ViewModelProvider(this).get(AboutUsViewModel::class.java)
        binding.lifecycleOwner = this

        binding.aboutsUsVM = aboutUsVM

        commonViewModel = activity?.run {
            ViewModelProvider(this).get(CommonViewModel::class.java)
        }!!

        commonViewModel.isHomeFragment = false

//        binding.tvDeviceVersionAboutUs.text = commonViewModel.firmwareVersion.value

        aboutUsVM.setVersionCode(binding.tvAppVersion)
    }

    private fun getFirmwareVersion() {
        val messageBytes = byteArrayOf(0x03.toByte())

        commonViewModel.hitCommandFor.value = EXTRA_FIRMWARE_VERSION

        mBluetoothLeService?.writeCharacteristic(messageBytes, com.savvytech.savvylevelfour.common.Attributes.UUID_HM11_CHARACTERISTIC_CMD)

        Handler(Looper.getMainLooper()).postDelayed({ mBluetoothLeService?.readCMDCharacteristic(com.savvytech.savvylevelfour.common.Attributes.UUID_HM11_CHARACTERISTIC_CMD) }, 500)
    }

    override fun onDestroy() {
        super.onDestroy()
//        binding.unbind()
    }
}