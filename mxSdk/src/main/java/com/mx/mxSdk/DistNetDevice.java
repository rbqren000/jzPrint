package com.mx.mxSdk;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mx.mxSdk.BleCenter.BleDevice;

import java.util.HashMap;
import java.util.Map;

public class DistNetDevice extends BleDevice {

	public DeviceModel deviceModel;

	// 当前设备支持的连接方式（用位运算表示多个类型）
	private int connTypes;

	// 固件类型及其支持的连接方式
	private Map<Integer, Integer> firmwareConfigs;

	// 蓝牙设备名称
	private String bleName;

	// 蓝牙mac地址
	private String bleAddress;

	// 本地名称
	private String localName;

	// ble广播中的 MAC 地址
	private String mac;

	// 设备状态
	private int state;

	@SuppressLint("MissingPermission")
	public DistNetDevice(@NonNull BluetoothDevice bleDevice, @Nullable String localName, @NonNull String mac, int state, int connTypes, @Nullable Map<Integer, Integer> firmwareConfigs, DeviceModel deviceModel) {
		super(bleDevice);
		this.bleName = bleDevice.getName();
		this.bleAddress = bleDevice.getAddress();
		this.localName = localName;
		this.mac = mac;
		this.state = state;
		this.connTypes = connTypes;
		this.firmwareConfigs = firmwareConfigs;
		this.deviceModel = deviceModel;
	}

	// Getters and Setters

	public DeviceModel getDeviceModel() {
		return deviceModel;
	}

	public void setDeviceModel(DeviceModel deviceModel) {
		this.deviceModel = deviceModel;
	}

	public int getConnTypes() {
		return connTypes;
	}

	public void setConnTypes(int connTypes) {
		this.connTypes = connTypes;
	}

	public Map<Integer, Integer> getFirmwareConfigs() {
		return firmwareConfigs;
	}

	public void setFirmwareConfigs(Map<Integer, Integer> firmwareConfigs) {
		this.firmwareConfigs = firmwareConfigs;
	}

	public String getBleName() {
		return bleName;
	}

	public void setBleName(String bleName) {
		this.bleName = bleName;
	}

	public String getBleAddress() {
		return bleAddress;
	}

	public void setBleAddress(String bleAddress) {
		this.bleAddress = bleAddress;
	}

	public String getLocalName() {
		return localName;
	}

	public void setLocalName(String localName) {
		this.localName = localName;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	// Connection Type Methods

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

	// Parcelable Implementation

	public static final Parcelable.Creator<DistNetDevice> CREATOR = new Parcelable.Creator<DistNetDevice>() {
		public DistNetDevice createFromParcel(Parcel in) {
			return new DistNetDevice(in);
		}

		public DistNetDevice[] newArray(int size) {
			return new DistNetDevice[size];
		}
	};

    public void writeToParcel(Parcel out, int flags) {
		super.writeToParcel(out, flags);
		out.writeInt(state);
		out.writeString(mac);
		out.writeString(bleName);
		out.writeString(bleAddress);
		out.writeString(localName);
		out.writeInt(connTypes);
//		out.writeMap(firmwareConfigs);
		// 明确写入 Map 的大小和内容，避免使用 writeMap
		if (firmwareConfigs != null) {
			out.writeInt(firmwareConfigs.size());
			for (Map.Entry<Integer, Integer> entry : firmwareConfigs.entrySet()) {
				out.writeInt(entry.getKey());
				out.writeInt(entry.getValue());
			}
		} else {
			out.writeInt(0);
		}
		out.writeParcelable(deviceModel, flags);
	}

	private DistNetDevice(Parcel in) {
		super(in);
		state = in.readInt();
		mac = in.readString();
		bleName = in.readString();
		bleAddress = in.readString();
		localName = in.readString();
		connTypes = in.readInt();
//		firmwareConfigs = in.readHashMap(Integer.class.getClassLoader());
		// 明确读取 Map 的大小和内容
		int mapSize = in.readInt();
		firmwareConfigs = new HashMap<>();
		for (int i = 0; i < mapSize; i++) {
			Integer key = in.readInt();
			Integer value = in.readInt();
			firmwareConfigs.put(key, value);
		}
		deviceModel = in.readParcelable(DeviceModel.class.getClassLoader());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DistNetDevice distNetDevice = (DistNetDevice) o;
		return (this.bleAddress!=null && this.bleAddress.equals(distNetDevice.bleAddress))
				||(this.mac!=null&&this.mac.equals(distNetDevice.mac));
	}

	@Override
	public String toString() {
		return "DistNetDevice{" +
				"deviceModel=" + deviceModel +
				", connTypes=" + connTypes +
				", firmwareConfigs=" + firmwareConfigs +
				", bleName='" + bleName + '\'' +
				", bleAddress='" + bleAddress + '\'' +
				", localName='" + localName + '\'' +
				", mac='" + mac + '\'' +
				", state=" + state +
				'}';
	}
}

