package com.mx.mxSdk;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mx.mxSdk.Utils.RBQLog;

public class Device implements Parcelable {

    public static final String defaultIP = "192.168.0.10";
    public static final int defaultPort = 35000;

    public static final int defaultPrinterHead = 0;
    public static final int defaultLandscapePix = 600;
    public static final int defaultPortraitPix = 600;
    public static final int defaultDistance = 0;
    public static final int defaultCirculationTime = -1;
    public static final int defaultRepeatTime = 1;
    public static final int defaultDirection = 1;
    public static final String defaultMcuVersion = "0.0.0";

    public static final String defaultMac = "00:00:00:00:00:00";

    public String ssid;
    public String wifiName;
    public String ip;
    public int port;

    public BluetoothDevice bleDevice;
    //蓝牙的mac地址
    public String bleAddress;
    public String bleName;
    public String localName;

    //保存经典蓝牙信息
    public BluetoothDevice bluetoothDevice;
    //经典蓝牙的mac地址
    public String bluetoothAddress;
    public String bluetoothName;

    // 广播中的 mac 地址 有可能为null
    public String mac;

    public int connTypes;
    public ConnType connType;

    public String sPort;

    public int batteryLevel = -1;

    public DeviceModel deviceModel;

    public String mcuName;
    public String printer_head_id;
    public String mcuVersion;
    public String wifiVersion;
    public String mcu_date;
    public float temperature;
    public int printer_head;
    public int l_pix;
    public int p_pix;
    public int distance;
    public int circulation;
    public int repeat_time;
    public int horizontalDirection;
    public int verticalDirection;

    public int state;
    public boolean silentState;
    public boolean autoPowerOffState;
    public boolean continuousPrintState;

    private Device() {
        // 私有构造函数，防止直接实例化
    }

