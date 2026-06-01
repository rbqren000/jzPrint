package com.org.jzprinter.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 *
 * @author RBQ
 * @since 2025
 */
public class StringUtils {

    private StringUtils() {
        throw new AssertionError("工具类不允许实例化");
    }

    /**
     * 判断字符串是否为空白（null、空字符串或只包含空格）
     *
     * <pre>
     * isBlank(null) = true;
     * isBlank("") = true;
     * isBlank("  ") = true;
     * isBlank("a") = false;
     * isBlank("a ") = false;
     * isBlank(" a") = false;
     * isBlank("a b") = false;
     * </pre>
     *
     * @param str 待检查的字符串
     * @return 如果字符串为null、长度为0或只包含空格，返回true，否则返回false
     */
    public static boolean isBlank(String str) {
        return (str == null || str.trim().length() == 0);
    }

    /**
     * 判断字符串是否为空（null或长度为0）
     *
     * <pre>
     * isEmpty(null) = true;
     * isEmpty("") = true;
     * isEmpty("  ") = false;
     * </pre>
     *
     * @param str 待检查的字符串
     * @return 如果字符串为null或长度为0，返回true，否则返回false
     */
    public static boolean isEmpty(CharSequence str) {
        return (str == null || str.length() == 0);
    }

    /**
     * 判断两个字符串是否相等
     *
     * @param str1 第一个字符串
     * @param str2 第二个字符串
     * @return 如果两个字符串相等返回true，否则返回false
     */
    public static boolean isEqual(String str1, String str2) {
        if (str1 == str2) return true;
        if (str1 == null || str2 == null) return false;
        return str1.equals(str2);
    }

    /**
     * 获取字符序列的长度
     *
     * <pre>
     * length(null) = 0;
     * length("") = 0;
     * length("abc") = 3;
     * </pre>
     *
     * @param str 字符序列
     * @return 如果str为null返回0，否则返回字符序列的长度
     */
    public static int length(CharSequence str) {
        return str == null ? 0 : str.length();
    }

    /**
     * 将null对象转换为空字符串
     *
     * <pre>
     * nullStrToEmpty(null) = "";
     * nullStrToEmpty("") = "";
     * nullStrToEmpty("aa") = "aa";
     * </pre>
     *
     * @param obj 输入对象
     * @return 如果对象为null返回空字符串，否则返回对象的字符串表示
     */
    public static String nullStrToEmpty(Object obj) {
        return (obj == null ? "" : (obj instanceof String ? (String)obj : obj.toString()));
    }

    /**
     * 首字母大写
     *
     * <pre>
     * capitalizeFirstLetter(null)     =   null;
     * capitalizeFirstLetter("")       =   "";
     * capitalizeFirstLetter("2ab")    =   "2ab"
     * capitalizeFirstLetter("a")      =   "A"
     * capitalizeFirstLetter("ab")     =   "Ab"
     * capitalizeFirstLetter("Abc")    =   "Abc"
     * </pre>
     *
     * @param str 输入字符串
     * @return 首字母大写的字符串
     */
    public static String capitalizeFirstLetter(String str) {
        if (isEmpty(str)) {
            return str;
        }

        char c = str.charAt(0);
        return (!Character.isLetter(c) || Character.isUpperCase(c)) ? str : Character.toUpperCase(c) + str.substring(1);
    }

