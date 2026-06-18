package com.mx.mxSdk.Packet;

import androidx.annotation.NonNull;

import com.mx.mxSdk.CMYKChannel;
import com.mx.mxSdk.CMYKMultiRowData;
import com.mx.mxSdk.CMYKRowData;
import com.mx.mxSdk.CRC16;
import com.mx.mxSdk.TransportProtocol;
import com.mx.mxSdk.Utils.RBQLog;

import java.util.Arrays;

/**
 * CMYK多行数据包
 * 传输顺序：按行遍历，每行内按通道顺序(C→M→Y→K)传输
 * 
 * 使用流程：
 * 1. 发送当前行当前通道的数据信息指令（包含数据大小等）
 * 2. 循环发送当前通道的数据包
 * 3. 当前通道发完后，发送通道完成指令
 * 4. 如果有下一个通道，切换通道并回到步骤1
 * 5. 如果当前行所有通道都发完，判断是否有下一行
 * 6. 有下一行则发送下一行指令，切换到下一行的C通道，回到步骤1
 * 7. 所有行都发完后，发送打印完成指令
 */
public final class CMYKMultiRowDataPacket extends BasePacket {

    public static final int packetHeadLen = 1;
    public static final int packetHeadXorLen = 1;
    public static final int crcLen = 2;

    // 通道顺序
    private static final CMYKChannel[] CHANNEL_ORDER = {CMYKChannel.C, CMYKChannel.M, CMYKChannel.Y, CMYKChannel.K};
    public static final int CHANNEL_COUNT = 4;

    public int compress = 1; // 数据是否为压缩数据

    public CMYKMultiRowData multiRowData;
    public CMYKRowData currentRowData;
    public byte[] currentChannelByteData; // 当前通道的数据

    public int fh = TransportProtocol.STX_E; // 默认发送帧头

    public int totalDataLen; // 总的数据byte个数（所有通道）
    public int totalPacketCount; // 总的包数量（所有通道）
    public int totalRowCount = 0;
    public int index = -1; // 每发送一包，则加1，用来记录已发送包数（全局）

    public int usefulPacketDataLength = 128; // 有效数据的长度
    public int fullPacketDataLen; // 整个数据包的长度

    public int progress = 0;

    // 当前通道相关
    public int currentChannelIndex = 0; // 当前通道索引 (0=C, 1=M, 2=Y, 3=K)
    public CMYKChannel currentChannel = CMYKChannel.C; // 当前通道

    // 当前行相关
    public int currentRow = 0;
    public int currentChannelDataLength = 0; // 当前通道的数据长度
    public int currentChannelTotalPacketCount = 0; // 当前通道的包数量
    public int indexInCurrentChannelPacket = -1; // 当前通道内的包索引

    public long startTime = 0; // 记发送数据包的开始时间
    public long currentTime = 0; // 记录当前时间

    // 预分配的可复用包缓冲区，在 set() 中初始化，clear() 中释放
    private byte[] reusablePacketBuffer;

    /**
     * @param multiRowData 数据  使用默认的帧头 STX_E
     */
    public void set(@NonNull CMYKMultiRowData multiRowData) {
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

        // 初始化到第一行第一个通道
        this.currentRow = 0;
        this.currentChannelIndex = 0;
        this.currentChannel = CHANNEL_ORDER[0];
        this.currentRowData = this.multiRowData.rowDataWithRowIndex(currentRow);
        loadCurrentChannelData();

        this.startTime = 0;
        this.currentTime = 0;

        RBQLog.i("CMYK打印数据 长度 :" + (float) totalDataLen / 1000.0f + "k; 共分" + totalPacketCount + "包; 共" + totalRowCount + "行");
    }

