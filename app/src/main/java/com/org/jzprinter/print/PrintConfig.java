package com.org.jzprinter.print;

import android.content.Context;

import com.org.jzprinter.utils.Storage.PreferencesUtils;

/**
 * 打印配置中心，统一管理所有打印相关的偏好设置。
 * 替代之前分散在 PrintSettingsActivity 中的静态配置方法。
 */
public class PrintConfig {

    private static final String KEY_ODD_PAGE_ON_RIGHT = "odd_page_on_right";

    public static boolean isOddPageOnRight(Context context) {
        return PreferencesUtils.getBoolean(context, KEY_ODD_PAGE_ON_RIGHT, true);
    }

    public static void setOddPageOnRight(Context context, boolean oddOnRight) {
        PreferencesUtils.putBoolean(context, KEY_ODD_PAGE_ON_RIGHT, oddOnRight);
    }

    public static PrintImagePreparer.RotationDirection getRotationForPage(Context context, int pageCode) {
        return PrintImagePreparer.getRotation(pageCode, isOddPageOnRight(context));
    }

    public static PrintImagePreparer.VerticalAlignment getAlignmentForPage(Context context, int pageCode) {
        return PrintImagePreparer.getAlignment(pageCode, isOddPageOnRight(context));
    }
}
