package com.mx.mxSdk;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Error implements Parcelable {
    int code;
    int cmd;//指令
    String describe;

    public Error(int code, String describe) {
        this.code = code;
        this.cmd = 0;
        this.describe = describe;
    }

    public Error(int code, int cmd, String describe) {
        this.code = code;
        this.cmd = cmd;
        this.describe = describe;
    }

    protected Error(Parcel in) {
        code = in.readInt();
        cmd = in.readInt();
        describe = in.readString();
    }

    public static final Creator<Error> CREATOR = new Creator<Error>() {
        @Override
        public Error createFromParcel(Parcel in) {
            return new Error(in);
        }

        @Override
        public Error[] newArray(int size) {
            return new Error[size];
        }
    };

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Error Error = (Error) o;
        return code == Error.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, describe);
    }

    @NonNull
    @Override
    public String toString() {
        return "Error{" +
                "Code=" + code +
                ", describe='" + describe + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(code);
        parcel.writeInt(cmd);
        parcel.writeString(describe);
    }
}
