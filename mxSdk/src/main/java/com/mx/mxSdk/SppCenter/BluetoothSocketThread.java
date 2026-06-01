package com.mx.mxSdk.SppCenter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.mx.mxSdk.Utils.RBQLog;
import java.util.UUID;

/**
 * Created by rbq on 2017/4/1.
 */

public class BluetoothSocketThread implements Runnable {
    
    public static final String TAG2 = BluetoothSocketThread.class.getSimpleName();
    
    private BluetoothDevice device; // 服务器设备
    private BluetoothSocket mSocket; // 通信Socket
    
    private int tryTime = 1;//连接次数
    private boolean isStart = false;
    private Thread thread;
    
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
    
    private OnSocketConnListener onSocketConnListener;
    
    public void registerSocketConnListener(OnSocketConnListener onSocketConnListener) {
        this.onSocketConnListener = onSocketConnListener;
    }
    
    public void unregisterSocketConnListener() {
        this.onSocketConnListener = null;
    }
    
    public boolean isStart() {
        return isStart;
    }
    
    synchronized public boolean isConned() {
        
        if (mSocket == null) {
            return false;
        }
        return mSocket.isConnected();
    }
    
    synchronized public void connect(BluetoothDevice device, int tryTime) {
        
        if (this.isStart || device == null) return;
        if (this.thread != null) {
            Thread tempThread = this.thread;
            tempThread.interrupt();
            thread = null;
        }
        
        this.isStart = true;
        this.device = device;
        //这里先将tryTime的值恢复到默认值，如果赋值的tryTime大于1，则有效并进行修改
        //做一个限制，只有当tryTime的值大于等于1的时候才为有效值
        this.tryTime = Math.max(tryTime, 1);
        
        if (onSocketConnListener != null) {
            
            runOnUiThread(() -> onSocketConnListener.onBluetoothSocketConnStart(this.device));
        }
        
        this.thread = new Thread(this);
        this.thread.start();
        
        RBQLog.i("ConnSocketThread开始执行");
    }
    
    synchronized public void cancel() {

        if (!this.isStart) return;
        this.isStart = false;
        if (this.thread != null) {
            Thread tempThread = this.thread;
            tempThread.interrupt();
            thread = null;
        }
        try {
            
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        RBQLog.i(TAG2, "ConnSocketThread停止执行");
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        
        int time = 0;
        
        while (isStart
                && mSocket == null
                && time < tryTime) {
            
            try {
                
                RBQLog.i(TAG2, "initSocket: 尝试创建Socket");
                int sdk = Build.VERSION.SDK_INT;
                if (sdk >= 10) {
                    mSocket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(BluetoothUUID.SPP_UUID));
                } else {
                    mSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(BluetoothUUID.SPP_UUID));
                }

            } catch (Exception e) {
                RBQLog.i(TAG2, "mSocket创建发生异常");
            }
            time = time + 1;
            if (mSocket==null){
                ThreadUtils.sleep(1);
            }
        }

        if (mSocket!=null){

            time = 0;
            boolean success = false;

            while (isStart
                    &&!success
                    &&time<tryTime){
                try {
                    mSocket.connect();
                    success = mSocket.isConnected();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                time = time + 1;
                //如果连接失败，则休眠300ms
                if (!success){
                    ThreadUtils.sleep(1);
                }
            }

            if (success){//连接成功
//                isStart = false;
                runOnUiThread(() -> onSocketConnListener.onBluetoothSocketConnSucceed(this.device,mSocket));

            }else {//连接失败，超过次数
                try {
                    if (mSocket!=null){
                        mSocket.close();//这里进行资源释放
                        mSocket=null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                isStart = false;
                runOnUiThread(()->onSocketConnListener.onBluetoothSocketConnTimeout(this.device));
            }
        }else {//socket创建失败，超过次数
            isStart = false;
            runOnUiThread(()->onSocketConnListener.onBluetoothSocketConnTimeout(this.device));
        }
    }

    public interface OnSocketConnListener {

        void onBluetoothSocketConnStart(BluetoothDevice device);
        void onBluetoothSocketConnSucceed(BluetoothDevice device, BluetoothSocket socket);
        void onBluetoothSocketConnTimeout(BluetoothDevice device);//连接超时，或者说超过次数
    }

}
