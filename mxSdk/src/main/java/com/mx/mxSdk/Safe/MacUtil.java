package com.mx.mxSdk.Safe;

import android.util.Base64;

public class MacUtil {

    //定义Base64的标志常量，参考android.util.Base64类
    public static final int DEFAULT = Base64.DEFAULT;
    public static final int NO_PADDING = Base64.NO_PADDING;
    public static final int NO_WRAP = Base64.NO_WRAP;
    public static final int CRLF = Base64.CRLF;
    public static final int URL_SAFE = Base64.URL_SAFE;
    public static final int NO_CLOSE = Base64.NO_CLOSE;

    /**
     * 将mac地址转换为字符串
     *
     * @param mac mac地址的字节数组，不能为空，长度必须为6
     * @return mac地址的字符串表示，用冒号分隔，如"00:11:22:33:44:55"
     * @throws IllegalArgumentException 如果mac地址为空或长度不为6
     */
    public static String macToString(byte[] mac) {
        if (mac == null || mac.length != 6) {
            throw new IllegalArgumentException("Invalid mac address");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            if (i != 0) {
                sb.append(":"); //改成冒号
            }
            //字节转换为整数
            int temp = mac[i] & 0xff;
            String str = Integer.toHexString(temp);
            if (str.length() == 1) {
                sb.append("0").append(str);
            } else {
                sb.append(str);
            }
        }
        return sb.toString();
    }

    /**
     * 将字符串转换为mac地址
     *
     * @param str mac地址的字符串表示，不能为空，用冒号分隔，如"00:11:22:33:44:55"
     * @return mac地址的字节数组，长度为6
     * @throws IllegalArgumentException 如果字符串为空或格式不正确
     */
    public static byte[] stringToMac(String str) {
        if (str == null || !str.matches("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")) {
            throw new IllegalArgumentException("Invalid mac address string");
        }
        String[] hex = str.split(":"); //改成冒号
        byte[] mac = new byte[6];
        for (int i = 0; i < 6; i++) {
            //16进制转换为字节
            mac[i] = (byte) Integer.parseInt(hex[i], 16);
        }
        return mac;
    }

    public static String encodeMac(byte[] mac) {
        return encodeMac(mac,Base64.DEFAULT);
    }

    /**
     * 将mac地址编码为固定的字符串
     *
     * @param mac  mac地址的字节数组，不能为空，长度必须为6
     * @param flag Base64的标志，可以是DEFAULT, NO_PADDING, NO_WRAP, CRLF, URL_SAFE或NO_CLOSE
     * @return 编码后的字符串，不为空
     * @throws IllegalArgumentException 如果mac地址为空或长度不为6，或者flag不合法
     */
    public static String encodeMac(byte[] mac, int flag) {
        if (mac == null || mac.length != 6) {
            throw new IllegalArgumentException("Invalid mac address");
        }
        if ((flag & ~(DEFAULT | NO_PADDING | NO_WRAP | CRLF | URL_SAFE | NO_CLOSE)) != 0) {
            throw new IllegalArgumentException("Invalid Base64 flag");
        }
        //使用Android的Base64编码
        return Base64.encodeToString(mac, flag);
    }

    public static byte[] decodeMac(String str){
        return decodeMac(str,Base64.DEFAULT);
    }
    /**
     * 将固定的字符串解码为mac地址
     *
     * @param str  编码后的字符串，不能为空
     * @param flag Base64的标志，可以是DEFAULT, NO_PADDING, NO_WRAP, CRLF, URL_SAFE或NO_CLOSE
     * @return mac地址的字节数组，长度为6
     * @throws IllegalArgumentException 如果字符串为空或编码不正确，或者flag不合法
     */
    public static byte[] decodeMac(String str, int flag) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("Invalid encoded string");
        }
        if ((flag & ~(DEFAULT | NO_PADDING | NO_WRAP | CRLF | URL_SAFE | NO_CLOSE)) != 0) {
            throw new IllegalArgumentException("Invalid Base64 flag");
        }
        //使用Android的Base64解码
        return Base64.decode(str, flag);
    }
}
