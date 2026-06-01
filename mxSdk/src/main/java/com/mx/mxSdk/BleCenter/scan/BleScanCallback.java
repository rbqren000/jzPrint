package com.mx.mxSdk.BleCenter.scan;

import com.mx.mxSdk.BleCenter.BleDevice;

public interface BleScanCallback {
    void onLeScan(BleDevice device, int rssi, byte[] scanRecord);

    void onStart(boolean startScanSuccess, String info);

    void onFinish();
}
