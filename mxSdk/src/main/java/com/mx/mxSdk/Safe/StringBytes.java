package com.mx.mxSdk.Safe;

import android.util.Base64;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

//这是一个用Java封装的Android上的常用的字符串到byte及byte到字符串的编码方式的工具类
public class StringBytes {

    //其他的编码方式
    public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
    public static final Charset BIG5 = Charset.forName("BIG5");
    public static final Charset UTF_16 = StandardCharsets.UTF_16;

    //支持中文的编码方式
    public static final Charset GB2312 = Charset.forName("GB2312");
    public static final Charset GB18030 = Charset.forName("GB18030");
    public static final Charset GBK = Charset.forName("GBK");
    public static final Charset UTF_8 = StandardCharsets.UTF_8;
    public static final Charset ASCII = StandardCharsets.US_ASCII;


    //定义Base64的标志常量，参考android.util.Base64类
    public static final int DEFAULT = Base64.DEFAULT;
    public static final int NO_PADDING = Base64.NO_PADDING;
    public static final int NO_WRAP = Base64.NO_WRAP;
    public static final int CRLF = Base64.CRLF;
    public static final int URL_SAFE = Base64.URL_SAFE;
    public static final int NO_CLOSE = Base64.NO_CLOSE;

    // 十六进制字符数组
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    // 十六进制字符对应的数字映射表
    private static final int[] HEX_VALUES = new int[128];
    static {
        for (int i = 0; i < 10; i++) {
            HEX_VALUES['0' + i] = i;
        }
        for (int i = 0; i < 6; i++) {
            HEX_VALUES['A' + i] = 10 + i;
            HEX_VALUES['a' + i] = 10 + i;
        }
    }

    // 定义一个枚举类型，用于表示不同的进制
    public enum Radix {

        // 定义常用进制的枚举常量，包括进制值和名称
        BINARY(2, "Binary", "0b"),
        OCTAL(8, "Octal", "0o"),
        DECIMAL(10, "Decimal", ""),
        HEXADECIMAL(16, "Hexadecimal", "0x");

        // 定义枚举的属性，包括进制值、名称和前缀
        private final int value;
        private final String name;
        private final String prefix;

        // 定义枚举的构造方法，初始化属性
        private Radix(int value, String name, String prefix) {
            this.value = value;
            this.name = name;
            this.prefix = prefix;
        }

        // 定义枚举的公共方法，获取属性值
        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }

        public String getPrefix() {
            return prefix;
        }