    /**
     * 将字符串进行UTF-8编码
     *
     * <pre>
     * utf8Encode(null)        =   null
     * utf8Encode("")          =   "";
     * utf8Encode("aa")        =   "aa";
     * utf8Encode("啊啊啊啊")   = "%E5%95%8A%E5%95%8A%E5%95%8A%E5%95%8A";
     * </pre>
     *
     * @param str 输入字符串
     * @return UTF-8编码后的字符串
     * @throws RuntimeException 如果编码出错
     */
    public static String utf8Encode(String str) {
        if (!isEmpty(str) && str.getBytes().length != str.length()) {
            try {
                return URLEncoder.encode(str, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8编码失败", e);
            }
        }
        return str;
    }

    /**
     * 将字符串进行UTF-8编码，如果编码出错则返回默认值
     *
     * @param str 输入字符串
     * @param defaultReturn 编码出错时的默认返回值
     * @return UTF-8编码后的字符串或默认值
     */
    public static String utf8Encode(String str, String defaultReturn) {
        if (!isEmpty(str) && str.getBytes().length != str.length()) {
            try {
                return URLEncoder.encode(str, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return defaultReturn;
            }
        }
        return str;
    }

    /**
     * 从href标签中提取内部HTML内容
     *
     * <pre>
     * getHrefInnerHtml(null)                                  = ""
     * getHrefInnerHtml("")                                    = ""
     * getHrefInnerHtml("mp3")                                 = "mp3";
     * getHrefInnerHtml("&lt;a innerHtml&lt;/a&gt;")                    = "&lt;a innerHtml&lt;/a&gt;";
     * getHrefInnerHtml("&lt;a&gt;innerHtml&lt;/a&gt;")                    = "innerHtml";
     * getHrefInnerHtml("&lt;a&lt;a&gt;innerHtml&lt;/a&gt;")                    = "innerHtml";
     * getHrefInnerHtml("&lt;a href="baidu.com"&gt;innerHtml&lt;/a&gt;")               = "innerHtml";
     * getHrefInnerHtml("&lt;a href="baidu.com" title="baidu"&gt;innerHtml&lt;/a&gt;") = "innerHtml";
     * getHrefInnerHtml("   &lt;a&gt;innerHtml&lt;/a&gt;  ")                           = "innerHtml";
     * getHrefInnerHtml("&lt;a&gt;innerHtml&lt;/a&gt;&lt;/a&gt;")                      = "innerHtml";
     * getHrefInnerHtml("jack&lt;a&gt;innerHtml&lt;/a&gt;&lt;/a&gt;")                  = "innerHtml";
     * getHrefInnerHtml("&lt;a&gt;innerHtml1&lt;/a&gt;&lt;a&gt;innerHtml2&lt;/a&gt;")        = "innerHtml2";
     * </pre>
     *
     * @param href href标签字符串
     * @return <ul>
     *         <li>如果href为null，返回空字符串</li>
     *         <li>如果不匹配正则表达式，返回原字符串</li>
     *         <li>返回匹配正则表达式的最后一个字符串</li>
     *         </ul>
     */
    public static String getHrefInnerHtml(String href) {
        if (isEmpty(href)) {
            return "";
        }

        String hrefReg = ".*<[\\s]*a[\\s]*.*>(.+?)<[\\s]*/a[\\s]*>.*";
        Pattern hrefPattern = Pattern.compile(hrefReg, Pattern.CASE_INSENSITIVE);
        Matcher hrefMatcher = hrefPattern.matcher(href);
        if (hrefMatcher.matches()) {
            return hrefMatcher.group(1);
        }
        return href;
    }

    /**
     * 将HTML转义字符转换为普通字符
     *
     * <pre>
     * htmlEscapeCharsToString(null) = null;
     * htmlEscapeCharsToString("") = "";
     * htmlEscapeCharsToString("mp3") = "mp3";
     * htmlEscapeCharsToString("mp3&lt;") = "mp3<";
     * htmlEscapeCharsToString("mp3&gt;") = "mp3\>";
     * htmlEscapeCharsToString("mp3&amp;mp4") = "mp3&mp4";
     * htmlEscapeCharsToString("mp3&quot;mp4") = "mp3\"mp4";
     * htmlEscapeCharsToString("mp3&lt;&gt;&amp;&quot;mp4") = "mp3\<\>&\"mp4";
     * </pre>
     *
     * @param source HTML转义字符串
     * @return 转换后的普通字符串
     */
    public static String htmlEscapeCharsToString(String source) {
        return StringUtils.isEmpty(source) ? source : source.replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&").replaceAll("&quot;", "\"");
    }

    /**
     * 将全角字符转换为半角字符
     *
     * <pre>
     * fullWidthToHalfWidth(null) = null;
     * fullWidthToHalfWidth("") = "";
     * fullWidthToHalfWidth(new String(new char[] {12288})) = " ";
     * fullWidthToHalfWidth("！＂＃＄％＆) = "!\"#$%&";
     * </pre>
     *
     * @param s 输入字符串
     * @return 转换后的半角字符串
     */
    public static String fullWidthToHalfWidth(String s) {
        if (isEmpty(s)) {
            return s;
        }

        char[] source = s.toCharArray();
        for (int i = 0; i < source.length; i++) {
            if (source[i] == 12288) {
                source[i] = ' ';
                // } else if (source[i] == 12290) {
                // source[i] = '.';
            } else if (source[i] >= 65281 && source[i] <= 65374) {
                source[i] = (char)(source[i] - 65248);
            } else {
                source[i] = source[i];
            }
        }
        return new String(source);
    }

    /**
     * 将半角字符转换为全角字符
     *
     * <pre>
     * halfWidthToFullWidth(null) = null;
     * halfWidthToFullWidth("") = "";
     * halfWidthToFullWidth(" ") = new String(new char[] {12288});
     * halfWidthToFullWidth("!\"#$%&) = "！＂＃＄％＆";
     * </pre>
     *
     * @param s 输入字符串
     * @return 转换后的全角字符串
     */
    public static String halfWidthToFullWidth(String s) {
        if (isEmpty(s)) {
            return s;
        }

        char[] source = s.toCharArray();
        for (int i = 0; i < source.length; i++) {
            if (source[i] == ' ') {
                source[i] = (char)12288;
                // } else if (source[i] == '.') {
                // source[i] = (char)12290;
            } else if (source[i] >= 33 && source[i] <= 126) {
                source[i] = (char)(source[i] + 65248);
            } else {
                source[i] = source[i];
            }
        }
        return new String(source);
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 需要转换的字节数组
     * @return 转换后的十六进制字符串
     */
    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1)
                sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 生成字符串的MD5哈希值
     *
     * @param str 输入字符串
     * @return MD5哈希值，如果出错则返回字符串的hashCode
     */
    public static String hashKey(String str) {
        String result = "";
        try {
            if (!TextUtils.isEmpty(str)) {
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                messageDigest.update(str.getBytes());
                result = bytesToHexString(messageDigest.digest());
                return result;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return String.valueOf(str.hashCode());
    }

    /**
     * 移除字符串开头的斜杠
     *
     * @param str 输入字符串
     * @return 移除开头斜杠后的字符串
     */
    public static String stripLeadingSlash(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        int i = 0;
        while (i < str.length() && str.charAt(i) == '/') {
            i++;
        }
        return str.substring(i);
    }
    
    /**
     * 格式化手机号，中间4位用*代替
     *
     * @param mobile 手机号
     * @return 格式化后的手机号
     */
    public static String getFormatPhone(String mobile) {
        if (isEmpty(mobile) || mobile.length() < 7) {
            return mobile;
        }
        
        char[] m = mobile.toCharArray();
        for (int i = 0; i < m.length; i++) {
            if (i > 2 && i < 7) {
                m[i] = '*';
            }
        }
        return String.valueOf(m);
    }
    
    /**
     * 字节数组转16进制字符串
     *
     * @param bytes 需要转换的byte数组
     * @return 转换后的Hex字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            String hex = Integer.toHexString(aByte & 0xFF);
            if (hex.length() < 2) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
    
    /**
     * 获取增量值（纯数字）
     * 增量i建议最大200，增加次数999（参考的汉码）
     *
     * @param content 原始内容
     * @param increment 增量值
     * @return 增加后的字符串
     */
    public static String getIncrementalValue(String content, int increment) {
        if (isEmpty(content)) {
            return content;
        }
        
        String newContent = content;
        String preContent = "";
        if (content.length() > 10) {
            preContent = newContent.substring(0, content.length() - 10);
            newContent = newContent.substring(content.length() - 10, content.length());
        }
        
        long base = 0;
        try {
            base = Long.parseLong(newContent);
        } catch (Exception e) {
            e.printStackTrace();
            return getIncrementalValueWithABC(content, increment);
        }
        
        long result = base + increment;
        String resultStr = String.valueOf(result);
        
        // 处理负数情况
        if (result < 0) {
            if (newContent.startsWith("0") && newContent.length() > 1) {
                return content; // 如果是0开头且长度大于1，则不处理
            }
        }
        
        // 如果原数是负数或者不是以0开头，直接返回结果
        if (base < 0 || !newContent.startsWith("0")) {
            return preContent + resultStr;
        }
        
        // 处理补0情况
        int length = newContent.length() - resultStr.length();
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < length; j++) {
            sb.append("0");
        }
        String value = sb.toString() + resultStr;
        
        return preContent + value;
    }
    
    /**
     * 获取增量值（包含字母的混合内容）
     *
     * @param content 原始内容
     * @param increment 增量值
     * @return 增加后的字符串
     */
    public static String getIncrementalValueWithABC(String content, int increment) {
        if (isEmpty(content)) {
            return content;
        }
        
        Map<Integer, Character> characterMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        
        // 分离数字和字符
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch >= '0' && ch <= '9') {
                sb.append(ch);
            } else {
                characterMap.put(i, ch);
            }
        }
        
        if (TextUtils.isEmpty(sb.toString())) {
            return content;
        }
        
        String value = getIncrementalValue(sb.toString(), increment);
        if (value.length() > sb.length()) {
            return content; // 如果结果长度超过原数字长度，不处理
        }
        
        List<Character> numbers = new ArrayList<>();
        for (int i = 0; i < sb.length(); i++) {
            numbers.add(value.charAt(i));
        }
        
        // 重新插入非数字字符
        for (Map.Entry<Integer, Character> entry : characterMap.entrySet()) {
            int key = entry.getKey();
            char ch = entry.getValue();
            if (key < numbers.size()) {
                numbers.add(key, ch);
            } else {
                numbers.add(ch);
            }
        }
    
        sb = new StringBuilder();
        for (int i = 0; i < numbers.size(); i++) {
            sb.append(numbers.get(i));
        }
        return sb.toString();
    }
    
