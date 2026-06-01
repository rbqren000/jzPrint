package com.org.jzprinter.utils.location;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;

/**
 * 定位工具类
 * - 仅提供定位开关检测与跳设置页的 Intent 构造
 */
public final class GPSUtils {

    private GPSUtils() {
        // no instance
    }

    /**
     * 判断定位是否开启：
     * - API 28+ 使用 LocationManager.isLocationEnabled
     * - 低版本近似判断：GPS 或 网络定位其一开启
     */
    public static boolean isLocationEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            return lm.isLocationEnabled();
        }
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * 兼容旧调用
     */
    public static boolean isGPSOpen(Context context) {
        return isLocationEnabled(context);
    }

    /**
     * 构造跳转到定位设置页的 Intent
     * 使用 Activity Result API 的 launcher 来启动该 Intent
     */
    public static Intent buildLocationSettingIntent() {
        return new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    }

    public static void openGPS(Context context) {
        context.startActivity(buildLocationSettingIntent());
    }
}