    protected Device(Parcel in) {
        ssid = in.readString();
        wifiName = in.readString();
        ip = in.readString();
        port = in.readInt();
        bleDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        bleAddress = in.readString();
        bleName = in.readString();
        localName = in.readString();
        bluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        bluetoothAddress = in.readString();
        bluetoothName = in.readString();
        mac = in.readString();
        connTypes = in.readInt();
        sPort = in.readString();
        deviceModel = in.readParcelable(DeviceModel.class.getClassLoader());
        mcuName = in.readString();
        printer_head_id = in.readString();
        mcuVersion = in.readString();
        wifiVersion = in.readString();
        mcu_date = in.readString();
        temperature = in.readFloat();
        printer_head = in.readInt();
        l_pix = in.readInt();
        p_pix = in.readInt();
        distance = in.readInt();
        circulation = in.readInt();
        repeat_time = in.readInt();
        horizontalDirection = in.readInt();
        verticalDirection = in.readInt();
        state = in.readInt();
        silentState = in.readByte() != 0;
        autoPowerOffState = in.readByte() != 0;
        // 读取 connType 的整数值并转换为 ConnType 枚举，默认值为0
        int connTypeValue = in.readInt();
        if (connTypeValue != 0) {
            connType = ConnType.fromValue(connTypeValue);
        } else {
            if (containsConnType(ConnType.SPP)) {
                connType = ConnType.SPP;
            } else if (containsConnType(ConnType.WiFi)) {
                connType = ConnType.WiFi;
            } else if (containsConnType(ConnType.AP)) {
                connType = ConnType.AP;
            } else if (containsConnType(ConnType.Serial)) {
                connType = ConnType.Serial;
            } else {
                connType = ConnType.SPP; // 如果没有匹配项，默认设置为 SPP
            }
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ssid);
        dest.writeString(wifiName);
        dest.writeString(ip);
        dest.writeInt(port);
        dest.writeParcelable(bleDevice, flags);
        dest.writeString(bleAddress);
        dest.writeString(bleName);
        dest.writeString(localName);
        dest.writeParcelable(bluetoothDevice, flags);
        dest.writeString(bluetoothAddress);
        dest.writeString(bluetoothName);
        dest.writeString(mac);
        dest.writeInt(connTypes);
        dest.writeString(sPort);
        dest.writeParcelable(deviceModel, flags);
        dest.writeString(mcuName);
        dest.writeString(printer_head_id);
        dest.writeString(mcuVersion);
        dest.writeString(wifiVersion);
        dest.writeString(mcu_date);
        dest.writeFloat(temperature);
        dest.writeInt(printer_head);
        dest.writeInt(l_pix);
        dest.writeInt(p_pix);
        dest.writeInt(distance);
        dest.writeInt(circulation);
        dest.writeInt(repeat_time);
        dest.writeInt(horizontalDirection);
        dest.writeInt(verticalDirection);
        dest.writeInt(state);
        dest.writeByte((byte) (silentState ? 1 : 0));
        dest.writeByte((byte) (autoPowerOffState ? 1 : 0));
        // 写入 connType 的整数值
        dest.writeInt(connType != null ? connType.getValue() : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    // 工厂方法用于创建串口类型设备
    public static Device createSerialDevice(String sPort) {
        Device device = new Device();
        device.sPort = sPort;
        device.connTypes = ConnType.Serial.getValue();
        device.connType = ConnType.Serial;
        device.initializeDefaults();
        return device;
    }

    // 工厂方法用于创建 Bluetooth 设备
    @SuppressLint("MissingPermission")
    public static Device createSppDevice(@NonNull BluetoothDevice bluetoothDevice, @Nullable BluetoothDevice bleDevice,String mac, int connTypes, @Nullable DeviceModel deviceModel) {
        Device device = new Device();
        device.bluetoothDevice = bluetoothDevice;
        device.bluetoothAddress = bluetoothDevice.getAddress();
        device.bluetoothName = bluetoothDevice.getName();
        device.connTypes = connTypes;
        device.connType = ConnType.SPP;
        device.bleDevice = bleDevice;
        if (bleDevice != null){
            device.bleName = bleDevice.getName();
            device.bleAddress = bleDevice.getAddress();
        }
        device.mac = mac;
        device.deviceModel = deviceModel;
        device.initializeDefaults();
        return device;
    }

    // 工厂方法用于创建 AP 类型设备
    @SuppressLint("MissingPermission")
    public static Device createApDevice(String ssid, BluetoothDevice bleDevice,String mac, int connTypes, @Nullable DeviceModel deviceModel) {
        Device device = new Device();
        device.ssid = ssid;
        device.connTypes = connTypes;
        device.connType = ConnType.AP;
        device.ip = defaultIP;
        device.port = defaultPort;
        device.mac = defaultMac;
        device.bleDevice = bleDevice;
        if (bleDevice != null){
            device.bleName = bleDevice.getName();
            device.bleAddress = bleDevice.getAddress();
        }
        device.mac = mac;
        device.deviceModel = deviceModel;
        device.initializeDefaults();
        return device;
    }

    // 工厂方法用于创建 WiFi 设备
    @SuppressLint("MissingPermission")
    public static Device createWifiDevice(String wifiName, String ip, int port, BluetoothDevice bleDevice,String mac, int connTypes, @Nullable DeviceModel deviceModel) {
        Device device = new Device();
        device.wifiName = wifiName;
        device.ip = ip;
        device.port = port;
        device.mac = mac;
        device.bleDevice = bleDevice;
        if (bleDevice != null){
            device.bleName = bleDevice.getName();
            device.bleAddress = bleDevice.getAddress();
        }
        device.initializeDefaults();
        device.connTypes = connTypes;
        device.connType = ConnType.WiFi;
        device.deviceModel = deviceModel;
        return device;
    }

    // 初始化默认值的公共方法
    private void initializeDefaults() {
        this.printer_head = defaultPrinterHead;
        this.l_pix = defaultLandscapePix;
        this.p_pix = defaultPortraitPix;
        this.distance = defaultDistance;
        this.circulation = defaultCirculationTime;
        this.repeat_time = defaultRepeatTime;
        this.horizontalDirection = defaultDirection;
        this.verticalDirection = defaultDirection;
        this.mcuVersion = defaultMcuVersion;
    }

    @SuppressLint("MissingPermission")
    public void setBleDevice(BluetoothDevice bleDevice) {
        this.bleDevice = bleDevice;
        if (bleDevice != null){
            bleAddress = bleDevice.getAddress();
            bleName = bleDevice.getName();
        }
    }

    @SuppressLint("MissingPermission")
    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        if (bluetoothDevice != null){
            bluetoothAddress = bluetoothDevice.getAddress();
            bluetoothName = bluetoothDevice.getName();
        }
    }

    public boolean isSPPConnType() {
        return this.connType == ConnType.SPP;
    }

    public boolean isApConnType() {
        return this.connType == ConnType.AP;
    }

    public boolean isWifiConnType() {
        return this.connType == ConnType.WiFi;
    }

    public boolean isApOrWifiConnType() {
        return this.connType == ConnType.AP || this.connType == ConnType.WiFi;
    }

    public boolean isSerialConnType() {
        return this.connType == ConnType.Serial;
    }

    public boolean containsConnType(ConnType connType) {
        return (connTypes & connType.getValue()) != 0;
    }

    public boolean containsConnType(int connTypes, ConnType connType) {
        return (connTypes & connType.getValue()) != 0;
    }

    public void addConnType(ConnType connType) {
        connTypes |= connType.getValue();
    }

    public void removeConnType(ConnType connType) {
        connTypes &= ~connType.getValue();
    }

    public int getDeviceType() {
        return deviceModel == null ? DeviceDefinitionRegistry.DeviceType_MX_02 : deviceModel.getDeviceType();
    }

    public String getAliases() {
        if (deviceModel == null){
//            if (this.isSPPConnType()){
//                return this.bluetoothName;
//            } else if (this.isApConnType()) {
//                return this.ssid;
//            } else if (this.isWifiConnType()) {
//                return this.wifiName;
//            } else if (this.isSerialConnType()) {
//                return this.sPort;
//            }else {
                return DeviceDefinitionRegistry.DeviceModel_MX_02;
//            }
        }
        return deviceModel.getAliases();
    }

    public String getShortAliases() {
        if (deviceModel == null){
//            if (this.isSPPConnType()){
//                return this.bluetoothName;
//            } else if (this.isApConnType()) {
//                return this.ssid;
//            } else if (this.isWifiConnType()) {
//                return this.wifiName;
//            } else if (this.isSerialConnType()) {
//                return this.sPort;
//            }else {
                return DeviceDefinitionRegistry.DeviceModel_MX_02;
//            }
        }
        return deviceModel.getShortAliases();
    }

    public Boolean isWifiReady() {
        return !TextUtils.isEmpty(ip) && port != 0;
    }

    public Boolean isSppReady(){
        return bluetoothDevice != null;
    }

    public String getName() {
        if (isSPPConnType()) {
            return bleName != null ? bleName : (localName != null ? localName : "");
        } else if (isApConnType()) {
            return ssid;
        } else if (isWifiConnType()) {
            return wifiName;
        } else if (isSerialConnType()) {
            return sPort;
        } else {
            return "";
        }
    }

    public String getMcuModel() {
        if (this.mcuVersion == null) {
            return "";
        }
        RBQLog.i("Version", "版本号 mcu_version: " + this.mcuVersion);
        String uppercasedVersion = this.mcuVersion.toUpperCase();

        if (uppercasedVersion.contains("_") && uppercasedVersion.contains("INKSI")) {
            String[] versionComponents = this.mcuVersion.split("_");
            for (String component : versionComponents) {
                if (component.toUpperCase().contains("INKSI")) {
                    return component;
                }
            }
            return "INKSI-01";
        }

        if (uppercasedVersion.contains("_") && uppercasedVersion.contains("CRYPTO")) {
            String[] versionComponents = this.mcuVersion.split("_");
            for (String component : versionComponents) {
                if (component.toUpperCase().contains("CRYPTO")) {
                    return component;
                }
            }
            return "CRYPTO STAMP";
        }

        if (uppercasedVersion.contains("_") && uppercasedVersion.contains("MX")) {
            String[] versionComponents = this.mcuVersion.split("_");
            for (String component : versionComponents) {
                if (component.toUpperCase().contains("MX")) {
                    return component;
                }
            }
            return "MX-06";
        }
        return "MX-02";
    }

    public String getMcu_Mode() {
        if (this.mcuVersion == null) return "";
        RBQLog.i("Version", "版本号 mcu_version: " + this.mcuVersion);

        String[] vsArr = this.mcuVersion.split("_");
        if (vsArr.length == 4) {
            return vsArr[2];
        }
        if (vsArr.length == 5) {
            return vsArr[3];
        }
        return "MX-02";
    }

    public String getMcuVersion() {
        if (this.mcuVersion == null) return "";
        RBQLog.i("Version", "版本号 mcu_version: " + this.mcuVersion);

        String[] vsArr = this.mcuVersion.split("_");
        if (vsArr.length == 4) {
            return vsArr[3];
        }
        if (vsArr.length == 5) {
            return vsArr[4];
        }
        if (vsArr.length > 0) {
            return vsArr[0];
        }
        return this.mcuVersion;
    }

    public String getMcuHwVersion() {
        if (this.mcuVersion == null) return "";
        RBQLog.i("Version", "版本号 mcu_version: " + this.mcuVersion);

        String[] vsArr = this.mcuVersion.split("_");
        if (vsArr.length == 4) {
            return vsArr[2];
        }
        if (vsArr.length == 5) {
            return vsArr[3];
        }
        if (vsArr.length > 0) {
            return vsArr[0];
        }
        return this.mcuVersion;
    }

    public String getWifiModel() {
        if (this.wifiVersion == null) return "";
        RBQLog.i("Version", "版本号 wifiVersion: " + this.wifiVersion);

        String[] vsArr = this.wifiVersion.split("_");
        if (vsArr.length == 4) {
            return vsArr[0] + "_" + vsArr[2];
        }
        if (vsArr.length == 5) {
            return vsArr[3];
        }
        return "MX-02";
    }

    public String getWifi_Model() {
        if (this.wifiVersion == null) return "";
        RBQLog.i("Version", "版本号 wifiVersion: " + this.wifiVersion);

        String[] vsArr = this.wifiVersion.split("_");
        if (vsArr.length == 4) {
            return vsArr[2];
        }
        if (vsArr.length == 5) {
            return vsArr[3];
        }
        return "MX-02";
    }

    public String getWifiVersion() {
        if (this.wifiVersion == null) return "";
        RBQLog.i("Version", "版本号 wifiVersion: " + this.wifiVersion);

        String[] vsArr = this.wifiVersion.split("_");
        if (vsArr.length == 4) {
            return vsArr[3];
        }
        if (vsArr.length == 5) {
            return vsArr[4];
        }
        if (vsArr.length > 0) {
            return vsArr[0];
        }
        return this.wifiVersion;
    }

    public String getWifiHwVersion() {
        if (this.wifiVersion == null) return "";
        RBQLog.i("Version", "版本号 wifiVersion: " + this.wifiVersion);

        String[] vsArr = this.wifiVersion.split("_");
        if (vsArr.length == 4) {
            return vsArr[2];
        }
        if (vsArr.length == 5) {
            return vsArr[3];
        }
        if (vsArr.length > 0) {
            return vsArr[0];
        }
        return this.wifiVersion;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device other = (Device) o;
        // (this.bluetoothDevice != null && this.bluetoothDevice.equals(other.bluetoothDevice))相当于(this.bluetoothAddress != null && this.bluetoothAddress.equals(other.address))
        return (this.bluetoothAddress != null && this.bluetoothAddress.equals(other.bluetoothAddress))
                || this.bluetoothAddress != null && this.bluetoothAddress.equals(other.mac)
                || (this.mac != null && this.mac.equals(other.mac))
                || this.mac != null && this.mac.equals(other.bluetoothAddress)
                || ((this.ip != null && this.ip.equals(other.ip))&&(this.sPort != null && this.sPort.equals(other.sPort)));
    }

    @Override
    public String toString() {
        return "Device{" +
                "ssid='" + ssid + '\'' +
                ", wifiName='" + wifiName + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", bleDevice=" + bleDevice +
                ", bleAddress='" + bleAddress + '\'' +
                ", bleName='" + bleName + '\'' +
                ", localName='" + localName + '\'' +
                ", bluetoothDevice=" + bluetoothDevice +
                ", bluetoothAddress='" + bluetoothAddress + '\'' +
                ", bluetoothName='" + bluetoothName + '\'' +
                ", mac='" + mac + '\'' +
                ", connTypes=" + connTypes +
                ", connType=" + connType +
                ", sPort='" + sPort + '\'' +
                ", deviceModel=" + deviceModel +
                ", mcuName='" + mcuName + '\'' +
                ", printer_head_id='" + printer_head_id + '\'' +
                ", mcuVersion='" + mcuVersion + '\'' +
                ", wifiVersion='" + wifiVersion + '\'' +
                ", mcu_date='" + mcu_date + '\'' +
                ", temperature=" + temperature +
                ", printer_head=" + printer_head +
                ", l_pix=" + l_pix +
                ", p_pix=" + p_pix +
                ", distance=" + distance +
                ", circulation=" + circulation +
                ", repeat_time=" + repeat_time +
                ", horizontalDirection=" + horizontalDirection +
                ", verticalDirection=" + verticalDirection +
                ", state=" + state +
                ", silentState=" + silentState +
                ", autoPowerOffState=" + autoPowerOffState +
                '}';
    }
}