    // ==================== 新增的常用方法 ====================
    
    /**
     * 判断字符串是否不为空（非null且长度大于0）
     *
     * @param str 待检查的字符串
     * @return 如果字符串不为空返回true，否则返回false
     */
    public static boolean isNotEmpty(CharSequence str) {
        return !isEmpty(str);
    }
    
    /**
     * 判断字符串是否不为空白（非null、非空字符串且不只包含空格）
     *
     * @param str 待检查的字符串
     * @return 如果字符串不为空白返回true，否则返回false
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * 字符串首字母小写
     *
     * @param str 输入字符串
     * @return 首字母小写的字符串
     */
    public static String uncapitalizeFirstLetter(String str) {
        if (isEmpty(str)) {
            return str;
        }
        
        char c = str.charAt(0);
        return (!Character.isLetter(c) || Character.isLowerCase(c)) ? str : Character.toLowerCase(c) + str.substring(1);
    }
    
    /**
     * 反转字符串
     *
     * @param str 输入字符串
     * @return 反转后的字符串
     */
    public static String reverse(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return new StringBuilder(str).reverse().toString();
    }
    
    /**
     * 将字符串按指定分隔符分割成字符串数组
     *
     * @param str 输入字符串
     * @param separator 分隔符
     * @return 分割后的字符串数组
     */
    public static String[] split(String str, String separator) {
        if (isEmpty(str)) {
            return new String[0];
        }
        
        if (separator == null) {
            return new String[]{str};
        }
        
        return str.split(Pattern.quote(separator));
    }
    
