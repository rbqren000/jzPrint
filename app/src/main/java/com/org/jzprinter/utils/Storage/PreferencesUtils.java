package com.org.jzprinter.utils.Storage;

/**
 * Created by rbq on 16/9/2.
 */
import android.content.Context;
import android.content.SharedPreferences;
import com.mx.mxSdk.Utils.RBQLog;
import java.util.Set;

public class PreferencesUtils {

    private static final String SP_NAME = "appInfo";

    /**
     * 获取 SharedPreferences 对象
     *
     * @param context 上下文
     * @return SharedPreferences 对象
     */
    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 设置 int 类型的数据
     */
    public static void putInt(Context context, String name, int value) {
        getPreferences(context).edit().putInt(name, value).apply();
    }

    /**
     * 获取 int 类型的数据
     */
    public static int getInt(Context context, String name, int defaultValue) {
        return getPreferences(context).getInt(name, defaultValue);
    }

    /**
     * 设置 String 类型的数据
     */
    public static void putString(Context context, String name, String value) {
        getPreferences(context).edit().putString(name, value).apply();
    }

    /**
     * 获取 String 类型的数据
     */
    public static String getString(Context context, String name, String defaultValue) {
        return getPreferences(context).getString(name, defaultValue);
    }

    /**
     * 设置 boolean 类型的数据
     */
    public static void putBoolean(Context context, String name, boolean value) {
        getPreferences(context).edit().putBoolean(name, value).apply();
    }

    /**
     * 获取 boolean 类型的数据
     */
    public static boolean getBoolean(Context context, String name, boolean defaultValue) {
        return getPreferences(context).getBoolean(name, defaultValue);
    }

    /**
     * 设置 long 类型的数据
     */
    public static void putLong(Context context, String name, long value) {
        getPreferences(context).edit().putLong(name, value).apply();
    }

    /**
     * 获取 long 类型的数据
     */
    public static long getLong(Context context, String name, long defaultValue) {
        return getPreferences(context).getLong(name, defaultValue);
    }

    /**
     * 设置 float 类型的数据
     */
    public static void putFloat(Context context, String name, float value) {
        getPreferences(context).edit().putFloat(name, value).apply();
    }

    /**
     * 获取 float 类型的数据
     */
    public static float getFloat(Context context, String name, float defaultValue) {
        return getPreferences(context).getFloat(name, defaultValue);
    }

    /**
     * 设置 Set<String> 类型的数据
     */
    public static void putStringSet(Context context, String name, Set<String> value) {
        getPreferences(context).edit().putStringSet(name, value).apply();
    }

    /**
     * 获取 Set<String> 类型的数据
     */
    public static Set<String> getStringSet(Context context, String name, Set<String> defaultValue) {
        return getPreferences(context).getStringSet(name, defaultValue);
    }

    /**
     * 删除指定键的数据
     *
     * @param context 上下文
     * @param name    键名
     */
    public static void remove(Context context, String name) {
        getPreferences(context).edit().remove(name).apply();
    }

    /**
     * 清空所有数据
     *
     * @param context 上下文
     */
    public static void clear(Context context) {
        getPreferences(context).edit().clear().apply();
    }

    /**
     * 检查键是否存在
     *
     * @param context 上下文
     * @param name    键名
     * @return 是否存在
     */
    public static boolean contains(Context context, String name) {
        return getPreferences(context).contains(name);
    }

    /**
     * 输出调试日志（可选）
     *
     * @param message 消息内容
     */
    private static void log(String message) {
        if (true) {
            RBQLog.d(message);
        }
    }
}


