package com.savvytech.savvylevelfour.ui.adapters

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.savvytech.savvylevelfour.R
import com.savvytech.savvylevelfour.common.EXTRA_LOW
import com.savvytech.savvylevelfour.common.EXTRA_MEDIUM
import com.savvytech.savvylevelfour.common.EXTRA_STRONG
import com.savvytech.savvylevelfour.ui.interfaces.SendStringInterfaces

class NearbyDevicesAdapter : RecyclerView.Adapter<NearbyDevicesAdapter.ViewHolder>() {

    //    var listOfNearbyDevice = ArrayList<NearbyDevicesModel>()
    var listOfNearbyDevice = ArrayList<com.savvytech.savvylevelfour.common.MyBluetoothDevice>()
    lateinit var sendStringInterfaces: SendStringInterfaces
    lateinit var context: Context
    lateinit var preference: com.savvytech.savvylevelfour.common.Preference

    fun addList(
        context: Context,
        listOfNearbyDevice: ArrayList<com.savvytech.savvylevelfour.common.MyBluetoothDevice>,
        sendStringInterfaces: SendStringInterfaces
    ) {
        this.context = context
        this.listOfNearbyDevice = listOfNearbyDevice
        this.sendStringInterfaces = sendStringInterfaces
        preference = com.savvytech.savvylevelfour.common.Preference(this.context)
    }

    var currentIndex = 0
    var preIndex = currentIndex

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvDeviceStrength: AppCompatTextView
        var tvDeviceNameItemConnectDevice: AppCompatTextView
        var ivDeviceStatusItemConnectDevice: AppCompatImageView
        var relImageItemConnectDevice: RelativeLayout
        var relDeviceNameItemConnectDevice: RelativeLayout

        init {
            tvDeviceStrength = itemView.findViewById(R.id.tvDeviceStrength)
            tvDeviceNameItemConnectDevice =
                itemView.findViewById(R.id.tvDeviceNameItemConnectDevice)
            ivDeviceStatusItemConnectDevice =
                itemView.findViewById(R.id.ivDeviceStatusItemConnectDevice)
            relImageItemConnectDevice = itemView.findViewById(R.id.relImageItemConnectDevice)
            relDeviceNameItemConnectDevice =
                itemView.findViewById(R.id.relDeviceNameItemConnectDevice)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_connect_device, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val mDevice: com.savvytech.savvylevelfour.common.MyBluetoothDevice = listOfNearbyDevice.get(position)

//        holder.tvDeviceNameTitle.text = listOfNearbyDevice.get(holder.adapterPosition).title
//        holder.tvDeviceNameTitle.text = "Savvy4"

        val rssi = mDevice.value
        Log.e("rssi", rssi.toString())

        when (rssi) {
            in 1 downTo -40 -> {
                holder.tvDeviceStrength.text = EXTRA_STRONG
            }
            in -41 downTo -70 -> {
                holder.tvDeviceStrength.text = EXTRA_MEDIUM
            }
            in -71 downTo -100 -> {
                holder.tvDeviceStrength.text = EXTRA_LOW
            }
        }

        holder.tvDeviceNameItemConnectDevice.text = mDevice.device.name
//        holder.tvDeviceNameItemConnectDevice.text = "Savvy4"

//        if (preference.getDeviceAddressTemp(com.savvytech.savvylevelfour.common.Preference.DEVICE_ADDRESS_TEMP).equals("")) {
//            if (preference.getData(com.savvytech.savvylevelfour.common.Preference.DEVICE_ADDRESS).equals(listOfNearbyDevice.get(position).device.address)) {
//                listOfNearbyDevice.get(position).isSelected = true
//            }
//        } else {
//            if (preference.getDeviceAddressTemp(com.savvytech.savvylevelfour.common.Preference.DEVICE_ADDRESS_TEMP).equals(listOfNearbyDevice.get(position).device.address)) {
//                listOfNearbyDevice.get(position).isSelected = true
//            }
//        }

        if (listOfNearbyDevice.get(position).isSelected) {
            holder.ivDeviceStatusItemConnectDevice.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_selected_device
                )
            )
            currentIndex = position
//            sendStringInterfaces.sendString(listOfNearbyDevice[currentIndex].name)
            if (mDevice.device.name != null) {
                sendStringInterfaces.sendString(mDevice.device.name, mDevice.device.address)
            }
        } else {
            holder.ivDeviceStatusItemConnectDevice.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_unselected_device
                )
            )
        }

        holder.relDeviceNameItemConnectDevice.setOnClickListener {
            if (!listOfNearbyDevice[position].isSelected) {
                preIndex = currentIndex
                currentIndex = position

                listOfNearbyDevice[preIndex].isSelected = false
                listOfNearbyDevice[currentIndex].isSelected = true


                notifyItemChanged(preIndex)
                notifyItemChanged(currentIndex)
                notifyDataSetChanged()

                preference.setDeviceAddressTemp(com.savvytech.savvylevelfour.common.Preference.DEVICE_ADDRESS_TEMP, mDevice.device.address)

                sendStringInterfaces.sendString(mDevice.device.name, mDevice.device.address)
            }


/*            for (i in 0 until listOfNearbyDevice.size){
                if (i == view_holder.adapterPosition){
                    listOfNearbyDevice.get(view_holder.adapterPosition).isSelected = true
                } else {
                    listOfNearbyDevice.get(view_holder.adapterPosition).isSelected = false
                }
            }*/

        }
        holder.relImageItemConnectDevice.setOnClickListener {
            holder.relDeviceNameItemConnectDevice.performClick()
        }

    }

    override fun getItemCount(): Int {
        return listOfNearbyDevice.size
    }
}