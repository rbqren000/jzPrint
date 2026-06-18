package com.mx.mxSdk.SppCenter;

public class ThreadUtils {

    /**
     * 休眠指定秒数，被中断时恢复中断标志但不抛出异常（吞掉中断）。
     * 适用于不需要响应中断的简单等待场景。
     */
    public static void sleep(float s) {
        try {
            int sleepTime = (int) (s * 1000.0f);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 休眠指定秒数，被中断时直接抛出 InterruptedException。
     * 适用于需要精确响应中断（如退出循环）的场景。
     */
    public static void sleepInterruptible(float s) throws InterruptedException {
        int sleepTime = (int) (s * 1000.0f);
        Thread.sleep(sleepTime);
    }
}
