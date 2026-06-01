package com.mx.mxSdk.BleCenter.gatt.callback;

import com.mx.mxSdk.BleCenter.BleDevice;

public interface BleNotifyCallback extends BleCallback {
    void onCharacteristicChanged(byte[] data, BleDevice device);

    void onNotifySuccess(String notifySuccessUuid, BleDevice device);
}
