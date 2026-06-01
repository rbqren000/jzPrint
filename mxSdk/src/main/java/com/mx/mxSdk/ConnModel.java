package com.mx.mxSdk;

import static com.mx.mxSdk.DeviceDefinitionRegistry.DeviceModel_MX_02;
import static com.mx.mxSdk.DeviceDefinitionRegistry.DeviceType_MX_02;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import androidx.annotation.Nullable;
import java.util.Map;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.HashMap;

public class ConnModel implements Parcelable {

    private DeviceModel deviceModel;
    private int connTypes;
    private Map<Integer, Integer> firmwareConfigs;

    private BluetoothDevice bleDevice;
    private String bleName;
    private String bleAddress;
    private String localName;

    private String mac;

    private BluetoothDevice bluetoothDevice;
    private String bluetoothAddress;
    private String bluetoothName;

    private final int state;
    private String wifiName;
    private String ip;
    private int port;

    @SuppressLint("MissingPermission")
    public ConnModel(@Nullable BluetoothDevice bleDevice, @Nullable String localName, int connTypes,
                     @Nullable Map<Integer, Integer> firmwareConfigs, @Nullable String mac,
                     @Nullable BluetoothDevice bluetoothDevice, int state, @Nullable String wifiName,
                     @Nullable String ip, int port, @Nullable DeviceModel deviceModel) {

        this.bleDevice = bleDevice;
        if (bleDevice != null){
            this.bleName = bleDevice.getName();
            this.bleAddress = bleDevice.getAddress();
        }

        this.localName = localName;
        this.connTypes = connTypes;
        this.firmwareConfigs = firmwareConfigs;
        this.mac = mac;

        this.bluetoothDevice = bluetoothDevice;
        if (this.bluetoothDevice != null) {
            this.bluetoothName = bluetoothDevice.getName();
            this.bluetoothAddress = bluetoothDevice.getAddress();
        }

        this.state = state;

        this.wifiName = wifiName;
        this.ip = ip;
        this.port = port;

        this.deviceModel = deviceModel;
    }

    public ConnModel(@Nullable BluetoothDevice bleDevice, @Nullable String localName, int connTypes,
                     @Nullable Map<Integer, Integer> firmwareConfigs, @Nullable String mac,
                     @Nullable BluetoothDevice bluetoothDevice, int state,
                     @Nullable DeviceModel deviceModel) {
        this(bleDevice, localName, connTypes, firmwareConfigs, mac, bluetoothDevice, state, null, null, 0, deviceModel);
    }

    protected ConnModel(Parcel in) {
        deviceModel = in.readParcelable(DeviceModel.class.getClassLoader());
        connTypes = in.readInt();
//        firmwareConfigs = new HashMap<>();
//        in.readMap(firmwareConfigs, Integer.class.getClassLoader());
// 明确读取 Map 的大小和内容
        int mapSize = in.readInt();
        firmwareConfigs = new HashMap<>();
        for (int i = 0; i < mapSize; i++) {
            Integer key = in.readInt();
            Integer value = in.readInt();
            firmwareConfigs.put(key, value);
        }
        bleDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        bleName = in.readString();
        bleAddress = in.readString();
        localName = in.readString();
        mac = in.readString();
        bluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        bluetoothAddress = in.readString();
        bluetoothName = in.readString();
        state = in.readInt();
        wifiName = in.readString();
        ip = in.readString();
        port = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(deviceModel, flags);
        dest.writeInt(connTypes);
//        dest.writeMap(firmwareConfigs);
        // 明确写入 Map 的大小和内容，避免使用 writeMap
        if (firmwareConfigs != null) {
            dest.writeInt(firmwareConfigs.size());
            for (Map.Entry<Integer, Integer> entry : firmwareConfigs.entrySet()) {
                dest.writeInt(entry.getKey());
                dest.writeInt(entry.getValue());
            }
        } else {
            dest.writeInt(0);
        }
        dest.writeParcelable(bleDevice, flags);
        dest.writeString(bleName);
        dest.writeString(bleAddress);
        dest.writeString(localName);
        dest.writeString(mac);
        dest.writeParcelable(bluetoothDevice, flags);
        dest.writeString(bluetoothAddress);
        dest.writeString(bluetoothName);
        dest.writeInt(state);
        dest.writeString(wifiName);
        dest.writeString(ip);
        dest.writeInt(port);
    }

