package com.mx.mxSdk.SppCenter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Set;

@SuppressLint("MissingPermission")
public class BluetoothAdapterUtils {

    private static final String TAG = BluetoothAdapterUtils.class.getSimpleName();
    public static final int REQUEST_OPEN_BT_CODE = 235;


    /**
     * 检查设备是否支持蓝牙，支持返回true，不支持返回false
     *
     * @return
     */
    public static Boolean isSupportBluetooth() {

        BluetoothAdapter adapter = getAdapter();

        return adapter!=null;
    }

    /**
     * 检查设备是否支持ble，如果支持返回true，否则返回false
     *
     * @return
     */
    public Boolean isSupportBle(Context context) {

        return context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static BluetoothAdapter getAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    public static boolean isOsSupport(){

        int sdkVersion = android.os.Build.VERSION.SDK_INT;

//        Log.i(TAG, "当前操作系统的版本: "+sdkVersion);

        return sdkVersion > 18;
    }

    public static void enable(Activity activity){
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, REQUEST_OPEN_BT_CODE);
    }

    public static Boolean enable(){
        BluetoothAdapter adapter = getAdapter();
        if (adapter==null){
            return false;
        }
        return adapter.enable();
    }

    public static Boolean disable(){
        BluetoothAdapter adapter = getAdapter();
        if (adapter==null){
            return false;
        }
        return adapter.disable();
    }

    public static Boolean isEnabled(){

        BluetoothAdapter adapter = getAdapter();

        if (adapter==null){
            return false;
        }
        return adapter.isEnabled();
    }

    public static ArrayList<BluetoothDevice> getBondDevice(){

        ArrayList<BluetoothDevice> arrayList = new ArrayList<BluetoothDevice>();

        BluetoothAdapter adapter = getAdapter();

        if (adapter==null){

            return arrayList;
        }

        Set<BluetoothDevice> devices = adapter.getBondedDevices();

        if (devices==null){

            return arrayList;
        }

        for (BluetoothDevice device : devices) {

            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {

                Log.i(TAG, "配对设备名称: " + device.getName());

                arrayList.add(device);
            }

        }
        return arrayList;
    }

    public static BluetoothDevice getConnedDevice(){

        ArrayList<BluetoothDevice> arrayList = getBondDevice();

        Log.i(TAG, "配对设备数量:"+arrayList.size());

        for (int i=0;i<arrayList.size();i++){

            BluetoothDevice device = arrayList.get(i);

            if (BluetoothUtils.isConnected(device)){

                Log.i(TAG, "已连接的设备: "+device.getName());

                return device;
            }
        }

        return null;
    }

    public static BluetoothDevice getRemoteDevice(String address){
        BluetoothAdapter adapter = getAdapter();
        if (adapter==null||!BluetoothAdapter.checkBluetoothAddress(address)){
            return null;
        }
        return adapter.getRemoteDevice(address);
    }

}

