package com.mx.mxSdk.SppCenter;

import android.os.Handler;
import android.os.Looper;
import com.mx.mxSdk.Utils.RBQLog;
import java.io.InputStream;
import tp.xmaihh.serialport.stick.AbsStickPackageHelper;

public class ReadThread implements Runnable {

    private static final String TAG = ReadThread.class.getSimpleName();

    private volatile boolean isStart = false;
    private InputStream inputStream;
    private OnReadDataListener onReadDataListener;
    private Thread readThread;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private AbsStickPackageHelper mStickPackageHelper;

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

    public void registerReadDataListener(OnReadDataListener onReadDataListener) {
        this.onReadDataListener = onReadDataListener;
    }

    public void unregisterReadDataListener() {
        this.onReadDataListener = null;
    }

    synchronized public void start(InputStream inputStream) {
        start(inputStream, null);
    }

    synchronized public void start(InputStream inputStream, AbsStickPackageHelper stickPackageHelper) {
        if (isStart||inputStream == null) return;

        cancel();

        isStart = true;
        this.inputStream = inputStream;
        this.mStickPackageHelper = stickPackageHelper;
        readThread = new Thread(this);
        readThread.start();
        RBQLog.i(TAG, "ReadCommandThread开始执行");
    }

    synchronized public void cancel() {
        if (!isStart) return;

        isStart = false;
        inputStream = null;
        mStickPackageHelper = null;

        if (readThread != null) {
            readThread.interrupt();
            readThread = null;
        }

        RBQLog.i(TAG, "ReadCommandThread停止执行");
    }

    @Override
    public void run() {
        while (isStart && inputStream!=null) {
            try {
                byte[] buffer;
                if (this.mStickPackageHelper != null) {
                    buffer = this.mStickPackageHelper.execute(inputStream);
                } else {
                    buffer = new byte[512];
                    int len = inputStream.read(buffer);
                    if (len == -1) break;
                    byte[] data = new byte[len];
                    System.arraycopy(buffer, 0, data, 0, len);
                    buffer = data;
                }

                if (buffer != null && buffer.length > 0 && onReadDataListener != null) {
                    byte[] finalBuffer = buffer;
                    runOnUiThread(() -> onReadDataListener.onReadData(finalBuffer));
                }

            } catch (Exception e) {
                RBQLog.i("读取数据异常！message:" + e.getMessage());

                if (onReadDataListener != null) {
                    runOnUiThread(onReadDataListener::onReadError);
                }
                break; // 结束循环
            }
        }
    }

    public interface OnReadDataListener {
        void onReadData(byte[] data);
        void onReadError(); // 读数据错误，说明连接断开
    }
}


