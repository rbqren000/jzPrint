package com.mx.mxSdk.BleCenter.gatt.callback;

import com.mx.mxSdk.BleCenter.BleDevice;

public interface BleWriteByBatchCallback extends BleCallback {
    void writeByBatchSuccess(byte[] data, BleDevice device);
}
