package com.org.jzprinter.print;

public enum PrintMode {
    ALL(1, "全部页"),
    ODD(2, "奇数页"),
    EVEN(3, "偶数页");

    private final int code;
    private final String label;

    PrintMode(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static PrintMode fromCode(int code) {
        for (PrintMode mode : values()) {
            if (mode.code == code) return mode;
        }
        throw new IllegalArgumentException("Unknown PrintMode code: " + code);
    }
}
