package com.mx.mxSdk;

/**
 * CMYK 通道枚举
 * 用于类型安全地表示 CMYK 四个颜色通道
 */
public enum CMYKChannel {
    C(0,"Cyan"),
    M(1,"Magenta"),
    Y(2,"Yellow"),
    K(3,"Black");

    private final int index;
    private final String name;
    CMYKChannel(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return this.index;
    }

    public String getName() {
        return this.name;
    }

    public static CMYKChannel fromIndex(int index) {
        for (CMYKChannel channel : CMYKChannel.values()) {
            if (channel.getIndex() == index) {
                return channel;
            }
        }
        return null;
    }

    public static CMYKChannel fromName(String name) {
        for (CMYKChannel channel : CMYKChannel.values()) {
            if (channel.getName().equals(name)) {
                return channel;
            }
        }
        return null;
    }
    
    public static CMYKChannel fromShortName(String shortName) {
        for (CMYKChannel channel : CMYKChannel.values()) {
            if (channel.toString().equals(shortName)) {
                return channel;
            }
        }
        return null;
    }
}
