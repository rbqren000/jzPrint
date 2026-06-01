package com.mx.mxSdk.Safe;

import android.util.Base64;
import android.util.Log;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**

 AES/CBC/PKCS5Padding：这个常量表示使用AES算法，CBC（密码块链）模式和PKCS5填充方式。CBC模式是一种将每个明文块与前一个密文块进行异或运算的模式，可以提高安全性，但是需要一个初始化向量。PKCS5填充方式是一种在最后一个字节指定填充长度，在其他字节用相同的值填充的方式，可以支持任意长度的数据。
 AES/ECB/PKCS5Padding：这个常量表示使用AES算法，ECB（电子密码本）模式和PKCS5填充方式。ECB模式是一种将每个明文块单独加密的模式，不需要初始化向量，但是安全性较低，容易暴露重复的模式。PKCS5填充方式同上。
 AES/ECB/NoPadding：这个常量表示使用AES算法，ECB模式和无填充方式。无填充方式表示不对数据进行任何处理，要求数据长度必须是16字节的整数倍。
 AES/GCM/NoPadding：这个常量表示使用AES算法，GCM（伽罗瓦/计数器模式）和无填充方式。GCM模式是一种同时提供加密和认证的模式，可以防止篡改或重放攻击，但是需要一个初始化向量和一个认证标签长度参数。无填充方式同上。
 AES/CTR/NoPadding：这个常量表示使用AES算法，CTR（计数器模式）和无填充方式。CTR模式是一种可以实现流式加密的模式，可以支持任意长度的数据和并行计算，但是需要一个初始化向量。无填充方式同上。
 AES/CFB/NoPadding：这个常量表示使用AES算法，CFB（密码反馈模式）和无填充方式。CFB模式是一种可以实现自同步流式加密的模式，可以支持任意长度的数据，但是容易受到误差传播的影响，需要一个初始化向量。无填充方式同上。
 AES/OFB/NoPadding：这个常量表示使用AES算法，OFB（输出反馈模式）和无填充方式。OFB模式是一种可以实现自同步流式加密的模式，可以支持任意长度的数据，但是容易受到同步丢失的影响，需要一个初始化向量。无填充方式同上。
 AES/CBC/NoPadding：使用CBC（密码块链）模式和无填充方式，要求数据长度必须是16字节的整数倍
 AES/ECB/PKCS5Padding：使用ECB（电子密码本）模式和PKCS5填充方式，可以支持任意长度的数据，但是安全性较低
 AES/GCM/PKCS5Padding：使用GCM（伽罗瓦/计数器模式）和PKCS5填充方式，可以同时提供加密和认证，但是需要额外的认证标签长度参数
 AES/CTR/PKCS5Padding：使用CTR（计数器模式）和PKCS5填充方式，可以实现流式加密，支持任意长度的数据和并行计算
 AES/CFB/PKCS5Padding：使用CFB（密码反馈模式）和PKCS5填充方式，可以实现自同步流式加密，支持任意长度的数据，但是容易受到误差传播的影响
 AES/OFB/PKCS5Padding：使用OFB（输出反馈模式）和PKCS5填充方式，可以实现自同步流式加密，支持任意长度的数据，但是容易受到同步丢失的影响
 AES/CBC/ISO10126Padding：使用CBC模式和ISO10126填充方式，可以支持任意长度的数据，填充方式为在最后一个字节指定填充长度，在其他字节随机填充
 AES/CBC/ANSIX923Padding：使用CBC模式和ANSIX923填充方式，可以支持任意长度的数据，填充方式为在最后一个字节指定填充长度，在其他字节用0填充
 AES/CBC/ZerosPadding：使用CBC模式和Zeros填充方式，可以支持任意长度的数据，填充方式为用0填充

 */

public class AESUtils {

    // 定义一些常用的加密模式和填充方式的常量

    //AES/ECB/PKCS5Padding是使用PKCS5Padding填充方式，它的原理是缺几个字节就填几个缺的字节数。
    public static final String AES_CBC_PKCS5PADDING = "AES/CBC/PKCS5Padding";
    // (ECB已被安卓不推荐)例如，如果明文长度是125位，那么就在后面填充3个字节的3。
    public static final String AES_ECB_PKCS5PADDING = "AES/ECB/PKCS5Padding";
    //AES/ECB/NoPadding是不使用任何填充方式，它的要求是明文长度必须是128位的倍数，否则无法加密。
    public static final String AES_ECB_NOPADDING = "AES/ECB/NoPadding";
    public static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";
    public static final String AES_CTR_NOPADDING = "AES/CTR/NoPadding";
    public static final String AES_CFB_NOPADDING = "AES/CFB/NoPadding";
    public static final String AES_OFB_NOPADDING = "AES/OFB/NoPadding";
    public static final String AES_CBC_NOPADDING = "AES/CBC/NoPadding";
    public static final String AES_GCM_PKCS5PADDING = "AES/GCM/PKCS5Padding";
    public static final String AES_CTR_PKCS5PADDING = "AES/CTR/PKCS5Padding";
    public static final String AES_CFB_PKCS5PADDING = "AES/CFB/PKCS5Padding";
    public static final String AES_OFB_PKCS5PADDING = "AES/OFB/PKCS5Padding";
    public static final String AES_CBC_ISO10126PADDING = "AES/CBC/ISO10126Padding";
    public static final String AES_CBC_ANSIX923PADDING = "AES/CBC/ANSIX923Padding";
    public static final String AES_CBC_ZEROSPADDING = "AES/CBC/ZerosPadding";
    // 你可以根据需要添加更多的常量

