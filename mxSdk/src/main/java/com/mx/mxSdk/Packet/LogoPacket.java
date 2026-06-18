package com.mx.mxSdk.Packet;

import androidx.annotation.NonNull;

import com.mx.mxSdk.CRC16;
import com.mx.mxSdk.LogoData;
import com.mx.mxSdk.TransportProtocol;
import com.mx.mxSdk.Utils.RBQLog;

import java.util.Arrays;

//logo烧录包
public final class LogoPacket extends BasePacket {
	
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

	// 预分配的可复用包缓冲区，在 set() 中初始化，clear() 中释放
	private byte[] reusablePacketBuffer;
	
	public Boolean hasData() {
		if (data == null) {
			return false;
		}
		return dataLength != 0;
	}

	public void set(@NonNull LogoData logoData) {
		this.clear();

        this.data = logoData.data();
		this.dataLength = this.data.length;

		this.fh = TransportProtocol.STX_E;

		this.usefulPacketDataLength = 124;

		this.fullPacketDataLen = this.usefulPacketDataLength + packetHeadLen + packetHeadXorLen + crcLen;
		this.reusablePacketBuffer = new byte[fullPacketDataLen]; // 预分配复用 buffer

		if (dataLength % usefulPacketDataLength == 0) {

			totalPacketCount = dataLength / usefulPacketDataLength;
		} else {

			totalPacketCount = dataLength / usefulPacketDataLength + 1;
		}
		RBQLog.i("logo data 长度 :"+(float) dataLength /1000.0f+"k; 共分"+ totalPacketCount +"包");
	}
	
	/**
	 * @param logoData 数据
	 * @param fh   帧头
	 */
	public void set(@NonNull LogoData logoData, int fh) {
		this.clear();
		
		this.data = logoData.data();
		this.dataLength = this.data.length;
		
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
		
		if (dataLength % usefulPacketDataLength == 0) {
			
			totalPacketCount = dataLength / usefulPacketDataLength;
		} else {
			
			totalPacketCount = dataLength / usefulPacketDataLength + 1;
		}
		RBQLog.i("logo data 长度 :" + (float) dataLength / 1000.0f + "k; 共分" + totalPacketCount + "包");
	}
	
	public void clear() {
		
		this.progress = 0;
		this.totalPacketCount = 0;
		this.index = -1;
		this.data = null;
		this.dataLength = 0;

		this.reusablePacketBuffer = null; // 释放复用 buffer
		
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
		
		if (nexIndex >= this.totalPacketCount) {
			return -1;
		}
		return nexIndex;
	}
	
	// ====== 复用 buffer 版本的方法 ======

	/**
	 * 获取下一包的完整格式化数据（含帧头+CRC），直接写入复用 buffer。
	 *
	 * ⚠️ 返回的是 reusablePacketBuffer 引用，安全前提：
	 * XModem ACK 停等协议保证收到 ACK 后才调用此方法生成下一包，
	 * 此时上一包数据已被 WriteThread 写入 OutputStream，buffer 可安全覆盖。
	 * 如需改为流水线/预取模式，须改用双缓冲。
	 */
	public byte[] buildNextFormattedPacket() {
		int nextIdx = this.getNextPacketIndex();
		if (nextIdx != -1) {
			this.index = nextIdx;
			fillFormattedPacket(nextIdx);
			return reusablePacketBuffer;
		}
		return null;
	}

	/**
	 * 获取当前包的完整格式化数据（用于 NAK 重传），直接写入复用 buffer。
	 */
	public byte[] buildCurrentFormattedPacket() {
		fillFormattedPacket(this.index);
		return reusablePacketBuffer;
	}

	/**
	 * 将指定索引的原始数据填充到 reusablePacketBuffer 中，包含帧头、数据和 CRC。
	 */
	private void fillFormattedPacket(int pktIndex) {
		int start = pktIndex * usefulPacketDataLength;
		int remainingData = dataLength - start;

		int offset = 0;
		reusablePacketBuffer[offset++] = (byte) fh;
		reusablePacketBuffer[offset++] = (byte) (~fh & 0xFF);

		if (remainingData >= usefulPacketDataLength) {
			System.arraycopy(this.data, start, reusablePacketBuffer, offset, usefulPacketDataLength);
		} else {
			System.arraycopy(this.data, start, reusablePacketBuffer, offset, remainingData);
			java.util.Arrays.fill(reusablePacketBuffer, offset + remainingData, offset + usefulPacketDataLength, (byte) 0x1A);
		}

		offset += usefulPacketDataLength;
		char crc = CRC16.crc16_calc(reusablePacketBuffer, 0, offset);
		reusablePacketBuffer[offset++] = (byte) (crc >> 8 & 0xFF);
		reusablePacketBuffer[offset] = (byte) (crc & 0xFF);
	}

	// ====== 以下方法保留用于兼容 ======

	@Deprecated
	public byte[] getNextPacket() {
		int index = this.getNextPacketIndex();
		if (index != -1) {
			return this.getPacket(index);
		}
		return null;
	}

	@Deprecated
	public byte[] getPacket() {
		return getPacket(this.index);
	}

	@Deprecated
	public byte[] getPacket(int index) {
		this.index = index;
		byte[] packet = new byte[usefulPacketDataLength];

		int start = index * usefulPacketDataLength;
		int remainingData = dataLength - start;

		if (remainingData >= usefulPacketDataLength) {
			System.arraycopy(this.data, start, packet, 0, usefulPacketDataLength);
		} else {
			System.arraycopy(this.data, start, packet, 0, remainingData);
			java.util.Arrays.fill(packet, remainingData, usefulPacketDataLength, (byte) 0x1A);
		}
		return packet;
	}

	@Deprecated
	public byte[] packetFormat(@NonNull byte[] data) {
		byte[] logoData = new byte[fullPacketDataLen];

		int offset = 0;
		logoData[offset++] = (byte) fh;
		logoData[offset++] = (byte) (~fh & 0xFF);
		System.arraycopy(data, 0, logoData, offset, data.length);

		char crc = CRC16.crc16_calc(logoData, 0, data.length + 2);
		offset = offset + data.length;
		logoData[offset++] = (byte) (crc >> 8 & 0xFF);
		logoData[offset] = (byte) (crc & 0xFF);

		return logoData;
	}
	
	public boolean invalidateProgress() {
		
		float a = this.getNextPacketIndex();
		
		if (a == -1) {
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