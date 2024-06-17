package com.savvytech.savvylevelfour.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.*
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.util.SharedPreferencesUtils
import com.savvytech.savvylevelfour.R
import com.savvytech.savvylevelfour.common.BluetoothLeService
import com.savvytech.savvylevelfour.common.EXTRA_FROM
import com.savvytech.savvylevelfour.common.Preference
import com.savvytech.savvylevelfour.common.Utils
import com.savvytech.savvylevelfour.databinding.ActivitySavvyMainBinding
import com.savvytech.savvylevelfour.ui.interfaces.SelectionInterfaces
import com.savvytech.savvylevelfour.viewmodels.CommonViewModel
import com.savvytech.savvylevelfour.viewmodels.SavvyMainActivityViewModel

class SavvyMainActivity : AppCompatActivity() {

    lateinit var binding: ActivitySavvyMainBinding
    lateinit var savvyMainVM: SavvyMainActivityViewModel
    lateinit var preference: Preference
    val commonViewModel: CommonViewModel by viewModels()
    var from = ""
    var isClickedOnChangeOrientation = false
    lateinit var popDialog: Dialog
    lateinit var registerForActivityResultBluetoth: ActivityResultLauncher<Array<String>>
    var selectedBluetoothAddress: String = ""
    var mBluetoothLeService: BluetoothLeService? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.e("ON_CREATE", "ON_CREATE")
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        preference = Preference(this)
        if (resources.configuration.orientation != preference.getIntData(Preference.SCREEN_ORIENTATION)) {
            requestedOrientation = preference.getIntData(Preference.SCREEN_ORIENTATION) ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        popDialog = Dialog(this)

        binding = DataBindingUtil.setContentView(this@SavvyMainActivity, R.layout.activity_savvy_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//        this.makeStatusBarTransparent()
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cvParent)) { _, insets ->
////            findViewById<FloatingActionButton>(R.id.fab1).setMarginTop(insets.systemWindowInsetTop)
////            findViewById<FloatingActionButton>(R.id.fab2).setMarginTop(insets.systemWindowInsetTop)
//            insets.consumeSystemWindowInsets()
//        }

        if (!binding.root.isFocused) {
            binding.root.requestLayout()
        }

        if (intent.extras != null) {
            from = intent.getStringExtra(EXTRA_FROM).toString()
        }

        init()
        setObserver()
        setListeners()

//        val previousProcessID: Int? = preference.getIntData(Preference.processId)
//        val currentProcessID = Process.myPid()

//        if (previousProcessID == currentProcessID && isClickedOnChangeOrientation) {
//            // This ensures application not killed yet either by clearing recent or anyway
//            Log.e("APP_STATUS", "APP_IS_NOT_KILLED_BY_RECENT_APPS")
//            preference.setBoolenData(Preference.isZoom, true)
//        } else {
//            // This ensures application killed either by clearing recent or by anyother means
//            Log.e("APP_STATUS", "APP_IS_KILLED_BY_RECENT_APPS")
//            preference.setBoolenData(Preference.isZoom, false)
//        }

//        preference.setIntData(Preference.processId, android.os.Process.myPid())
    }

    private fun setObserver() {
        savvyMainVM.isShowNotConnectedDialog.observe(this, Observer {
            if (it) {
                savvyMainVM.isShowNotConnectedDialog.value = false
                showDialog()
            }
        })

        commonViewModel.onChangeOrientation.observe(this, {
            if (preference.getBooleanData(Preference.isZoom)) {
                isClickedOnChangeOrientation = it

                val previousProcessID: Int? = preference.getIntData(Preference.processId)
                val currentProcessID = Process.myPid()
//                preference.setBoolenData(Preference.isZoom, true)
                if (previousProcessID == currentProcessID && isClickedOnChangeOrientation) {
                    // This ensures application not killed yet either by clearing recent or anyway
                    Log.e("APP_STATUS", "APP_IS_NOT_KILLED_BY_RECENT_APPS")
                    preference.setBoolenData(Preference.isZoom, true)
                } else {
                    // This ensures application killed either by clearing recent or by anyother means
                    Log.e("APP_STATUS", "APP_IS_KILLED_BY_RECENT_APPS")
                    preference.setBoolenData(Preference.isZoom, false)
                    preference.setIntData(Preference.processId, Process.myPid())
                }
//
            } else {
                isClickedOnChangeOrientation = false
//                preference.setBoolenData(Preference.isZoom, false)
//
                val previousProcessID: Int? = preference.getIntData(Preference.processId)
                val currentProcessID = Process.myPid()

                if (previousProcessID == currentProcessID && isClickedOnChangeOrientation) {
                    // This ensures application not killed yet either by clearing recent or anyway
                    Log.e("APP_STATUS", "APP_IS_NOT_KILLED_BY_RECENT_APPS")
                    preference.setBoolenData(Preference.isZoom, true)
                } else {
                    // This ensures application killed either by clearing recent or by anyother means
                    Log.e("APP_STATUS", "APP_IS_KILLED_BY_RECENT_APPS")
                    preference.setIntData(Preference.processId, Process.myPid())
                    preference.setBoolenData(Preference.isZoom, false)
                }
            }
        })
    }

    fun init() {
        savvyMainVM = ViewModelProvider(this).get(SavvyMainActivityViewModel::class.java)
        binding.savvyMainVM = savvyMainVM
        binding.lifecycleOwner = this

        savvyMainVM.setCommonViewModels(commonViewModel)

        savvyMainVM.sendNavGraph(binding.navContainer, binding.conAsBottomView, this)
        com.savvytech.savvylevelfour.common.DataStoreClass().storeObj = binding

        commonViewModel.onChangeOrientation.value = true
    }

