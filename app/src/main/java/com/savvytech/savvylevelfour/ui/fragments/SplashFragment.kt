package com.savvytech.savvylevelfour.ui.fragments

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.savvytech.savvylevelfour.R
import com.savvytech.savvylevelfour.common.*
import com.savvytech.savvylevelfour.databinding.FragmentSplashBinding
import com.savvytech.savvylevelfour.ui.adapters.NearbyDevicesAdapter
import com.savvytech.savvylevelfour.ui.interfaces.SelectionInterfaces
import com.savvytech.savvylevelfour.ui.interfaces.SendStringInterfaces
import com.savvytech.savvylevelfour.viewmodels.CommonViewModel
import com.savvytech.savvylevelfour.viewmodels.SplashScreenViewModel
import java.util.*


class SplashFragment : Fragment() {

    var startScanDeviceHandler: Handler = Handler(Looper.getMainLooper())
    lateinit var runnable: Runnable

    var scannerDelay = 1000

    var clickOnConnectButton = false
    private var doubleBackToExitPressedOnce = false

    var scannerCount = 0

    lateinit var binding: FragmentSplashBinding
    lateinit var splashVM: SplashScreenViewModel

    var dialog: Dialog? = null

    var selectedBluetoothAddress: String = ""
    var selectedBluetoothName: String = ""

    var nearbyDevicesAdapter: NearbyDevicesAdapter = NearbyDevicesAdapter()

    lateinit var registerForActivityResult: ActivityResultLauncher<Array<String>>
    lateinit var registerForActivityResultBluetoth: ActivityResultLauncher<Array<String>>
    lateinit var mLunch: ActivityResultLauncher<IntentSenderRequest>
    lateinit var popDialog: Dialog

    var mBluetoothAdapter: BluetoothAdapter? = null
    var mBluetoothLeService: BluetoothLeService? = null
    lateinit var mBluetoothScanner: BluetoothLeScanner
    private var mLeDevices: ArrayList<MyBluetoothDevice>? = null
    lateinit var commonViewModel: CommonViewModel

    var goToHome = false
    var isConnected = false
    var isSearchFromInit = true

    var againStartScanner = 0
    var type = 0
    var screenNumber = 0
    var hitCommandFor = ""
    var currentFragment = ""
    var isFromChangeDevice = ""
    var clickedOnCancel = false
    var isConnectBluetoothCalled = false

    lateinit var preference: Preference

    var isFragmentVisible = false

