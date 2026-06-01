package com.org.jzprinter.utils.SoftKeyBoard;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by RBQ on 2025/10/13.
 * <p>
 * 软键盘工具类
 * </p>
 */
public final class KeyboardUtils {

    private static final String TAG = KeyboardUtils.class.getSimpleName();

    private KeyboardUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 自动弹软键盘
     * <p>
     * 使用 view.post() 替代 Timer，更轻量且能保证在UI线程执行。
     *
     * @param et 需要获取焦点的EditText
     */
    public static void showSoftInput(final EditText et) {
        if (et == null) return;
        et.post(() -> {
            et.setFocusable(true);
            et.setFocusableInTouchMode(true);
            // 请求获得焦点
            et.requestFocus();
            // 调用系统输入法
            InputMethodManager inputManager = (InputMethodManager) et.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null) {
                inputManager.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    /**
     * 强制显示软键盘
     *
     * @param activity 当前Activity
     */
    public static void show(Activity activity) {
        if (activity == null) return;
        View view = activity.getWindow().peekDecorView();
        if (view != null) {
            InputMethodManager inputManger = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManger != null) {
                inputManger.showSoftInput(view, InputMethodManager.SHOW_FORCED);
            }
        }
    }


    /**
     * 关闭软键盘（最通用的方法）
     *
     * @param context 上下文
     * @param view    当前拥有焦点的View
     */
    public static void closeKeyboard(Context context, View view) {
        if (context == null || view == null) return;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * 关闭软键盘
     *
     * @param activity 当前Activity
     */
    public static void closeKeyboard(Activity activity) {
        if (activity == null) return;
        View view = activity.getWindow().getCurrentFocus();
        if (view == null) {
            view = activity.getWindow().getDecorView();
        }
        closeKeyboard(activity, view);
    }


    /**
     * 输入法在窗口上已经显示，则隐藏，反之则显示
     *
     * @param activity 当前Activity
     */
    public static void toggle(Activity activity) {
        if (activity == null) return;
        InputMethodManager inputManger = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManger != null) {
            inputManger.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 判断键盘是否显示
     * <p>
     * 注意：此方法在某些设备或Android版本上（特别是使用全面屏手势时）可能不准确。
     * 推荐使用 androidx.core.view.WindowInsetsCompat 来进行更可靠的监听。
     *
     * @param activity
     * @return
     */
    public static boolean isSoftShowing(Activity activity) {
        if (activity == null) return false;
        // 获取当前屏幕内容的高度
        int screenHeight = activity.getWindow().getDecorView().getHeight();
        // 获取View可见区域的bottom
        Rect rect = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);

        return screenHeight - rect.bottom - getSoftButtonsBarHeight(activity) != 0;
    }


    /**
     * 获取底部虚拟按键栏的高度
     *
     * @return
     */
    private static int getSoftButtonsBarHeight(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        // 这个方法获取可能不是真实屏幕的高度
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        // 获取当前屏幕的真实高度
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;
        if (realHeight > usableHeight) {
            return realHeight - usableHeight;
        } else {
            return 0;
        }
    }
}