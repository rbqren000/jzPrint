package com.mx.mxSdk;

public enum ConnType {

    SPP(0b00000001),    // 0x01, BlueTooth SPP 连接
    WiFi(0b00000001 << 1),   // 0x02, WiFi 连接
    AP(0b00000001 << 2),     // 0x04, AP 连接
    Serial(0b00000001 << 3); // 0x08, Serial 连接

    private final int value;

    ConnType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ConnType fromValue(int value) {
        for (ConnType type : ConnType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown enum value: " + value);
    }
}


