package com.mx.mxSdk.BleCenter.gatt.callback;

import com.mx.mxSdk.BleCenter.BleDevice;

public interface BleReadCallback extends BleCallback {
    void onReadSuccess(byte[] data, BleDevice device);
}
