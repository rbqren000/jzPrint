package com.mx.mxSdk;

import android.os.Parcel;
import android.os.Parcelable;

public class ExtLogoData implements Parcelable {

    private final int validValueCount;
    private final LogoData logoData;

    private ExtLogoData(LogoData logoData, int validValueCount) {
        this.logoData = logoData;
        this.validValueCount = validValueCount;
    }

    public static ExtLogoData createInstance(LogoData logoData, int validValueCount) {
        return new ExtLogoData(logoData, validValueCount);
    }

    protected ExtLogoData(Parcel in) {
        logoData = in.readParcelable(LogoData.class.getClassLoader());
        validValueCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(logoData, flags);
        dest.writeInt(validValueCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ExtLogoData> CREATOR = new Creator<ExtLogoData>() {
        @Override
        public ExtLogoData createFromParcel(Parcel in) {
            return new ExtLogoData(in);
        }

        @Override
        public ExtLogoData[] newArray(int size) {
            return new ExtLogoData[size];
        }
    };

    public int getValidValueCount() {
        return validValueCount;
    }

    public LogoData getLogoData() {
        return logoData;
    }
}