    // 定义一些常用的编码方式的常量
    public static final String BASE64 = "Base64";
    public static final String HEX = "Hex";
    // 你可以根据需要添加更多的常量

    // 生成一个随机的AES密钥，密钥的字节数组，长度可以是16、24或32字节 既 128位 192位 256位
    public static Key generateKey(int keySize) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        // 设置密钥长度，可以是128或256
        keyGenerator.init(keySize);
        return keyGenerator.generateKey();
    }

    // 生成一个随机的初始化向量
    public static byte[] generateIV() {
        byte[] iv = new byte[16]; // AES算法的块大小为16字节
        new SecureRandom().nextBytes(iv); // 使用安全的随机数生成器
        return iv;
    }

    // 将密钥转换为字符串
    public static String keyToString(Key key) {
        return Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
    }

    // 将字符串转换为密钥
    public static Key stringToKey(String keyStr) {
        byte[] keyBytes = Base64.decode(keyStr, Base64.DEFAULT);
        return new SecretKeySpec(keyBytes, "AES");
    }

    // 将初始化向量转换为字符串
    public static String ivToString(byte[] iv) {
        return Base64.encodeToString(iv, Base64.DEFAULT);
    }

    // 将字符串转换为初始化向量
    public static byte[] stringToIV(String ivStr) {
        return Base64.decode(ivStr, Base64.DEFAULT);
    }

    // 使用AES加密数据，可以指定不同的加密模式，例如AES_CBC_PKCS5PADDING或AES_GCM_NOPADDING，返回指定编码方式的字符串，例如BASE64或HEX
    public static String encrypt(byte[] data, Key key, byte[] iv, String mode, String encoding) {
        // 检查输入数据是否为空或无效
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Invalid data: null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Invalid key: null");
        }
        if (iv == null) {
            throw new IllegalArgumentException("Invalid iv: null");
        }
        // 使用日志记录器记录程序开始加密
//        Log.d("AESUtils", "Start encrypting data with mode: " + mode + ", encoding: " + encoding);
        try {
            Cipher cipher = Cipher.getInstance(mode); // 指定加密模式和填充方式
            if (mode.contains("CBC") || mode.contains("CFB") || mode.contains("OFB") || mode.contains("CTR")) {
                // 如果使用了需要初始化向量的加密模式，就使用初始化向量初始化加密器
                cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            } else if (mode.contains("GCM")) {
                // 如果使用了GCM模式，就使用初始化向量和认证标签长度初始化加密器
                cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            } else if (mode.contains("ECB")) {
                // 如果使用了ECB模式，就只使用密钥初始化加密器
                cipher.init(Cipher.ENCRYPT_MODE, key);
            } else {
                throw new IllegalArgumentException("Invalid mode: " + mode); // 抛出非法参数异常
            }
            byte[] encryptedData = cipher.doFinal(data); // 加密数据
            if (encoding.equals(BASE64)) {
                return Base64.encodeToString(encryptedData, Base64.DEFAULT); // 使用Base64编码返回字符串
            } else if (encoding.equals(HEX)) {
                return bytesToHex(encryptedData); // 使用Hex编码返回字符串
            } else {
                throw new IllegalArgumentException("Invalid encoding: " + encoding); // 抛出非法参数异常
            }
        } catch (Exception e) {
            // 捕获可能发生的异常，并给出合理的提示或反馈
            e.printStackTrace(); // 处理异常
            Log.e("AESUtils", "Failed to encrypt data: " + e.getMessage()); // 使用日志记录器记录程序失败原因
            return null; // 返回空值
        } finally {
            // 使用日志记录器记录程序结束加密
//            Log.d("AESUtils", "End encrypting data");
        }
    }

    // 使用AES解密数据，需要指定和加密时相同的加密模式，例如AES_CBC_PKCS5PADDING或AES_GCM_NOPADDING，输入指定编码方式的字符串，例如BASE64或HEX，返回原始字节数组
    public static byte[] decrypt(String data, Key key, byte[] iv, String mode, String encoding) throws Exception {
        // 检查输入数据是否为空或无效
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Invalid data: null or empty");
        }
        if (key == null) {
            throw new IllegalArgumentException("Invalid key: null");
        }
        if (iv == null) {
            throw new IllegalArgumentException("Invalid iv: null");
        }
        // 使用日志记录器记录程序开始解密
