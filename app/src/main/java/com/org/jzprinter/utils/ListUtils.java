package com.org.jzprinter.utils;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * List工具类
 * 提供常用的List操作和集合处理方法
 *
 * @author RBQ 2025/10/12
 */
public class ListUtils {

    /** 默认连接分隔符 **/
    public static final String DEFAULT_JOIN_SEPARATOR = ",";

    private ListUtils() {
        throw new AssertionError();
    }

    /**
     * 获取List的大小
     *
     * <pre>
     * getSize(null)   =   0;
     * getSize({})     =   0;
     * getSize({1})    =   1;
     * </pre>
     *
     * @param <V> 元素类型
     * @param sourceList 源列表
     * @return 如果列表为null或空，返回0，否则返回{@link List#size()}
     */
    public static <V> int getSize(List<V> sourceList) {
        return sourceList == null ? 0 : sourceList.size();
    }

    /**
     * 判断List是否为空
     *
     * <pre>
     * isEmpty(null)   =   true;
     * isEmpty({})     =   true;
     * isEmpty({1})    =   false;
     * </pre>
     *
     * @param <V> 元素类型
     * @param sourceList 源列表
     * @return 如果列表为null或大小为0，返回true，否则返回false
     */
    public static <V> boolean isEmpty(List<V> sourceList) {
        return (sourceList == null || sourceList.isEmpty());
    }

    /**
     * 判断List是否不为空
     *
     * <pre>
     * isNotEmpty(null)   =   false;
     * isNotEmpty({})     =   false;
     * isNotEmpty({1})    =   true;
     * </pre>
     *
     * @param <V> 元素类型
     * @param sourceList 源列表
     * @return 如果列表不为null且大小大于0，返回true，否则返回false
     */
    public static <V> boolean isNotEmpty(List<V> sourceList) {
        return !isEmpty(sourceList);
    }