    /**
     * 将字符串数组用指定分隔符连接成字符串
     *
     * @param array 字符串数组
     * @param separator 分隔符
     * @return 连接后的字符串
     */
    public static String join(String[] array, String separator) {
        if (array == null || array.length == 0) {
            return "";
        }
        
        if (separator == null) {
            separator = "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(array[i]);
        }
        return sb.toString();
    }
    
    /**
     * 将字符串重复指定次数
     *
     * @param str 输入字符串
     * @param count 重复次数
     * @return 重复后的字符串
     */
    public static String repeat(String str, int count) {
        if (isEmpty(str) || count <= 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    /**
     * 截取字符串，从指定位置开始到末尾
     *
     * @param str 输入字符串
     * @param start 开始位置（包含）
     * @return 截取后的字符串
     */
    public static String substring(String str, int start) {
        if (str == null) {
            return "";
        }
        
        if (start < 0) {
            start = str.length() + start;
        }
        
        if (start < 0) {
            start = 0;
        }
        
        if (start > str.length()) {
            return "";
        }
        
        return str.substring(start);
    }
    
    /**
     * 截取字符串，从指定位置开始到指定位置结束
     *
     * @param str 输入字符串
     * @param start 开始位置（包含）
     * @param end 结束位置（不包含）
     * @return 截取后的字符串
     */
    public static String substring(String str, int start, int end) {
        if (str == null) {
            return "";
        }
        
        if (start < 0) {
            start = str.length() + start;
        }
        
        if (end < 0) {
            end = str.length() + end;
        }
        
        if (start > end) {
            return "";
        }
        
        if (start < 0) {
            start = 0;
        }
        
        if (end > str.length()) {
            end = str.length();
        }
        
        return str.substring(start, end);
    }
    
    /**
     * 去除字符串两端的指定字符
     *
     * @param str 输入字符串
     * @param stripChar 要去除的字符
     * @return 处理后的字符串
     */
    public static String strip(String str, char stripChar) {
        if (isEmpty(str)) {
            return str;
        }
        
        int start = 0;
        int end = str.length();
        
        while (start < end && str.charAt(start) == stripChar) {
            start++;
        }
        
        while (end > start && str.charAt(end - 1) == stripChar) {
            end--;
        }
        
        return str.substring(start, end);
    }
    
    /**
     * 去除字符串左端的指定字符
     *
     * @param str 输入字符串
     * @param stripChar 要去除的字符
     * @return 处理后的字符串
     */
    public static String stripStart(String str, char stripChar) {
        if (isEmpty(str)) {
            return str;
        }
        
        int start = 0;
        int end = str.length();
        
        while (start < end && str.charAt(start) == stripChar) {
            start++;
        }
        
        return str.substring(start, end);
    }
    
    /**
     * 去除字符串右端的指定字符
     *
     * @param str 输入字符串
     * @param stripChar 要去除的字符
     * @return 处理后的字符串
     */
    public static String stripEnd(String str, char stripChar) {
        if (isEmpty(str)) {
            return str;
        }
        
        int end = str.length();
        
        while (end > 0 && str.charAt(end - 1) == stripChar) {
            end--;
        }
        
        return str.substring(0, end);
    }
    
    /**
     * 左填充字符串
     *
     * @param str 原始字符串
     * @param size 填充后的总长度
     * @param padChar 填充字符
     * @return 填充后的字符串
     */
    public static String leftPad(String str, int size, char padChar) {
        if (str == null) {
            return null;
        }
        
        int padLength = size - str.length();
        if (padLength <= 0) {
            return str;
        }
        
        StringBuilder sb = new StringBuilder(padLength + str.length());
        for (int i = 0; i < padLength; i++) {
            sb.append(padChar);
        }
        sb.append(str);
        return sb.toString();
    }
    
    /**
     * 右填充字符串
     *
     * @param str 原始字符串
     * @param size 填充后的总长度
     * @param padChar 填充字符
     * @return 填充后的字符串
     */
    public static String rightPad(String str, int size, char padChar) {
        if (str == null) {
            return null;
        }
        
        int padLength = size - str.length();
        if (padLength <= 0) {
            return str;
        }
        
        StringBuilder sb = new StringBuilder(str.length() + padLength);
        sb.append(str);
        for (int i = 0; i < padLength; i++) {
            sb.append(padChar);
        }
        return sb.toString();
    }
    
    /**
     * 计算字符串中指定子字符串出现的次数
     *
     * @param str 原始字符串
     * @param sub 要查找的子字符串
     * @return 出现的次数
     */
    public static int countMatches(String str, String sub) {
        if (isEmpty(str) || isEmpty(sub)) {
            return 0;
        }
        
        int count = 0;
        int index = 0;
        
        while ((index = str.indexOf(sub, index)) != -1) {
            count++;
            index += sub.length();
        }
        
        return count;
    }
    
    /**
     * 判断字符串是否只包含字母
     *
     * @param str 输入字符串
     * @return 如果只包含字母返回true，否则返回false
     */
    public static boolean isAlpha(String str) {
        if (isEmpty(str)) {
            return false;
        }
        
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isLetter(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 判断字符串是否只包含数字
     *
     * @param str 输入字符串
     * @return 如果只包含数字返回true，否则返回false
     */
    public static boolean isNumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 判断字符串是否只包含字母和数字
     *
     * @param str 输入字符串
     * @return 如果只包含字母和数字返回true，否则返回false
     */
    public static boolean isAlphanumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isLetterOrDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 判断字符串是否只包含空白字符
     *
     * @param str 输入字符串
     * @return 如果只包含空白字符返回true，否则返回false
     */
    public static boolean isWhitespace(String str) {
        if (isEmpty(str)) {
            return false;
        }
        
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 判断字符串是否全部为大写
     *
     * @param str 输入字符串
     * @return 如果全部为大写返回true，否则返回false
     */
    public static boolean isAllUpperCase(String str) {
        if (isEmpty(str)) {
            return false;
        }
        
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isUpperCase(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 判断字符串是否全部为小写
     *
     * @param str 输入字符串
     * @return 如果全部为小写返回true，否则返回false
     */
    public static boolean isAllLowerCase(String str) {
        if (isEmpty(str)) {
            return false;
        }
        
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isLowerCase(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 将字符串转换为大写
     *
     * @param str 输入字符串
     * @return 转换为大写的字符串
     */
    public static String upperCase(String str) {
        if (str == null) {
            return null;
        }
        return str.toUpperCase();
    }
    
    /**
     * 将字符串转换为小写
     *
     * @param str 输入字符串
     * @return 转换为小写的字符串
     */
    public static String lowerCase(String str) {
        if (str == null) {
            return null;
        }
        return str.toLowerCase();
    }
    
    /**
     * 将字符串首字母大写，其他字母小写
     *
     * @param str 输入字符串
     * @return 转换后的字符串
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    /**
     * 安全地将字符串转换为整数
     *
     * @param str 输入字符串
     * @param defaultValue 转换失败时的默认值
     * @return 转换后的整数或默认值
     */
    public static int toInt(String str, int defaultValue) {
        if (isEmpty(str)) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 安全地将字符串转换为长整数
     *
     * @param str 输入字符串
     * @param defaultValue 转换失败时的默认值
     * @return 转换后的长整数或默认值
     */
    public static long toLong(String str, long defaultValue) {
        if (isEmpty(str)) {
            return defaultValue;
        }
        
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 安全地将字符串转换为浮点数
     *
     * @param str 输入字符串
     * @param defaultValue 转换失败时的默认值
     * @return 转换后的浮点数或默认值
     */
    public static float toFloat(String str, float defaultValue) {
        if (isEmpty(str)) {
            return defaultValue;
        }
        
        try {
            return Float.parseFloat(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 安全地将字符串转换为双精度浮点数
     *
     * @param str 输入字符串
     * @param defaultValue 转换失败时的默认值
     * @return 转换后的双精度浮点数或默认值
     */
    public static double toDouble(String str, double defaultValue) {
        if (isEmpty(str)) {
            return defaultValue;
        }
        
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 安全地将字符串转换为布尔值
     *
     * @param str 输入字符串
     * @param defaultValue 转换失败时的默认值
     * @return 转换后的布尔值或默认值
     */
    public static boolean toBoolean(String str, boolean defaultValue) {
        if (isEmpty(str)) {
            return defaultValue;
        }
        
        try {
            return Boolean.parseBoolean(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * 生成指定长度的随机字符串
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String randomString(int length) {
        if (length <= 0) {
            return "";
        }
        
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        
        return sb.toString();
    }
    
    /**
     * 生成指定长度的随机数字字符串
     *
     * @param length 字符串长度
     * @return 随机数字字符串
     */
    public static String randomNumeric(int length) {
        if (length <= 0) {
            return "";
        }
        
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        
        return sb.toString();
    }
    
    /**
     * 生成指定长度的随机字母字符串
     *
     * @param length 字符串长度
     * @return 随机字母字符串
     */
    public static String randomAlphabetic(int length) {
        if (length <= 0) {
            return "";
        }
        
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        
        return sb.toString();
    }
    
    /**
     * 生成指定长度的随机字母数字字符串
     *
     * @param length 字符串长度
     * @return 随机字母数字字符串
     */
    public static String randomAlphanumeric(int length) {
        if (length <= 0) {
            return "";
        }
        
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        
        return sb.toString();
    }
}
