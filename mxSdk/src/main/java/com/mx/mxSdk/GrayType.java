package com.mx.mxSdk;

public enum GrayType {
    RGB(0), R(1), G(2), B(3);

    private final int type;

    GrayType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
