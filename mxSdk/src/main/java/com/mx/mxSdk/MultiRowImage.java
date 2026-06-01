package com.mx.mxSdk;

import static com.mx.mxSdk.RowLayoutDirection.RowLayoutDirectionVertical;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;

public class MultiRowImage implements Parcelable {

    private final ArrayList<RowImage> rowImages;
    /** 缩略图地址*/
    private final String thumbPath;
    /** 多张图的排布方向 RowLayoutDirectionVertical 从上到下排列 RowLayoutDirectionHorizontal从左到右排列   */
    private final RowLayoutDirection rowLayoutDirection;
    /** 图片是否为连续图（既由一张大图裁切而来的连续图片） */
    private final boolean isContiguousCroppedImages;

    private MultiRowImage(@NonNull ArrayList<RowImage> rowImages, @Nullable String thumbPath) {
        this.rowImages = rowImages;
        this.thumbPath = thumbPath;
        this.rowLayoutDirection = RowLayoutDirectionVertical;
        this.isContiguousCroppedImages = false;
    }

    private MultiRowImage(@NonNull ArrayList<RowImage> rowImages, @Nullable String thumbPath,RowLayoutDirection rowLayoutDirection,boolean isContiguousCroppedImages) {
        this.rowImages = rowImages;
        this.thumbPath = thumbPath;
        this.rowLayoutDirection = rowLayoutDirection;
        this.isContiguousCroppedImages = isContiguousCroppedImages;
    }

    public static MultiRowImage createInstance(@NonNull ArrayList<RowImage> rowImages, @Nullable String thumbPath) {
        return new MultiRowImage(rowImages, thumbPath);
    }

    public static MultiRowImage createInstance(@NonNull ArrayList<RowImage> rowImages, @Nullable String thumbPath,RowLayoutDirection rowLayoutDirection,boolean isContiguousCroppedImages) {
        return new MultiRowImage(rowImages, thumbPath,rowLayoutDirection, isContiguousCroppedImages);
    }

    public static final Creator<MultiRowImage> CREATOR = new Creator<MultiRowImage>() {
        @Override
        public MultiRowImage createFromParcel(Parcel in) {
            return new MultiRowImage(in);
        }

        @Override
        public MultiRowImage[] newArray(int size) {
            return new MultiRowImage[size];
        }
    };

    public ArrayList<RowImage> getRowImages() {
        return rowImages;
    }

    public String getThumbPath() {
        return thumbPath;
    }

    public RowLayoutDirection getRowLayoutDirection() {
        return rowLayoutDirection;
    }

    public boolean isContiguousCroppedImages() {
        return isContiguousCroppedImages;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected MultiRowImage(Parcel in) {
        rowImages = in.createTypedArrayList(RowImage.CREATOR);
        thumbPath = in.readString();
        String directionName = in.readString();
        rowLayoutDirection = RowLayoutDirection.valueOf(directionName);
        isContiguousCroppedImages = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(rowImages);
        dest.writeString(thumbPath);
        dest.writeString(rowLayoutDirection.name());
        dest.writeByte((byte)(isContiguousCroppedImages ?1:0));
    }

    @NonNull
    @Override
    public String toString() {
        return "MultiRowImage{" +
                "rowImages=" + rowImages +
                ", thumbPath='" + thumbPath + '\'' +
                ", rowLayoutDirection=" + rowLayoutDirection +
                ", isContiguousCroppedImages=" + isContiguousCroppedImages +
                '}';
    }
}
