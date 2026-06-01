package com.mx.mxSdk.SppCenter;

import static android.content.Context.RECEIVER_EXPORTED;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.mx.mxSdk.Utils.BroadcastHelper;
import com.mx.mxSdk.Utils.RBQLog;

public class BluetoothDiscoverUtils {

    /**
     * Bluetooth device type, Unknown   未知类型
     */
    public static final int DEVICE_TYPE_UNKNOWN = 0;

    /**
     * Bluetooth device type, Classic - BR/EDR devices  传统类型
     */
    public static final int DEVICE_TYPE_CLASSIC = 1;

    /**
     * Bluetooth device type, Low Energy - LE-only      ble类型
     */
    public static final int DEVICE_TYPE_LE = 2;

    /**
     * Bluetooth device type, Dual Mode - BR/EDR/LE     传统和ble双重类型
     */
    public static final int DEVICE_TYPE_DUAL = 3;


    private static final String TAG2 = BluetoothDiscoverUtils.class.getSimpleName();

    private final Application application;

    private final IntentFilter bluetoothBroadcastReceiverIntentFilter;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private void runOnUiThread(Runnable runnable) {
        if (isAndroidMainThread()) {
            runnable.run();
        } else {
            mHandler.post(runnable);
        }
    }

    private static boolean isAndroidMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private OnBluetoothDeviceListener onBluetoothDeviceListener;

    public void registerBluetoothDeviceDiscoverListener(OnBluetoothDeviceListener onBluetoothDeviceListener) {

        this.onBluetoothDeviceListener = onBluetoothDeviceListener;

    }

    public void unregisterBluetoothDeviceDiscoverListener() {

        this.onBluetoothDeviceListener = null;

    }

