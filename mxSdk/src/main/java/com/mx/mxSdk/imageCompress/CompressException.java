package com.mx.mxSdk.imageCompress;

/**
 * Created by RBQ on 2025/8/15.
 * <p>
 * 自定义压缩异常
 */
public class CompressException extends RuntimeException {
    public CompressException(String message) {
        super(message);
    }

    public CompressException(String message, Throwable cause) {
        super(message, cause);
    }
}