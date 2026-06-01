package com.mx.mxSdk.Serial;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.mx.mxSdk.SppCenter.ThreadUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import android_serialport_api.SerialPort;

public class SerialThread implements Runnable {

    private static final String TAG = SerialThread.class.getSimpleName();
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private String sPort = "/dev/ttyS1";
    private int iBaudRate = 9600;
    private int stopBits = 1;
    private int dataBits = 8;
    private int parity = 0;
    private int flowCon = 0;
    private int flags = 0;
    private volatile boolean isOpen = false;

    private volatile boolean isStart;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Thread serialThread;

    private OnSerialConnectListener onSerialConnectListener;

    public void registerSerialConnectListener(OnSerialConnectListener onSerialConnectListener) {
        this.onSerialConnectListener = onSerialConnectListener;
    }

    public void unregisterSerialConnectListener() {
        this.onSerialConnectListener = null;
    }

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

    public void start(String sPort, int baudRate, int stopBits, int dataBits) {
        this.start(sPort, baudRate, stopBits, dataBits, 0, 0);
    }

    public void start(String sPort, int baudRate, int stopBits, int dataBits, int parity, int flowCon) {
        this.start(sPort, baudRate, stopBits, dataBits, parity, flowCon, 0);
    }

    public void start(String sPort, int baudRate, int stopBits, int dataBits, int parity, int flowCon, int flags) {
        if (this.isStart) {
            return;
        }
        cancel();
        if (TextUtils.isEmpty(sPort)) {
            runOnUiThread(() -> {
                if (onSerialConnectListener != null) {
                    onSerialConnectListener.onSerialOpenFail("串口路径不能为空");
                }
            });
            return;
        }
        this.isStart = true;
        this.sPort = sPort;
        this.iBaudRate = baudRate;
        this.stopBits = stopBits;
        this.dataBits = dataBits;
        this.parity = parity;
        this.flowCon = flowCon;
        this.flags = flags;

        runOnUiThread(() -> {
            if (onSerialConnectListener != null) {
                onSerialConnectListener.onSerialOpenStart();
            }
        });

        serialThread = new Thread(this);
        serialThread.start();
        Log.i(TAG, "SerialThread开始执行");
    }

    public void cancel() {
        if (!isStart) return;

        isStart = false;

        if (serialThread != null) {
            serialThread.interrupt();
            serialThread = null;
        }
        close();
        Log.i(TAG, "SerialThread停止执行");
    }

    private void close() {
        if (this.mSerialPort != null) {
            this.mSerialPort.close();
            this.mSerialPort = null;
        }
        this.isOpen = false;
        if (onSerialConnectListener != null){
            onSerialConnectListener.onSerialClose();
        }
    }

    @Override
    public void run() {
        try {

            this.mSerialPort = new SerialPort(new File(this.sPort), this.iBaudRate, this.stopBits, this.dataBits, this.parity, this.flowCon, this.flags);
            this.mOutputStream = this.mSerialPort.getOutputStream();
            this.mInputStream = this.mSerialPort.getInputStream();
            this.isOpen = true;

            ThreadUtils.sleep(0.5f);

            runOnUiThread(() -> {
                if (onSerialConnectListener != null) {
                    onSerialConnectListener.onSerialOpenSuccess(mInputStream, mOutputStream);
                }
            });

        } catch (Exception e) {
            this.isOpen = false;
            runOnUiThread(() -> {
                if (onSerialConnectListener != null) {
                    onSerialConnectListener.onSerialOpenFail("打开串口失败");
                }
            });
        }
    }

    public InputStream getmInputStream() {
        return mInputStream;
    }

    public OutputStream getmOutputStream() {
        return mOutputStream;
    }

    public int getBaudRate() {
        return this.iBaudRate;
    }

    public int getStopBits() {
        return this.stopBits;
    }

    public int getDataBits() {
        return this.dataBits;
    }

    public int getParity() {
        return this.parity;
    }

    public int getFlowCon() {
        return this.flowCon;
    }

    public String getPort() {
        return this.sPort;
    }

    public boolean isOpen() {
        return this.isOpen;
    }

    public interface OnSerialConnectListener {
        void onSerialOpenStart();
        void onSerialOpenSuccess(InputStream inputStream, OutputStream outputStream);
        void onSerialOpenFail(String msg);
        void onSerialClose();
    }
}


