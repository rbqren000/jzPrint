package com.mx.mxSdk;

import android.os.Parcel;
import android.os.Parcelable;

public class ExtCMYKMultiRowData implements Parcelable {

    private final int validValueCount;
    private final CMYKMultiRowData cmykMultiRowData;

    ExtCMYKMultiRowData(CMYKMultiRowData cmykMultiRowData, int validValueCount) {
        this.cmykMultiRowData = cmykMultiRowData;
        this.validValueCount = validValueCount;
    }

    public static ExtCMYKMultiRowData createInstance(CMYKMultiRowData cmykMultiRowData, int validValueCount) {
        return new ExtCMYKMultiRowData(cmykMultiRowData, validValueCount);
    }

    protected ExtCMYKMultiRowData(Parcel in) {
        cmykMultiRowData = in.readParcelable(CMYKMultiRowData.class.getClassLoader());
        validValueCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(cmykMultiRowData, flags);
        dest.writeInt(validValueCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ExtCMYKMultiRowData> CREATOR = new Creator<ExtCMYKMultiRowData>() {
        @Override
        public ExtCMYKMultiRowData createFromParcel(Parcel in) {
            return new ExtCMYKMultiRowData(in);
        }

        @Override
        public ExtCMYKMultiRowData[] newArray(int size) {
            return new ExtCMYKMultiRowData[size];
        }
    };

    public int getValidValueCount() {
        return validValueCount;
    }

    public CMYKMultiRowData getCmykMultiRowData() {
        return cmykMultiRowData;
    }
}

