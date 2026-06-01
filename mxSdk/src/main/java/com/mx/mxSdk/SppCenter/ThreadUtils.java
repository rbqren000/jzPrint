package com.mx.mxSdk.SppCenter;

public class ThreadUtils {

    public static void sleep(float s) {
        try {
            int sleepTime = (int) (s * 1000.0f);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
