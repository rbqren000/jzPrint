package com.mx.mxSdk.media;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 媒体信息类
 * 用于图片/视频选择结果的简化数据模型
 */
public class MediaInfo implements Parcelable {

    private String path;
    private String realPath;
    private String compressPath;
    private String mimeType;
    private long size;
    private int width;
    private int height;

    public MediaInfo() {
    }

    protected MediaInfo(Parcel in) {
        path = in.readString();
        realPath = in.readString();
        compressPath = in.readString();
        mimeType = in.readString();
        size = in.readLong();
        width = in.readInt();
        height = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(realPath);
        dest.writeString(compressPath);
        dest.writeString(mimeType);
        dest.writeLong(size);
        dest.writeInt(width);
        dest.writeInt(height);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MediaInfo> CREATOR = new Creator<MediaInfo>() {
        @Override
        public MediaInfo createFromParcel(Parcel in) {
            return new MediaInfo(in);
        }

        @Override
        public MediaInfo[] newArray(int size) {
            return new MediaInfo[size];
        }
    };

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRealPath() {
        return realPath;
    }

    public void setRealPath(String realPath) {
        this.realPath = realPath;
    }

    public String getCompressPath() {
        return compressPath;
    }

    public void setCompressPath(String compressPath) {
        this.compressPath = compressPath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isCompressed() {
        return compressPath != null && !compressPath.isEmpty();
    }

    public boolean isVideo() {
        return mimeType != null && mimeType.contains("video");
    }

    public boolean isImage() {
        return mimeType != null && mimeType.contains("image");
    }

    @Override
    public String toString() {
        return "MediaInfo{" +
                "path='" + path + '\'' +
                ", realPath='" + realPath + '\'' +
                ", compressPath='" + compressPath + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", size=" + size +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
