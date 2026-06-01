package com.mx.mxSdk;

public enum CMYKType {
    CMYK(0), C(1), M(2), Y(3), K(4);

    private final int type;

    CMYKType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}

