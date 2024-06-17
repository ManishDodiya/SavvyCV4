/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//-----------------------------------------------------------------------------
//
//	BluetoothLeService
//
//	Author:		Mike Smits
//	Date:		05 May 18
//	Revision:	1.2.180505.1755
//
//-----------------------------------------------------------------------------
package com.savvytech.savvylevelfour.common;

import static com.savvytech.savvylevelfour.common.ConstantsKt.EXTRA_EXTRA_DATA;
import static com.savvytech.savvylevelfour.common.ConstantsKt.EXTRA_RSSI;
import static com.savvytech.savvylevelfour.common.ConstantsKt.EXTRA_UUID;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;

public class BluetoothLeService extends Service {
    public BluetoothManager mBluetoothManager;
    public BluetoothAdapter mBluetoothAdapter;
    public String mBluetoothDeviceAddress;
    public BluetoothGatt mBluetoothGatt;

    public UUID serviceUUID;

    public int mConnectionState = STATE_DISCONNECTED;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public final static String ACTION_READ_RSSI = "com.example.bluetooth.le.ACTION_READ_RSSI";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
    public final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e("CONNECTION_STATE", "CONNECTED");
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e("CONNECTION_STATE", "DISCONNECTED");
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpdate(intentAction);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                Log.e("CONNECTION_STATE", "DISCONNECTING");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
//            Log.e("RSSI_ON_READ", rssi + "  **  ");
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                if (gatt.getDevice().getAddress().equalsIgnoreCase(pre))
                final Intent intent = new Intent(ACTION_READ_RSSI);
                intent.putExtra(EXTRA_DATA, String.valueOf(rssi));
                sendBroadcast(intent);
//                broadcastUpdate(intent, rssi);
            }
        }
    };

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    public boolean connect(final String address, String from) {
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null
                && from.equalsIgnoreCase("")) {
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        final boolean[] isReturn = {false};
        Context context = this;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (device == null) {
                    isReturn[0] = false;
                }

                mBluetoothGatt = device.connectGatt(context, true, mGattCallback);
                mBluetoothDeviceAddress = address;
                mConnectionState = STATE_CONNECTING;
                isReturn[0] = true;
            }
        }, 2000);
        return isReturn[0];
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void setServiceUUID(UUID service) {
        serviceUUID = service;
    }

    public void readCharacteristic(UUID characteristicUUIDPitch, UUID characteristicUUIDRoll, UUID characteristicUUIDBatt, UUID characteristicUUIDCMD, UUID descriptorUUID) {
        if (mBluetoothGatt != null) {
            BluetoothGattService mGattService = mBluetoothGatt.getService(serviceUUID);
            if (mGattService != null) {
                BluetoothGattCharacteristic characteristicPitch = mGattService.getCharacteristic(characteristicUUIDPitch);
                BluetoothGattCharacteristic characteristicRoll = mGattService.getCharacteristic(characteristicUUIDRoll);
                BluetoothGattCharacteristic characteristicBatt = mGattService.getCharacteristic(characteristicUUIDBatt);
                BluetoothGattCharacteristic characteristicCMD = mGattService.getCharacteristic(characteristicUUIDCMD);

                readCharacteristic(characteristicPitch, characteristicRoll, characteristicBatt, characteristicCMD);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setCharacteristicNotification(characteristicPitch, characteristicRoll, characteristicBatt, characteristicCMD, descriptorUUID, true);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }, 1000);
            }
        }
    }

    public void readCMDCharacteristic(UUID characteristicUUIDCMD) {
        if (mBluetoothGatt != null) {
            BluetoothGattService mGattService = mBluetoothGatt.getService(serviceUUID);
            if (mGattService != null) {
                BluetoothGattCharacteristic characteristicCMD = mGattService.getCharacteristic(characteristicUUIDCMD);
                readCMDCharacteristic2(characteristicCMD);
            }
        }
    }

    public void readStatusCharacteristic(UUID characteristicUUIDStatus) {
        if (mBluetoothGatt != null) {
            BluetoothGattService mGattService = mBluetoothGatt.getService(serviceUUID);
            if (mGattService != null) {
                BluetoothGattCharacteristic characteristicStatus = mGattService.getCharacteristic(characteristicUUIDStatus);
                readStatusCharacteristic2(characteristicStatus);
            }
        }
    }

    public void readVersionCharacteristic(UUID characteristicUUIDFirmVer) {
        if (mBluetoothGatt != null) {
            BluetoothGattService mGattService = mBluetoothGatt.getService(serviceUUID);
            if (mGattService != null) {
                BluetoothGattCharacteristic characteristicFirmVer = mGattService.getCharacteristic(characteristicUUIDFirmVer);
                readVersionCharacteristic2(characteristicFirmVer);
            }
        }
    }

    public void writeCharacteristic(byte[] value, UUID characteristicUUID) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        BluetoothGattService mCustomService = mBluetoothGatt.getService(serviceUUID);
        if (mCustomService == null) {
            return;
        }
        BluetoothGattCharacteristic characteristic = mCustomService.getCharacteristic(characteristicUUID);
        if (value != null)
            characteristic.setValue(value);
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
//        Log.e(EXTRA_UUID, characteristic.getUuid().toString());
        if (data != null && data.length > 0) {
//            intent.putExtra(EXTRA_DATA, new String(data));
//            Log.e("PITCH_ROLL_DATA", Arrays.toString(data));
            if (characteristic.getUuid().toString().equals("31fc2d5c-e5de-11eb-ba80-0242ac130004") ||
                    characteristic.getUuid().toString().equals("31fc2e24-e5de-11eb-ba80-0242ac130004")) {
//                Float valueInFloat = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getFloat();
//                String valueHex = new BigInteger(1, data).toString(16);
                String valueHex = "";
                Log.e("DATA", Arrays.toString(data));
                valueHex = byteToHexString(data);
//                Double number2digits = Double.valueOf(String.format("%.1f", valueInFloat));

                intent.putExtra(EXTRA_EXTRA_DATA, valueHex);
                intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
            } else if (characteristic.getUuid().toString().equals("31fc2ee2-e5de-11eb-ba80-0242ac130004")) {
                Log.e("DATA", Arrays.toString(data));
                int valueInInt = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getInt();
                Log.e("DATA_INT", String.valueOf(valueInInt));
                intent.putExtra(EXTRA_EXTRA_DATA, String.valueOf(valueInInt));
                intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
            } else {
                Float valueInFloat = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getFloat();
                Double number2digits = Double.valueOf(String.format("%.1f", valueInFloat));

                intent.putExtra(EXTRA_EXTRA_DATA, number2digits);
                intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
            }

//            Log.e("PITCH_ROLL_DATA", ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getFloat() + "   *****  ");
        }
        sendBroadcast(intent);
    }

    // Java method
    private String byteToHexString(byte[] payload) {
        if (payload == null) return "";
        StringBuilder stringBuilder = new StringBuilder(payload.length);
        for (byte byteChar : payload)
            stringBuilder.append(String.format("%02X ", byteChar));
        return stringBuilder.toString();
    }

    private void broadcastUpdate(final String action, int rssi) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_RSSI, rssi);
        Log.e("RSSI", rssi + " ** ");
        sendBroadcast(intent);
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristicPitch, BluetoothGattCharacteristic characteristicRoll, BluetoothGattCharacteristic
            characteristicBatt, BluetoothGattCharacteristic characteristicCMD) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }

        mBluetoothGatt.readCharacteristic(characteristicPitch);
        mBluetoothGatt.readCharacteristic(characteristicRoll);
        mBluetoothGatt.readCharacteristic(characteristicBatt);
        mBluetoothGatt.readCharacteristic(characteristicCMD);
    }

    public void readCMDCharacteristic2(BluetoothGattCharacteristic characteristicCMD) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristicCMD);
    }

    public void readStatusCharacteristic2(BluetoothGattCharacteristic characteristicStatus) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristicStatus);
    }

    public void readVersionCharacteristic2(BluetoothGattCharacteristic characteristicFirmVer) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristicFirmVer);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristicPitch, BluetoothGattCharacteristic characteristicRoll,
                                              BluetoothGattCharacteristic characteristicBatt, BluetoothGattCharacteristic characteristicCMD, UUID descriptorUUID,
                                              boolean enabled) throws InterruptedException {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristicPitch, enabled);
        BluetoothGattDescriptor descriptor = characteristicPitch.getDescriptor(descriptorUUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.setCharacteristicNotification(characteristicRoll, enabled);
//                BluetoothGattDescriptor descriptor2 = characteristicRoll.getDescriptor(descriptorUUID);
                BluetoothGattDescriptor descriptor2 = characteristicRoll.getDescriptor(descriptorUUID);
                descriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor2);
            }
        }, 500);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.setCharacteristicNotification(characteristicBatt, enabled);
                BluetoothGattDescriptor descriptor3 = characteristicBatt.getDescriptor(descriptorUUID);
                descriptor3.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor3);
            }
        }, 800);
    }

    public boolean getRssiVal() {
        if (mBluetoothGatt == null)
            return false;
        return mBluetoothGatt.readRemoteRssi();

    }
}