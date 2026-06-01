package com.mx.mxSdk;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ExtCMYKRowData implements Parcelable {

    private final int validValueCount;
    private final CMYKRowData cmykRowData;

    ExtCMYKRowData(@NonNull CMYKRowData cmykRowData, int validValueCount) {
        this.cmykRowData = cmykRowData;
        this.validValueCount = validValueCount;
    }

    public static ExtCMYKRowData createInstance(@NonNull CMYKRowData cmykRowData, int validValueCount) {
        return new ExtCMYKRowData(cmykRowData, validValueCount);
    }

    protected ExtCMYKRowData(Parcel in) {
        cmykRowData = in.readParcelable(CMYKRowData.class.getClassLoader());
        validValueCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(cmykRowData, flags);
        dest.writeInt(validValueCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ExtCMYKRowData> CREATOR = new Creator<ExtCMYKRowData>() {
        @Override
        public ExtCMYKRowData createFromParcel(Parcel in) {
            return new ExtCMYKRowData(in);
        }

        @Override
        public ExtCMYKRowData[] newArray(int size) {
            return new ExtCMYKRowData[size];
        }
    };

    public int getValidValueCount() {
        return validValueCount;
    }

    public CMYKRowData getCmykRowData() {
        return cmykRowData;
    }
}


