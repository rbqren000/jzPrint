package com.mx.mxSdk.imageCompress;

/**
 * Created by RBQ on 2025/8/15.
 * <p>
 * 压缩配置类
 */
public class CompressConfig {

    /**
     * 默认最大宽度
     */
    private int maxWidth = 720;

    /**
     * 默认最大高度
     */
    private int maxHeight = 1280;

    /**
     * 默认最大文件大小，单位 bytes
     */
    private long maxSize = 200 * 1024; // 200KB

    /**
     * 尺寸搜索步长
     */
    private float scaleStep = 0.05f;

    /**
     * 最小尺寸比例
     */
    private float minScale = 0.5f;

    /**
     * 最低压缩质量
     */
    private int minQuality = 10;

    /**
     * 默认构造函数
     */
    public CompressConfig() {
    }

    /**
     * 拷贝构造函数
     * @param other 要拷贝的配置对象
     */
    public CompressConfig(CompressConfig other) {
        if (other != null) {
            this.maxWidth = other.maxWidth;
            this.maxHeight = other.maxHeight;
            this.maxSize = other.maxSize;
            this.scaleStep = other.scaleStep;
            this.minScale = other.minScale;
            this.minQuality = other.minQuality;
        }
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public float getScaleStep() {
        return scaleStep;
    }

    public void setScaleStep(float scaleStep) {
        this.scaleStep = scaleStep;
    }

    public float getMinScale() {
        return minScale;
    }

    public void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    public int getMinQuality() {
        return minQuality;
    }

    public void setMinQuality(int minQuality) {
        this.minQuality = minQuality;
    }

    public static class Builder {
        private CompressConfig config;

        public Builder() {
            config = new CompressConfig();
        }

        public Builder setMaxWidth(int width) {
            config.setMaxWidth(width);
            return this;
        }

        public Builder setMaxHeight(int height) {
            config.setMaxHeight(height);
            return this;
        }

        public Builder setMaxSize(long size) {
            config.setMaxSize(size);
            return this;
        }

        public Builder setScaleStep(float step) {
            config.setScaleStep(step);
            return this;
        }

        public Builder setMinScale(float scale) {
            config.setMinScale(scale);
            return this;
        }

        public Builder setMinQuality(int quality) {
            config.setMinQuality(quality);
            return this;
        }

        public CompressConfig build() {
            return config;
        }
    }
}
