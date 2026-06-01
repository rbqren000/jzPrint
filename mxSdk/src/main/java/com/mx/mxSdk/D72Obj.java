package com.mx.mxSdk;

import android.os.Parcel;
import android.os.Parcelable;

public class D72Obj implements Parcelable {

    private final byte[] d72;
    private final int validValueCount;

    D72Obj(byte[] d72, int validValueCount) {
        this.d72 = d72;
        this.validValueCount = validValueCount;
    }

    public static D72Obj createInstance(byte[] d72, int validValueCount) {
        return new D72Obj(d72, validValueCount);
    }

    protected D72Obj(Parcel in) {
        d72 = in.createByteArray();
        validValueCount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(d72);
        dest.writeInt(validValueCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<D72Obj> CREATOR = new Creator<D72Obj>() {
        @Override
        public D72Obj createFromParcel(Parcel in) {
            return new D72Obj(in);
        }

        @Override
        public D72Obj[] newArray(int size) {
            return new D72Obj[size];
        }
    };

    public byte[] getD72() {
        return d72;
    }

    public int getValidValueCount() {
        return validValueCount;
    }
}

