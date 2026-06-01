package com.mx.mxSdk.SppCenter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ServiceThread implements Runnable {

    private static final String TAG = ServiceThread.class.getSimpleName();
    private static final UUID SPP_UUID = UUID.fromString(BluetoothUUID.SPP_UUID);

    private volatile boolean isStart = false;
    private BluetoothServerSocket serverSocket = null;
    private BluetoothAdapter adapter;
    private int tryTime = 1; // 连接次数

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Thread serviceThread;
    private OnServiceListener onServiceListener;

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

    public void registerSppServiceListener(OnServiceListener onServiceListener) {
        this.onServiceListener = onServiceListener;
    }

    public void unregisterSppServiceListener() {
        this.onServiceListener = null;
    }

    synchronized public void start(BluetoothAdapter adapter, int tryTime) {
        if (isStart) return;

        cancel();

        isStart = true;
        this.adapter = adapter;
        this.tryTime = Math.max(tryTime, 1);

        runOnUiThread(() -> {
            if (onServiceListener != null) {
                onServiceListener.onSppServiceStart();
            }
        });

        serviceThread = new Thread(this);
        serviceThread.start();
        Log.i(TAG, "ServiceThread开始执行");
    }

    synchronized public void cancel() {
        if (!isStart) return;

        isStart = false;

        if (serviceThread != null) {
            serviceThread.interrupt();
            serviceThread = null;
        }

        try {
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "ServiceThread停止执行");
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        int time = 0;

        while (isStart && serverSocket == null && time < tryTime) {
            try {
                serverSocket = adapter.listenUsingRfcommWithServiceRecord("rbq", SPP_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ThreadUtils.sleep(0.3f);
            time++;
        }

        try {
            if (serverSocket != null) {
                BluetoothSocket bluetoothSocket = serverSocket.accept(); // 阻塞，接收用户请求
                if (onServiceListener != null) {
                    runOnUiThread(() -> {
                        if (onServiceListener != null){
                            onServiceListener.onSppServiceSucceed(bluetoothSocket);
                        }
                    });
                }
            } else {
                if (onServiceListener != null) {
                    runOnUiThread(() -> {
                        if (onServiceListener != null){
                            onServiceListener.onSppServiceTimeout();
                        }
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            isStart = false;
        }
    }

    public interface OnServiceListener {
        void onSppServiceStart();
        void onSppServiceSucceed(BluetoothSocket socket);
        void onSppServiceTimeout();
    }
}


