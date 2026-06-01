package com.mx.mxSdk.imageCompress;

import java.io.File;

/**
 * Created by RBQ on 2025/8/15.
 * <p>
 * 压缩状态回调接口
 */
public interface CompressListener {
    /**
     * 压缩开始
     */
    void onStart();

    /**
     * 压缩成功
     *
     * @param file 压缩后的文件
     */
    void onSuccess(File file);

    /**
     * 压缩失败
     *
     * @param e 异常信息
     */
    void onError(Throwable e);
}