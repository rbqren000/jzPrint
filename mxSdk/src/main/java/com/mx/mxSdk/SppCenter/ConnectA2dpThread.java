package com.mx.mxSdk.SppCenter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Created by rbq on 2017/5/18.
 */
public class ConnectA2dpThread implements Runnable {

    private static final String TAG = ConnectA2dpThread.class.getSimpleName();

    private boolean isStart = false;
    private BluetoothDevice device;
    private BluetoothProfile profile;
    private int tryTime = 1; // 连接次数

    private OnA2dpConnListener onA2DpConnListener;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private Thread connectThread;

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

    public void registerA2dpConnListener(OnA2dpConnListener onA2DpConnListener) {
        this.onA2DpConnListener = onA2DpConnListener;
    }

    public void unregisterA2dpConnListener() {
        this.onA2DpConnListener = null;
    }

    public boolean isStart() {
        return this.isStart;
    }

    synchronized public void start(BluetoothDevice device, BluetoothProfile profile, int tryTime) {
        if (this.isStart) {
            return;
        }

        cancel();

        this.isStart = true;
        this.device = device;
        this.profile = profile;
        this.tryTime = Math.max(tryTime, 1);

        if (onA2DpConnListener != null) {
            runOnUiThread(() -> onA2DpConnListener.onA2dpConnStart(this.device));
        }

        connectThread = new Thread(this);
        connectThread.start();
        Log.i(TAG, "ConnectA2dpThread开始执行");
    }

    synchronized public void cancel() {
        if (!this.isStart) {
            return;
        }

        this.isStart = false;

        if (connectThread != null) {
            connectThread.interrupt();
            connectThread = null;
        }

        Log.i(TAG, "ConnectA2dpThread停止执行");
    }

    @Override
    public void run() {
        boolean isSuccess = false;
        int time = 0;

        while (isStart && !isSuccess && time < tryTime) {
            try {
                Log.i(TAG, "进入A2dp连接线程:" + Thread.currentThread());
                isSuccess = A2dpUtils.connectA2dp(device, profile);
                Log.i(TAG, "A2dp是否连接成功: " + isSuccess);
            } catch (Exception e) {
                e.printStackTrace();
            }

            time++;

            if (!isSuccess) {
                try {
                    Thread.sleep(1000); // 1秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        isStart = false;

        if (onA2DpConnListener != null) {
            if (isSuccess) {
                runOnUiThread(() -> onA2DpConnListener.onA2dpConnSucceed(this.device));
            } else {
                runOnUiThread(() -> onA2DpConnListener.onA2dpConnTimeout(device));
            }
        }
    }

    public interface OnA2dpConnListener {
        void onA2dpConnStart(BluetoothDevice bluetoothDevice);
        void onA2dpConnSucceed(BluetoothDevice bluetoothDevice);
        void onA2dpConnTimeout(BluetoothDevice bluetoothDevice);
    }
}
