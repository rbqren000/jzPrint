package com.mx.mxSdk;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.mx.mxSdk.Utils.MxSdkStore;
import java.util.HashMap;
import java.util.Map;

/**
 * CMYK通道数据管理类
 * 用于存储和管理CMYK四个通道的数据保存位置和相关信息
 */
public class CMYKRowData implements Parcelable {

    public static final String ROW_DATA = "rowData";

    /**
     * 各通道数据路径映射表
     */
    private final Map<CMYKChannel, String> channelPathsMap;

    private final int channelDataLength;

    /**
     * 构造函数 - 直接传入各通道数据路径映射表
     * @param channelPathsMap 各通道数据路径映射表
     * @param channelDataLength 通道数据长度
     */
    CMYKRowData(@NonNull Map<CMYKChannel, String> channelPathsMap, int channelDataLength)  {
        this.channelPathsMap = new HashMap<>(channelPathsMap);
        this.channelDataLength = channelDataLength;
    }

    public static CMYKRowData createInstance(@NonNull Map<CMYKChannel, String> channelPathsMap, int channelDataLength) {
        return new CMYKRowData(channelPathsMap, channelDataLength);
    }

    public static final Creator<CMYKRowData> CREATOR = new Creator<CMYKRowData>() {
        @Override
        public CMYKRowData createFromParcel(Parcel in) {
            return new CMYKRowData(in);
        }

        @Override
        public CMYKRowData[] newArray(int size) {
            return new CMYKRowData[size];
        }
    };

    /**
     * 从Parcel中读取数据创建CMYKRowData实例
     * @param in Parcel对象，包含序列化后的数据
     */
    protected CMYKRowData(Parcel in) {
        channelDataLength = in.readInt();
        
        // 从Parcel中读取通道路径映射表
        int mapSize = in.readInt();
        channelPathsMap = new HashMap<>(mapSize);
        for (int i = 0; i < mapSize; i++) {
            CMYKChannel channel = CMYKChannel.fromIndex(in.readInt());
            String path = in.readString();
            if (channel != null) {
                channelPathsMap.put(channel, path);
            }
        }
    }

    /**
     * 将CMYKRowData对象序列化到Parcel中
     * @param parcel Parcel对象，用于存储序列化后的数据
     * @param i 附加的标志位，通常是0
     */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        // 写入通道数据长度
        parcel.writeInt(channelDataLength);
        
        // 将通道路径映射表写入Parcel
        parcel.writeInt(channelPathsMap.size());
        for (Map.Entry<CMYKChannel, String> entry : channelPathsMap.entrySet()) {
            parcel.writeInt(entry.getKey().getIndex());
            parcel.writeString(entry.getValue());
        }
    }

    /**
     * 获取单通道数据长度
     */
    public int getChannelDataLength() {
        return channelDataLength;
    }

    /**
     * 获取所有通道的总数据长度
     */
    public int totalDataLength() {
        return channelDataLength * 4;
    }

    /**
     * 获取所有通道的总包数量
     * 注意：每个通道独立计算包数（因为每个通道最后一包可能需要填充）
     * @param usefulDataLen 每包有效数据长度
     */
    public int totalPacketCount(int usefulDataLen) {
        return totalPacketCountByChannel(usefulDataLen) * 4;
    }

    /**
     * 单通道数据包数量
     * @param usefulDataLen 每包有效数据长度
     */
    public int totalPacketCountByChannel(int usefulDataLen) {
        if (channelDataLength == 0) return 0;
        if (channelDataLength % usefulDataLen == 0) {
            return channelDataLength / usefulDataLen;
        } else {
            return channelDataLength / usefulDataLen + 1;
        }
    }

    /**
     * 获取指定通道的数据
     * @param channel 颜色通道
     * @return 通道数据字节数组
     */
    public byte[] dataByChannel(CMYKChannel channel) {
        String dataPath = channelPathsMap.get(channel);
        if (dataPath != null) {
            return MxSdkStore.readByteArrToCacheDataFile(dataPath);
        }
        return null;
    }
    
    /**
     * 获取指定通道的数据保存路径
     * @param channel 颜色通道
     * @return 数据保存路径
     */
    public String getChannelDataPath(CMYKChannel channel) {
        return channelPathsMap.get(channel);
    }
    
    /**
     * 获取所有通道的数据保存路径
     * @return 通道和路径的映射表
     */
    public Map<CMYKChannel, String> getAllChannelPaths() {
        return new HashMap<>(channelPathsMap);
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
