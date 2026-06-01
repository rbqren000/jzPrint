package com.mx.mxSdk.imageCompress;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by RBQ on 2025/8/15.
 * <p>
 * 批量压缩任务配置
 */
public class BatchCompressTask {
    
    private final List<BatchCompressItem> items;
    private final CompressConfig globalConfig;
    private final BatchCompressListener listener;
    private final File targetDir;
    private int maxConcurrency = 3;
    private boolean stopOnError = false;
    
    private BatchCompressTask(Builder builder) {
        this.items = builder.items;
        this.globalConfig = builder.globalConfig;
        this.listener = builder.listener;
        this.targetDir = builder.targetDir;
        this.maxConcurrency = builder.maxConcurrency;
        this.stopOnError = builder.stopOnError;
    }
    
    /**
     * 获取压缩项目列表
     */
    public List<BatchCompressItem> getItems() {
        return items;
    }
    
    /**
     * 获取全局配置
     */
    public CompressConfig getGlobalConfig() {
        return globalConfig;
    }
    
    /**
     * 获取监听器
     */
    public BatchCompressListener getListener() {
        return listener;
    }
    
    /**
     * 获取目标目录
     */
    public File getTargetDir() {
        return targetDir;
    }
    
    /**
     * 获取最大并发数
     */
    public int getMaxConcurrency() {
        return maxConcurrency;
    }
    
    /**
     * 是否遇到错误时停止
     */
    public boolean isStopOnError() {
        return stopOnError;
    }
    
    /**
     * 获取任务总数
     */
    public int getTotalCount() {
        return items.size();
    }
    
    /**
     * 构建器
     */
    public static class Builder {
        private final Context context;
        private List<BatchCompressItem> items = new ArrayList<>();
        private CompressConfig globalConfig;
        private BatchCompressListener listener;
        private File targetDir;
        private int maxConcurrency = 3;
        private boolean stopOnError = false;
        
        public Builder(Context context) {
            this.context = context.getApplicationContext();
            this.globalConfig = new CompressConfig(); // 默认配置
            this.targetDir = context.getCacheDir(); // 默认输出到缓存目录
        }
        
        /**
         * 添加单个压缩项目
         */
        public Builder addItem(BatchCompressItem item) {
            if (item != null) {
                this.items.add(item);
            }
            return this;
        }
        
        /**
         * 添加多个压缩项目
         */
        public Builder addItems(List<BatchCompressItem> items) {
            if (items != null) {
                this.items.addAll(items);
            }
            return this;
        }
        
        /**
         * 添加文件路径（使用默认配置）
         */
        public Builder addPath(String inputPath) {
            return addItem(new BatchCompressItem(inputPath));
        }
        
        /**
         * 添加文件路径列表（使用默认配置）
         */
        public Builder addPaths(List<String> inputPaths) {
            if (inputPaths != null) {
                for (String path : inputPaths) {
                    addPath(path);
                }
            }
            return this;
        }
        
        /**
         * 设置全局压缩配置
         */
        public Builder setGlobalConfig(CompressConfig config) {
            if (config != null) {
                this.globalConfig = new CompressConfig(config); // 创建副本，确保配置隔离
            } else {
                this.globalConfig = new CompressConfig(); // 如果传入null，则使用默认配置
            }
            return this;
        }
        
        /**
         * 设置监听器
         */
        public Builder setListener(BatchCompressListener listener) {
            this.listener = listener;
            return this;
        }
        
        /**
         * 设置目标目录
         */
        public Builder setTargetDir(String path) {
            this.targetDir = new File(path);
            return this;
        }
        
        /**
         * 设置目标目录
         */
        public Builder setTargetDir(File dir) {
            this.targetDir = dir;
            return this;
        }
        
        /**
         * 设置最大并发数
         */
        public Builder setMaxConcurrency(int maxConcurrency) {
            this.maxConcurrency = Math.max(1, maxConcurrency);
            return this;
        }
        
        /**
         * 设置是否遇到错误时停止
         */
        public Builder setStopOnError(boolean stopOnError) {
            this.stopOnError = stopOnError;
            return this;
        }
        
        /**
         * 构建任务
         */
        public BatchCompressTask build() {
            if (items.isEmpty()) {
                throw new IllegalArgumentException("批量压缩任务不能为空");
            }
            // 确保目标目录不为null
            if (targetDir == null) {
                throw new IllegalArgumentException("目标目录不能为空");
            }
            return new BatchCompressTask(this);
        }
    }
}