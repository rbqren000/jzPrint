package com.mx.mxSdk;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.mx.mxSdk.Utils.MxSdkStore;

public class LogoData implements Parcelable {
    /** 数据长度 */
    private final int dataLength;
    /** 数据存储的路径 */
    private final String dataPath;
    /** 生成的预览图存储的路径 */
    private final String imagePath;

    private LogoData(@NonNull String dataPath, int dataLength,String imagePath) {
        this.dataPath = dataPath;
        this.dataLength = dataLength;
        this.imagePath = imagePath;
    }

    public static LogoData createInstance(@NonNull String path, int dataLength,String imagePath) {
        return new LogoData(path, dataLength,imagePath);
    }

    protected LogoData(Parcel in) {
        dataLength = in.readInt();
        dataPath = in.readString();
        imagePath = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(dataLength);
        dest.writeString(dataPath);
        dest.writeString(imagePath);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LogoData> CREATOR = new Creator<LogoData>() {
        @Override
        public LogoData createFromParcel(Parcel in) {
            return new LogoData(in);
        }

        @Override
        public LogoData[] newArray(int size) {
            return new LogoData[size];
        }
    };

    public int getDataLength() {
        return dataLength;
    }

    public String getDataPath() {
        return dataPath;
    }

    public String getImagePath() {
        return imagePath;
    }

    public int totalPacketCount(int usefulDataLen) {
        if (dataLength % usefulDataLen == 0) {
            return dataLength / usefulDataLen;
        } else {
            return dataLength / usefulDataLen + 1;
        }
    }

    public byte[] data() {

        return MxSdkStore.readByteArrToCacheDataFile(dataPath);
    }
}
