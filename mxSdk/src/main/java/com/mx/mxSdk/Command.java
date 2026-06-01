package com.mx.mxSdk;

import androidx.annotation.NonNull;
import com.mx.mxSdk.Utils.Arrays;

public class Command {

    public int index;
    public byte[] data;
    public Object tag;
    public long createTime;//指令生成时的时间，单位时间戳
    public int delayTime;//延时时间，秒
    //超时时是否继续发送，默认false表示继续发送，当期值为true时，则超时不再发送该指令(目前时间本身也不准确，暂时未支持该属性，默认都发送)
    public boolean isLossOnTimeout = false;

    public Command(byte[] data) {
        this.index = -1;
        this.data = data;
        this.tag = null;
        this.createTime = -1;
        this.delayTime = -1;
        this.isLossOnTimeout =  false;
    }
    public Command(int index,byte[] data) {
        this.index = index;
        this.data = data;
        this.tag = null;
        this.createTime = -1;
        this.delayTime = -1;
        this.isLossOnTimeout =  false;
    }
    public Command(byte[] data, int delayTime) {
        this.index = -1;
        this.data = data;
        this.tag = null;
        this.createTime = -1;
        this.delayTime = delayTime;
        this.isLossOnTimeout =  false;
    }

    public Command(byte[] data, int delayTime, Object tag) {
        this.index = -1;
        this.data = data;
        this.tag = tag;
        this.createTime = -1;
        this.delayTime = delayTime;
        this.isLossOnTimeout =  false;
    }

    public Command(int index, byte[] data, Object tag, long createTime, int delayTime, boolean isLossOnTimeout) {
        this.index = index;
        this.data = data;
        this.tag = tag;
        this.createTime = createTime;
        this.delayTime = delayTime;
        this.isLossOnTimeout = isLossOnTimeout;
    }

    public Command(byte[] data, Object tag) {
        this.index = -1;
        this.data = data;
        this.tag = tag;
        this.createTime = -1;
        this.delayTime = -1;
        this.isLossOnTimeout =  false;
    }

    public Command(byte[] data, Object tag, int delayTime) {
        this.index = -1;
        this.data = data;
        this.tag = tag;
        this.createTime = -1;
        this.delayTime = delayTime;
        this.isLossOnTimeout =  false;
    }

    public Command(int index, byte[] data, Object tag, int delayTime) {
        this.index = index;
        this.data = data;
        this.tag = tag;
        this.createTime = -1;
        this.delayTime = delayTime;
        this.isLossOnTimeout =  false;
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

    public int getDelayTime() {
        return delayTime;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public boolean isLossOnTimeout() {
        return isLossOnTimeout;
    }

    public void setLossOnTimeout(boolean lossOnTimeout) {
        isLossOnTimeout = lossOnTimeout;
    }

    public void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
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

        return "{ tag : " + this.tag + ",  data: " + d + " delay :" + delayTime + "}";
    }

    public interface Callback {
        void success(Command command, Object obj);
        void error(Command command, String errorMsg);
        boolean timeout(Command command, boolean delayEfficacy);//指令超时
    }
}
