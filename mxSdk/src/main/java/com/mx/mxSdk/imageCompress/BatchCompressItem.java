package com.mx.mxSdk.imageCompress;

/**
 * Created by RBQ on 2025/8/15.
 * <p>
 * 批量压缩单项配置
 */
public class BatchCompressItem {
    
    private final String inputPath;
    private String outputPath;
    private CompressConfig customConfig;
    
    public BatchCompressItem(String inputPath) {
        if (inputPath == null || inputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("输入文件路径不能为空");
        }
        this.inputPath = inputPath.trim();
    }
    
    public BatchCompressItem(String inputPath, String outputPath) {
        if (inputPath == null || inputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("输入文件路径不能为空");
        }
        this.inputPath = inputPath.trim();
        this.outputPath = outputPath != null ? outputPath.trim() : null;
    }
    
    public BatchCompressItem(String inputPath, String outputPath, CompressConfig customConfig) {
        if (inputPath == null || inputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("输入文件路径不能为空");
        }
        this.inputPath = inputPath.trim();
        this.outputPath = outputPath != null ? outputPath.trim() : null;
        this.customConfig = customConfig;
    }
    
    /**
     * 获取输入文件路径
     */
    public String getInputPath() {
        return inputPath;
    }
    
    /**
     * 获取输出文件路径（可能为null，表示自动生成）
     */
    public String getOutputPath() {
        return outputPath;
    }
    
    /**
     * 设置输出文件路径
     */
    public BatchCompressItem setOutputPath(String outputPath) {
        this.outputPath = outputPath;
        return this;
    }
    
    /**
     * 获取自定义配置（可能为null，表示使用全局配置）
     */
    public CompressConfig getCustomConfig() {
        return customConfig;
    }
    
    /**
     * 设置自定义压缩配置
     */
    public BatchCompressItem setCustomConfig(CompressConfig customConfig) {
        this.customConfig = customConfig;
        return this;
    }
    
    /**
     * 判断是否有自定义配置
     */
    public boolean hasCustomConfig() {
        return customConfig != null;
    }
    
    /**
     * 判断是否有自定义输出路径
     */
    public boolean hasCustomOutputPath() {
        return outputPath != null && !outputPath.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "BatchCompressItem{" +
                "inputPath='" + inputPath + '\'' +
                ", outputPath='" + outputPath + '\'' +
                ", hasCustomConfig=" + hasCustomConfig() +
                '}';
    }
}