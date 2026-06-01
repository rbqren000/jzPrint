package com.mx.mxSdk;

import android.text.TextUtils;

public class MxUtils {
    public static boolean isSSIDConnModel(ConnModel model, String ssid) {
        // 参数检查
        if (model == null || ssid == null || ssid.length() < 4) {
            return false;
        }

        // 计算 ssid 的后四位
        String ssidLastFourMac = ssid.substring(ssid.length() - 4).toLowerCase();

        String modelMac = model.getMac();
        if (modelMac == null) {
            return false;
        }

        // 去掉 MAC 地址中的冒号
        String modelMacWithoutColons = modelMac.replace(":", "").toLowerCase();

        // 如果处理后的 MAC 地址长度小于 4，跳过此检查
        if (modelMacWithoutColons.length() < 4) {
            return false;
        }

        // 提取处理后 MAC 地址的后四位并转换为小写
        String modelLastFourMac = modelMacWithoutColons.substring(modelMacWithoutColons.length() - 4);

        // 如果 MAC 地址的后四位与 SSID 的最后一部分匹配
        return ssidLastFourMac.equals(modelLastFourMac);
    }

    public static boolean isSSidDevice(Device device, String ssid) {
        // 参数检查
        if (device == null || TextUtils.isEmpty(device.mac) || ssid == null || ssid.length() < 4) {
            return false;
        }
        // 计算 ssid 的后四位
        String ssidLastFourMac = ssid.substring(ssid.length() - 4).toLowerCase();

        // 去掉 MAC 地址中的冒号
        String deviceMacWithoutColons = device.mac.replace(":", "").toLowerCase();

        // 如果处理后的 MAC 地址长度小于 4，跳过此检查
        if (deviceMacWithoutColons.length() < 4) {
            return false;
        }

        // 提取处理后 MAC 地址的后四位并转换为小写
        String deviceLastFourMac = deviceMacWithoutColons.substring(deviceMacWithoutColons.length() - 4);
        // 如果 mac 的后四位与 ssid 的最后一部分匹配
        return ssidLastFourMac.equals(deviceLastFourMac);
    }

    public static boolean isEqualModel(ConnModel model, Device device) {
        if (model == null || device == null) {
            return false;
        }
        return (model.getBluetoothAddress() != null && model.getBluetoothAddress().equals(device.bluetoothAddress)) ||
                (model.getBleAddress() != null && model.getBleAddress().equals(device.bleAddress)) ||
                (model.getMac() != null && model.getMac().equals(device.mac)) ||
                ((model.getPort() == device.port) && model.getIp() != null && model.getIp().equals(device.ip));
    }

    public static boolean isPrinterAp(String ssid) {
        return !TextUtils.isEmpty(ssid) && ssid.toLowerCase().contains("inksi")||ssid.toLowerCase().contains("crypto stamp");
    }

}
