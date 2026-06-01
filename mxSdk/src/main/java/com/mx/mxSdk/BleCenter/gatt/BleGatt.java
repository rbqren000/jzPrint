package com.mx.mxSdk.BleCenter.gatt;

import android.bluetooth.BluetoothGatt;

import com.mx.mxSdk.BleCenter.BleDevice;
import com.mx.mxSdk.BleCenter.gatt.callback.BleConnectCallback;
import com.mx.mxSdk.BleCenter.gatt.callback.BleMtuCallback;
import com.mx.mxSdk.BleCenter.gatt.callback.BleNotifyCallback;
import com.mx.mxSdk.BleCenter.gatt.callback.BleReadCallback;
import com.mx.mxSdk.BleCenter.gatt.callback.BleRssiCallback;
import com.mx.mxSdk.BleCenter.gatt.callback.BleWriteByBatchCallback;
import com.mx.mxSdk.BleCenter.gatt.callback.BleWriteCallback;
import com.mx.mxSdk.BleCenter.gatt.data.ServiceInfo;

import java.util.List;

public interface BleGatt {
    void connect(int timeoutMills, BleDevice device, BleConnectCallback callback,
                 BleHandlerThread bleHandlerThread);

    void disconnect(String address);

    void disconnectAll();

    void notify(BleDevice device, String serviceUuid, String notifyUuid, BleNotifyCallback callback);

    void cancelNotify(BleDevice device, String serviceUuid, String characteristicUuid);

    void read(BleDevice device, String serviceUuid, String readUuid, BleReadCallback callback);

    void write(BleDevice device, String serviceUuid, String writeUuid, byte[] data, BleWriteCallback callback);

    void writeByBatch(BleDevice device, String serviceUuid, String writeUuid, byte[] data,
                      int lengthPerPackage, long writeDelay, BleWriteByBatchCallback callback);

    void readRssi(BleDevice device, BleRssiCallback callback);

    void setMtu(BleDevice device, int mtu, BleMtuCallback callback);

    List<BleDevice> getConnectedDevices();

    List<ServiceInfo> getDeviceServices(String address);

    BluetoothGatt getBluetoothGatt(String address);

    boolean isConnecting(String address);

    boolean isConnected(String address);

    void destroy();
}
