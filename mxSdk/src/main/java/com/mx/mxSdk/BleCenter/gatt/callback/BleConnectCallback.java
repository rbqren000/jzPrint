package com.mx.mxSdk.BleCenter.gatt.callback;

import com.mx.mxSdk.BleCenter.BleDevice;

public interface BleConnectCallback extends BleCallback {
    void onStart(boolean startSuccess, String info, BleDevice device);

    void onConnected(BleDevice device);

    void onDisconnected(String info, int status, BleDevice device);
}
