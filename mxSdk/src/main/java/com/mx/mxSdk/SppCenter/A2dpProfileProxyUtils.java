package com.mx.mxSdk.SppCenter;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class A2dpProfileProxyUtils {

    private static final String TAG = A2dpProfileProxyUtils.class.getSimpleName();

    private final Application application;
//    private BluetoothAdapterUtils bluetoothAdapterUtils;

    private final List<OnA2dpServiceConnStatedListener> listeners = new ArrayList<OnA2dpServiceConnStatedListener>();

    public A2dpProfileProxyUtils(Application application) {

        this.application = application;
//        bluetoothAdapterUtils = new BluetoothAdapterUtils(this.application);
    }

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

    private void notifyA2dpServiceConnected(int profile, BluetoothProfile proxy){

        for (int i = 0; i < listeners.size(); i++) {

            OnA2dpServiceConnStatedListener listener = listeners.get(i);

            if (listener != null) {

                runOnUiThread(() -> listener.onA2dpServiceConnected(profile, proxy));
            }

        }

    }

    private void notifyA2dpServiceDisconnected(int profile){

        for (int i = 0; i < listeners.size(); i++) {

            OnA2dpServiceConnStatedListener listener = listeners.get(i);

            if (listener != null) {

                runOnUiThread(() -> listener.onA2dpServiceDisconnected(profile));
            }

        }

    }

    private void notifyA2dpServiceTakeFail(){

        for (int i = 0; i < listeners.size(); i++) {

            OnA2dpServiceConnStatedListener listener = listeners.get(i);

            if (listener != null) {

                runOnUiThread(listener::onA2dpServiceTakeFail);
            }

        }

    }


    public void registerA2dpServiceConnStatedListener(OnA2dpServiceConnStatedListener onServiceConnStatedListener) {

        if (!listeners.contains(onServiceConnStatedListener)){

            listeners.add(onServiceConnStatedListener);
        }

    }

    public void unRegisterA2dpServiceConnStatedListener(OnA2dpServiceConnStatedListener onServiceConnStatedListener){

        listeners.remove(onServiceConnStatedListener);

    }

    //注册
    public void getProfileProxy(){

        BluetoothAdapter adapter = BluetoothAdapterUtils.getAdapter();

        if (adapter==null){

            notifyA2dpServiceTakeFail();

            return;
        }
        adapter.getProfileProxy(application, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {

                Log.i(TAG, "A2dp--onServiceConnected");

                notifyA2dpServiceConnected(profile,proxy);

            }

            @Override
            public void onServiceDisconnected(int profile) {

                Log.i(TAG, "A2dp--onServiceDisconnected");

                notifyA2dpServiceDisconnected(profile);

            }
        }, BluetoothProfile.A2DP);

    }


    public interface OnA2dpServiceConnStatedListener{

        void onA2dpServiceConnected(int profile, BluetoothProfile proxy);
        void onA2dpServiceDisconnected(int profile);
        void onA2dpServiceTakeFail();
    }

}
