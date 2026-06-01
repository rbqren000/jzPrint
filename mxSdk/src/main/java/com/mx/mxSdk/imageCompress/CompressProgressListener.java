package com.mx.mxSdk.imageCompress;

/**
 * Created by RBQ on 2025/8/16.
 * <p>
 * 带进度回调的压缩监听接口
 * 继承自基本的CompressListener，增加了进度回调功能
 */
public interface CompressProgressListener extends CompressListener {
    /**
     * 压缩进度回调
     *
     * @param progress 压缩进度，范围0.0-1.0
     */
    void onProgress(float progress);
}