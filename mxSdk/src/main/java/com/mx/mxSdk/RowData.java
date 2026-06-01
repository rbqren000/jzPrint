package com.mx.mxSdk;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.mx.mxSdk.Utils.MxSdkStore;

public class RowData implements Parcelable {

    public static final String ROW_DATA = "rowData";

    private final int dataLength;
    private final String dataPath;
    private final boolean compress;

    RowData(@NonNull String dataPath, int dataLength, boolean compress) {
        this.dataPath = dataPath;
        this.dataLength = dataLength;
        this.compress = compress;
    }

    public static RowData createInstance(@NonNull String path, int dataLength, boolean compress) {
        return new RowData(path, dataLength, compress);
    }

    protected RowData(Parcel in) {
        dataLength = in.readInt();
        dataPath = in.readString();
        compress = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(dataLength);
        dest.writeString(dataPath);
        dest.writeByte((byte) (compress ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RowData> CREATOR = new Creator<RowData>() {
        @Override
        public RowData createFromParcel(Parcel in) {
            return new RowData(in);
        }

        @Override
        public RowData[] newArray(int size) {
            return new RowData[size];
        }
    };

    public int getDataLength() {
        return dataLength;
    }

    public String getDataPath() {
        return dataPath;
    }

    public boolean isCompress() {
        return compress;
    }

    public int compressValue() {
        return this.compress ? 1 : 0;
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
