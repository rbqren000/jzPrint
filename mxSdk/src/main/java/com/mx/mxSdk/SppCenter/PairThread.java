package com.mx.mxSdk.SppCenter;

import android.bluetooth.BluetoothDevice;
import com.mx.mxSdk.Utils.RBQLog;

/**
 * Created by rbq on 2017/5/18.
 */
public class PairThread implements Runnable {

    private static final String TAG2 = PairThread.class.getSimpleName();

    private boolean isStart = false;
    private BluetoothDevice device;
    private int tryTime = 1; // 连接次数
    private Thread pairThread;

    public boolean isStart() {
        return isStart;
    }

    synchronized public void start(BluetoothDevice device, int tryTime) {
        RBQLog.i(TAG2, "调用BondThread的Start");

        if (isStart || device == null) {
            return;
        }
        cancel();

        isStart = true;
        this.device = device;
        this.tryTime = Math.max(tryTime, 1);

        pairThread = new Thread(this);
        pairThread.start();
        RBQLog.i(TAG2, "BondThread开始执行");
    }

    synchronized public void cancel() {
        if (!isStart) {
            return;
        }

        isStart = false;

        if (pairThread != null) {
            pairThread.interrupt();
            pairThread = null;
        }

        RBQLog.i(TAG2, "BondThread停止执行");
    }

    @Override
    public void run() {
        int time = 0;
        boolean isPin = false;

        while (isStart && !isPin && time < tryTime) {
            isPin = tryPairingWithPins(device, "0000", "1234", "8888");
            time++;
            if (!isPin) {
                ThreadUtils.sleep(0.01f);
            }
        }

        time = 0;
        boolean isCancel = false;
        while (isStart && !isCancel && time < tryTime) {
            try {
                isCancel = BluetoothUtils.cancelPairingUserInput(device);
                RBQLog.i(TAG2, "取消配对对话框是否成功:" + isCancel);
            } catch (Exception e) {
                e.printStackTrace();
            }
            time++;
            if (!isCancel) {
                ThreadUtils.sleep(0.01f);
            }
        }

        time = 0;
        boolean isBond = false;
        while (isStart && !isBond && time < tryTime) {
            try {
                isBond = BluetoothUtils.createBond(device);
                RBQLog.i(TAG2, "进行配对设备");
            } catch (Exception e) {
                e.printStackTrace();
            }
            time++;
            if (!isBond) {
                ThreadUtils.sleep(1.5f);
            }
        }

        isStart = false;
    }

    private boolean tryPairingWithPins(BluetoothDevice device, String... pins) {
        boolean isPin = false;
        for (String pin : pins) {
            if (isStart) {
                try {
                    isPin = BluetoothUtils.setPin(device, pin);
                    RBQLog.i(TAG2, "设置配对密码" + pin + "是否成功:" + isPin);
                    if (isPin) break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!isPin) {
                    ThreadUtils.sleep(0.01f);
                }
            }
        }
        return isPin;
    }
}

