package com.org.jzprinter.utils;

import android.content.ContentValues;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by RBQ on 2025/10/16.
 * 对象工具类：提供对象比较、空字符串处理、数组类型转换、反射取值、对象转 ContentValues 等功能。
 */
public final class ObjectUtils {

    private static final String TAG = "ObjectUtils";

    private ObjectUtils() {
        throw new AssertionError("No ObjectUtils instances for you!");
    }

    /**
     * 比较两个对象是否相等（安全空判断）
     * 等价于 Objects.equals(actual, expected)
     */
    public static boolean isEquals(Object actual, Object expected) {
        return Objects.equals(actual, expected);
    }

    /**
     * 将可能为 null 的对象转为字符串。
     * - 为 null 时返回空串 ""
     * - 为 String 时直接返回
     * - 其他对象调用 toString()
     */
    public static String nullStrToEmpty(Object str) {
        return (str == null ? "" : (str instanceof String ? (String) str : String.valueOf(str)));
    }

    /**
     * 原始 long[] 转 Long[]（空安全）
     * - source 为 null 返回空数组
     */
    public static Long[] transformLongArray(long[] source) {
        if (source == null) return new Long[0];
        Long[] destin = new Long[source.length];
        for (int i = 0; i < source.length; i++) {
            destin[i] = source[i];
        }
        return destin;
    }

    /**
     * Long[] 转原始 long[]（空安全）
     * - source 为 null 返回空数组
     * - 元素为 null 时按 0L 处理，避免 NPE
     */
    public static long[] transformLongArray(Long[] source) {
        if (source == null) return new long[0];
        long[] destin = new long[source.length];
        for (int i = 0; i < source.length; i++) {
            Long v = source[i];
            destin[i] = (v == null ? 0L : v);
        }
        return destin;
    }

    /**
     * 原始 int[] 转 Integer[]（空安全）
     * - source 为 null 返回空数组
     */
    public static Integer[] transformIntArray(int[] source) {
        if (source == null) return new Integer[0];
        Integer[] destin = new Integer[source.length];
        for (int i = 0; i < source.length; i++) {
            destin[i] = source[i];
        }
        return destin;
    }

    /**
     * Integer[] 转原始 int[]（空安全）
     * - source 为 null 返回空数组
     * - 元素为 null 时按 0 处理，避免 NPE
     */
    public static int[] transformIntArray(Integer[] source) {
        if (source == null) return new int[0];
        int[] destin = new int[source.length];
        for (int i = 0; i < source.length; i++) {
            Integer v = source[i];
            destin[i] = (v == null ? 0 : v);
        }
        return destin;
    }

    /**
     * 比较两个可比较对象（含空值比较）
     * 规则：
     * - v1 与 v2 同为 null：返回 0
     * - v1 为 null：返回 -1
     * - v2 为 null：返回 1
     * - 否则使用 v1.compareTo(v2)
     */
    public static <V extends Comparable<? super V>> int compare(V v1, V v2) {
        if (v1 == v2) return 0;
        if (v1 == null) return -1;
        if (v2 == null) return 1;
        return v1.compareTo(v2);
    }

    /**
     * 将对象的非空属性写入 ContentValues（反射）
     * - 支持父类字段
     * - 跳过 static、transient、synthetic 字段
     * - 优先通过 getter（getX / isX）获取；失败则直接字段访问
     * - 按实际类型写入（String、数字、Boolean、byte[] 等），其他转为字符串
     *
     * @param o 目标对象（可为任意 Java Bean）
     * @return ContentValues（若 o 为 null 返回空的 ContentValues）
     */
    public static ContentValues getContentValues(Object o) {
        ContentValues values = new ContentValues();
        if (o == null) return values;

        try {
            List<Field> fields = getAllDeclaredFields(o.getClass());
            for (Field field : fields) {
                // 跳过 static/transient/synthetic 字段
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || field.isSynthetic()) {
                    continue;
                }
                String key = field.getName();

                Object value = null;

                // 1) 尝试从 getter 读取（getX / isX）
                value = getFieldValueByName(key, o);

                // 2) 若 getter 不可用或返回 null，尝试直接字段读取
                if (value == null) {
                    value = readField(field, o);
                }

                // 3) 非空则写入
                if (value != null) {
                    safePut(values, key, value);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getContentValues 失败：" + e.getMessage(), e);
        }
        return values;
    }

    /**
     * 使用反射根据属性名称获取属性值，优先通过 getter 方法：
     * - 优先尝试 getX
     * - 对布尔类型同时尝试 isX
     * - 若不存在或调用失败，返回 null（不抛异常）
     *
     * @param fieldName 属性名称
     * @param o         目标对象
     * @return 属性值，获取失败或不存在时返回 null
     */
    public static Object getFieldValueByName(String fieldName, Object o) {
        if (o == null || fieldName == null || fieldName.length() == 0) return null;
        Class<?> cls = o.getClass();
        String first = fieldName.substring(0, 1).toUpperCase();
        String suffix = fieldName.length() > 1 ? fieldName.substring(1) : "";
        String getter = "get" + first + suffix;
        String isGetter = "is" + first + suffix;

        try {
            Method m = cls.getMethod(getter);
            return m.invoke(o);
        } catch (Exception ignore) {
            // ignore and try boolean isX
        }

        try {
            Method m = cls.getMethod(isGetter);
            return m.invoke(o);
        } catch (Exception ignore) {
            // ignore and fallback
        }

        // 最后尝试直接字段访问（若字段存在且可访问）
        try {
            Field f = findField(cls, fieldName);
            if (f != null) {
                return readField(f, o);
            }
        } catch (Exception ignore) {
            // 返回 null
        }
        return null;
    }

    // ============== 私有辅助方法 ==============

    /**
     * 将任意对象按类型安全写入 ContentValues。
     * 对不支持的类型使用 toString。
     */
    private static void safePut(ContentValues values, String key, Object value) {
        if (value instanceof String) {
            values.put(key, (String) value);
        } else if (value instanceof Integer) {
            values.put(key, (Integer) value);
        } else if (value instanceof Long) {
            values.put(key, (Long) value);
        } else if (value instanceof Short) {
            values.put(key, (Short) value);
        } else if (value instanceof Byte) {
            values.put(key, (Byte) value);
        } else if (value instanceof Boolean) {
            values.put(key, (Boolean) value);
        } else if (value instanceof Float) {
            values.put(key, (Float) value);
        } else if (value instanceof Double) {
            values.put(key, (Double) value);
        } else if (value instanceof byte[]) {
            values.put(key, (byte[]) value);
        } else {
            values.put(key, String.valueOf(value));
        }
    }

    /**
     * 读取字段值，必要时设置可访问。
     */
    private static Object readField(Field field, Object target) {
        boolean accessible = field.isAccessible();
        try {
            if (!accessible) field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            Log.w(TAG, "读取字段失败: " + field.getName() + " - " + e.getMessage());
            return null;
        } finally {
            // 不强制恢复可访问性，JDK 9+ setAccessible 具备开销，但此处影响可忽略
        }
    }

    /**
     * 遍历当前类及其父类，收集所有声明字段（不包含 Object）。
     */
    private static List<Field> getAllDeclaredFields(Class<?> cls) {
        List<Field> list = new ArrayList<>();
        Class<?> cur = cls;
        while (cur != null && cur != Object.class) {
            Field[] arr = cur.getDeclaredFields();
            if (arr != null && arr.length > 0) {
                for (Field f : arr) list.add(f);
            }
            cur = cur.getSuperclass();
        }
        return list;
    }

    /**
     * 在类层级中查找指定名称字段。
     */
    private static Field findField(Class<?> cls, String name) {
        Class<?> cur = cls;
        while (cur != null && cur != Object.class) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException ignore) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }

