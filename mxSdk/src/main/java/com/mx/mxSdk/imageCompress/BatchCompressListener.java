package com.mx.mxSdk.imageCompress;

import java.io.File;
import java.util.List;

/**
 * Created by RBQ on 2025/8/15.
 * <p>
 * 批量压缩状态回调接口
 */
public interface BatchCompressListener {
    
    /**
     * 批量压缩开始
     * @param totalCount 总任务数量
     */
    void onBatchStart(int totalCount);
    
    /**
     * 单个图片压缩开始
     * @param index 当前索引（从0开始）
     * @param inputPath 输入文件路径
     */
    void onSingleStart(int index, String inputPath);
    
    /**
     * 单个图片压缩成功
     * @param index 当前索引
     * @param inputPath 输入文件路径
     * @param outputFile 输出文件
     */
    void onSingleSuccess(int index, String inputPath, File outputFile);
    
    /**
     * 单个图片压缩失败
     * @param index 当前索引
     * @param inputPath 输入文件路径
     * @param error 错误信息
     */
    void onSingleError(int index, String inputPath, Throwable error);
    
    /**
     * 批量压缩进度更新
     * @param successCount 成功数量
     * @param totalCount 总数量
     * @param errorCount 失败数量
     */
    void onProgress(int successCount, int totalCount, int errorCount);
    
    /**
     * 批量压缩完成
     * @param successFiles 成功压缩的文件列表
     * @param errorList 失败的错误列表
     */
    void onBatchComplete(List<File> successFiles, List<BatchCompressError> errorList);
    
    /**
     * 批量压缩被取消
     * @param completedCount 取消时已完成的数量
     */
    void onBatchCancelled(int completedCount);
    
    /**
     * 批量压缩发生致命错误（如初始化失败、启动失败等）
     * @param error 错误信息
     */
    void onBatchError(Throwable error);

}