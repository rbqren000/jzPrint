package com.org.jzprinter.print;

import android.content.Context;

import com.org.jzprinter.R;
import com.org.jzprinter.utils.Storage.PreferencesUtils;

/**
 * 打印配置中心，统一管理所有打印相关的偏好设置。
 * 替代之前分散在 PrintSettingsActivity 中的静态配置方法。
 */
public class PrintConfig {

    private static final String KEY_ODD_PAGE_ON_RIGHT = "odd_page_on_right";
    private static final String KEY_LEFT_PRINT_DIRECTION_BTT = "left_print_direction_btt";
    private static final String KEY_RIGHT_PRINT_DIRECTION_BTT = "right_print_direction_btt";

    // region 奇数页位置

    public static boolean isOddPageOnRight(Context context) {
        return PreferencesUtils.getBoolean(context, KEY_ODD_PAGE_ON_RIGHT, true);
    }

    public static void setOddPageOnRight(Context context, boolean oddOnRight) {
        PreferencesUtils.putBoolean(context, KEY_ODD_PAGE_ON_RIGHT, oddOnRight);
    }

    // endregion

    // region 打印方向（左侧/右侧独立）

    public static boolean isLeftBottomToTop(Context context) {
        return PreferencesUtils.getBoolean(context, KEY_LEFT_PRINT_DIRECTION_BTT, false);
    }

    public static void setLeftBottomToTop(Context context, boolean bottomToTop) {
        PreferencesUtils.putBoolean(context, KEY_LEFT_PRINT_DIRECTION_BTT, bottomToTop);
    }

    public static boolean isRightBottomToTop(Context context) {
        return PreferencesUtils.getBoolean(context, KEY_RIGHT_PRINT_DIRECTION_BTT, false);
    }

    public static void setRightBottomToTop(Context context, boolean bottomToTop) {
        PreferencesUtils.putBoolean(context, KEY_RIGHT_PRINT_DIRECTION_BTT, bottomToTop);
    }

    // endregion

    // region 便捷方法

    public static PrintImagePreparer.RotationDirection getRotationForPage(Context context, int pageCode) {
        return PrintImagePreparer.getRotation(pageCode,
            isOddPageOnRight(context), isLeftBottomToTop(context), isRightBottomToTop(context));
    }

    public static PrintImagePreparer.VerticalAlignment getAlignmentForPage(Context context, int pageCode) {
        return PrintImagePreparer.getAlignment(pageCode,
            isOddPageOnRight(context), isLeftBottomToTop(context), isRightBottomToTop(context));
    }

    /**
     * 生成当前打印设置的描述文案。
     * 所有需要显示设置摘要的页面统一调用此方法，文案源自 strings.xml 资源，
     * 修改资源文件即可同步更新所有引用处。
     */
    public static String getSettingsSummary(Context context) {
        String oddPage = isOddPageOnRight(context)
            ? context.getString(R.string.print_setting_odd_right)
            : context.getString(R.string.print_setting_odd_left);
        String leftDir = isLeftBottomToTop(context)
            ? context.getString(R.string.print_setting_direction_btot)
            : context.getString(R.string.print_setting_direction_ttob);
        String rightDir = isRightBottomToTop(context)
            ? context.getString(R.string.print_setting_direction_btot)
            : context.getString(R.string.print_setting_direction_ttob);
        return context.getString(R.string.print_setting_summary_fmt, oddPage, leftDir, rightDir);
    }

    // endregion
}
