package com.mx.mxSdk.Packet;

import androidx.annotation.NonNull;

import com.mx.mxSdk.CRC16;
import com.mx.mxSdk.MultiRowData;
import com.mx.mxSdk.RowData;
import com.mx.mxSdk.TransportProtocol;
import com.mx.mxSdk.Utils.RBQLog;

import java.util.Arrays;

public final class MultiRowDataPacket extends BasePacket {

    public static final int packetHeadLen = 1;
    public static final int packetHeadXorLen = 1;
    public static final int crcLen = 2;

    public int compress = 1;//数据是否为压缩数据

    public MultiRowData multiRowData;
    public RowData currentRowData;
    public byte[] currentRowImageByteData;

    public int fh = TransportProtocol.STX_E;//默认发送soh包

    public int totalDataLen;//总的数据byte个数
    public int totalPacketCount;//总的包数量
    public int totalRowCount = 0;
    public int index = -1;//每发送一包，则加1，用来记录已发送包数

    public int usefulPacketDataLength = 128; //有效数据的长度
    public int fullPacketDataLen;//整个数据包的长度

    public int progress = 0;

    public int currentRow = 0;
    public int currentRowDataLength = 0;
    public int currentRowTotalPacketCount = 0;
    public int indexInCurrentRowPacket = -1;

    public long startTime = 0;//记发送数据包的开始时间
    public long currentTime = 0;//记录当前时间

    /**
     *
     * @param multiRowData 数据  使用默认的帧头 STX_E
     */
    public void set(@NonNull MultiRowData multiRowData) {
        this.clear();

        this.multiRowData = multiRowData;
        this.compress = this.multiRowData.compressValue();
        this.fh = TransportProtocol.STX_E;
        this.usefulPacketDataLength = 124; // STX_E 对应 124
        this.fullPacketDataLen = this.usefulPacketDataLength + packetHeadLen + packetHeadXorLen + crcLen;
        this.totalDataLen = this.multiRowData.totalDataLength();
        this.totalPacketCount = this.multiRowData.totalPacketCount(usefulPacketDataLength);
        this.totalRowCount = this.multiRowData.totalRowCount();
        this.index = -1;
        this.progress = 0;
        this.currentRow = 0;
        this.currentRowData = this.multiRowData.rowDataWithRowIndex(currentRow);
        this.currentRowImageByteData = this.currentRowData.data();
        this.currentRowDataLength = this.currentRowData.getDataLength();
        this.currentRowTotalPacketCount = this.currentRowData.totalPacketCount(usefulPacketDataLength);
        this.indexInCurrentRowPacket = -1;
        this.startTime = 0;//记发送数据包的开始时间
        this.currentTime = 0;//记录当前时间

        RBQLog.i("打印数据 长度 :"+(float) totalDataLen /1000.0f+"k; 共分"+ totalPacketCount +"包");

    }

    /**
     *
     * @param multiRowData  数据
     * @param fh  帧头
     */
    public void set(@NonNull MultiRowData multiRowData, int fh) {
        this.clear();

        this.multiRowData = multiRowData;
        this.compress = multiRowData.compressValue();
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
        this.totalDataLen = this.multiRowData.totalDataLength();
        this.totalPacketCount = this.multiRowData.totalPacketCount(usefulPacketDataLength);
        this.totalRowCount = this.multiRowData.totalRowCount();
        this.index = -1;
        this.progress = 0;
        this.currentRow = 0;
        this.currentRowData = this.multiRowData.rowDataWithRowIndex(currentRow);
        this.currentRowImageByteData = this.currentRowData.data();
        this.currentRowDataLength = this.currentRowData.getDataLength();
        this.currentRowTotalPacketCount = this.currentRowData.totalPacketCount(usefulPacketDataLength);
        this.indexInCurrentRowPacket = -1;
        this.startTime = 0;//记发送数据包的开始时间
        this.currentTime = 0;//记录当前时间

        RBQLog.i("打印数据 长度 :"+(float) totalDataLen /1000.0f+"k; 共分"+ totalPacketCount +"包");
    }