    /**
     * @param multiRowData 数据
     * @param fh           帧头
     */
    public void set(@NonNull CMYKMultiRowData multiRowData, int fh) {
        this.clear();

        this.multiRowData = multiRowData;
        this.compress = multiRowData.compressValue();
        this.fh = fh;

        switch (fh) {
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

        // 初始化到第一行第一个通道
        this.currentRow = 0;
        this.currentChannelIndex = 0;
        this.currentChannel = CHANNEL_ORDER[0];
        this.currentRowData = this.multiRowData.rowDataWithRowIndex(currentRow);
        loadCurrentChannelData();

        this.startTime = 0;
        this.currentTime = 0;

        RBQLog.i("CMYK打印数据 长度 :" + (float) totalDataLen / 1000.0f + "k; 共分" + totalPacketCount + "包; 共" + totalRowCount + "行");
    }

    /**
     * 加载当前通道的数据
     */
    private void loadCurrentChannelData() {
        if (currentRowData != null) {
            this.currentChannelByteData = this.currentRowData.dataByChannel(currentChannel);
            this.currentChannelDataLength = this.currentChannelByteData != null ? this.currentChannelByteData.length : 0;
            this.currentChannelTotalPacketCount = calculatePacketCount(currentChannelDataLength);
        } else {
            this.currentChannelByteData = null;
            this.currentChannelDataLength = 0;
            this.currentChannelTotalPacketCount = 0;
        }
        this.indexInCurrentChannelPacket = -1;
    }

    private int calculatePacketCount(int dataLength) {
        if (dataLength == 0) return 0;
        if (dataLength % usefulPacketDataLength == 0) {
            return dataLength / usefulPacketDataLength;
        } else {
            return dataLength / usefulPacketDataLength + 1;
        }
    }

    public void clear() {
        this.progress = 0;
        this.totalPacketCount = 0;
        this.totalRowCount = 0;
        this.index = -1;
        this.currentRow = 0;
        this.indexInCurrentChannelPacket = -1;
        this.totalDataLen = 0;
        this.fullPacketDataLen = 0;

        this.currentChannelIndex = 0;
        this.currentChannel = CMYKChannel.C;

        this.multiRowData = null;
        this.currentRowData = null;
        this.currentChannelByteData = null;
        this.currentChannelTotalPacketCount = 0;
        this.currentChannelDataLength = 0;

        this.reusablePacketBuffer = null; // 释放复用 buffer

        this.startTime = 0;
        this.currentTime = 0;

        super.clear();
    }

    // ==================== 状态查询方法 ====================

    public Boolean hasData() {
        if (multiRowData == null) {
            return false;
        }
        return totalDataLen != 0;
    }

    public int getCurrentRow() {
        return currentRow;
    }

    public CMYKChannel getCurrentChannel() {
        return currentChannel;
    }

    public int getCurrentChannelIndex() {
        return currentChannelIndex;
    }

    /**
     * 获取当前通道的数据长度（用于发送指令告诉MCU数据大小）
     */
    public int getCurrentChannelDataLength() {
        return currentChannelDataLength;
    }

    /**
     * 获取当前通道的总包数
     */
    public int getCurrentChannelTotalPacketCount() {
        return currentChannelTotalPacketCount;
    }

    // ==================== 判断方法 ====================

    /**
     * 判断当前通道是否还有下一包数据要发送
     */
    public Boolean hasNextPacketInCurrentChannel() {
        if (this.multiRowData == null || this.currentChannelByteData == null) {
            return false;
        }
        return this.currentChannelTotalPacketCount > 0
                && (this.indexInCurrentChannelPacket + 1) < this.currentChannelTotalPacketCount;
    }

    /**
     * 判断当前行是否还有下一个通道
     */
    public Boolean hasNextChannelInCurrentRow() {
        return (currentChannelIndex + 1) < CHANNEL_COUNT;
    }

    /**
     * 判断是否还有下一行
     */
    public Boolean hasNextRow() {
        if (this.multiRowData == null) {
            return false;
        }
        return (currentRow + 1) < totalRowCount;
    }

    /**
     * 判断当前通道数据是否已全部发送完毕
     * （当前通道没有下一包了）
     */
    public Boolean isCurrentChannelComplete() {
        return !hasNextPacketInCurrentChannel() && indexInCurrentChannelPacket >= 0;
    }

    /**
     * 判断当前行的所有通道是否都发送完毕
     * （当前是最后一个通道K，且K通道数据发完了）
     */
    public Boolean isCurrentRowComplete() {
        return currentChannelIndex == (CHANNEL_COUNT - 1) && isCurrentChannelComplete();
    }

    /**
     * 判断所有数据是否都发送完毕
     */
    public Boolean isAllDataComplete() {
        return isCurrentRowComplete() && !hasNextRow();
    }

    // ==================== 游标移动方法 ====================

    /**
     * 切换到当前行的下一个通道
     * 调用前应先发送当前通道完成的指令
     * @return 是否成功切换
     */
    public Boolean moveToNextChannel() {
        if (!hasNextChannelInCurrentRow()) {
            return false;
        }

        currentChannelIndex = currentChannelIndex + 1;
        currentChannel = CHANNEL_ORDER[currentChannelIndex];
        loadCurrentChannelData();

        RBQLog.i("切换到通道: " + currentChannel.getName() + ", 行: " + currentRow);

        return true;
    }

    /**
     * 切换到下一行（自动重置到C通道）
     * 调用前应先发送当前行完成的指令
     * @return 是否成功切换
     */
    public Boolean moveToNextRow() {
        if (!hasNextRow()) {
            return false;
        }

        currentRow = currentRow + 1;
        currentRowData = multiRowData.rowDataWithRowIndex(currentRow);

        // 重置到C通道
        currentChannelIndex = 0;
        currentChannel = CHANNEL_ORDER[0];
        loadCurrentChannelData();

        RBQLog.i("切换到下一行: " + currentRow + ", 通道: " + currentChannel.getName());

        return true;
    }

    // ==================== 数据包获取方法（复用 buffer，减少 GC）====================

    /**
     * 获取下一包的完整格式化数据（含帧头+CRC），直接写入复用 buffer。
     * 消除原 getNextPacket() + packetFormat() 两次分配。
     *
     * ⚠️ 返回的是 reusablePacketBuffer 引用，安全前提：
     * XModem ACK 停等协议保证收到 ACK 后才调用此方法生成下一包，
     * 此时上一包数据已被 WriteThread 写入 OutputStream，buffer 可安全覆盖。
     * 如需改为流水线/预取模式，须改用双缓冲。
     */
    public byte[] buildNextFormattedPacket() {
        if (currentChannelByteData == null) {
            return null;
        }

        index++;
        indexInCurrentChannelPacket++;
        fillFormattedPacket(indexInCurrentChannelPacket);
        return reusablePacketBuffer;
    }

    /**
     * 获取当前包的完整格式化数据（用于 NAK 重传），直接写入复用 buffer。
     */
    public byte[] buildCurrentFormattedPacket() {
        fillFormattedPacket(indexInCurrentChannelPacket);
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
        int remainingData = currentChannelDataLength - start;

        if (remainingData >= usefulPacketDataLength) {
            System.arraycopy(currentChannelByteData, start, reusablePacketBuffer, offset, usefulPacketDataLength);
        } else {
            System.arraycopy(currentChannelByteData, start, reusablePacketBuffer, offset, remainingData);
            Arrays.fill(reusablePacketBuffer, offset + remainingData, offset + usefulPacketDataLength, (byte) 0x1A);
        }

        offset += usefulPacketDataLength;
        char crc = CRC16.crc16_calc(reusablePacketBuffer, 0, offset);
        reusablePacketBuffer[offset++] = (byte) (crc >> 8 & 0xFF);
        reusablePacketBuffer[offset] = (byte) (crc & 0xFF);
    }

    // ====== 以下方法保留用于兼容，内部改为复用 buffer ======

    /**
     * @deprecated 请使用 {@link #buildNextFormattedPacket()} 替代。
     */
    @Deprecated
    public byte[] getNextPacket() {
        if (currentChannelByteData == null) {
            return null;
        }

        index++;
        indexInCurrentChannelPacket++;

        return getRawPacketData(indexInCurrentChannelPacket);
    }

    /**
     * @deprecated 请使用 {@link #buildCurrentFormattedPacket()} 替代。
     */
    @Deprecated
    public byte[] getCurrentPacket() {
        if (currentChannelByteData == null || indexInCurrentChannelPacket < 0) {
            return null;
        }
        return getRawPacketData(indexInCurrentChannelPacket);
    }

    private byte[] getRawPacketData(int pktIndex) {
        byte[] packet = new byte[usefulPacketDataLength];
        int start = pktIndex * usefulPacketDataLength;
        int remainingData = currentChannelDataLength - start;

        if (remainingData >= usefulPacketDataLength) {
            System.arraycopy(currentChannelByteData, start, packet, 0, usefulPacketDataLength);
        } else {
            System.arraycopy(currentChannelByteData, start, packet, 0, remainingData);
            Arrays.fill(packet, remainingData, usefulPacketDataLength, (byte) 0x1A);
        }
        return packet;
    }

    /**
     * @deprecated 请使用 {@link #buildNextFormattedPacket()} / {@link #buildCurrentFormattedPacket()} 替代。
     */
    @Deprecated
    public byte[] packetFormat(@NonNull byte[] data) {
        byte[] packetData = new byte[fullPacketDataLen];

        int offset = 0;
        packetData[offset++] = (byte) fh;
        packetData[offset++] = (byte) (~fh & 0xFF);
        System.arraycopy(data, 0, packetData, offset, data.length);

        char crc = CRC16.crc16_calc(packetData, 0, data.length + 2);
        offset = offset + data.length;
        packetData[offset++] = (byte) (crc >> 8 & 0xFF);
        packetData[offset] = (byte) (crc & 0xFF);

        return packetData;
    }

    // ==================== 进度相关 ====================

    public boolean invalidateProgress() {
        int progress = (int) Math.floor((float) this.index / (float) this.totalPacketCount * 100);

        if (progress == this.progress)
            return false;

        this.progress = progress;

        return true;
    }

    public int getProgress() {
        return this.progress;
    }
}