    public void registerBluetoothBroadcastReceiver(){

        if (!bluetoothBroadcastReceiverIntentFilter.hasAction(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            bluetoothBroadcastReceiverIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);// 本地适配器状态发生变化
        }
        if (!bluetoothBroadcastReceiverIntentFilter.hasAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
            bluetoothBroadcastReceiverIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        }
        if (!bluetoothBroadcastReceiverIntentFilter.hasAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
            bluetoothBroadcastReceiverIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        }
        if (!bluetoothBroadcastReceiverIntentFilter.hasAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
            bluetoothBroadcastReceiverIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        }
        if (!bluetoothBroadcastReceiverIntentFilter.hasAction(BluetoothDevice.ACTION_FOUND)){
            bluetoothBroadcastReceiverIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        }
        if (!bluetoothBroadcastReceiverIntentFilter.hasAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)){
            bluetoothBroadcastReceiverIntentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.application.registerReceiver(broadcastReceiver, bluetoothBroadcastReceiverIntentFilter,RECEIVER_EXPORTED);
        }else{
            this.application.registerReceiver(broadcastReceiver, bluetoothBroadcastReceiverIntentFilter);
        }

    }

    public void unregisterBluetoothBroadcastReceiver(){

        this.application.unregisterReceiver(broadcastReceiver);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action!=null&&action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {// 蓝牙开关状态通知

                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

                if(state == BluetoothAdapter.STATE_TURNING_ON){

                    RBQLog.i(TAG2, "蓝牙正在打开....");
                    if (onBluetoothDeviceListener !=null){
                        runOnUiThread(() -> onBluetoothDeviceListener.onOpeningBlueTooth());
                    }

                }else if (state == BluetoothAdapter.STATE_ON) {

                    RBQLog.i(TAG2,"蓝牙已经打开");
                    if (onBluetoothDeviceListener !=null){
                        runOnUiThread(() -> onBluetoothDeviceListener.onOpenedBlueTooth());
                    }

                }else if (state==BluetoothAdapter.STATE_TURNING_OFF){

                    RBQLog.i(TAG2,"蓝牙正在关闭....");
                    if (onBluetoothDeviceListener !=null){
                        runOnUiThread(() -> onBluetoothDeviceListener.onClosingBlueTooth());
                    }

                } else if (state == BluetoothAdapter.STATE_OFF) {
                    // 本地适配器关闭
                    RBQLog.i(TAG2, "蓝牙设备已关闭");
                    if (onBluetoothDeviceListener !=null){
                        runOnUiThread(() -> onBluetoothDeviceListener.onClosedBlueTooth());
                    }
                }

            } else if (action!=null&&action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {

                RBQLog.i(TAG2,"-------------------蓝牙配对状态发生变化-----------------");
                // 发现远程蓝牙设备
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (bluetoothDevice==null) return;

                if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE){//配对断开

                    RBQLog.i(TAG2,"-------蓝牙设备" + bluetoothDevice.getAddress() + "断开配对-------");
                    if (onBluetoothDeviceListener !=null){
                        runOnUiThread(() -> onBluetoothDeviceListener.onDisBond(bluetoothDevice));
                    }

                }else if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {//配对成功

                    RBQLog.i(TAG2,"-------蓝牙设备" + bluetoothDevice.getAddress() + "配对成功-------");
                    if (onBluetoothDeviceListener !=null){
                        runOnUiThread(() -> onBluetoothDeviceListener.onBonded(bluetoothDevice));
                    }

                }else if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDING){//正在配对

                    RBQLog.i(TAG2, "-------蓝牙设备" + bluetoothDevice.getAddress()+ "正在配对-------");
                    if (onBluetoothDeviceListener !=null){
                        runOnUiThread(() -> onBluetoothDeviceListener.onBonding(bluetoothDevice));
                    }
                }

            }else if (action!=null&&action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){//开始搜索

                RBQLog.i(TAG2, "onReceive: 开始搜索设备");
                if (onBluetoothDeviceListener !=null){
                    runOnUiThread(() -> onBluetoothDeviceListener.onStartDiscovering());
                }

            }else if (action!=null&&action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){//结束搜索

                RBQLog.i(TAG2,"onReceive: 搜索设备结束");
                if (onBluetoothDeviceListener !=null){
                    runOnUiThread(() -> onBluetoothDeviceListener.onStopDiscovering());
                }

            }else if (action!=null&&action.equals(BluetoothDevice.ACTION_FOUND)){//搜索到蓝牙设备

                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (onBluetoothDeviceListener !=null){
                    runOnUiThread(() -> onBluetoothDeviceListener.onDiscovered(bluetoothDevice));
                }
            }

        }
    };

    public BluetoothDiscoverUtils(Application application) {

        this.application = application;
        bluetoothBroadcastReceiverIntentFilter = new IntentFilter();
    }

    //蓝牙2.0搜索功能
    @SuppressLint("MissingPermission")
    public void startDiscovery(){

        if (isDiscovering()){
            return;
        }
        BluetoothAdapterUtils.getAdapter().startDiscovery();
        RBQLog.i("----开始搜索设备----");
    }

    //停止蓝牙2.0的搜索功能
    @SuppressLint("MissingPermission")
    public void cancelDiscovery(){

        if (!isDiscovering()){
            return;
        }
        BluetoothAdapterUtils.getAdapter().cancelDiscovery();
        RBQLog.i("----停止搜索设备----");
    }

    //蓝牙2.0是否处于搜索状态
    @SuppressLint("MissingPermission")
    public boolean isDiscovering(){

        if (!BluetoothAdapterUtils.isOsSupport()||!BluetoothAdapterUtils.isSupportBluetooth()){
            return false;
        }
        return BluetoothAdapterUtils.getAdapter().isDiscovering();
    }

    public interface OnBluetoothDeviceListener {

        void onStartDiscovering();
        void onStopDiscovering();
        void onDiscovered(BluetoothDevice bluetoothDevice);

        void onOpeningBlueTooth();//蓝牙正在打开
        void onOpenedBlueTooth();//蓝牙打开
        void onClosingBlueTooth();//蓝牙正在关闭
        void onClosedBlueTooth();//蓝牙关闭

        void onBonding(BluetoothDevice bluetoothDevice);
        void onBonded(BluetoothDevice bluetoothDevice);
        void onDisBond(BluetoothDevice bluetoothDevice);

    }

}
