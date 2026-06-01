package com.mx.mxSdk.BleCenter.gatt.callback;

import com.mx.mxSdk.BleCenter.BleDevice;

public interface BleWriteCallback extends BleCallback {
    void onWriteSuccess(byte[] data, BleDevice device);
}
