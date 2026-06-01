package com.org.jzprinter.manager;

import android.os.Handler;
import android.os.Looper;
import com.mx.mxSdk.ConnectManager;

public class DeviceScanner {

    private static final String TAG = DeviceScanner.class.getSimpleName();

    private static final long SCAN_DURATION = 30 * 1000; // 30秒
    private static final long PAUSE_DURATION = 1500; // 1秒，作为扫描任务之间的间隔
    private static final long DEFAULT_START_SCAN_DURATION = 2 * 1000; // 2秒，作为默认开始扫描的最小间隔时间
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean isScanning = false;
    private long lastScanStopTime = 0;

    // 单例实例
    private static DeviceScanner instance;

    // 私有构造函数，防止外部实例化
    private DeviceScanner() {
    }

    // 获取单例实例的方法
    public static synchronized DeviceScanner share() {
        if (instance == null) {
            instance = new DeviceScanner();
        }
        return instance;
    }

    private final Runnable delayedStartTask = new Runnable() {
        @Override
        public void run() {
            startScanningTask();
        }
    };

    private final Runnable scanTask = new Runnable() {
        @Override
        public void run() {
            if (isScanning) {
                localStartScan();
                handler.postDelayed(pauseScanTask, SCAN_DURATION);
            }
        }
    };

    private final Runnable pauseScanTask = new Runnable() {
        @Override
        public void run() {
            if (isScanning) {
                localStopScan();
                lastScanStopTime = System.currentTimeMillis();
                handler.postDelayed(scanTask, PAUSE_DURATION);
            }
        }
    };

    public void startScanning() {
        startScanning(DEFAULT_START_SCAN_DURATION);
    }

    public synchronized void startScanning(long minInterval) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastStop = currentTime - lastScanStopTime;

        if (timeSinceLastStop < minInterval) {
            handler.postDelayed(delayedStartTask, minInterval - timeSinceLastStop);
        } else {
            startScanningTask();
        }
    }

    private void startScanningTask() {
        if (!isScanning) {
            isScanning = true;
            handler.post(scanTask); // 开始第一次扫描
        }
    }

    public synchronized void stopScanning() {
        handler.removeCallbacks(delayedStartTask); // 清除等待任务
        if (isScanning) {
            isScanning = false;
            handler.removeCallbacks(scanTask);
            handler.removeCallbacks(pauseScanTask);
            localStopScan(); // 确保所有扫描都停止
        }
        lastScanStopTime = System.currentTimeMillis();
    }

    private void localStartScan() {
//        RBQLog.i(TAG,"【localStartScan】");
        // 启动扫描
        ConnectManager.share().discoverConnModel(SCAN_DURATION);       // 扫描所有设备 30 秒
        ConnectManager.share().discoverWifiDevice(SCAN_DURATION);      // 扫描 WiFi 设备 30 秒
        ConnectManager.share().discoverBluetoothDevice(SCAN_DURATION); // 扫描 Bluetooth 设备 30 秒
    }

    private void localStopScan() {
//        RBQLog.i(TAG,"【localStopScan】");
        // 停止扫描
        ConnectManager.share().cancelDiscoverWifiDevice();      // 取消 WiFi 扫描
        ConnectManager.share().cancelDiscoveryBluetoothDevice(); // 取消 Bluetooth 扫描
        ConnectManager.share().cancelDiscoverConnModel();       // 取消ConnMode的扫描
    }
}





