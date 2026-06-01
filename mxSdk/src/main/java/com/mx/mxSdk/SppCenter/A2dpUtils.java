package com.mx.mxSdk.SppCenter;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rbq on 2016/12/20.
 */

public class A2dpUtils{

    public static final String TAG = A2dpUtils.class.getSimpleName();
    /*
      The profile is in disconnected state
        int STATE_DISCONNECTED = 0;
     The profile is in connecting state
        int STATE_CONNECTING = 1;
     The profile is in connected state
        int STATE_CONNECTED = 2;
    The profile is in disconnecting state
        int STATE_DISCONNECTING = 3;
     */
    public static int getConnectionState(BluetoothDevice device,BluetoothProfile proxy){//获取设备的连接状态

        int state = -1;

        try {

            BluetoothA2dp a2dp = (BluetoothA2dp) proxy;

            Class<?> mClass = a2dp.getClass();
            Method m1 = mClass.getDeclaredMethod("getConnectionState", BluetoothDevice.class);

            state = (int)m1.invoke(a2dp,device);

        }catch (Exception e){

            e.printStackTrace();
        }

        return state;
    }

    public static List<BluetoothDevice> getConnedDevices(BluetoothProfile proxy){

        List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();

        try {

            BluetoothA2dp a2dp = (BluetoothA2dp) proxy;

            Class<?> mClass = a2dp.getClass();

            Method m1 = mClass.getDeclaredMethod("getConnectedDevices");
            List<BluetoothDevice> list =  (List<BluetoothDevice>)m1.invoke(a2dp);

            Log.i(TAG, "A2dp连接的设备的个数为: "+list.size());

            for (int i=0;i<list.size();i++){

                BluetoothDevice device = list.get(i);

                Log.i(TAG, "A2dp连接的设备"+i+"的名称："+device.getName());

                devices.add(device);
            }

        }catch (Exception e){

            e.printStackTrace();

        }

        return devices;

    }

    public static boolean isHasA2dpConnDevice(BluetoothProfile proxy){

        List<BluetoothDevice> devices = getConnedDevices(proxy);

        return !devices.isEmpty();
    }

    public static boolean isA2dpDevice(BluetoothDevice device,BluetoothProfile proxy){

        List<BluetoothDevice> devices = getConnedDevices(proxy);

        return devices.contains(device);
    }

    /**
     *
     * @param device
     * @param proxy
     * @return
     */

    public static boolean connectA2dp(BluetoothDevice device,BluetoothProfile proxy) {

        Log.i(TAG, "a2dpConnect: "+device.getAddress());

        boolean connected = false;

        try {

            BluetoothA2dp a2dp = (BluetoothA2dp) proxy;

            Class<? extends BluetoothA2dp> clazz = a2dp.getClass();

            Log.i(TAG, "use reflect to connect a2dp");
            Method m2 = clazz.getMethod("connect", BluetoothDevice.class);

            connected = (boolean)m2.invoke(a2dp, device);

        } catch (Exception e) {

            e.printStackTrace();
            Log.e(TAG, "error:" + e.toString());
        }

        return connected;
    }

    /**
     * 断开a2dp连接
     * @param device
     * @param proxy
     * @return
     */
    public static boolean disA2dpConnect(BluetoothDevice device,BluetoothProfile proxy) {

        boolean connected = false;

        try {

            BluetoothA2dp a2dp = (BluetoothA2dp) proxy;

//        a2dp.isA2dpPlaying(device);//用于判断是否在a2dp播放

            Class<? extends BluetoothA2dp> clazz = a2dp.getClass();

            Log.i(TAG, "use reflect to connect a2dp");

            Method method = clazz.getMethod("disconnect", BluetoothDevice.class);
            connected = (boolean)method.invoke(a2dp, device);

            Log.i(TAG, "蓝牙音频已断开");

        } catch (Exception e) {

            e.printStackTrace();
            Log.e(TAG, "error:" + e.toString());
        }

        return connected;
    }

    public static boolean isA2dpPlaying(BluetoothDevice device,BluetoothProfile proxy){

        try {

            BluetoothA2dp a2dp = (BluetoothA2dp) proxy;

//         boolean isplay = a2dp.isA2dpPlaying(device);

            Class<? extends BluetoothA2dp> clazz = a2dp.getClass();

            Method method = clazz.getMethod("isA2dpPlaying", BluetoothDevice.class);

            return (boolean)method.invoke(a2dp,device);


        }catch (Exception e){

            e.printStackTrace();
        }

        return false;
    }
}
