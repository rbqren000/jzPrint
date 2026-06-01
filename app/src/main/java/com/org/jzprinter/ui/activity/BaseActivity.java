package com.org.jzprinter.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import com.org.jzprinter.R;
import com.org.jzprinter.manager.RBQAppManager;

import com.org.jzprinter.utils.SoftKeyBoard.KeyboardUtils;
import com.mx.mxSdk.Utils.RBQLog;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.content.res.Configuration;

public abstract class BaseActivity extends AppCompatActivity implements StatusBarColorProvider {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RBQAppManager.share().addActivity(this);



    }

    private void applyStatusBarColor(int color) {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(color);
        // 自动设置状态栏字体颜色
        setStatusBarTextColor(isColorLight(color));
    }

    public void setupStatusBarWithDefaultColorResId() {
        int colorResId = getStatusBarColorResId();
        if (colorResId != 0) {
            applyStatusBarColor(ContextCompat.getColor(this, colorResId));
        }
    }

    public void setupStatusBarWithCustomColorResId(int colorResId) {
        applyStatusBarColor(ContextCompat.getColor(this, colorResId));
    }

    public void setupStatusBarWithCustomColor(int color) {
        applyStatusBarColor(color);
    }

    public void setupStatusBarWithCustomHexColor(String hexColor) {
        applyStatusBarColor(Color.parseColor(hexColor));
    }

    public void setupStatusBarWithColorAttributeId(int colorAttributeId) {
        applyStatusBarColor(getThemeColorByAttributeId(colorAttributeId, Color.BLACK));
    }

    private void setStatusBarTextColor(boolean isLightStatusBar) {
        // 从 Android 6.0 (API level 23) 开始支持
        Window window = getWindow();
        int flags = window.getDecorView().getSystemUiVisibility();
        if (isLightStatusBar) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; // 添加标志以使状态栏文字变为浅色
        } else {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; // 移除标志以使状态栏文字恢复为深色
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    public static boolean isColorLight(int color) {
        // 计算颜色的亮度
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return luminance > 0.5;
    }

    public int getThemeColorByAttributeId(int attributeId) {
        return getThemeColorByAttributeId(attributeId, Color.WHITE);  // 使用黑色作为默认颜色
    }

    public int getThemeColorByAttributeId(int attributeId, int defaultColor) {
        TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(attributeId, typedValue, true)) {
            if (typedValue.resourceId != 0) {
                return ContextCompat.getColor(this, typedValue.resourceId);
            } else {
                return typedValue.data;
            }
        } else {
            return defaultColor;
        }
    }

    public void hideKeyboardOnTouch(View view, Activity context) {
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    KeyboardUtils.closeKeyboard(context);
                    return false;
                }
            });
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                hideKeyboardOnTouch(innerView,context);
            }
        }
    }

    public static boolean isPad(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);

        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
        double screenInches = Math.sqrt(x + y); // 屏幕尺寸

        int screenLayout = context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        return screenInches >= 7.0 && screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    //主线程运行
    public void rbqRunOnUiThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    public void showToast(String message){

        if (TextUtils.isEmpty(message)) {
            RBQLog.i("Toast message is empty, skipping display.");
            return;
        }
        rbqRunOnUiThread(() -> Toast.makeText(BaseActivity.this,message,Toast.LENGTH_SHORT).show());

    }

    public void showToast(int id){

        String message = getString(id);
        if (TextUtils.isEmpty(message)) {
            RBQLog.i("Toast message is empty, skipping display.");
            return;
        }
        rbqRunOnUiThread(() -> Toast.makeText(BaseActivity.this,message,Toast.LENGTH_SHORT).show());
    }

    public void showImageToast(String message) {

        if (TextUtils.isEmpty(message)) {
            RBQLog.i("Toast message is empty, skipping display.");
            return;
        }
        rbqRunOnUiThread(() -> Toast.makeText(BaseActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    public void showImageToast(int message_id) {

        String message = getString(message_id);

        if (TextUtils.isEmpty(message)) {
            RBQLog.i("Toast message is empty, skipping display.");
            return;
        }
        rbqRunOnUiThread(() -> Toast.makeText(BaseActivity.this, message, Toast.LENGTH_SHORT).show());
    }

}
