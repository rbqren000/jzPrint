package com.mx.mxSdk.Packet;

import androidx.annotation.NonNull;

import com.mx.mxSdk.CRC16;
import com.mx.mxSdk.TransportProtocol;
import com.mx.mxSdk.Utils.RBQLog;

public final class OtaPacket extends BasePacket {

    public byte[] data;
    public int dataLength;
    public int totalPacketCount;//包数量
    public int progress;
    public int index = -1;
    public int fh = TransportProtocol.STX_E;//默认发送soh包

    public static final int packetHeadLen = 1;
    public static final int packetHeadXorLen = 1;

    public int usefulPacketDataLength = 124; //有效数据的长度
    public static final int crcLen = 2;
    public int fullPacketDataLen;//整个XModem包的长度

    public long startTime = 0;//记发送数据包的开始时间
    public long currentTime = 0;//记录当前时间

    public Boolean hasData(){
        if (data==null){
            return false;
        }
        return dataLength != 0;
    }

    /**
     *
     * @param data 数据  使用默认的帧头 SOH  0x01
     */
    public void set(@NonNull byte[] data) {
        this.clear();

        this.data = data;
        this.dataLength = this.data.length;

        this.fh = TransportProtocol.STX_E;

        this.usefulPacketDataLength = 124;

        this.fullPacketDataLen = this.usefulPacketDataLength + packetHeadLen + packetHeadXorLen + crcLen;

        if (dataLength % usefulPacketDataLength == 0) {

            totalPacketCount = dataLength / usefulPacketDataLength;
        } else {

            totalPacketCount = dataLength / usefulPacketDataLength + 1;
        }
        RBQLog.i("ota 长度 :"+(float) dataLength /1000.0f+"k; 共分"+ totalPacketCount +"包");
    }

    /**
     *
     * @param data  数据
     * @param fh  帧头
     */
    public void set(@NonNull byte[] data,int fh) {
        this.clear();

        this.data = data;
        this.dataLength = this.data.length;

        this.fh = fh;

        switch (fh){
            case TransportProtocol.SOH:
                this.usefulPacketDataLength = 128;
                break;
            case TransportProtocol.STX:
                this.usefulPacketDataLength = 512;
                break;
            case TransportProtocol.STX_A:
                this.usefulPacketDataLength = 1024;
                break;
            case TransportProtocol.STX_B:
                this.usefulPacketDataLength = 2048;
                break;
            case TransportProtocol.STX_C:
                this.usefulPacketDataLength = 5120;
                break;
            case TransportProtocol.STX_D:
                this.usefulPacketDataLength = 10240;
                break;
            case TransportProtocol.STX_E:
                this.usefulPacketDataLength = 124;
                break;
        }

        this.fullPacketDataLen = this.usefulPacketDataLength + packetHeadLen + packetHeadXorLen + crcLen;

        if (dataLength % usefulPacketDataLength == 0) {

            totalPacketCount = dataLength / usefulPacketDataLength;
        } else {

            totalPacketCount = dataLength / usefulPacketDataLength + 1;
        }
        RBQLog.i("ota 长度 :"+(float) dataLength /1000.0f+"k; 共分"+ totalPacketCount +"包");
    }

    public void clear() {

        this.progress = 0;
        this.totalPacketCount = 0;
        this.index = -1;
        this.data = null;
        this.dataLength = 0;

        this.startTime = 0;
        this.currentTime = 0;

        super.clear();
    }

    public boolean hasNextPacket() {
        return this.totalPacketCount > 0 && (this.index + 1) < this.totalPacketCount;
    }
    //如果返回-1表示已经没有下一包了
    public int getNextPacketIndex() {

        int nexIndex = this.index + 1;

        if (nexIndex>=this.totalPacketCount){
            return -1;
        }
        return nexIndex;
    }
    //返回null说明已经没有下一包数据了
    public byte[] getNextPacket() {

        int index = this.getNextPacketIndex();

        if (index != -1) {
            return this.getPacket(index);
        }
        return null;
    }
    //获取当前index的数据包
    public byte[] getPacket() {

        return getPacket(this.index);
    }

    public byte[] getPacket(int index) {

        this.index = index;
        int start = index * usefulPacketDataLength;
        int end = Math.min(dataLength, start + usefulPacketDataLength);
        int length = end - start;

        byte[] packet = new byte[length];
        System.arraycopy(this.data, start, packet, 0, length);

        return packet;
    }


    public byte[] packetFormat(@NonNull byte[] data){

        byte[] otaData = new byte[fullPacketDataLen];

        //指令序列 1位 ，这里+1是因为起始帧的包序列是0，这里应该最开始的时候从1开始
//            int num = (index+1) % 255;

        int offset = 0;
        //指令头
        otaData[offset++] = (byte) fh;
        otaData[offset++] = (byte)(~fh & 0xFF);
        // 1
//            otaData[offset++] = (byte)(num & 0xFF);
        //2
        //指令序列取反
//            otaData[offset++] = (byte)(~num & 0xFF);
        //3
        System.arraycopy(data, 0, otaData, offset, data.length);

        char crc = CRC16.crc16_calc(otaData,0,data.length+2);

        //crc部分  //3+length
        offset = offset + data.length;

        otaData[offset++] = (byte) (crc >> 8 & 0xFF);
        otaData[offset] = (byte) (crc & 0xFF);

        return otaData;
    }

    public boolean invalidateProgress() {

        float a = this.getNextPacketIndex();

        if (a==-1){
            return false;
        }

        float b = this.totalPacketCount;

        int progress = (int) Math.floor((a / b * 100));

        if (progress == this.progress)
            return false;

        this.progress = progress;

        return true;
    }

    public int getProgress() {
        return this.progress;
    }

}