    private var isReadRssi = true
    private var readRSSI: Thread? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        retainInstance = true

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_splash, container, false)
        Log.e("onCreateView", "onCreateView")
        init()
        setObservable()
        setListener()
        onBackPresser()
        return binding.root
    }

    private fun onBackPresser() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(),
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (screenNumber == 1 && commonViewModel.currentFragment.value.toString()
                            .equals(EXTRA_HOME)
                    ) {
                        if (doubleBackToExitPressedOnce) {
                            requireActivity().finish()
                            return
                        }
                        requireActivity().toast(getString(R.string.pleaseClickBackPress))

                        doubleBackToExitPressedOnce = true
                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            doubleBackToExitPressedOnce = false
                        }, 2000)
                    } else if (screenNumber == 1) {
                        findNavController().navigate(R.id.splashToSetting)
//                        findNavController().popBackStack()
                    } else {
                        if (binding.relFSNearByDevice.visibility == View.VISIBLE) {
                            if (doubleBackToExitPressedOnce) {
                                requireActivity().finish()
                                return
                            }
                            requireActivity().toast(getString(R.string.pleaseClickBackPress))

                            doubleBackToExitPressedOnce = true
                            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                                doubleBackToExitPressedOnce = false
                            }, 2000)
                        }
                    }
                }
            })
    }

    private fun init() {
        splashVM = ViewModelProvider(this).get(SplashScreenViewModel::class.java)
        commonViewModel = activity?.run {
            ViewModelProvider(this).get(CommonViewModel::class.java)
        }!!

        splashVM.init(requireActivity(), commonViewModel)
        commonViewModel.isHomeFragment = false

        preference = Preference(requireActivity())
        popDialog = Dialog(requireActivity())

        isFromChangeDevice = ""
        preference.getData(Preference.DEVICE_ADDRESS)?.let { Log.e("DEVICE_ADDRESS", it) }
        screenNumber = arguments?.getInt(EXTRA_SCREEN_NUMBER) ?: 0
        Log.e("initScreenNumber", screenNumber.toString())

        when (screenNumber) {
            0 -> {
                splashVM.splashDelay()
            }
            1 -> {
                if (!clickedOnCancel) {
                    selectedBluetoothAddress = ""
                }
                againStartScanner = 0
                getRuntimePermission()
            }
            2 -> {
                selectedBluetoothAddress = ""
                againStartScanner = 0
                getRuntimePermission()
            }
        }
        binding.splashScreenVM = splashVM
        binding.lifecycleOwner = this

        dialog = Dialog(requireContext())
    }

    private fun setObservable() {
        splashVM.isFinishedTime.observe(viewLifecycleOwner, { timeOverSplash ->
            if (timeOverSplash) {
                if (preference.getBooleanData(Preference.isFirstFragmentLoaded)) {
                    againStartScanner = 0
                    getRuntimePermission()
                } else {
                    binding.relSplash.visibility = View.GONE
                    binding.relSearchingDevices.visibility = View.GONE
                    binding.relFSNearByDevice.visibility = View.GONE
                    binding.relSpConnectedView.visibility = View.GONE
                    binding.relStartConnecting.visibility = View.GONE
                    binding.clFirst.visibility = View.VISIBLE

                    binding.tvPrivacyTerms.makeLinks(
                        Pair(EXTRA_PRIVACY_POLICY, View.OnClickListener {
                            splashVM.clickOnPrivacyRead(binding.tvPrivacyTerms)
                        }),
                        Pair(EXTRA_TERMS_AND_CONDITIONS, View.OnClickListener {
                            splashVM.clickOnEnduserAgreement(binding.tvPrivacyTerms)
                        })
                    )
                }
            }
        })

        splashVM.getView().observe(viewLifecycleOwner, { t ->
            binding.relNoDeviceFound.visibility = View.GONE
            if (t!!) {
                if (isSearchFromInit) {
                    stop()
                }
                scannerCount = 0
                againStartScanner = 1
                getRuntimePermission()
            }
        })

        splashVM.onCancelClick.observe(viewLifecycleOwner, object : Observer<String> {
            override fun onChanged(t: String) {
                if (t.isNotEmpty()) {
                    commonViewModel.isDeviceListVisible.value = true
                    selectedBluetoothAddress =
                        preference.getData(Preference.DEVICE_ADDRESS).toString()
                    selectedBluetoothName = preference.getData(Preference.DEVICE_NAME).toString()

                    clickedOnCancel = true
                    scannerCount = 0
                    againStartScanner = 1
                    getRuntimePermission()
                }
            }
        })

        commonViewModel.currentFragment.observe(viewLifecycleOwner, Observer {
            currentFragment = it
        })

        commonViewModel.hitCommandFor.observe(requireActivity(), Observer {
            hitCommandFor = it
            Log.e("ZERO_CALI_DEBUG", "hitCommandFor_value_assigned")
        })

        commonViewModel.fromChangeDevice.observe(viewLifecycleOwner, Observer {
            isFromChangeDevice = it
        })
    }

    fun setListener() {
        binding.relConnect.setOnClickListener {
            if (selectedBluetoothAddress.equals("")) {
                Toast.makeText(requireContext(), R.string.select_device, Toast.LENGTH_LONG).show()
            } else {
                scannerCount = 0

//                isFromChangeDevice = ""
                binding.relFSNearByDevice.visibility = View.VISIBLE
                commonViewModel.isDeviceListVisible.value = true
//          `      binding.relStartConnecting.visibility = View.VISIBLE
                binding.prbFSLoader.visibility = View.VISIBLE
                binding.txtFsStatusView.text = getString(R.string.pleaseWait)
                againStartScanner = 2
                getRuntimePermission()
            }
        }
        binding.viewHeaderDeviceList.setOnClickListener {
            selectedBluetoothAddress = ""
            scannerCount = 0
            againStartScanner = 1
            getRuntimePermission()
        }

        binding.tvScanDevice.setOnClickListener {
            preference.setBoolenData(Preference.isFirstFragmentLoaded, true)
            againStartScanner = 0
            getRuntimePermission()
        }
    }

    private fun bluetoothScannerStartScan() {
        mLeDevices = ArrayList()

        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build()
        val filters = ArrayList<ScanFilter>()
        val serviceUUID = ParcelUuid.fromString(Attributes.UUID_HM11_SERVICE_SAVVY4.toString())
        val uuidFilter = ScanFilter.Builder().setServiceUuid(serviceUUID).build()
        filters.add(uuidFilter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        } else {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        mBluetoothScanner.startScan(filters, settings, mScanCallback)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private val mScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            Log.e("ON_SCAN_RESULT", "FAILED")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // check if scanned device is a SavvyLevel
            Log.e("SCAN_SCANNER_COUNT", "***   " + scannerCount.toString())
            if (scannerCount <= 10) {
//                if (mLeDevices!!.size == 0 || isFromChangeDevice == EXTRA_CHANGE_DEVICE) {
                if (isSavvyLevelServiceUUID(result)) {
                    Log.e("IS_SAVVY_DEVICE", "YES")
                    if (clickedOnCancel && preference.getData(Preference.DEVICE_ADDRESS).toString().equals(selectedBluetoothAddress) && !isConnectBluetoothCalled) {
                        isConnectBluetoothCalled = true
                        selectedBluetoothAddress = preference.getData(Preference.DEVICE_ADDRESS).toString()
                        selectedBluetoothName = preference.getData(Preference.DEVICE_NAME).toString()
                        connectBluetooth()
                    } else {
                        val mDevice = MyBluetoothDevice()
                        mDevice.device = result.device
                        mDevice.rssi = result.rssi

                        if (selectedBluetoothAddress.equals("")) {
                            if (preference.getBooleanData(Preference.isToolTipCompleted)) {
                                mDevice.isSelected = false
                            } else {
//                                mDevice.isSelected = true
                                mDevice.isSelected = false
                            }
                            val itr = mLeDevices!!.iterator()
                            // check if device is already listed
                            while (itr.hasNext()) {
                                val leDevice = itr.next()
                                if (mDevice.device.equals(leDevice.device)) {
                                    // device is listed so remove coz we will re-list device with updated rssi
                                    itr.remove()
                                }
                            }
                            mLeDevices!!.add(mDevice)
                            Log.e("SCAN_DEVICE_ADDED", "DEVICE_ADDED")
//                            selectedBluetoothName = mDevice.device.name
//                            selectedBluetoothAddress = mDevice.device.address
//                            binding.tvDeviceName.setText(selectedBluetoothName)
                        } else {
                            if (isFromChangeDevice != EXTRA_CHANGE_DEVICE && !isConnectBluetoothCalled) {
                                clickOnConnectButton = true
                                isConnectBluetoothCalled = true
                                connectBluetooth()
                            } else if (isFromChangeDevice == EXTRA_CHANGE_DEVICE && !isConnectBluetoothCalled) {
                                clickOnConnectButton = true
                                isConnectBluetoothCalled = true
                                connectBluetooth()
                            }
                        }
                    }
                } else {
                    Log.e("IS_SAVVY_DEVICE", "NO${scannerCount}")
                    if (scannerCount > 10) {
                        Log.e("SCAN_DEVICE_NOT_FOUND", "DEVICE_NOT_FOUND")
                        whenNoDataFound()
                    }
                }
//                }
            } else {
                if (mLeDevices!!.size > 0) {

                    Log.e("SCAN_SCANNER_COUNT_", "SCANNER_COUNT_")

                    binding.rvNearbyDeviceList.layoutManager = LinearLayoutManager(requireContext())
                    binding.rvNearbyDeviceList.adapter = nearbyDevicesAdapter

                    // sort list by rssi signal strength
                    Collections.sort(mLeDevices, compareRssi)
                    Collections.reverse(mLeDevices)

                    val myBluetoothDevice = mLeDevices!!.get(0)
                    for (i in 0 until mLeDevices!!.size) {
//                        if (i != 0) {
//                            if (myBluetoothDevice.isSelected) {
//                                mLeDevices!!.get(i).isSelected = false
//                            }
//                        }
                        // Always selected closest device
                        if (i == 0) {
                            mLeDevices!!.get(i).isSelected = true
                        } else {
                            mLeDevices!!.get(i).isSelected = false
                        }
                    }

                    requireActivity().runOnUiThread { // add device to device list
                        nearbyDevicesAdapter.addList(
                            requireContext(),
                            mLeDevices!!,
                            object : SendStringInterfaces {
                                override fun sendString(deviceName: String, deviceAddress: String) {
                                    selectedBluetoothName = deviceName
                                    selectedBluetoothAddress = deviceAddress
                                    binding.tvDeviceName.setText(selectedBluetoothName)
                                    Log.e("DEVICE_NAME", "***   " + deviceName)
                                    Log.e("DEVICE_ADDRESS", "***   " + deviceAddress)
                                }
                            })
                        nearbyDevicesAdapter.notifyDataSetChanged()
                    }

                    Log.e("DEVICES_FOUND", "DEVICE_FOUND")

                    binding.relSearchingDevices.visibility = View.GONE
                    binding.relFSNearByDevice.visibility = View.VISIBLE
                    commonViewModel.isDeviceListVisible.value = true
                    binding.relCancel.visibility = if (screenNumber == 1) View.VISIBLE else View.GONE
                    stop()
                } else {
                    if (!clickOnConnectButton) {
                        if (!clickedOnCancel) {
                            selectedBluetoothAddress = ""
                        }
                        binding.prbFSLoader.visibility = View.GONE
                        binding.txtFsStatusView.text = getString(R.string.connect)
                        binding.relSearchingDevices.visibility = View.VISIBLE
                        binding.relNoDeviceFound.visibility = View.VISIBLE
                    }
                    whenNoDataFound()
                }
            }
        }
    }

    protected var compareRssi: Comparator<MyBluetoothDevice?> =
        object : Comparator<MyBluetoothDevice?> {
            override fun compare(p0: MyBluetoothDevice?, p1: MyBluetoothDevice?): Int {
                return p0!!.value.compareTo(p1!!.value)
            }
        }

    private fun whenNoDataFound() {
        scannerCount = 0
        binding.relBluetooth.visibility = View.GONE
        if (!clickedOnCancel) {
            selectedBluetoothAddress = ""
        }
        stop()
        if (splashVM.isClickedOnSearchAgain) {
            splashVM.tvQuestionsVisible.value = View.VISIBLE
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        } else {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        mBluetoothScanner.stopScan(mScanCallback)
    }

    private fun isSavvyLevelServiceUUID(scanResult: ScanResult): Boolean {
        var result = false
        if (scanResult.scanRecord!!.serviceUuids != null) {
            val parcelUuids = scanResult.scanRecord!!.serviceUuids
            for (i in parcelUuids.indices) {
                val serviceUUID = parcelUuids[i].uuid
//                if (serviceUUID == Attributes.UUID_HM11_SERVICE) result =
//                if (serviceUUID == Attributes.UUID_HM11_SERVICE || serviceUUID == Attributes.UUID_HM11_SERVICE_SAVVY4) {
//                    result = true
//                }
                if (serviceUUID == Attributes.UUID_HM11_SERVICE_SAVVY4) result =
                    true
            }
        }
        return result
    }

    // Bluetooth --------------------------------------------------------------
    val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).getService()
            if (!mBluetoothLeService!!.initialize()) {
            } else {
                try {
                    mBluetoothLeService!!.disconnect()
                } catch (e: Exception) {
                    Log.e("BLUETOOTH_SERVICE_NULL", "***   " + e.message.toString())
                }

//                if (mBluetoothLeService != null) {
//                    mBluetoothLeService!!.disconnect()
//                }
                commonViewModel.passBluetoothService(mBluetoothLeService!!)

                if (goToHome) {
                    selectedBluetoothName = preference.getData(Preference.DEVICE_NAME).toString()

                    if (preference.getData(Preference.DEVICE_ADDRESS).equals("") && preference.getDeviceAddressTemp(Preference.DEVICE_ADDRESS_TEMP).equals("")) {
                        Toast.makeText(requireContext(), R.string.select_atleast_1_device, Toast.LENGTH_LONG).show()
                        return
                    } else {
                        if (preference.getData(Preference.DEVICE_ADDRESS).equals("")) {
                            if (mBluetoothLeService != null) mBluetoothLeService!!.connect(preference.getDeviceAddressTemp(Preference.DEVICE_ADDRESS_TEMP), "")
                        } else {
                            if (mBluetoothLeService != null) mBluetoothLeService!!.connect(preference.getData(Preference.DEVICE_ADDRESS), "")
                        }
                    }
                    setTenSecHandler()
                } else {
                    connectAndStartScanner()
                }
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    private fun setTenSecHandler() {
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                if (!isConnected && isFragmentVisible) {

                    isSearchFromInit = false

                    binding.prbFSLoader.visibility = View.GONE
                    binding.relSplash.visibility = View.GONE
                    binding.relBluetooth.visibility = View.GONE
                    binding.txtFsStatusView.text = getString(R.string.connect)
                    binding.relSearchingDevices.visibility = View.VISIBLE
                    binding.relNoDeviceFound.visibility = View.GONE
                }
            }
        }, 10000)
    }

    private fun connectAndStartScanner() {
        goToHome = false

        if (isSearchFromInit) {
            mBluetoothLeService!!.connect("99:00:00:00:00:00", "")

            if (mBluetoothAdapter!!.bluetoothLeScanner != null) {
                mBluetoothScanner = mBluetoothAdapter!!.bluetoothLeScanner

                bluetoothScannerStartScan()
            } else {
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    try {
                        if (mBluetoothAdapter == null) {
                            val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                            mBluetoothAdapter = bluetoothManager.adapter
                            mBluetoothScanner = mBluetoothAdapter!!.bluetoothLeScanner
                        } else {
                            mBluetoothScanner = mBluetoothAdapter!!.bluetoothLeScanner
                        }
                        bluetoothScannerStartScan()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 6000)
            }
        } else {
            goToHome = true
            selectedBluetoothName = preference.getData(Preference.DEVICE_NAME).toString()
//            mBluetoothLeService!!.connect(preference.getData(Preference.DEVICE_ADDRESS), "")
            if (preference.getData(Preference.DEVICE_ADDRESS)
                    .equals("") && preference.getDeviceAddressTemp(Preference.DEVICE_ADDRESS_TEMP)
                    .equals("")
            ) {
                Toast.makeText(
                    requireContext(),
                    R.string.select_atleast_1_device,
                    Toast.LENGTH_LONG
                ).show()
                return
            } else {
                if (preference.getData(Preference.DEVICE_ADDRESS).equals("")) {
                    if (mBluetoothLeService != null) mBluetoothLeService!!.connect(
                        preference.getDeviceAddressTemp(Preference.DEVICE_ADDRESS_TEMP), ""
                    )
                } else {
                    if (mBluetoothLeService != null) mBluetoothLeService!!.connect(
                        preference.getData(Preference.DEVICE_ADDRESS), ""
                    )
                }
            }
            setTenSecHandler()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        binding.unbind()
    }

    override fun onResume() {
        super.onResume()

        isFragmentVisible = true
        requireActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
//        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
//        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
//        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        requireActivity().registerReceiver(mBluetoothReceiver, filter)

        goToHome = false

        when (type) {
            1 -> {
                runtimePermission()
            }
            2 -> {
                if (!checkPermisssion()) {
                    //Utils.showAlertPermision(requireActivity(), dialog!!)
                    Utils.showLocationAlertPermision(requireActivity(), dialog!!)
                } else {
                    againStartScanner = 0
                    checkLocationEnableOrNot()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isFragmentVisible = false
    }

    private val mBluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action != null && action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {

                        if (screenNumber != 1) {
                            val bluetoothManager = activity?.run {
                                this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                            }
                            val bluetoothAdapter = bluetoothManager?.adapter

                            if (bluetoothAdapter != null) {
                                if (!bluetoothAdapter.isEnabled) {

                                    commonViewModel.lossBlutoothConenction.value = EXTRA_DISCONNECT

                                    Utils.customeDialog(popDialog,
                                        requireActivity(),
                                        context?.getString(R.string.blutoothPermission).toString(),
                                        context?.getString(R.string.turn_on_bluetooth).toString(),
                                        context?.getString(R.string.allow).toString(),
                                        commonViewModel,
                                        object : SelectionInterfaces {
                                            override fun clickOnView() {
                                                if (!checkPermissionForBluetoothScan()) {
                                                    runtimeBluetoothScanPermission()
                                                } else {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                                            return
                                                        }
                                                    } else {
                                                        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                                                            return
                                                        }
                                                    }

                                                    bluetoothAdapter.enable()

                                                    if (binding.relFSNearByDevice.visibility != View.VISIBLE) {
                                                        if (bluetoothAdapter.isEnabled) {
                                                            if (!preference.getData(Preference.DEVICE_ADDRESS).toString().equals("") || !preference.getData(Preference.DEVICE_ADDRESS).toString().isEmpty() ||
                                                                preference.getData(Preference.DEVICE_ADDRESS).toString() != null
                                                            ) {
                                                                selectedBluetoothAddress = preference.getData(Preference.DEVICE_ADDRESS).toString()
                                                            }
                                                            if (mBluetoothLeService != null) mBluetoothLeService!!.connect(selectedBluetoothAddress, EXTRA_BLUETOOTH_ENABLE)
                                                            goToHome = false
                                                        } else {
                                                            Handler(Looper.getMainLooper()).postDelayed(
                                                                {
                                                                    if (!preference.getData(Preference.DEVICE_ADDRESS).toString().equals("") || !preference.getData(Preference.DEVICE_ADDRESS).toString().isEmpty() ||
                                                                        preference.getData(Preference.DEVICE_ADDRESS).toString() != null
                                                                    ) {
                                                                        selectedBluetoothAddress = preference.getData(Preference.DEVICE_ADDRESS).toString()
                                                                    }
                                                                    if (mBluetoothLeService != null) mBluetoothLeService!!.connect(selectedBluetoothAddress, EXTRA_BLUETOOTH_ENABLE)
                                                                    goToHome = false
                                                                },
                                                                2000
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            override fun clickOnClearErrorCode() {

                                            }
                                        })
                                } else {
                                    commonViewModel.lossBlutoothConenction.value = EXTRA_DISCONNECT
                                }
                            } else {
                                Log.e("Error", "ERROR")
                                commonViewModel.lossBlutoothConenction.value = EXTRA_DISCONNECT
                            }
                        }
                    }
                    BluetoothAdapter.STATE_ON -> {
                        Log.e("BLUETOOTH_STATE", "ON")
//                        startSearchingDevice()
                    }
                }
            }
        }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        intentFilter.addAction(BluetoothLeService.ACTION_READ_RSSI)
        return intentFilter
    }

    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.e("ACTION_SPLASH", action!!)
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

                Log.e("CONNECTION", "DEVICE_CONNECTED")

                commonViewModel.isDeviceConnected = true

                if (!commonViewModel.lossBlutoothConenction.value.toString().equals(EXTRA_CONNECT)) {
                    commonViewModel.lossBlutoothConenction.value = EXTRA_CONNECT
                }
                if (currentFragment.equals(EXTRA_HOME)) {
                    commonViewModel.isDeviceListVisible.value = false
                } else {
                    commonViewModel.isDeviceListVisible.value = true
                }
                isConnected = true

                if (goToHome) {
                    goToHome = false
                } else {
                    if (selectedBluetoothAddress.isNotEmpty()) {
                        if (!selectedBluetoothAddress.equals(preference.getData(Preference.DEVICE_ADDRESS))) {
                            preference.setBattData(Preference.batteryStatus, true)
                            preference.setBoolenData(Preference.tiltHeight, true)
                            preference.setBoolenData(Preference.blockType, false)
                            preference.setSpanType(Preference.spanType, EXTRA_MM)
                            preference.setWheelToWheelData(Preference.wheelToWheel, "2100")
                            preference.setWheelToJockeyWheelData(Preference.wheelToJockeyWheel, "4500")
                            preference.setBlockTypeName(Preference.blockTypeName, EXTRA_OZI)
                            preference.setCustomeBlockValue(Preference.customBlockTypeValue, "25")
                            preference.setBoolenData(Preference.hitchEnabled, false)
                            preference.setBoolenData(Preference.isZoom, false)
                            preference.setFloatData(Preference.hitchValue, 0f)
                        }
                        preference.setData(Preference.DEVICE_ADDRESS, selectedBluetoothAddress)
                        preference.setData(Preference.DEVICE_NAME, selectedBluetoothName)
                    }
                }

                startReadRssi()

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

                Log.e("GATT_CONNECTION", "DISCONNECTED")

                commonViewModel.isDeviceConnected = false
                isConnected = false

                if (screenNumber != 1) {
                    val bluetoothManager = activity?.run {
                        this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                    }
                    val bluetoothAdapter = bluetoothManager?.adapter

                    if (bluetoothAdapter != null) {
                        if (!bluetoothAdapter.isEnabled) {
                            if (!commonViewModel.lossBlutoothConenction.value.toString().equals(EXTRA_DISCONNECT)) {
                                commonViewModel.lossBlutoothConenction.value = EXTRA_DISCONNECT
                            }

                            Utils.customeDialog(popDialog,
                                requireActivity(),
                                context.getString(R.string.blutoothPermission),
                                context.getString(R.string.turn_on_bluetooth),
                                context.getString(R.string.allow),
                                commonViewModel,
                                object : SelectionInterfaces {
                                    override fun clickOnView() {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                                return
                                            }
                                        } else {
                                            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                                                return
                                            }
                                        }
                                        bluetoothAdapter.enable()
                                        if (binding.relFSNearByDevice.visibility != View.VISIBLE) {
                                            if (bluetoothAdapter.isEnabled) {
                                                if (!preference.getData(Preference.DEVICE_ADDRESS).toString().equals("") || !preference.getData(Preference.DEVICE_ADDRESS).toString().isEmpty() || preference.getData(Preference.DEVICE_ADDRESS).toString() != null
                                                ) {
                                                    selectedBluetoothAddress = preference.getData(Preference.DEVICE_ADDRESS).toString()
                                                }
                                                if (mBluetoothLeService != null) mBluetoothLeService!!.connect(
                                                    selectedBluetoothAddress,
                                                    EXTRA_BLUETOOTH_ENABLE
                                                )
                                                goToHome = false
                                            } else {
                                                Handler(Looper.getMainLooper()).postDelayed({
                                                    if (!preference.getData(Preference.DEVICE_ADDRESS).toString().equals("") || !preference.getData(Preference.DEVICE_ADDRESS).toString().isEmpty() || preference.getData(Preference.DEVICE_ADDRESS)
                                                            .toString() != null
                                                    ) {
                                                        selectedBluetoothAddress = preference.getData(Preference.DEVICE_ADDRESS).toString()
                                                    }
                                                    if (mBluetoothLeService != null) mBluetoothLeService!!.connect(
                                                        selectedBluetoothAddress,
                                                        EXTRA_BLUETOOTH_ENABLE
                                                    )
                                                    goToHome = false
                                                }, 2000)
                                            }
                                        }
                                    }

                                    override fun clickOnClearErrorCode() {

                                    }
                                })
                        } else {
                            if (!commonViewModel.lossBlutoothConenction.value.toString().equals(EXTRA_DISCONNECT)) {
                                commonViewModel.lossBlutoothConenction.value = EXTRA_DISCONNECT
                            }
                        }
                    } else {
                        Log.e("Error", "ERROR")
                        if (!commonViewModel.lossBlutoothConenction.value.toString().equals(EXTRA_DISCONNECT)) {
                            commonViewModel.lossBlutoothConenction.value = EXTRA_DISCONNECT
                        }
                    }
                }
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // discover supported services and characteristics
                servicesDiscovered()
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // receive data packets and form an SDCoS sentence

                var uuid = ""
                uuid = intent.getStringExtra(EXTRA_UUID).toString()

                var data: Double = 0.0
                var dataString: String = ""
                if (uuid.contains("2d5c") || uuid.contains("2e24") || uuid.contains("2ee2")) {
                    dataString = intent.getStringExtra(EXTRA_EXTRA_DATA).toString()
                } else {
                    data = intent.getDoubleExtra(EXTRA_EXTRA_DATA, 0.0)
                }

