package com.mx.mxSdk.BleCenter.gatt.callback;

import com.mx.mxSdk.BleCenter.BleDevice;

public interface BleMtuCallback extends BleCallback {
    void onMtuChanged(int mtu, BleDevice device);
}