    fun showDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_change_connect_device)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)

        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        val height = ViewGroup.LayoutParams.WRAP_CONTENT

        dialog.findViewById<View>(R.id.tvChangeDeviceMainActivity).setOnClickListener {
            dialog.dismiss()
            savvyMainVM.changeDevice()
        }

        dialog.findViewById<View>(R.id.btnZCOkMainActivity).setOnClickListener {
            dialog.dismiss()
            showBluetoothDialog()
        }

        dialog.window?.decorView?.setBackgroundResource(android.R.color.transparent)
        dialog.window?.setLayout(width, height)
        dialog.window?.setGravity(Gravity.CENTER)

        dialog.show()
    }

    fun setListeners() {
//        binding.relBlankBottomMenu?.setOnClickListener {
//
//        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
//        outState.putString("currentFragment", commonViewModel.currentFragment.value)
        super.onSaveInstanceState(outState)
//        Log.e("==>CONFIG_CHANGE", "   *******   ")
//        isClickedOnChangeOrientation = true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
//        binding = DataBindingUtil.setContentView(this, R.layout.activity_savvy_main)
        super.onConfigurationChanged(newConfig)
//        Log.e("==>CONFIG_CHANGE", newConfig.orientation.toString() + "   *******   ")
//        recreate()
    }

    override fun attachBaseContext(newBase: Context?) {
        val newOverride = Configuration(newBase?.resources?.configuration)
        newOverride.fontScale = 1.0f
        applyOverrideConfiguration(newOverride)
        super.attachBaseContext(newBase)

        registerForActivityResultBluetoth =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

                permission.entries.forEach { it ->
                    val granted = it.value
                    val permission = it.key

                    if (!granted) {
                        val neverAskAgain = !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                        if (neverAskAgain) {
//                            type = 2
                            Utils.showBlutoothAlertPermision(this, popDialog)
                            // Utils.showAlertPermision(requireActivity(), dialog!!)
                        } else {
//                            type = 1
                            runtimeBluetoothScanPermission()
                        }
                    } else {
                        val bluetoothManager = run {
                            this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        }
                        val bluetoothAdapter = bluetoothManager.adapter
                        if (bluetoothAdapter != null) {
                            if (!bluetoothAdapter.isEnabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                        return@registerForActivityResult
                                    }
                                }
                                bluetoothAdapter.enable()

                                Handler(Looper.getMainLooper()).postDelayed(
                                    {
                                        if (!preference.getData(Preference.DEVICE_ADDRESS).toString().equals("") ||
                                            !preference.getData(Preference.DEVICE_ADDRESS).toString().isEmpty() ||
                                            preference.getData(Preference.DEVICE_ADDRESS).toString() != null
                                        ) {
                                            selectedBluetoothAddress = preference.getData(Preference.DEVICE_ADDRESS).toString()
                                        }
                                        Log.e("SELECTED_ADDRESS", selectedBluetoothAddress)
                                        if (mBluetoothLeService != null) {
                                            mBluetoothLeService!!.connect(selectedBluetoothAddress, "bluetoothEnable")
                                        }
                                    },
                                    2000
                                )
                            }
                        }
//                        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
//                            override fun run() {
//                                startSearchingDevice()
//                            }
//                        }, 2000)
                    }
                }
            }
    }

    override fun onResume() {
        super.onResume()
        Log.e("ON_RESUME", "On_Resume")
        val bluetoothManager = run {
            this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        }
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled) {
                commonViewModel.isDeviceConnected = false
            }
        } else {
            commonViewModel.isDeviceConnected = false
        }

        val gattServiceIntent = Intent(this@SavvyMainActivity, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mBluetoothReceiver, filter)
    }

    private val mBluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action != null && action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR
                )
                when (state) {
                    BluetoothAdapter.STATE_OFF -> {
                        Log.e("BLUETOOTH_STATE", "OFF")
                    }
                    BluetoothAdapter.STATE_ON -> {
                        Log.e("BLUETOOTH_STATE", "ON")
                    }
                }
            }
        }
    }

    fun showBluetoothDialog() {
        Utils.customeDialog(popDialog,
            this,
            getString(R.string.blutoothPermission),
            getString(R.string.turn_on_bluetooth),
            getString(R.string.allow),
            commonViewModel,
            object : SelectionInterfaces {
                override fun clickOnView() {
                    if (!checkPermissionForBluetoothScan()) {
                        runtimeBluetoothScanPermission()
                    } else {
                        val bluetoothManager = run {
                            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        }
                        val bluetoothAdapter = bluetoothManager?.adapter

                        if (ActivityCompat.checkSelfPermission(this@SavvyMainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return
                        }
                        bluetoothAdapter.enable()

                        Handler(Looper.getMainLooper()).postDelayed(
                            {

                                if (!preference.getData(Preference.DEVICE_ADDRESS).toString().equals("") ||
                                    !preference.getData(Preference.DEVICE_ADDRESS).toString().isEmpty() ||
                                    preference.getData(Preference.DEVICE_ADDRESS).toString() != null
                                ) {
                                    selectedBluetoothAddress = preference.getData(Preference.DEVICE_ADDRESS).toString()
                                }
                                Log.e("SELECTED_ADDRESS", selectedBluetoothAddress)
                                if (mBluetoothLeService != null) {
                                    mBluetoothLeService!!.connect(selectedBluetoothAddress, "bluetoothEnable")
                                }
                            },
                            2000
                        )
                    }
                }

                override fun clickOnClearErrorCode() {

                }
            })
    }

    val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).getService()
            // automatically connects to the device upon successful start-up initialization.
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    private fun checkPermissionForBluetoothScan(): Boolean {
        val scanPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
        }
        val connectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
        }
        return scanPermission == PackageManager.PERMISSION_GRANTED && connectPermission == PackageManager.PERMISSION_GRANTED
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
}