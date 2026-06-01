package com.org.jzprinter.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import java.util.ArrayList;
import java.util.List;
import androidx.core.app.ActivityCompat;

/**
 * WiFi连接管理
 * 申请权限
 * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
 * <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
 * <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
 * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
 * 动态权限
 * Manifest.permission.ACCESS_COARSE_LOCATION
 * Manifest.permission.ACCESS_FINE_LOCATION
 *
 */
public class WifiUtils {

    private WifiUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Checks if WiFi is enabled.
     *
     * @param context Context object.
     * @return True if WiFi is enabled, false otherwise.
     */
    public static boolean isWifiEnabled(Context context) {
        WifiManager wifiManager = getWifiManager(context);
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    /**
     * Enables WiFi.
     *
     * @param context Context object.
     */
    public static void enableWifi(Context context) {
        WifiManager wifiManager = getWifiManager(context);
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
    }

    /**
     * Disables WiFi.
     *
     * @param context Context object.
     */
    public static void disableWifi(Context context) {
        WifiManager wifiManager = getWifiManager(context);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
    }

    /**
     * Gets the current connected WiFi SSID.
     *
     * @param context Context object.
     * @return The current SSID, or an empty string if not connected.
     */
    @SuppressLint("MissingPermission")
    public static String getCurrentSsid(Context context) {
        WifiManager wifiManager = getWifiManager(context);
        if (!hasLocationPermission(context) || wifiManager == null) {
            return "";
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return "";
        }

        String ssid = wifiInfo.getSSID();
        if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    /**
     * Gets the list of available WiFi networks.
     *
     * @param context Context object.
     * @return List of ScanResult objects representing available networks.
     */
    @SuppressLint("MissingPermission")
    public static List<ScanResult> getAvailableNetworks(Context context) {
        WifiManager wifiManager = getWifiManager(context);
        return (wifiManager != null && hasLocationPermission(context))
                ? wifiManager.getScanResults()
                : new ArrayList<>();
    }

    /**
     * Connects to a WiFi network with the given SSID and password.
     *
     * @param context  Context object.
     * @param ssid     The SSID of the WiFi network.
     * @param password The password for the WiFi network.
     */
    public static void connectToNetwork(Context context, String ssid, String password) {
        connectToNetwork(context, ssid, password, true);
    }

    /**
     * Connects to an open WiFi network with the given SSID.
     *
     * @param context Context object.
     * @param ssid    The SSID of the WiFi network.
     */
    public static void connectToOpenNetwork(Context context, String ssid) {
        connectToNetwork(context, ssid, "", false);
    }

    private static void connectToNetwork(Context context, String ssid, String password, boolean hasPassword) {
        WifiManager wifiManager = getWifiManager(context);
        if (wifiManager == null) return;

        WifiConfiguration config = createWifiConfig(context, ssid, password, hasPassword);
        if (config == null) return;

        wifiManager.disableNetwork(wifiManager.getConnectionInfo().getNetworkId());
        int netId = wifiManager.addNetwork(config);
        wifiManager.enableNetwork(netId, true);
    }

    private static WifiConfiguration createWifiConfig(Context context, String ssid, String password, boolean hasPassword) {
        if (ssid == null || ssid.isEmpty()) return null;

        WifiManager wifiManager = getWifiManager(context);
        WifiConfiguration existingConfig = findExistingConfig(context, ssid);
        if (existingConfig != null) {
            wifiManager.removeNetwork(existingConfig.networkId);
        }

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";

        if (hasPassword) {
            config.preSharedKey = "\"" + password + "\"";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        } else {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        return config;
    }

    @SuppressLint("MissingPermission")
    private static WifiConfiguration findExistingConfig(Context context, String ssid) {
        WifiManager wifiManager = getWifiManager(context);
        if (!hasLocationPermission(context) || wifiManager == null) return null;

        List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations != null) {
            for (WifiConfiguration config : configurations) {
                if (("\"" + ssid + "\"").equals(config.SSID)) {
                    return config;
                }
            }
        }
        return null;
    }

    private static WifiManager getWifiManager(Context context) {
        if (context == null) return null;
        return (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    private static boolean hasLocationPermission(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}