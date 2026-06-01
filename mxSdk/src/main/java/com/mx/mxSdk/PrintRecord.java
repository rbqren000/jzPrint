package com.mx.mxSdk;

import android.os.Parcel;
import android.os.Parcelable;

public class PrintRecord implements Parcelable  {

    private String uniqueId;
    private int state;//0表示未打印，1表示已打印

    public PrintRecord(String uniqueId, int state) {
        this.uniqueId = uniqueId;
        this.state = state;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    protected PrintRecord(Parcel in) {
        uniqueId = in.readString();
        state = in.readInt();
    }

    public static final Creator<PrintRecord> CREATOR = new Creator<PrintRecord>() {
        @Override
        public PrintRecord createFromParcel(Parcel in) {
            return new PrintRecord(in);
        }

        @Override
        public PrintRecord[] newArray(int size) {
            return new PrintRecord[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(uniqueId);
        parcel.writeInt(state);
    }

    @Override
    public String toString() {
        return "PrintRecord{" +
                "uniqueId='" + uniqueId + '\'' +
                ", state=" + state +
                '}';
    }
}
