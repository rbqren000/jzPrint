package com.mx.mxSdk.BleCenter.gatt.callback;

import com.mx.mxSdk.BleCenter.BleDevice;

public interface BleRssiCallback extends BleCallback {

    void onRssi(int rssi, BleDevice bleDevice);
}
