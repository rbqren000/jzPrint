package com.mx.mxSdk;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class LogoImage implements Parcelable {

    private final String imagePath;

    private LogoImage(@NonNull String imagePath) {
        this.imagePath = imagePath;
    }

    public static LogoImage createInstance(@NonNull String imagePath) {
        return new LogoImage(imagePath);
    }

    public static final Creator<LogoImage> CREATOR = new Creator<LogoImage>() {
        @Override
        public LogoImage createFromParcel(Parcel in) {
            return new LogoImage(in);
        }

        @Override
        public LogoImage[] newArray(int size) {
            return new LogoImage[size];
        }
    };

    public String getImagePath() {
        return imagePath;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected LogoImage(Parcel in) {
        imagePath = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(imagePath);
    }
}
