package com.mx.mxSdk.imageCompress;

import java.io.Serializable;

/**
 * 批量压缩错误信息
 */
public class BatchCompressError implements Serializable {
    private final int index;
    private final String inputPath;
    private final Throwable error;

    public BatchCompressError(int index, String inputPath, Throwable error) {
        this.index = index;
        this.inputPath = inputPath;
        this.error = error;
    }

    public int getIndex() {
        return index;
    }

    public String getInputPath() {
        return inputPath;
    }

    public Throwable getError() {
        return error;
    }
}
