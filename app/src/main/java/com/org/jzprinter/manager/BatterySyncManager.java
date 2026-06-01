package com.org.jzprinter.manager;

import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import com.org.jzprinter.R;
import com.mx.mxSdk.ConnectManager;
import com.mx.mxSdk.Device;
import com.mx.mxSdk.Opcode;
import com.mx.mxSdk.Utils.RBQLog;
import java.util.ArrayList;
import java.util.List;

public class BatterySyncManager implements Runnable {

    private volatile boolean isStart = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private void runOnUiThread(Runnable runnable) {
        if (isAndroidMainThread()) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    private static boolean isAndroidMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private final List<OnBatteryReadListener> onBatteryReadListeners = new ArrayList<OnBatteryReadListener>();

    private final List<Integer> batteryIcons = new ArrayList<Integer>();

    private BatterySyncManager() {

        batteryIcons.add(R.mipmap.icon_horz_bat_low);
        batteryIcons.add(R.mipmap.icon_horz_bat_one);
        batteryIcons.add(R.mipmap.icon_horz_bat_two);
        batteryIcons.add(R.mipmap.icon_horz_bat_three);
        batteryIcons.add(R.mipmap.icon_horz_bat_four);
        batteryIcons.add(R.mipmap.icon_horz_bat_five);
    }

    // 静态内部类，用于实现单例模式
    private static class Holder {
        private static final BatterySyncManager INSTANCE = new BatterySyncManager();
    }

    public static BatterySyncManager share() {
        return Holder.INSTANCE;
    }

    public synchronized Integer getIconByValue(int value){
        if (value >= 0 && value < batteryIcons.size()){
            return batteryIcons.get(value);
        }
        return -1;
    }

    /**
     * 这里延时开始，主要是怕跟同步方向的指令重叠发送
     */
    public synchronized void startSyncBattery() {
        if (isStart){
            return;
        }
        isStart = true;
        ConnectManager.share().registerReceiveMessageListener(onReceiveMsgListener);
        handler.postDelayed(this, 1000);
    }

    public synchronized void stopSyncBattery() {
        if (!isStart){
            return;
        }
        isStart = false;
        ConnectManager.share().unregisterReceiveMessageListener(onReceiveMsgListener);
        handler.removeCallbacks(this);
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void run() {
        // 发送电量读取指令
        if (ConnectManager.share().isConnected()) {
            RBQLog.e("发送读取电量指令2");
            ConnectManager.share().sendCommand(Opcode.ReadBattery, null);
        }
        handler.postDelayed(this, 5 * 60 * 1000);
    }

    public void registerBatteryReadListener(OnBatteryReadListener onBatteryReadListener) {
        if (!onBatteryReadListeners.contains(onBatteryReadListener)){
            onBatteryReadListeners.add(onBatteryReadListener);
        }
    }

    public void unregisterBatteryReadListener(OnBatteryReadListener onBatteryReadListener) {
        onBatteryReadListeners.remove(onBatteryReadListener);
    }

    public void notifyBatteryRead(Device device, int bat) {
        for (OnBatteryReadListener onBatteryReadListener : onBatteryReadListeners){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBatteryReadListener.onReadBattery(device, bat);
                }
            });
        }
    }

    private final ConnectManager.OnReceiveMsgListener onReceiveMsgListener = new ConnectManager.OnReceiveMsgListener() {
        @Override
        public void onReadPrinterHeadParameter(Device device, int headValue, int l_pix, int p_pix, int distance) {}

        @Override
        public void onReadCirculationAndRepeatTime(Device device, int circulation_time, int repeat_time) {}

        @Override
        public void onReadDirection(Device device, int oldHorizontalDirection, int horizontalDirection, int oldVerticalDirection, int verticalDirection) {}

        @Override
        public void onReadSoftwareInfo(Device device, String id, String name, String mcu_version, String mcu_date) {}

        @Override
        public void onReadTemperature(Device device, int temp) {}

        @Override
        public void onReadBattery(Device device, int bat) {
            RBQLog.e("读取到电池电量 bat:" + bat);
            notifyBatteryRead(device, bat);
        }

        @Override
        public void onReadCartridgeId(Device device, String cartridgeId) {}

        @Override
        public void onReadSilentState(Device device, boolean silentState) {}

        @Override
        public void onReadAutoPowerOffState(Device device, boolean autoPowerOff) {}

        @Override
        public void onReadContinuousPrintState(Device device, boolean continuousPrintState) {

        }

        @Override
        public void onError(Device device, String error) {}
    };

    public interface OnBatteryReadListener {
        void onReadBattery(Device device, int bat);
    }
}
