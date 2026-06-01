package com.mx.mxSdk;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

public class RowImage implements Parcelable {

    private final String imagePath;
    private final int topBeyondDistance;
    private final int bottomBeyondDistance;

    private RowImage(@NonNull String imagePath) {
        this.imagePath = imagePath;
        this.topBeyondDistance = 0;
        this.bottomBeyondDistance = 0;
    }

    private RowImage(String imagePath, int topBeyondDistance, int bottomBeyondDistance) {
        this.imagePath = imagePath;
        this.topBeyondDistance = topBeyondDistance;
        this.bottomBeyondDistance = bottomBeyondDistance;
    }

    public static RowImage createInstance(String imagePath) {
        return new RowImage(imagePath);
    }

    public static RowImage createInstance(String imagePath, int topBeyondDistance, int bottomBeyondDistance) {
        return new RowImage(imagePath,topBeyondDistance,bottomBeyondDistance);
    }

    public static final Creator<RowImage> CREATOR = new Creator<RowImage>() {
        @Override
        public RowImage createFromParcel(Parcel in) {
            return new RowImage(in);
        }

        @Override
        public RowImage[] newArray(int size) {
            return new RowImage[size];
        }
    };

    public String getImagePath() {
        return imagePath;
    }

    public int getTopBeyondDistance() {
        return topBeyondDistance;
    }

    public int getBottomBeyondDistance() {
        return bottomBeyondDistance;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected RowImage(Parcel in) {
        imagePath = in.readString();
        topBeyondDistance = in.readInt();
        bottomBeyondDistance = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(imagePath);
        dest.writeInt(topBeyondDistance);
        dest.writeInt(bottomBeyondDistance);
    }

    @NonNull
    @Override
    public String toString() {
        return "RowImage{" +
                "imagePath='" + imagePath + '\'' +
                ", topBeyondDistance=" + topBeyondDistance +
                ", bottomBeyondDistance=" + bottomBeyondDistance +
                '}';
    }
}
