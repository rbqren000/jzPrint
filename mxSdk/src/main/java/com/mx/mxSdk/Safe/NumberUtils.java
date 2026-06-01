package com.mx.mxSdk.Safe;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NumberUtils {

    public static byte[] toHexBytes(Object num) {
        return toHexBytes(num,ByteOrder.nativeOrder());
    }

    // 将数字转为16进制byte数组，支持所有数据类型的转换，支持大小端
    public static byte[] toHexBytes(Object num, ByteOrder byteOrder) {
        // 检查输入参数是否为空
        if (num == null) {
            throw new NullPointerException("num cannot be null");
        }
        // 检查输入参数是否为数字类型
        if (!(num instanceof Number)) {
            throw new IllegalArgumentException("num must be a Number");
        }
        ByteBuffer buffer;
        if (num instanceof Byte) {
            buffer = ByteBuffer.allocate(1);
            buffer.put((Byte) num);
        } else if (num instanceof Short) {
            buffer = ByteBuffer.allocate(2);
            buffer.putShort((Short) num);
        } else if (num instanceof Integer) {
            buffer = ByteBuffer.allocate(4);
            buffer.putInt((Integer) num);
        } else if (num instanceof Long) {
            buffer = ByteBuffer.allocate(8);
            buffer.putLong((Long) num);
        } else if (num instanceof Float) {
            buffer = ByteBuffer.allocate(4);
            buffer.putFloat((Float) num);
        } else if (num instanceof Double) {
            buffer = ByteBuffer.allocate(8);
            buffer.putDouble((Double) num);
        } else {
            throw new IllegalArgumentException("Unsupported data type: " + num.getClass());
        }
        // 根据大小端设置字节顺序
        buffer.order(byteOrder);
        return buffer.array();
    }

    public static Object fromHexBytes(byte[] bytes, Class<?> type) {
        return fromHexBytes(bytes,type,ByteOrder.nativeOrder());
    }
    // 将16进制byte数组转为数字，支持所有数据类型的转换，支持大小端
    public static Object fromHexBytes(byte[] bytes, Class<?> type, ByteOrder byteOrder) {
        // 检查输入参数是否为空
        if (bytes == null || type == null) {
            throw new NullPointerException("bytes and type cannot be null");
        }
        // 检查输入参数是否为数字类型
        if (!Number.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("type must be a subclass of Number");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        // 根据大小端设置字节顺序
        buffer.order(byteOrder);
        // 使用if-else语句来替换switch语句，因为Java 7不支持在switch中使用字符串
        if (type == Byte.class) {
            return type.cast(buffer.get());
        } else if (type == Short.class) {
            return type.cast(buffer.getShort());
        } else if (type == Integer.class) {
            return type.cast(buffer.getInt());
        } else if (type == Long.class) {
            return type.cast(buffer.getLong());
        } else if (type == Float.class) {
            return type.cast(buffer.getFloat());
        } else if (type == Double.class) {
            return type.cast(buffer.getDouble());
        } else {
            throw new IllegalArgumentException("Unsupported data type: " + type);
        }
    }

    // 将16进制字符串转为byte数组
    public static byte[] hexStringToBytes(String hexString) {
        // 检查输入参数是否为空
        if (hexString == null) {
            throw new NullPointerException("hexString cannot be null");
        }
        // 去除空格并转为大写
        hexString = hexString.replaceAll("\\s+", "").toUpperCase();
        // 检查输入参数是否为合法的16进制字符串
        if (!hexString.matches("[0-9A-F]+")) {
            throw new IllegalArgumentException("hexString must be a valid hexadecimal string");
        }
        // 如果长度为奇数，则在前面补0
        if (hexString.length() % 2 != 0) {
            hexString = "0" + hexString;
        }
        int len = hexString.length() / 2;
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            // 每两个字符转为一个字节
            bytes[i] = (byte) (Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16));
        }
        return bytes;
    }

    // 将byte数组转为16进制字符串
    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF).toUpperCase();
            if (hex.length() == 1) {
                sb.append("0");
            }
            sb.append(hex).append(" ");
        }
        return sb.toString().trim();
    }

    // 将byte数组转为二进制字符串
    public static String bytesToBinaryString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            StringBuilder bin = new StringBuilder(Integer.toBinaryString(b & 0xFF));
            // 补齐8位
            while (bin.length() < 8) {
                bin.insert(0, "0");
            }
            sb.append(bin).append(" ");
        }
        return sb.toString().trim();
    }

    // 将二进制字符串转为byte数组
    public static byte[] binaryStringToBytes(String binaryString) {
        // 检查输入参数是否为空
        if (binaryString == null) {
            throw new NullPointerException("binaryString cannot be null");
        }
        // 去除空格
        binaryString = binaryString.replaceAll("\\s+", "");
        // 检查输入参数是否为合法的二进制字符串
        if (!binaryString.matches("[01]+")) {
            throw new IllegalArgumentException("binaryString must be a valid binary string");
        }
        // 如果长度不是8的倍数，则在前面补0
        StringBuilder binaryStringBuilder = new StringBuilder(binaryString);
        while (binaryStringBuilder.length() % 8 != 0) {
            binaryStringBuilder.insert(0, "0");
        }
        binaryString = binaryStringBuilder.toString();
        int len = binaryString.length() / 8;
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            // 每八个字符转为一个字节
            bytes[i] = (byte) (Integer.parseInt(binaryString.substring(i * 8, i * 8 + 8), 2));
        }
        return bytes;
    }

    // 将byte数组转为十进制字符串
    public static String bytesToDecimalString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(b & 0xFF).append(" ");
        }
        return sb.toString().trim();
    }

    // 将十进制字符串转为byte数组
    public static byte[] decimalStringToBytes(String decimalString) {
        // 检查输入参数是否为空
        if (decimalString == null) {
            throw new NullPointerException("decimalString cannot be null");
        }

        // 去除空格
        decimalString = decimalString.replaceAll("\\s+", "");

        // 检查输入参数是否为合法的十进制字符串
        if (!decimalString.matches("[0-9]+")) {
            throw new IllegalArgumentException("decimalString must be a valid decimal string");
        }
        String[] nums = decimalString.split(" ");
        int len = nums.length;
        byte[] bytes = new byte[len];

        for (int i = 0; i < len; i++) {
            // 每个数字转为一个字节
            bytes[i] = (byte) (Integer.parseInt(nums[i]));
        }
        return bytes;
    }

    // 将byte数组转为其他进制的字符串，支持2到36进制
    public static String bytesToRadixString(byte[] bytes, int radix) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        // 检查进制是否在合法范围内
        if (radix < 2 || radix > 36) {
            throw new IllegalArgumentException("radix must be between 2 and 36");
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Integer.toString(b & 0xFF, radix)).append(" ");
        }
        return sb.toString().trim();
    }

    // 将其他进制的字符串转为byte数组，支持2到36进制
    public static byte[] radixStringToBytes(String radixString, int radix) {
        // 检查输入参数是否为空
        if (radixString == null) {
            throw new NullPointerException("radixString cannot be null");
        }
        // 检查进制是否在合法范围内
        if (radix < 2 || radix > 36) {
            throw new IllegalArgumentException("radix must be between 2 and 36");
        }
        // 去除空格
        radixString = radixString.replaceAll("\\s+", "");

        // 检查输入参数是否为合法的进制字符串
        if (!radixString.matches("[0-9A-Z]+")) {
            throw new IllegalArgumentException("radixString must be a valid radix string");
        }
        String[] nums = radixString.split(" ");
        int len = nums.length;
        byte[] bytes = new byte[len];

        for (int i = 0; i < len; i++) {
            // 每个数字转为一个字节
            bytes[i] = (byte) (Integer.parseInt(nums[i], radix));
        }
        return bytes;
    }

    // 将byte数组转为其他数据类型的字符串，支持char和boolean类型
    public static String bytesToTypeString(byte[] bytes, Class<?> type) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            if (type == Character.class) {
                sb.append((char) (b & 0xFF)).append(" ");
            } else if (type == Boolean.class) {
                sb.append(b != 0).append(" ");
            } else {
                throw new IllegalArgumentException("Unsupported data type: " + type);
            }
        }
        return sb.toString().trim();
    }

    // 将其他数据类型的字符串转为byte数组，支持char和boolean类型
    public static byte[] typeStringToBytes(String typeString, Class<?> type) {
        // 检查输入参数是否为空
        if (typeString == null) {
            throw new NullPointerException("typeString cannot be null");
        }
        // 去除空格
        typeString = typeString.replaceAll("\\s+", "");

        int len = typeString.length();
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            if (type == Character.class) {
                bytes[i] = (byte) (typeString.charAt(i));
            } else if (type == Boolean.class) {
                bytes[i] = (byte) (Boolean.parseBoolean(typeString.substring(i, i + 1)) ? 1 : 0);
            } else {
                throw new IllegalArgumentException("Unsupported data type: " + type);
            }
        }
        return bytes;
    }

    // 使用工厂方法来创建ByteBuffer，根据不同的数据类型返回不同的大小的buffer
    private static ByteBuffer createBuffer(Object num) {
        int size = 0;
        if (num instanceof Byte) {
            size = 1;
        } else if (num instanceof Short) {
            size = 2;
        } else if (num instanceof Integer || num instanceof Float) {
            size = 4;
        } else if (num instanceof Long || num instanceof Double) {
            size = 8;
        } else {
            throw new IllegalArgumentException("Unsupported data type: " + num.getClass());
        }
        return ByteBuffer.allocate(size);
    }

    /**
     int num = 1234567890;

     byte[] bytes = toHexBytes(num, ByteOrder.BIG_ENDIAN); // 大端
     System.out.println(bytesToHexString(bytes)); // 49 96 02 D2
     int num2 = (Integer) fromHexBytes(bytes, Integer.class, ByteOrder.BIG_ENDIAN); // 大端
     System.out.println(num2); // 1234567890

     bytes = toHexBytes(num, ByteOrder.LITTLE_ENDIAN); // 小端
     System.out.println(bytesToHexString(bytes)); // D2 02 96 49
     num2 = (Integer) fromHexBytes(bytes, Integer.class, ByteOrder.LITTLE_ENDIAN); // 小端
     System.out.println(num2); // 1234567890

     String hexString = "49 96 02 D2";

     bytes = hexStringToBytes(hexString);
     System.out.println(Arrays.toString(bytes)); // [73, -106, 2, -46]

     hexString = bytesToHexString(bytes);
     System.out.println(hexString); // 49 96 02 D2

     String binaryString = "01001001 10010110 00000010 11010010";

     bytes = binaryStringToBytes(binaryString);
     System.out.println(Arrays.toString(bytes)); // [73, -106, 2, -46]

     binaryString = bytesToBinaryString(bytes);
     System.out.println(binaryString); // 01001001 10010110 00000010 11010010

     String decimalString = "73 150 2 210";

     bytes = decimalStringToBytes(decimalString);
     System.out.println(Arrays.toString(bytes)); // [73, -106, 2, -46]

     decimalString = bytesToDecimalString(bytes);
     System.out.println(decimalString); // 73 150 2 210

     String radixString = "I Z 2 R";

     bytes = radixStringToBytes(radixString, 36);
     System.out.println(Arrays.toString(bytes)); // [73, -106, 2, -46]

     radixString = bytesToRadixString(bytes, 36);
     System.out.println(radixString); // I Z 2 R

     String typeString = "I Z ! T";

     bytes = typeStringToBytes(typeString, Character.class);

     bytes = typeStringToBytes(typeString, Character.class);
     System.out.println(Arrays.toString(bytes)); // [73, 90, 33, 84]

     typeString = bytesToTypeString(bytes, Character.class);
     System.out.println(typeString); // I Z ! T

     typeString = "true false true true";

     bytes = typeStringToBytes(typeString, Boolean.class);
     System.out.println(Arrays.toString(bytes)); // [1, 0, 1, 1]

     typeString = bytesToTypeString(bytes, Boolean.class);
     System.out.println(typeString); // true false true true


     int num1 = 1234567890;
     // 调用createBuffer方法，创建一个4字节的ByteBuffer对象
     ByteBuffer buffer = createBuffer(num1);
     // 将数字写入buffer
     buffer.putInt(num1);
     // 返回buffer的字节数组
     byte[] bytes1 = buffer.array();
     */

}