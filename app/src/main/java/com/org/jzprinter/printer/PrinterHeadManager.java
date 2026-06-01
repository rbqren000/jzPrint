package com.org.jzprinter.printer;

/**
 * 打印头参数管理器
 * 
 * 管理打印头的物理参数配置
 */
public class PrinterHeadManager {
    private static PrinterHeadManager instance;
    private PrinterHeadParameters currentParameters;
    private String headModel;

    private PrinterHeadManager() {
        this.currentParameters = createDefaultParameters();
        this.headModel = "default";
    }

    public static synchronized PrinterHeadManager getInstance() {
        if (instance == null) {
            instance = new PrinterHeadManager();
        }
        return instance;
    }

    public void setHeadModel(String headModel) {
        this.headModel = headModel;
        this.currentParameters = getParametersForModel(headModel);
    }

    public String getHeadModel() {
        return headModel;
    }

    private PrinterHeadParameters getParametersForModel(String headModel) {
        return createDefaultParameters();
    }

    public PrinterHeadParameters getParameters() {
        return currentParameters;
    }

    /**
     * 创建默认打印头参数
     * 
     * 默认参数：
     * - Y轴（纵向）：打印头物理高度 23.3mm，552像素
     * - X轴（横向）：编码轮基准距离 22.5mm，552像素（与Y轴相同）
     * - 型号：default
     * 
     * 注意：X轴和Y轴可以独立调整以校准不同方向的偏差
     */
    private PrinterHeadParameters createDefaultParameters() {
        PrinterHeadParameters params = new PrinterHeadParameters();
        
        // Y轴参数（打印头高度 - 纵向）
        params.setHeaderSizeMm(23.3f);
        params.setRowPixDistance(552);
        
        // X轴参数（编码轮滚动 - 横向）
        // 默认与Y轴相同，可以根据实际测量结果独立调整
        // 
        // 校准规则：
        // - 内容偏右 → 增大 encoderWheelMm
        // - 内容偏左 → 减小 encoderWheelMm
        // 
        // 当前值 23.6 的由来：手持机按压力度影响纸速，不同手法下表现：
        //   - 23.8 → 重手法偏左 ~1.5mm
        //   - 23.6 → 中等力度刚好居中，轻手法略微偏后（可接受）
        //   - 23.7 → 折中反而不上不下
        //   → 定在 23.6，优先保证常用力道下的精度
        params.setEncoderWheelMm(23.6f);  // 已实测校准
        params.setEncoderWheelPx(552);
        
        params.setHeadModel("default");
        return params;
    }
}
