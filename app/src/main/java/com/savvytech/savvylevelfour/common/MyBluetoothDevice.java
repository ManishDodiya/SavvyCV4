package com.savvytech.savvylevelfour.common;

import android.bluetooth.BluetoothDevice;

public class MyBluetoothDevice {
    public BluetoothDevice device;
    public Integer rssi;
    public Boolean isSelected;
    public Integer getValue() {
        return rssi;
    }
}
