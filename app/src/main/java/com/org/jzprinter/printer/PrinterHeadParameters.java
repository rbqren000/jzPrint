package com.org.jzprinter.printer;

/**
 * 打印头参数模型
 * 
 * 包含打印头和编码轮的物理参数，支持X/Y轴独立校准
 */
public class PrinterHeadParameters {
    // Y轴参数（打印头高度 - 纵向）
    private float headerSizeMm;      // 打印头物理高度（毫米）
    private int rowPixDistance;      // 打印头像素高度（像素）
    
    // X轴参数（编码轮滚动 - 横向）
    private float encoderWheelMm;    // 编码轮基准距离（毫米），默认与headerSizeMm相同
    private int encoderWheelPx;      // 编码轮对应像素数，默认与rowPixDistance相同
    
    private String headModel;        // 打印头型号标识

    public float getHeaderSizeMm() { return headerSizeMm; }
    public void setHeaderSizeMm(float headerSizeMm) { this.headerSizeMm = headerSizeMm; }
    
    public int getRowPixDistance() { return rowPixDistance; }
    public void setRowPixDistance(int rowPixDistance) { this.rowPixDistance = rowPixDistance; }
    
    public float getEncoderWheelMm() { 
        return encoderWheelMm > 0 ? encoderWheelMm : headerSizeMm; 
    }
    public void setEncoderWheelMm(float encoderWheelMm) { this.encoderWheelMm = encoderWheelMm; }
    
    public int getEncoderWheelPx() { 
        return encoderWheelPx > 0 ? encoderWheelPx : rowPixDistance; 
    }
    public void setEncoderWheelPx(int encoderWheelPx) { this.encoderWheelPx = encoderWheelPx; }
    
    public String getHeadModel() { return headModel; }
    public void setHeadModel(String headModel) { this.headModel = headModel; }
}