    // ============== 常用通用扩展方法 ==============

    /**
     * 判断字符序列是否为空
     * - 为 null 或长度为 0 则为空
     */
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * 判断字符串是否为仅空白（null、长度为 0 或全部是空白字符）
     */
    public static boolean isBlank(String s) {
        if (s == null) return true;
        for (int i = 0, n = s.length(); i < n; i++) {
            if (!Character.isWhitespace(s.charAt(i))) return false;
        }
        return true;
    }

    /**
     * 判断集合是否为空
     */
    public static boolean isEmpty(java.util.Collection<?> c) {
        return c == null || c.isEmpty();
    }

    /**
     * 判断 Map 是否为空
     */
    public static boolean isEmpty(java.util.Map<?, ?> m) {
        return m == null || m.isEmpty();
    }

    /**
     * 判断任意对象数组是否为空（支持 Object[]）
     */
    public static boolean isEmpty(Object[] arr) {
        return arr == null || arr.length == 0;
    }

    /**
     * 判断原始类型数组是否为空（泛化处理）
     * - 传入任意数组对象（含原始类型数组），否则返回 true
     */
    public static boolean isArrayEmpty(Object array) {
        if (array == null || !array.getClass().isArray()) return true;
        return java.lang.reflect.Array.getLength(array) == 0;
    }

    /**
     * 若对象为 null 则返回默认值
     */
    public static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * 在一组值中返回第一个非 null 的元素，若都为 null 返回 null
     */
    @SafeVarargs
    public static <T> T coalesce(T... values) {
        if (values == null) return null;
        for (T v : values) {
            if (v != null) return v;
        }
        return null;
    }

    /**
     * 将空字符串转换为 null，其余原样返回
     */
    public static String nullIfEmpty(String s) {
        return isEmpty(s) ? null : s;
    }

    /**
     * 将 null 转换为 ""，其余调用 toString
     */
    public static String emptyIfNull(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }

    /**
     * 安全类型转换，失败返回 null
     */
    public static <T> T safeCast(Object obj, Class<T> type) {
        if (type == null) return null;
        if (obj == null) return null;
        return type.isInstance(obj) ? type.cast(obj) : null;
        }

    /**
     * 安全解析 int，解析失败返回默认值
     */
    public static int parseInt(String s, int defaultValue) {
        if (s == null) return defaultValue;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    /**
     * 安全解析 long，解析失败返回默认值
     */
    public static long parseLong(String s, long defaultValue) {
        if (s == null) return defaultValue;
        try {
            return Long.parseLong(s.trim());
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    /**
     * 安全解析 boolean（大小写不敏感，"true" 返回 true），异常或 null 返回默认值
     */
    public static boolean parseBoolean(String s, boolean defaultValue) {
        if (s == null) return defaultValue;
        String t = s.trim();
        if ("true".equalsIgnoreCase(t)) return true;
        if ("false".equalsIgnoreCase(t)) return false;
        return defaultValue;
    }

    /**
     * 将数值限定在 [min, max] 区间（int）
     */
    public static int clamp(int value, int min, int max) {
        if (min > max) { int t = min; min = max; max = t; }
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 将数值限定在 [min, max] 区间（long）
     */
    public static long clamp(long value, long min, long max) {
        if (min > max) { long t = min; min = max; max = t; }
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 将数值限定在 [min, max] 区间（double）
     */
    public static double clamp(double value, double min, double max) {
        if (min > max) { double t = min; min = max; max = t; }
        return Math.max(min, Math.min(max, value));
    }
}
