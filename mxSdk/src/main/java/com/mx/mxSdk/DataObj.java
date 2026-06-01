package com.mx.mxSdk;

import androidx.annotation.NonNull;
import com.mx.mxSdk.Utils.Arrays;

public class DataObj {
    public int index;
    public byte[] data;
    public Object tag;

    public DataObj(byte[] data) {
        this.index = -1;
        this.data = data;
        this.tag = null;
    }
    public DataObj(int index,byte[] data) {
        this.index = index;
        this.data = data;
        this.tag = null;
    }

    public DataObj(int index, byte[] data, Object tag) {
        this.index = index;
        this.data = data;
        this.tag = tag;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public void clear() {
        this.data = null;
    }

    @Override
    @NonNull
    public String toString() {

        String d = "";

        if (data != null)
            d = Arrays.bytesToPrefixedHexString(this.data, ",");

        return "{ tag : " + this.tag + ",  data: " + d +" }";
    }

    public interface Callback {
        void success(DataObj dataObj, Object obj);
        void error(DataObj dataObj, String errorMsg);
        boolean timeout(DataObj dataObj, boolean delayEfficacy);//指令超时
    }
}