//                currentTimeMillis = System.currentTimeMillis()
//                Log.i("Time Class ", " Time value in millisecinds $currentTimeMillis")
//
//                previousTimeMillis = currentTimeMillis

                if (binding.relFSNearByDevice.visibility != View.VISIBLE) {
                    if (uuid.contains("28d4")) {
                        var dataNew = ""
                        if (data < 0) {
                            data = Math.abs(data)
                            commonViewModel.pitch.value = data.toFloat()
                        } else {
                            if (data != -0.0) {
                                dataNew = "-" + data
                                commonViewModel.pitch.value = dataNew.toFloat()
                            } else {
                                commonViewModel.pitch.value = data.toFloat()
                            }
                        }
                    } else if (uuid.contains("29c4")) {
                        var dataNew = ""
                        if (data < 0) {
                            data = Math.abs(data)
                            commonViewModel.roll.value = data.toFloat()
                        } else {
                            if (data != -0.0) {
                                dataNew = "-" + data
                                commonViewModel.roll.value = dataNew.toFloat()
                            } else {
                                commonViewModel.roll.value = data.toFloat()
                            }
                        }
                    } else if (uuid.contains("2b4a")) {
                        commonViewModel.batt.value = data.toFloat()
                    } else if (uuid.contains("2e24")) {
                        Log.e("DATA_STRING_2E24", dataString)
                        if (hitCommandFor.equals(EXTRA_REQUEST_ERROR)) {
                            commonViewModel.errorCode.value = "0x" + dataString
                        } else if (hitCommandFor.equals(EXTRA_CLEAR_ERROR)) {
                            if (dataString.trim().equals("00")) {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.error_value_cleared,
                                    Toast.LENGTH_LONG
                                ).show()

                                hitCommandFor = ""
                            }
                            commonViewModel.errorCode.value = "0x" + dataString
                        } else if (hitCommandFor == EXTRA_ZERO_CALIBRATION) {
                            if (dataString.trim().equals("0A")) {
                                Log.e("ZERO_CALI_DEBUG", "DATA_AVAILABLE")
                                commonViewModel.zeroCallibrationSuccess.value = true
                            }
                        }
                    } else if (uuid.contains("2d5c")) {
                        Log.e("DATA_STRING_2D5C", dataString)
                        if (dataString.trim().equals("0A")) {
                            Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                                override fun run() {
                                    if (hitCommandFor.equals(EXTRA_FIRMWARE_VERSION)) {
                                        mBluetoothLeService?.readVersionCharacteristic(Attributes.UUID_HM11_CHARACTERISTIC_FIRM_VERSION)
                                    } else {
                                        mBluetoothLeService?.readStatusCharacteristic(Attributes.UUID_HM11_CHARACTERISTIC_STATUS)
                                    }
                                }
                            }, 600)
                        }
                    } else if (uuid.contains("2ee2")) {
                        Log.e("DATA_STRING_2EE2", dataString)
//                        commonViewModel.firmwareVersion.value = "0" + dataString
                        commonViewModel.firmwareVersion.value = dataString
                    }
                }

                if (clickOnConnectButton || clickedOnCancel) {
                    binding.relFSNearByDevice.visibility = View.GONE
                    binding.prbFSLoader.visibility = View.GONE
                    binding.txtFsStatusView.text = getString(R.string.connect)
                    binding.relSpConnectedView.visibility = View.VISIBLE

                    stop()

                    clickOnConnectButton = false
                    clickedOnCancel = false

                    Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                        override fun run() {
                            val bundle = bundleOf()
                            bundle.putInt(EXTRA_SCREEN_NUMBER, screenNumber)
                            findNavController().navigate(R.id.splashToHome, bundle)
                        }
                    }, 1000)
                }
            } else if (BluetoothLeService.ACTION_READ_RSSI.equals(action)) {
                Log.e("RSSI_DATA", "***   " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA).toString())
                commonViewModel.rssiValue.value =
                    intent.getStringExtra(BluetoothLeService.EXTRA_DATA)?.toInt()
            }
        }
    }

    private fun startReadRssi() {
        isReadRssi = true
        readRSSI = object : Thread() {
            override fun run() {
                while (isReadRssi) {
                    try {
                        sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    if (mBluetoothLeService != null && mBluetoothLeService!!.rssiVal) {
                    }
                }
            }
        }
        (readRSSI as Thread).start()
    }

    private fun servicesDiscovered() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService!!.setServiceUUID(Attributes.UUID_HM11_SERVICE_SAVVY4);
            mBluetoothLeService!!.readCharacteristic(
                Attributes.UUID_HM11_CHARACTERISTIC_PITCH,
                Attributes.UUID_HM11_CHARACTERISTIC_ROLL,
                Attributes.UUID_HM11_CHARACTERISTIC_BATT,
                Attributes.UUID_HM11_CHARACTERISTIC_CMD,
                Attributes.UUID_HM11_CONFIG
            )
        }
    }

    fun connectBluetooth() {
        stop()
        if (mBluetoothLeService != null) mBluetoothLeService!!.connect(selectedBluetoothAddress, "")
        goToHome = false
    }

    private fun getRuntimePermission() {
        if (checkPermisssion()) {
            checkLocationEnableOrNot()
        } else {
            runtimePermission()
        }
    }

    private fun checkLocationEnableOrNot() {
        val locationRequest: LocationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        val result = LocationServices.getSettingsClient(requireActivity())
            .checkLocationSettings(builder.build())
        result.addOnSuccessListener(requireActivity(),
            object : OnSuccessListener<LocationSettingsResponse> {
                override fun onSuccess(p0: LocationSettingsResponse) {
                    checkBlutoothOpenOrNot()
                }
            })
        result.addOnFailureListener(requireActivity(), object : OnFailureListener {
            override fun onFailure(p0: Exception) {
                if (p0 is ResolvableApiException) {
                    try {
                        val resolvable = p0 as ResolvableApiException
                        val intentSenderRequest =
                            IntentSenderRequest.Builder(resolvable.resolution).build()
                        mLunch.launch(intentSenderRequest)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun checkBlutoothOpenOrNot() {
        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        val bluetoothAdapter = bluetoothManager.adapter

        if (!bluetoothAdapter.isEnabled) {
            Utils.customeDialog(popDialog,
                requireActivity(),
                getString(R.string.blutoothPermission),
                getString(R.string.turn_on_bluetooth),
                getString(R.string.allow),
                commonViewModel,
                object : SelectionInterfaces {
                    override fun clickOnView() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (!checkPermissionForBluetoothScan()) {
                                runtimeBluetoothScanPermission()
                            } else {
                                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                    return
                                }

                                bluetoothAdapter.enable()

                                Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                                    override fun run() {
                                        startSearchingDevice()
                                    }
                                }, 2000)
                            }
                        } else {
                            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                                return
                            }
                            bluetoothAdapter.enable()
                            Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                                override fun run() {
                                    startSearchingDevice()
                                }
                            }, 2000)
                        }
                    }

                    override fun clickOnClearErrorCode() {

                    }
                })
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!checkPermissionForBluetoothScan()) {
                    runtimeBluetoothScanPermission()
                } else {
                    startSearchingDevice()
                }
            } else {
                startSearchingDevice()
            }
        }
    }

    private fun startSearchingDevice() {

        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        val gattServiceIntent = Intent(requireContext(), BluetoothLeService::class.java)
        requireContext().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)

        startScanDeviceHandler.postDelayed(Runnable {
            if (scannerCount == 10) {
//            if (scannerCount == 3) {
                startScanDeviceHandler.removeCallbacks(runnable)

                if (!clickOnConnectButton) {
                    if (!clickedOnCancel) {
                        selectedBluetoothAddress = ""
                    }
                    binding.prbFSLoader.visibility = View.GONE
                    binding.txtFsStatusView.text = requireContext().getString(R.string.connect)
                    binding.relSearchingDevices.visibility = View.VISIBLE
                    binding.relBluetooth.visibility = View.GONE
                    //TODO DEBUG
                    if (mLeDevices != null && mLeDevices!!.size > 0) {
                        binding.relNoDeviceFound.visibility = View.GONE
                    } else {
                        binding.relNoDeviceFound.visibility = View.VISIBLE
                    }
                }
            } else {
                startScanDeviceHandler.postDelayed(
                    runnable,
                    scannerDelay.toLong()
                )
            }
            scannerCount = scannerCount + 1
        }.also { runnable = it }, scannerDelay.toLong())

        when (againStartScanner) {
            1 -> {
                showSearchingDevice()
                connectAndStartScanner()
            }
            0 -> {
                showSearchingDevice()
            }
            2 -> {
                connectAndStartScanner()
            }
            3 -> {
                connectBluetooth()
            }
        }
    }

    private fun showSearchingDevice() {
        if (preference.getData(Preference.DEVICE_ADDRESS).equals("") || screenNumber == 1 || !isSearchFromInit) {
            goToHome = false
            binding.relSplash.visibility = View.GONE
            binding.relFSNearByDevice.visibility = View.GONE

            binding.prbFSLoader.visibility = View.GONE
            binding.txtFsStatusView.text = requireContext().getString(R.string.connect)

            binding.relSpConnectedView.visibility = View.GONE

            binding.relSearchingDevices.visibility = View.VISIBLE
            binding.relBluetooth.visibility = View.VISIBLE
            binding.relNoDeviceFound.visibility = View.GONE
        } else {
            goToHome = true
            val bundle = bundleOf()
            bundle.putInt(EXTRA_SCREEN_NUMBER, 0)
            findNavController().navigate(R.id.splashToHome, bundle)
        }
    }

    private fun checkPermisssion(): Boolean {
        val locationPermission = ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
        val locationCoarsePermission = ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
        val bluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH)
        }
        val bluetoothAdminPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_SCAN)
        } else {
            ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_ADMIN)
        }

        return locationPermission == PackageManager.PERMISSION_GRANTED && locationCoarsePermission == PackageManager.PERMISSION_GRANTED && bluetoothPermission == PackageManager.PERMISSION_GRANTED && bluetoothAdminPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun checkPermissionForBluetoothScan(): Boolean {
        val scanPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_SCAN)
        } else {
            ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_ADMIN)
        }
        val connectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH)
        }

        return scanPermission == PackageManager.PERMISSION_GRANTED && connectPermission == PackageManager.PERMISSION_GRANTED
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        registerForActivityResult =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

                permission.entries.forEach { it ->
                    val granted = it.value
                    val permission = it.key

                    if (!granted) {
                        val neverAskAgain = !ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
                            permission
                        )
                        if (neverAskAgain) {
                            type = 2
                            Utils.showLocationAlertPermision(requireActivity(), dialog!!)
                            // Utils.showAlertPermision(requireActivity(), dialog!!)
                        } else {
                            type = 1
                            runtimePermission()
                        }
                    } else {
                        againStartScanner = 0
                        checkLocationEnableOrNot()
                    }
                }
            }

        registerForActivityResultBluetoth =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

                permission.entries.forEach { it ->
                    val granted = it.value
                    val permission = it.key

                    if (!granted) {
                        val neverAskAgain = !ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
                            permission
                        )
                        if (neverAskAgain) {
//                            type = 2
                            Utils.showBlutoothAlertPermision(requireActivity(), dialog!!)
                            // Utils.showAlertPermision(requireActivity(), dialog!!)
                        } else {
//                            type = 1
                            runtimeBluetoothScanPermission()
                        }
                    } else {
                        val bluetoothManager = activity?.run {
                            this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        }
                        val bluetoothAdapter = bluetoothManager?.adapter
                        if (bluetoothAdapter != null) {
                            if (!bluetoothAdapter.isEnabled) {
                                if (!checkPermissionForBluetoothScan()) {
                                    runtimeBluetoothScanPermission()
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                            return@forEach
                                        }
                                    } else {
                                        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                                            return@forEach
                                        }
                                    }
                                    bluetoothAdapter.enable()
                                }
                            }
                        }
                        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                            override fun run() {
                                startSearchingDevice()
                            }
                        }, 2000)
                    }
                }
            }
        mLunch =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
                if (activityResult.resultCode == Activity.RESULT_OK) {
                    getRuntimePermission()
                } else {
                    checkLocationEnableOrNot()
                }
            }
    }

    private fun runtimePermission() {
        registerForActivityResult.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun runtimeBluetoothScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerForActivityResultBluetoth.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        } else {
            registerForActivityResultBluetoth.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        }
    }

    fun AppCompatTextView.makeLinks(vararg links: Pair<String, View.OnClickListener>) {
        val spannableString = SpannableString(this.text)
        var startIndexOfLink = -1
        for (link in links) {
            val clickableSpan = object : ClickableSpan() {
                override fun updateDrawState(textPaint: TextPaint) {
                    // use this to change the link color
                    textPaint.color = ContextCompat.getColor(requireContext(), R.color.dark_blue)
                    // toggle below value to enable/disable
                    // the underline shown below the clickable text
                    textPaint.isUnderlineText = false
                }

                override fun onClick(view: View) {
                    Selection.setSelection((view as TextView).text as Spannable, 0)
                    view.invalidate()
                    link.second.onClick(view)
                }
            }
            startIndexOfLink = this.text.toString().indexOf(link.first, startIndexOfLink + 1)
//      if(startIndexOfLink == -1) continue // todo if you want to verify your texts contains links text
            spannableString.setSpan(
                clickableSpan, startIndexOfLink, startIndexOfLink + link.first.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        this.movementMethod =
            LinkMovementMethod.getInstance() // without LinkMovementMethod, link can not click
        this.setText(spannableString, TextView.BufferType.SPANNABLE)
    }
}