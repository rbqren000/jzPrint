package com.org.jzprinter.utils;

import java.util.Calendar;

/**
 * 日期格式化工具类
 */
public class DateFormatter {
    
    /**
     * 格式化日期用于打印
     * @param year 年
     * @param month 月 (1-12)
     * @param day 日
     * @param format 格式字符串，如 "MMddyyyy", "yyyy-MM-dd", "MM-dd-yyyy"
     * @return 格式化后的字符串
     */
    public static String format(int year, int month, int day, String format) {
        if (format == null || format.isEmpty()) {
            format = "yyyy-MM-dd";
        }
        
        String result = format;
        result = result.replace("yyyy", String.format("%04d", year));
        result = result.replace("MM", String.format("%02d", month));
        result = result.replace("dd", String.format("%02d", day));
        
        return result;
    }
    
    /**
     * 从 Calendar 格式化
     */
    public static String format(Calendar calendar, String format) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;  // Calendar.MONTH 是 0-11
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return format(year, month, day, format);
    }
    
    /**
     * 获取当前日期的格式化字符串
     */
    public static String formatNow(String format) {
        Calendar calendar = Calendar.getInstance();
        return format(calendar, format);
    }
}
