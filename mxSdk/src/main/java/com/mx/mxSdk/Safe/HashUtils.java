package com.mx.mxSdk.Safe;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

    // 哈希一个字符串并返回十六进制字符串
    public static String hashString(String string, String algorithm) throws NoSuchAlgorithmException {
        // 检查输入参数是否合法
        if (string == null || algorithm == null) {
            throw new IllegalArgumentException("Invalid input parameter");
        }
        // 使用MessageDigest类来获取哈希算法的实例
        MessageDigest digest = MessageDigest.getInstance(algorithm);

        // 使用哈希算法对字符串进行哈希，并将结果转换为字节数组
        byte[] hash = digest.digest(string.getBytes(StandardCharsets.UTF_8));

        // 将字节数组转换为十六进制字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    // 哈希一个字节数组并返回十六进制字符串
    public static String hashData(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        // 检查输入参数是否合法
        if (data == null || algorithm == null) {
            throw new IllegalArgumentException("Invalid input parameter");
        }
        // 使用MessageDigest类来获取哈希算法的实例
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        // 使用哈希算法对字节数组进行哈希，并将结果转换为字节数组
        byte[] hash = digest.digest(data);
        // 将字节数组转换为十六进制字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}
/**
 哈希算法是一种将任意长度的数据映射为固定长度的数据的算法，通常用于验证数据的完整性或生成数字签名。
 哈希算法的一个重要特性是单向性，也就是说，给定一个哈希值，很难找到一个与之对应的原始数据123。这是因为哈希算法具有以下特点：
 * 碰撞抵抗性：不同的数据很难产生相同的哈希值，即使只有一个比特的差别，也会导致哈希值的巨大变化。
 * 雪崩效应：每一个输出比特都依赖于每一个输入比特，这使得无法通过分割算法来逐个求解输入比特。
 * 压缩性：哈希值的长度通常远小于输入数据的长度，这意味着有无限多个数据可能对应同一个哈希值，无法一一对应。
 因此，哈希算法是不可逆的，也就是说，不能从哈希值还原出原始数据。但是，这并不意味着哈希值是安全的，有些方法可以尝试破解哈希值，例如：
 * 暴力破解：尝试所有可能的输入数据，直到找到一个与目标哈希值匹配的数据。
 * 彩虹表：预先计算并存储大量数据和哈希值的对应关系，然后在表中查找目标哈希值对应的数据。
 * 密码学攻击：利用某些哈希算法存在的弱点或漏洞，降低破解难度或提高成功率。
 因此，为了保证哈希值的安全性，需要使用一些措施，例如：
 * 选择强大的哈希算法：避免使用已经被证明不安全或存在缺陷的哈希算法，例如MD5或SHA-1123。
 * 增加复杂度：在输入数据中添加一些随机或不可预测的元素，例如盐（salt）或随机数（nonce），使得相同的数据产生不同的哈希值。
 * 限制访问：保护好哈希值和输入数据，防止被恶意获取或篡改。
 */