package com.org.jzprinter.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.ArrayList;

public class NetBroadcastReceiver extends BroadcastReceiver {
    public static ArrayList<netEventHandler> mListeners = new ArrayList<netEventHandler>();
    private static final String NET_CHANGE_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction()!=null && intent.getAction().equals(NET_CHANGE_ACTION)) {
//            Toast.makeText(context, "0NET_CHANGE_ACTION", Toast.LENGTH_SHORT).show();
//            Application.mNetWorkState = NetUtil.getNetworkState(context);
            if (!mListeners.isEmpty())// 通知接口完成加载
                for (netEventHandler handler : mListeners) {
                    handler.onNetChange();
                }
        }
//        else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
//            Toast.makeText(context, "1WIFI_STATE_CHANGED_ACTION", Toast.LENGTH_SHORT).show();
//            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
//
//            switch (wifiState) {
//                case WifiManager.WIFI_STATE_ENABLED:
//                    // Wifi已启用
//                    Toast.makeText(context, "Wifi已启用", Toast.LENGTH_SHORT).show();
//                    break;
//                case WifiManager.WIFI_STATE_DISABLED:
//                    // Wifi已禁用
//                    Toast.makeText(context, "Wifi已禁用", Toast.LENGTH_SHORT).show();
//                    break;
//            }
//        } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
//            Toast.makeText(context, "2NETWORK_STATE_CHANGED_ACTION", Toast.LENGTH_SHORT).show();
//            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//            if (networkInfo != null) {
//                if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
//                    // Wifi已连接
//                    Toast.makeText(context, "Wifi已连接", Toast.LENGTH_SHORT).show();
//                } else if (networkInfo.getState() == NetworkInfo.State.DISCONNECTED) {
//                    // Wifi已断开连接
//                    Toast.makeText(context, "Wifi已断开", Toast.LENGTH_SHORT).show();
//                }
//            }
//        }
        
    }

    public static abstract interface netEventHandler {

        public abstract void onNetChange();
    }
}