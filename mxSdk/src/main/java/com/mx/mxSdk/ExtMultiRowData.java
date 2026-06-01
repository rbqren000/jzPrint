package com.mx.mxSdk;

import android.os.Parcel;
import android.os.Parcelable;

public class ExtMultiRowData implements Parcelable {

    private final int validValueCount;
    private final MultiRowData multiRowData;

    ExtMultiRowData(MultiRowData multiRowData, int validValueCount) {
        this.multiRowData = multiRowData;
        this.validValueCount = validValueCount;
    }

    public static ExtMultiRowData createInstance(MultiRowData multiRowData, int validValueCount) {
        return new ExtMultiRowData(multiRowData, validValueCount);
    }

    protected ExtMultiRowData(Parcel in) {
        multiRowData = in.readParcelable(MultiRowData.class.getClassLoader());
        validValueCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(multiRowData, flags);
        dest.writeInt(validValueCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ExtMultiRowData> CREATOR = new Creator<ExtMultiRowData>() {
        @Override
        public ExtMultiRowData createFromParcel(Parcel in) {
            return new ExtMultiRowData(in);
        }

        @Override
        public ExtMultiRowData[] newArray(int size) {
            return new ExtMultiRowData[size];
        }
    };

    public int getValidValueCount() {
        return validValueCount;
    }

    public MultiRowData getMultiRowData() {
        return multiRowData;
    }
}

