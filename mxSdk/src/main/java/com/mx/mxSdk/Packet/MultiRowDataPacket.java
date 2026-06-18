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

    // 预分配的可复用包缓冲区，在 set() 中初始化，clear() 中释放
    // 利用 XModem ACK 协议的串行特性，收到 ACK 后才会生成下一包，此时上一包已被 WriteThread 消费完毕
    private byte[] reusablePacketBuffer;

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
        this.reusablePacketBuffer = new byte[fullPacketDataLen]; // 预分配复用 buffer
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
        this.reusablePacketBuffer = new byte[fullPacketDataLen]; // 预分配复用 buffer
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

        this.reusablePacketBuffer = null; // 释放复用 buffer

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

    /**
     * 获取下一包的完整格式化数据（含帧头+CRC），直接写入复用 buffer。
     * 消除原 getNextPacket() + packetFormat() 两次分配为一次复用。
     *
     * ⚠️ 返回的是 reusablePacketBuffer 引用，安全前提：
     * XModem ACK 停等协议保证收到 ACK 后才调用此方法生成下一包，
     * 此时上一包数据已被 WriteThread 写入 OutputStream，buffer 可安全覆盖。
     * 如需改为流水线/预取模式，须改用双缓冲。
     */
    public byte[] buildNextFormattedPacket() {
        index++;
        indexInCurrentRowPacket++;
        fillFormattedPacket(indexInCurrentRowPacket);
        return reusablePacketBuffer;
    }

    /**
     * 获取当前包的完整格式化数据（用于 NAK 重传），直接写入复用 buffer。
     */
    public byte[] buildCurrentFormattedPacket() {
        fillFormattedPacket(indexInCurrentRowPacket);
        return reusablePacketBuffer;
    }

    /**
     * 将指定索引的原始数据填充到 reusablePacketBuffer 中，包含帧头、数据和 CRC。
     */
    private void fillFormattedPacket(int pktIndex) {
        int offset = 0;
        reusablePacketBuffer[offset++] = (byte) fh;
        reusablePacketBuffer[offset++] = (byte) (~fh & 0xFF);

        int start = pktIndex * usefulPacketDataLength;
        int remainingData = currentRowDataLength - start;

        if (remainingData >= usefulPacketDataLength) {
            System.arraycopy(currentRowImageByteData, start, reusablePacketBuffer, offset, usefulPacketDataLength);
        } else {
            System.arraycopy(currentRowImageByteData, start, reusablePacketBuffer, offset, remainingData);
            Arrays.fill(reusablePacketBuffer, offset + remainingData, offset + usefulPacketDataLength, (byte) 0x1A);
        }

        offset += usefulPacketDataLength;
        char crc = CRC16.crc16_calc(reusablePacketBuffer, 0, offset);
        reusablePacketBuffer[offset++] = (byte) (crc >> 8 & 0xFF);
        reusablePacketBuffer[offset] = (byte) (crc & 0xFF);
    }

    // ====== 以下方法保留用于兼容，内部改为复用 buffer ======

    /**
     * @deprecated 请使用 {@link #buildNextFormattedPacket()} 替代，减少 GC 压力。
     */
    @Deprecated
    public byte[] getNextPacket() {
        index++;
        indexInCurrentRowPacket++;
        return getRawPacketData(indexInCurrentRowPacket);
    }

    /**
     * @deprecated 请使用 {@link #buildCurrentFormattedPacket()} 替代，减少 GC 压力。
     */
    @Deprecated
    public byte[] getCurrentPacket() {
        return getRawPacketData(indexInCurrentRowPacket);
    }

    private byte[] getRawPacketData(int pktIndex) {
        byte[] packet = new byte[usefulPacketDataLength];
        int start = pktIndex * usefulPacketDataLength;
        int remainingData = currentRowDataLength - start;

        if (remainingData >= usefulPacketDataLength) {
            System.arraycopy(currentRowImageByteData, start, packet, 0, usefulPacketDataLength);
        } else {
            System.arraycopy(currentRowImageByteData, start, packet, 0, remainingData);
            Arrays.fill(packet, remainingData, usefulPacketDataLength, (byte) 0x1A);
        }
        return packet;
    }

    /**
     * @deprecated 请使用 {@link #buildNextFormattedPacket()} / {@link #buildCurrentFormattedPacket()} 替代。
     */
    @Deprecated
    public byte[] packetFormat(@NonNull byte[] data){
        byte[] packetData = new byte[fullPacketDataLen];

        int offset = 0;
        packetData[offset++] = (byte) fh;
        packetData[offset++] = (byte)(~fh & 0xFF);
        System.arraycopy(data, 0, packetData, offset, data.length);

        char crc = CRC16.crc16_calc(packetData,0,data.length+2);
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
