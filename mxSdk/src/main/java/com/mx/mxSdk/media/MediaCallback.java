package com.mx.mxSdk.media;

import java.util.ArrayList;

/**
 * 媒体选择回调接口
 */
public interface MediaCallback {
    /**
     * 返回选择结果
     *
     * @param result 媒体信息列表
     */
    void onResult(ArrayList<MediaInfo> result);

    /**
     * 取消选择
     */
    void onCancel();
}
