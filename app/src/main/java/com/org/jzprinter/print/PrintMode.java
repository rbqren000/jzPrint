package com.org.jzprinter.print;

import android.content.Context;

import com.org.jzprinter.R;

public enum PrintMode {
    ALL(1),
    ODD(2),
    EVEN(3);

    private final int code;

    PrintMode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public String getLabel(Context context) {
        return context.getString(getLabelRes());
    }

    public int getLabelRes() {
        switch (this) {
            case ALL: return R.string.print_mode_all;
            case ODD: return R.string.print_mode_odd;
            case EVEN: return R.string.print_mode_even;
            default: throw new IllegalStateException("Unknown mode: " + this);
        }
    }

    public static PrintMode fromCode(int code) {
        for (PrintMode mode : values()) {
            if (mode.code == code) return mode;
        }
        throw new IllegalArgumentException("Unknown PrintMode code: " + code);
    }
}