    public void clear() {

        this.progress = 0;
        this.totalPacketCount = 0;
        this.totalRowCount = 0;
        this.index = -1;
        this.currentRow = 0;
        this.indexInCurrentRowPacket = -1;
        this.totalDataLen = 0;
        this.fullPacketDataLen =  0;

        this.multiRowData = null;
        this.currentRowData = null;
        this.currentRowImageByteData = null;
        this.currentRowTotalPacketCount = 0;
        this.currentRowDataLength = 0;

        this.startTime = 0;
        this.currentTime = 0;

        super.clear();
    }
    /*
    public Boolean hasData1() {
        if (this.multiRowImageData==null){
            return false;
        }
        return this.multiRowImageData.hasImageData();
    }
    */
    //hasData()等效于hasData1()  totalDataLen之前set的时候计算好的，没必要每次计算
    public Boolean hasData(){
        if (multiRowData ==null){
            return false;
        }
        return totalDataLen != 0;
    }

    public int getCurrentRow() {
        return currentRow;
    }

    /*
     *   只判断当前packet
     */
    public Boolean hasNextPacketWithCurrentRow() {
        if (this.multiRowData ==null) {
            return false;
        }
        return this.currentRowTotalPacketCount>0 && (this.indexInCurrentRowPacket +1)<this.currentRowTotalPacketCount;
    }

    public Boolean hasNextRow(){
        if (this.multiRowData ==null) {
            return false;
        }
        return (currentRow + 1) < totalRowCount;
    }

    /**
     * 移动到下一个行
     * @return
     */
    public Boolean cursorMoveToNext(){
        if (!hasNextRow()){
            return false;
        }
        currentRow = currentRow + 1;
        currentRowData = multiRowData.rowDataWithRowIndex(currentRow);
        this.currentRowImageByteData = this.currentRowData.data();
        indexInCurrentRowPacket = -1;
        currentRowDataLength = currentRowData.getDataLength();
        currentRowTotalPacketCount = currentRowData.totalPacketCount(usefulPacketDataLength);

        return true;
    }

    public byte[] getCurrentPacket() {
        byte[] packet = new byte[usefulPacketDataLength];
        int start = indexInCurrentRowPacket * usefulPacketDataLength;
        int remainingData = currentRowDataLength - start;

        if (remainingData >= usefulPacketDataLength) {
            System.arraycopy(currentRowImageByteData, start, packet, 0, usefulPacketDataLength);
        } else {
            System.arraycopy(currentRowImageByteData, start, packet, 0, remainingData);
            Arrays.fill(packet, remainingData, usefulPacketDataLength, (byte) 0x1A);
        }
        return packet;
    }


    public byte[] getNextPacket() {

        index++;
        indexInCurrentRowPacket++;

        byte[] packet = new byte[usefulPacketDataLength];
        int start = indexInCurrentRowPacket * usefulPacketDataLength;
        int remainingData = currentRowDataLength - start;

        if (remainingData >= usefulPacketDataLength) {
            System.arraycopy(currentRowImageByteData, start, packet, 0, usefulPacketDataLength);
        } else {
            System.arraycopy(currentRowImageByteData, start, packet, 0, remainingData);
            Arrays.fill(packet, remainingData, usefulPacketDataLength, (byte) 0x1A);
        }

        return packet;
    }

    public byte[] packetFormat(@NonNull byte[] data){

        byte[] packetData = new byte[fullPacketDataLen];

        int offset = 0;
        //指令头
        packetData[offset++] = (byte) fh;
        packetData[offset++] = (byte)(~fh & 0xFF);
        //3
        System.arraycopy(data, 0, packetData, offset, data.length);

        char crc = CRC16.crc16_calc(packetData,0,data.length+2);

        //crc部分  //3+length
        offset = offset + data.length;

        packetData[offset++] = (byte) (crc >> 8 & 0xFF);
        packetData[offset] = (byte) (crc & 0xFF);

        return packetData;
    }

    public boolean invalidateProgress() {

        int progress = (int) Math.floor((float)this.index / (float)this.totalPacketCount * 100);
       
        if (progress == this.progress)
            return false;

        this.progress = progress;

        return true;
    }

    public int getProgress() {
        return this.progress;
    }

}
