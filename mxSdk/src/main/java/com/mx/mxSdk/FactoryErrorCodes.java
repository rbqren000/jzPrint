package com.mx.mxSdk;

public class FactoryErrorCodes {
    /**上下文对象为null*/
    public static final int Context_NULL_ERROR = 1 << 0;
    /**图片路径为null*/
    public static final int IMAGE_PATH_NULL_ERROR = 1 << 1;
    /**图片为null*/
    public static final int IMAGE_NULL_ERROR = 1 << 2;
    /**RowImage为null*/
    public static final int ROW_IMAGE_NULL_ERROR = 1 << 3;
    /**MultiRowImage为null*/
    public static final int MULTI_ROW_IMAGE_NULL_ERROR = 1 << 4;
    /**MultiRowImage创建失败null*/
    public static final int MULTI_ROW_IMAGE_CREATION_FAILED = 1 << 5;
    /**文件没有找到*/
    public static final int FILE_NOT_FOUND = 1 << 6;
    /**io异常*/
    public static final int IOException_ERROR = 1 << 7;  // 16
}

