package com.mx.mxSdk;

import static com.mx.mxSdk.DeviceDefinitionRegistry.DeviceModel_MX_02;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;

public class DeviceModel implements Parcelable {

    //设备型号
    private final int deviceType;
    private String aliases;
    private String shortAliases;
    //设备型号，例如inksi-01的BX20
    private String deviceModel;

    public DeviceModel(int deviceType, String aliases, @Nullable String deviceModel) {
        this.deviceType = deviceType;
        this.aliases = aliases;
        this.shortAliases = generateShortAliases(aliases);
        this.deviceModel = deviceModel;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public String getAliases() {
        return aliases;
    }

    public void setAliases(String aliases) {
        this.aliases = aliases;
        this.shortAliases = generateShortAliases(aliases);
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getShortAliases() {
        if (this.aliases == null) {
            return DeviceModel_MX_02;
        }
        String[] components = aliases.split("_");
        return components.length > 0 ? components[0] : DeviceModel_MX_02;
    }

    private String generateShortAliases(String aliases) {
        // 根据传入的 aliases 生成短别名的逻辑
        return aliases.replace("-", ""); // 示例：去掉所有的“-”
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(deviceType);
        dest.writeString(aliases);
        dest.writeString(shortAliases);
        dest.writeString(deviceModel); // 新增的字段
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected DeviceModel(Parcel in) {
        deviceType = in.readInt();
        aliases = in.readString();
        shortAliases = in.readString();
        deviceModel = in.readString(); // 新增的字段
    }

    public static final Creator<DeviceModel> CREATOR = new Creator<DeviceModel>() {
        @Override
        public DeviceModel createFromParcel(Parcel in) {
            return new DeviceModel(in);
        }

        @Override
        public DeviceModel[] newArray(int size) {
            return new DeviceModel[size];
        }
    };

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return this.deviceType == ((DeviceModel) obj).deviceType;
    }

    @Override
    public String toString() {
        return "DeviceModel{" +
                "deviceType=" + deviceType +
                ", aliases='" + aliases + '\'' +
                ", shortAliases='" + shortAliases + '\'' +
                ", device_model='" + deviceModel + '\'' + // 新增的字段
                '}';
    }
}