        // 定义枚举的静态方法，根据进制值获取对应的枚举常量
        public static Radix of(int value) {
            // 遍历所有的枚举常量，比较进制值是否相等，如果相等则返回该枚举常量
            for (Radix radix : Radix.values()) {
                if (radix.value == value) {
                    return radix;
                }
            }
            //没找到，则返回默认的10进制
            return DECIMAL;
        }
    }

    public static byte[] stringToBytes(String input) {
        // 检查输入参数是否为空，如果为空，返回null
        if (input == null) {
            return null;
        }
        // 调用String类的getBytes方法，将字符串转换成byte数组，使用指定的字符集
        // 返回byte数组
        return input.getBytes();
    }

    /**
     * 将一个字符串转换成一个byte数组，使用指定的编码类型
     * @param input 要转换的字符串
     * @param charset 要使用的编码类型
     * @return 字符串对应的byte数组，如果转换失败，返回null
     */
    public static byte[] stringToBytes(String input, Charset charset) {
        // 检查输入参数是否为空，如果为空，返回null
        if (input == null || charset == null) {
            return null;
        }
        // 调用String类的getBytes方法，将字符串转换成byte数组，使用指定的字符集
        // 返回byte数组
        return input.getBytes(charset);
    }

    public static String bytesToString(byte[] input) {
        // 检查输入参数是否为空，如果为空，返回null
        if (input == null) {
            return null;
        }
        // 调用String类的构造方法，将byte数组转换成字符串，使用指定的字符集
        // 返回字符串
        return new String(input);
    }

    /**
     * 将一个byte数组转换成一个字符串，使用指定的编码类型
     * @param input 要转换的byte数组
     * @param charset 要使用的编码类型
     * @return byte数组对应的字符串，如果转换失败，返回null
     */
    public static String bytesToString(byte[] input, Charset charset) {
        // 检查输入参数是否为空，如果为空，返回null
        if (input == null || charset == null) {
            return null;
        }
        // 调用String类的构造方法，将byte数组转换成字符串，使用指定的字符集
        // 返回字符串
        return new String(input, charset);
    }

    /**
     * 将一个byte数组转换成一个Base64编码的字符串
     * @param input 要转换的byte数组
     * @return byte数组对应的Base64编码的字符串，如果转换失败，返回null
     */
    public static String bytesToBase64String(byte[] input, int flag) {
        // 检查输入参数是否为空，如果为空，返回null
        if (input == null) {
            return null;
        }
        return Base64.encodeToString(input, flag);
    }

    public static String bytesToBase64String(byte[] input) {
        // 检查输入参数是否为空，如果为空，返回null
        if (input == null) {
            return null;
        }
        return Base64.encodeToString(input, DEFAULT);
    }

    /**
     * 将一个Base64编码的字符串转换成一个byte数组
     * @param input 要转换的Base64编码的字符串
     * @return Base64编码的字符串对应的byte数组，如果转换失败，返回null
     */
    public static byte[] base64StringToBytes(String input, int flag) {
        // 检查输入参数是否为空，如果为空，返回null
        if (input == null) {
            return null;
        }
        return Base64.decode(input, flag);
    }
    public static byte[] base64StringToBytes(String input) {
        // 检查输入参数是否为空，如果为空，返回null
        if (input == null) {
            return null;
        }
        return Base64.decode(input, Base64.DEFAULT);
    }

    public static String toString(byte[] bytes) {
        return toString(bytes,Radix.HEXADECIMAL,ByteOrder.nativeOrder());
    }

    public static String toString(byte[] bytes, Radix radix) {
        return toString(bytes,radix,ByteOrder.nativeOrder());
    }

    // 定义公共的静态方法，将字节数组转成字符串，需要传入进制和大小端参数
    //将一个byte数组转化为不同进制的字符串
    public static String toString(byte[] bytes, Radix radix, ByteOrder order) {
        //检查输入参数的合法性
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        //根据不同的字节顺序，将字节数组转为BigInteger
        BigInteger num;
        if (order == ByteOrder.BIG_ENDIAN) {
            num = new BigInteger(bytes);
        } else {
            //反转字节数组的顺序
            byte[] reversedBytes = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                reversedBytes[i] = bytes[bytes.length - 1 - i];
            }
            num = new BigInteger(reversedBytes);
        }
        //根据不同的进制，将BigInteger转为字符串
        String output = num.toString(radix.getValue());
        //根据不同的进制，补充输出字符串中的前导零
        switch (radix) {
            case BINARY:
                output = String.format("%" + bytes.length * 8 + "s", output).replace(' ', '0');
                break;
            case OCTAL:
            case DECIMAL:
                output = String.format("%" + bytes.length * 3 + "s", output).replace(' ', '0');
                break;
            case HEXADECIMAL:
                output = String.format("%" + bytes.length * 2 + "s", output).replace(' ', '0');
                break;
        }
        return output;
    }

    /**
     * 该方法只能转字符串为数
     * @param input
     * @return
     */
    public static byte[] toBytes(String input) {
        return toBytes(input,Radix.HEXADECIMAL,ByteOrder.nativeOrder());
    }

    public static byte[] toBytes(String input, Radix radix) {
        return toBytes(input,radix,ByteOrder.nativeOrder());
    }

    // 定义公共的静态方法，将字符串转成字节数组，需要传入进制和大小端参数
    public static byte[] toBytes(String input, Radix radix, ByteOrder order) {
        //检查输入参数的合法性
        if (input == null || input.isEmpty()) {
            return null;
        }
        //去除输入字符串中的前导零
        input = input.replaceFirst("^0+(?!$)", "");
        //根据不同的进制，将字符串转为BigInteger
        BigInteger num = new BigInteger(input, radix.getValue());
        //根据字符串的长度和进制，计算字节数组的长度
        int bitLength = num.bitLength();
        int byteLength = (bitLength + 7) / 8;
        //根据不同的字节顺序，将BigInteger转为字节数组
        byte[] bytes = num.toByteArray();
        if (order == ByteOrder.BIG_ENDIAN) {
            //如果字节数组长度小于计算的长度，说明有前导零被省略了，需要补充
            if (bytes.length < byteLength) {
                byte[] paddedBytes = new byte[byteLength];
                System.arraycopy(bytes, 0, paddedBytes, byteLength - bytes.length, bytes.length);
                bytes = paddedBytes;
            }
            //如果字节数组长度大于计算的长度，说明有符号位被多余地添加了，需要去除
            if (bytes.length > byteLength) {
                byte[] trimmedBytes = new byte[byteLength];
                System.arraycopy(bytes, bytes.length - byteLength, trimmedBytes, 0, byteLength);
                bytes = trimmedBytes;
            }
        } else {
            //如果字节数组长度小于计算的长度，说明有前导零被省略了，需要补充
            if (bytes.length < byteLength) {
                byte[] paddedBytes = new byte[byteLength];
                System.arraycopy(bytes, 0, paddedBytes, 0, bytes.length);
                bytes = paddedBytes;
            }
            //如果字节数组长度大于计算的长度，说明有符号位被多余地添加了，需要去除
            if (bytes.length > byteLength) {
                byte[] trimmedBytes = new byte[byteLength];
                System.arraycopy(bytes, 0, trimmedBytes, 0, byteLength);
                bytes = trimmedBytes;
            }
            //反转字节数组的顺序
            for (int i = 0; i < bytes.length / 2; i++) {
                byte temp = bytes[i];
                bytes[i] = bytes[bytes.length - 1 - i];
                bytes[bytes.length - 1 - i] = temp;
            }
        }
        //返回字节数组
        return bytes;
    }

    public static String convert(String input, Radix from, Radix to) {
        return convert(input,from,to,ByteOrder.nativeOrder());
    }

    // 定义公共的静态方法，将任意进制的字符串或字节数组转成另一个进制的字符串或字节数组，需要传入源进制、目标进制和大小端参数
    public static String convert(String input, Radix from, Radix to, ByteOrder order) {
        // 检查输入是否为空或无效
        if (input == null || input.length() == 0 || from==null || to==null || order == null) {
            return "";
        }
        // 先将输入字符串转成字节数组，再将字节数组转成目标进制的字符串，并返回
        return toString(toBytes(input, from, order), to, order);
    }

    public static byte[] convert(byte[] input, Radix from, Radix to){
        return convert(input,from,to,ByteOrder.nativeOrder());
    }

    public static byte[] convert(byte[] input, Radix from, Radix to, ByteOrder order) {
        // 检查输入是否为空或无效
        if (input == null || input.length == 0 || from==null || to==null || order == null) {
            return new byte[0];
        }
        // 先将输入字节数组转成字符串，再将字符串转成目标进制的字节数组，并返回
        return toBytes(toString(input, from, order), to, order);
    }

    // 对输入字符串进行预处理
    public static String preprocess(String input, Radix radix) {
        // 去除空格或其他分隔符
        input = input.replaceAll("\\s+", "");
        // 统一大小写
        input = input.toUpperCase();
        // 去除进制前缀或后缀
        input = input.replaceFirst("^(0b|0o|0x)", "");
        // 检查无效字符，并抛出异常或返回null
        for (char c : input.toCharArray()) {
            if (Character.digit(c, radix.value) == -1) {
                return null;
            }
        }
        // 如果字符串长度为奇数，补充一个0在前面或后面，根据进制不同而定
        if (input.length() % 2 != 0) {
            if (radix.value <= 10) {
                input = "0" + input;
            } else {
                input = input + "0";
            }
        }
        // 返回预处理后的字符串
        return input;
    }

    //对输入字节数组进行预处理
    public static byte[] preprocess(byte[] bytes, Radix radix) {
        // 去除多余的0，保证每个字节都在有效范围内，并抛出异常或返回null
        int start = 0;
        int end = bytes.length - 1;
        int mask = (1 << radix.value) - 1;
        while (start < end && bytes[start] == 0) {
            start++;
        }
        while (start < end && bytes[end] == 0) {
            end--;
        }
        for (int i = start; i <= end; i++) {
            if (bytes[i] < 0 || bytes[i] > mask) {
                return null;
            }
        }
        // 如果字节数组长度为奇数，补充一个0在前面或后面，根据进制不同而定
        if ((end - start + 1) % 2 != 0) {
            if (radix.value <= 10) {
                start--;
                bytes[start] = 0;
            } else {
                end++;
                bytes[end] = 0;
            }
        }
        // 返回预处理后的字节数组
        return Arrays.copyOfRange(bytes, start, end + 1);
    }

    // 定义一个私有的静态方法，反转字节数组
    private static byte[] reverse(byte[] bytes) {
        // 创建一个新的字节数组，长度与原数组相同
        byte[] reversed = new byte[bytes.length];
        // 遍历原数组的每个字节，从后往前复制到新数组中
        for (int i = 0; i < bytes.length; i++) {
            reversed[i] = bytes[bytes.length - 1 - i];
        }
        // 返回反转后的字节数组
        return reversed;
    }


    /**
     * 将字符串转成byte字符串，指定编码类型和进制
     * @param str 要转换的字符串
     * @param charset 编码类型
     * @param radix 进制，范围是2到36
     * @return 转换后的byte字符串，如果发生异常则返回null
     */
    public static String stringToByteString(String str, Charset charset, Radix radix) {
        try {
            // 检查参数是否为空
            if (str == null || charset == null||radix==null) {
                return "";
            }
            // 将字符串转成字节数组
            byte[] bytes = str.getBytes(charset);
            // 将字节数组转成指定进制表示的字符串
            BigInteger bi = new BigInteger(1, bytes);
            return bi.toString(radix.value);
        } catch (IllegalArgumentException e) {

            e.printStackTrace();
            return "";
        }
    }

    /**
     * 将byte字符串转为原字符串，指定编码类型和进制
     * @param byteStr 要转换的byte字符串
     * @param charset 编码类型
     * @param radix 进制，范围是2到36
     * @return 转换后的原字符串，如果发生异常则返回null
     */
    public static String byteStringToString(String byteStr, Charset charset, Radix radix) {
        try {
            // 检查参数是否为空
            if (byteStr == null || charset == null|| radix==null) {
                return "";
            }
            // 将指定进制表示的字符串转成字节数组
            BigInteger bi = new BigInteger(byteStr, radix.value);
            byte[] bytes = bi.toByteArray();
            // 找到第一个不是0的字节的位置
            int offset = 0;
            while (offset < bytes.length && bytes[offset] == 0) {
                offset++;
            }
            int length = bytes.length - offset;
            // 将字节数组转成字符串
            return new String(bytes, offset, length, charset);
        } catch (IllegalArgumentException e) {

            e.printStackTrace();
            return "";
        }
    }

    /**
     * 将字符串转成byte字符串，使用默认的UTF-8编码类型和16进制
     * @param str 要转换的字符串
     * @return 转换后的byte字符串，如果发生异常则返回null
     */
    public static String stringToByteString(String str) {
        return stringToByteString(str, UTF_8, Radix.HEXADECIMAL);
    }

    /**
     * 将byte字符串转为原字符串，使用默认的UTF-8编码类型和16进制
     * @param byteStr 要转换的byte字符串
     * @return 转换后的原字符串，如果发生异常则返回null
     */
    public static String byteStringToString(String byteStr) {
        return byteStringToString(byteStr, UTF_8, Radix.HEXADECIMAL);
    }

    // 字节数组转16进制字符串
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[(b >> 4) & 0x0F]);
            sb.append(HEX_CHARS[b & 0x0F]);
        }
        return sb.toString();
    }

    // 16进制字符串转字节数组
    public static byte[] hexToBytes(String hex) {
        if (hex == null||hex.length() % 2 != 0) {
            return null;
        }
        int len = hex.length() / 2;
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            char high = hex.charAt(i * 2);
            char low = hex.charAt(i * 2 + 1);
            if (high >= HEX_VALUES.length || low >= HEX_VALUES.length) {
                throw new IllegalArgumentException("hex contains invalid characters");
            }
            data[i] = (byte) ((HEX_VALUES[high] << 4) + HEX_VALUES[low]);
        }
        return data;
    }

}