//        Log.d("AESUtils", "Start decrypting data with mode: " + mode + ", encoding: " + encoding);
        try {
            Cipher cipher = Cipher.getInstance(mode); // 指定加密模式和填充方式
            if (mode.contains("CBC") || mode.contains("CFB") || mode.contains("OFB") || mode.contains("CTR")) {
                // 如果使用了需要初始化向量的加密模式，就使用初始化向量初始化解密器
                cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            } else if (mode.contains("GCM")) {
                // 如果使用了GCM模式，就使用初始化向量和认证标签长度初始化解密器
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            } else if (mode.contains("ECB")) {
                // 如果使用了ECB模式，就只使用密钥初始化解密器
                cipher.init(Cipher.DECRYPT_MODE, key);
            } else {
                throw new IllegalArgumentException("Invalid mode: " + mode); // 抛出非法参数异常
            }
            byte[] encryptedData; // 定义加密后的字节数组
            if (encoding.equals(BASE64)) {
                encryptedData = Base64.decode(data, Base64.DEFAULT); // 使用Base64解码输入字符串
            } else if (encoding.equals(HEX)) {
                encryptedData = hexToBytes(data); // 使用Hex解码输入字符串
            } else {
                throw new IllegalArgumentException("Invalid encoding: " + encoding); // 抛出非法参数异常
            }
            return cipher.doFinal(encryptedData); // 解密数据并返回原始字节数组
        } catch (Exception e) {
            // 捕获可能发生的异常，并给出合理的提示或反馈
            e.printStackTrace(); // 处理异常
            Log.e("AESUtils", "Failed to decrypt data: " + e.getMessage()); // 使用日志记录器记录程序失败原因
            return null; // 返回空值
        } finally {
            // 使用日志记录器记录程序结束解密
//            Log.d("AESUtils", "End decrypting data");
        }
    }


    // 将字节数组转换为十六进制字符串（辅助方法）
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b)); // 将每个字节转换为两位十六进制数，并拼接成字符串
        }
        return sb.toString();
    }

    // 将十六进制字符串转换为字节数组（辅助方法）
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string: " + hex); // 如果字符串长度不是偶数，抛出非法参数异常
        }
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
            // 将每两位十六进制数转换为一个字节，并存入字节数组中
        }
        return bytes;
    }
}

/**
 一般来说，加密算法可以分为两种：对称加密和非对称加密。

 对称加密是指使用相同的密钥（key）来加密和解密数据的算法。例如，AES，DES，RC4等。这种算法的优点是速度快，
 但是缺点是需要安全地传输和存储密钥，否则会被破解。对称加密的例子是：

 明文：Hello 密钥：1234 加密：Jgnnq 解密：Hello

 非对称加密是指使用一对不同的密钥来加密和解密数据的算法。这对密钥通常被称为公钥（public key）和私钥（private key）。
 公钥可以公开发布，用来加密数据，而私钥必须保密，用来解密数据。例如，RSA，ECC，DSA等。这种算法的优点是安全性高，
 不需要传输和存储密钥，但是缺点是速度慢。非对称加密的例子是：

 明文：Hello 公钥：(e,n) = (3,55) 私钥：(d,n) = (27,55)
 加密：C = M^e mod n = 8^3 mod 55 = 17 解密：M = C^d mod n = 17^27 mod 55 = 8

 所以，根据你的问题，如果你使用的是对称加密算法，那么加密和解密的key必须一样；
 如果你使用的是非对称加密算法，那么加密和解密的key必须不一样。
 */


/**
 对称加密：使用相同的密钥（key）来加密和解密数据的算法。例如，AES，DES(已被认为不安全)，3DES，RC4等。
 这种算法的优点是速度快，但是缺点是需要安全地传输和存储密钥，否则会被破解。

 非对称加密：使用一对不同的密钥来加密和解密数据的算法。这对密钥通常被称为公钥（public key）和私钥（private key）。
 公钥可以公开发布，用来加密数据，而私钥必须保密，用来解密数据。
 例如，RSA，ECC，DSA等。这种算法的优点是安全性高，不需要传输和存储密钥，但是缺点是速度慢。

 哈希算法：把任意长度的数据转换成固定长度的字符串（hash）的算法。这种算法不能用来解密数据，
 只能用来验证数据的完整性和一致性。例如，MD5，SHA-1，SHA-256等。这种算法的优点是简单高效，
 但是缺点是可能存在碰撞（不同的数据生成相同的hash）。

 数字签名：使用非对称加密和哈希算法结合的方式来验证数据的来源和真实性的算法。数字签名通常由发送方用私钥对数据的hash进行加密生成，
 然后由接收方用公钥进行解密验证。
 例如，RSA签名，ECDSA签名等。这种算法的优点是提供了非否认性（发送方不能否认发送过数据），但是缺点是需要管理好公钥和私钥。
 */