    public static final Creator<ConnModel> CREATOR = new Creator<ConnModel>() {
        @Override
        public ConnModel createFromParcel(Parcel in) {
            return new ConnModel(in);
        }

        @Override
        public ConnModel[] newArray(int size) {
            return new ConnModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean containsConnType(int connType) {
        return (connTypes & connType) != 0;
    }

    public void addConnType(int connType) {
        connTypes |= connType;
    }

    public void removeConnType(int connType) {
        connTypes &= ~connType;
    }

    public boolean containsFirmwareType(int firmwareType) {
        return firmwareConfigs != null && firmwareConfigs.containsKey(firmwareType);
    }

    public boolean containsFirmwareTypeWithConnType(int firmwareType, int connType) {
        if (firmwareConfigs == null || !firmwareConfigs.containsKey(firmwareType)) {
            return false;
        }
        Integer connTypesForFirmware = firmwareConfigs.get(firmwareType);
        return connTypesForFirmware != null && (connTypesForFirmware & connType) != 0;
    }

    public int connTypesForFirmwareType(int firmwareType) {
        if (firmwareConfigs == null) {
            return 0;
        }
        Integer connTypesForFirmware = firmwareConfigs.get(firmwareType);
        return connTypesForFirmware != null ? connTypesForFirmware : 0;
    }

    public int getDeviceType() {
        if (deviceModel == null) {
            return DeviceType_MX_02;
        }
        return deviceModel.getDeviceType();
    }

    public boolean isWifiReady() {
        return !TextUtils.isEmpty(ip) && port != 0;
    }

    @SuppressLint("MissingPermission")
    public void setBleDevice(BluetoothDevice bleDevice) {
        this.bleDevice = bleDevice;
        if (bleDevice != null){
            this.bleAddress = bleDevice.getAddress();
            this.bleName = bleDevice.getName();
        }
    }

    public void setLocalName(@Nullable String localName) {
        this.localName = localName;
    }

    public String getLocalName() {
        return localName;
    }

    public void setFirmwareConfigs(@Nullable Map<Integer, Integer> firmwareConfigs) {
        this.firmwareConfigs = firmwareConfigs;
    }

    public Map<Integer, Integer> getFirmwareConfigs() {
        return firmwareConfigs;
    }

    public void setMac(@Nullable String mac) {
        this.mac = mac;
    }

    public String getMac() {
        return mac;
    }

    public void setWifiName(@Nullable String wifiName) {
        this.wifiName = wifiName;
    }

    public String getWifiName() {
        return wifiName;
    }

    public void setDeviceModel(@Nullable DeviceModel deviceModel) {
        this.deviceModel = deviceModel;
    }

    public DeviceModel getDeviceModel() {
        return deviceModel;
    }

    public int getConnTypes() {
        return connTypes;
    }

    public void setConnTypes(int connTypes) {
        this.connTypes = connTypes;
    }

    public String getAliases() {
        if (deviceModel == null){
            return DeviceModel_MX_02;
        }
        return deviceModel.getAliases();
    }

    public String getShortAliases() {
        if (deviceModel == null){
            // 02是没有deviceModel可初始化的，因为无法验证ble和bluetooth的对应关系
            return DeviceModel_MX_02;
        }
        return deviceModel.getShortAliases();
    }

    public BluetoothDevice getBleDevice() {
        return bleDevice;
    }

    public String getBleName() {
        return bleName;
    }

    public String getBleAddress() {
        return bleAddress;
    }

    @SuppressLint("MissingPermission")
    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        if (bluetoothDevice != null) {
            bluetoothAddress = bluetoothDevice.getAddress();
            bluetoothName = bluetoothDevice.getName();
        }
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public String getBluetoothName() {
        return bluetoothName;
    }

    public void setIp(@Nullable String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getState() {
        return state;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ConnModel other = (ConnModel) obj;
        return (this.bleAddress != null && this.bleAddress.equals(other.bleAddress))
                || (this.bluetoothAddress != null && this.bluetoothAddress.equals(other.bluetoothAddress))
                || this.bluetoothAddress != null && this.bluetoothAddress.equals(other.mac)
                || this.mac != null && this.mac.equals(other.bluetoothAddress)
                || (this.mac != null && this.mac.equals(other.mac));
    }

    @Override
    public String toString() {
        return "ConnModel{" +
                "deviceModel=" + deviceModel +
                ", connTypes=" + connTypes +
                ", firmwareConfigs=" + firmwareConfigs +
                ", bleDevice=" + bleDevice +
                ", bleName='" + bleName + '\'' +
                ", bleAddress='" + bleAddress + '\'' +
                ", localName='" + localName + '\'' +
                ", mac='" + mac + '\'' +
                ", bluetoothDevice=" + bluetoothDevice +
                ", bluetoothAddress='" + bluetoothAddress + '\'' +
                ", bluetoothName='" + bluetoothName + '\'' +
                ", state=" + state +
                ", wifiName='" + wifiName + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}
