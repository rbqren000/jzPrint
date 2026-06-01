package com.mx.mxSdk;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

public class ExtRowData implements Parcelable {

    private final int validValueCount;
    private final RowData rowData;

    ExtRowData(@NonNull RowData rowData, int validValueCount) {
        this.rowData = rowData;
        this.validValueCount = validValueCount;
    }

    public static ExtRowData createInstance(@NonNull RowData rowData, int validValueCount) {
        return new ExtRowData(rowData, validValueCount);
    }

    protected ExtRowData(Parcel in) {
        rowData = in.readParcelable(RowData.class.getClassLoader());
        validValueCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(rowData, flags);
        dest.writeInt(validValueCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ExtRowData> CREATOR = new Creator<ExtRowData>() {
        @Override
        public ExtRowData createFromParcel(Parcel in) {
            return new ExtRowData(in);
        }

        @Override
        public ExtRowData[] newArray(int size) {
            return new ExtRowData[size];
        }
    };

    public int getValidValueCount() {
        return validValueCount;
    }

    public RowData getRowData() {
        return rowData;
    }
}