    /**
     * 比较两个ArrayList是否相等
     *
     * <pre>
     * isEquals(null, null) = true;
     * isEquals(new ArrayList<String>(), null) = false;
     * isEquals(null, new ArrayList<String>()) = false;
     * isEquals(new ArrayList<String>(), new ArrayList<String>()) = true;
     * </pre>
     *
     * @param <V> 元素类型
     * @param actual 实际列表
     * @param expected 期望列表
     * @return 如果两个列表相等返回true，否则返回false
     */
    public static <V> boolean isEquals(ArrayList<V> actual, ArrayList<V> expected) {
        if (actual == null) {
            return expected == null;
        }
        if (expected == null) {
            return false;
        }
        if (actual.size() != expected.size()) {
            return false;
        }

        for (int i = 0; i < actual.size(); i++) {
            if (!ObjectUtils.isEquals(actual.get(i), expected.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 比较两个任意List是否相等
     *
     * @param <V> 元素类型
     * @param list1 第一个列表
     * @param list2 第二个列表
     * @return 如果两个列表相等返回true，否则返回false
     */
    public static <V> boolean isEquals(List<V> list1, List<V> list2) {
        if (list1 == list2) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;
        
        return list1.containsAll(list2) && list2.containsAll(list1);
    }

    /**
     * 将List连接成字符串，使用默认分隔符","
     *
     * <pre>
     * join(null)      =   "";
     * join({})        =   "";
     * join({a,b})     =   "a,b";
     * </pre>
     *
     * @param list 字符串列表
     * @return 连接后的字符串，如果列表为空，返回空字符串
     */
    public static String join(List<String> list) {
        return join(list, DEFAULT_JOIN_SEPARATOR);
    }

    /**
     * 将List连接成字符串
     *
     * <pre>
     * join(null, '#')     =   "";
     * join({}, '#')       =   "";
     * join({a,b,c}, ' ')  =   "abc";
     * join({a,b,c}, '#')  =   "a#b#c";
     * </pre>
     *
     * @param list 字符串列表
     * @param separator 分隔符
     * @return 连接后的字符串，如果列表为空，返回空字符串
     */
    public static String join(List<String> list, char separator) {
        return join(list, new String(new char[] {separator}));
    }

    /**
     * 将List连接成字符串，如果分隔符为null，使用{@link #DEFAULT_JOIN_SEPARATOR}
     *
     * <pre>
     * join(null, "#")     =   "";
     * join({}, "#$")      =   "";
     * join({a,b,c}, null) =   "a,b,c";
     * join({a,b,c}, "")   =   "abc";
     * join({a,b,c}, "#")  =   "a#b#c";
     * join({a,b,c}, "#$") =   "a#$b#$c";
     * </pre>
     *
     * @param list 字符串列表
     * @param separator 分隔符
     * @return 连接后的字符串，如果列表为空，返回空字符串
     */
    public static String join(List<String> list, String separator) {
        return list == null ? "" : TextUtils.join(separator == null ? DEFAULT_JOIN_SEPARATOR : separator, list);
    }

    /**
     * 向列表中添加不重复的元素
     *
     * @param <V> 元素类型
     * @param sourceList 源列表
     * @param entry 要添加的元素
     * @return 如果元素已存在返回false，否则添加并返回true
     */
    public static <V> boolean addDistinctEntry(List<V> sourceList, V entry) {
        return (sourceList != null && !sourceList.contains(entry)) && sourceList.add(entry);
    }

    /**
     * 将另一个列表中的不重复元素添加到源列表中
     *
     * @param <V> 元素类型
     * @param sourceList 源列表
     * @param entryList 要添加的列表
     * @return 添加的元素数量
     */
    public static <V> int addDistinctList(List<V> sourceList, List<V> entryList) {
        if (sourceList == null || isEmpty(entryList)) {
            return 0;
        }

        int sourceCount = sourceList.size();
        for (V entry : entryList) {
            if (!sourceList.contains(entry)) {
                sourceList.add(entry);
            }
        }
        return sourceList.size() - sourceCount;
    }

    /**
     * 移除列表中的重复元素
     *
     * @param <V> 元素类型
     * @param sourceList 源列表
     * @return 被移除的元素数量
     */
    public static <V> int distinctList(List<V> sourceList) {
        if (isEmpty(sourceList)) {
            return 0;
        }

        // 使用Set去重，效率更高
        Set<V> set = new HashSet<>(sourceList);
        int originalSize = sourceList.size();
        sourceList.clear();
        sourceList.addAll(set);
        return originalSize - sourceList.size();
    }

    /**
     * 向列表中添加非空元素
     *
     * @param <V> 元素类型
     * @param sourceList 源列表
     * @param value 要添加的值
     * @return 添加成功返回true，否则返回false
     */
    public static <V> boolean addListNotNullValue(List<V> sourceList, V value) {
        return (sourceList != null && value != null) && sourceList.add(value);
    }

    /**
     * 反转列表
     *
     * @param <V> 元素类型
     * @param sourceList 源列表
     * @return 反转后的新列表
     */
    public static <V> List<V> invertList(List<V> sourceList) {
        if (isEmpty(sourceList)) {
            return sourceList;
        }

        List<V> invertList = new ArrayList<V>(sourceList.size());
        for (int i = sourceList.size() - 1; i >= 0; i--) {
            invertList.add(sourceList.get(i));
        }
        return invertList;
    }

    /**
     * 获取列表的第一个元素
     *
     * @param <V> 元素类型
     * @param list 列表
     * @return 第一个元素，如果列表为空返回null
     */
    public static <V> V getFirst(List<V> list) {
        return isEmpty(list) ? null : list.get(0);
    }

    /**
     * 获取列表的最后一个元素
     *
     * @param <V> 元素类型
     * @param list 列表
     * @return 最后一个元素，如果列表为空返回null
     */
    public static <V> V getLast(List<V> list) {
        return isEmpty(list) ? null : list.get(list.size() - 1);
    }

    /**
     * 将数组转换为ArrayList
     *
     * @param <V> 元素类型
     * @param array 数组
     * @return ArrayList
     */
    @SafeVarargs
    public static <V> ArrayList<V> newArrayList(V... array) {
        ArrayList<V> list = new ArrayList<>();
        if (array != null) {
            for (V item : array) {
                list.add(item);
            }
        }
        return list;
    }

    /**
     * 将集合转换为ArrayList
     *
     * @param <V> 元素类型
     * @param collection 集合
     * @return ArrayList
     */
    public static <V> ArrayList<V> newArrayList(Collection<V> collection) {
        return collection == null ? new ArrayList<>() : new ArrayList<>(collection);
    }

    /**
     * 安全地获取列表中的元素
     *
     * @param <V> 元素类型
     * @param list 列表
     * @param index 索引
     * @param defaultValue 默认值
     * @return 如果索引有效返回对应元素，否则返回默认值
     */
    public static <V> V getSafe(List<V> list, int index, V defaultValue) {
        if (isEmpty(list) || index < 0 || index >= list.size()) {
            return defaultValue;
        }
        return list.get(index);
    }

    /**
     * 过滤列表中的null值
     *
     * @param <V> 元素类型
     * @param list 源列表
     * @return 不包含null值的新列表
     */
    public static <V> List<V> filterNull(List<V> list) {
        if (isEmpty(list)) {
            return new ArrayList<>();
        }
        
        List<V> result = new ArrayList<>();
        for (V item : list) {
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 将列表分页
     *
     * @param <V> 元素类型
     * @param list 源列表
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 指定页的数据
     */
    public static <V> List<V> paginate(List<V> list, int page, int pageSize) {
        if (isEmpty(list) || page < 1 || pageSize < 1) {
            return new ArrayList<>();
        }
        
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, list.size());
        
        if (start >= list.size()) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(list.subList(start, end));
    }

    /**
     * 检查列表是否包含指定元素（null安全）
     *
     * @param <V> 元素类型
     * @param list 列表
     * @param element 要检查的元素
     * @return 如果包含返回true，否则返回false
     */
    public static <V> boolean contains(List<V> list, V element) {
        return list != null && list.contains(element);
    }

    /**
     * 将列表转换为Set（去重）
     *
     * @param <V> 元素类型
     * @param list 列表
     * @return Set集合
     */
    public static <V> Set<V> toSet(List<V> list) {
        return isEmpty(list) ? new HashSet<>() : new HashSet<>(list);
    }
}