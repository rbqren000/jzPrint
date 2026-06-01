package com.mx.mxSdk;

public class MxConfig {

    public static final float defaultRowReferenceDistance = 184;// 显示的是实际的的行参考线距离
    public static final float defaultRowPixDistance = 552; //最终截图的时候进行换算的

    public static final float defaultColumnReferenceDistance = 184;//实际显示列参考线距离
    public static final float defaultColumnPixDistance = 552;//最终截图的时候进行换算的

    // 23.3mm
    public static final float defaultPrinterHeaderSize = 23.3f;

    public static float mm_Unit(){
        return  defaultRowReferenceDistance / defaultPrinterHeaderSize;
    }

    public static float cm_Unit_2(){
        return  defaultRowReferenceDistance * 5f / defaultPrinterHeaderSize;
    }

    public static float cm_Unit(){
        return defaultRowReferenceDistance * 10f / defaultPrinterHeaderSize;
    }

    public static float dp2mm(float dp){
        return dp / mm_Unit();
    }

    public static float dp2cm(float dp){
        return dp / cm_Unit();
    }

    public static float mm2dp(float mm){
        return mm_Unit() * mm;
    }

    public static float cm2dp(float cm){
        return cm_Unit() * cm;
    }
}
