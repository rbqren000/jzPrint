package com.mx.mxSdk.Safe;

import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class RC4 {

    // The state array
    private final byte[] S = new byte[256];

    // The index variables
    private int i = 0;
    private int j = 0;

    // The constructor that initializes the state and key arrays
    public RC4(byte[] key) {
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        // The key array
        byte[] k1 = new byte[256];
        for (int i = 0; i < 256; i++) {
            S[i] = (byte) i;
            k1[i] = key[i % key.length];
        }
        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + S[i] + k1[i]) & 0xFF;
            swap(S, i, j);
        }
        // Discard the first 256 bytes of the output keystream
        for (int k = 0; k < 256; k++) {
            i = (i + 1) & 0xFF;
            j = (j + S[i]) & 0xFF;
            swap(S, i, j);
        }
    }

    // The method that encrypts or decrypts a byte array
    public byte[] encrypt(byte[] plaintext) {
        if (plaintext == null) {
            return null;
        }
        byte[] ciphertext = new byte[plaintext.length];
        for (int k = 0; k < plaintext.length; k++) {
            i = (i + 1) & 0xFF;
            j = (j + S[i]) & 0xFF;
            swap(S, i, j);
            int t = (S[i] + S[j]) & 0xFF;
            byte keystream = S[t];
            ciphertext[k] = (byte) (plaintext[k] ^ keystream);
        }
        return ciphertext;
    }

    // The method that decrypts a byte array
    public byte[] decrypt(byte[] ciphertext) {
        return encrypt(ciphertext); // Encryption and decryption are the same operation in RC4
    }

    // The helper method that swaps two elements in an array
    private void swap(byte[] array, int i, int j) {
        byte temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    // The method that generates a random key of a given length
    public static byte[] generateKey(int length) {
        if (length <= 0 || length > 256) {
            throw new IllegalArgumentException("Key length must be between 1 and 256 bytes");
        }
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[length];
        random.nextBytes(key);
        return key;
    }

    // The method that computes the HMAC-SHA256 of a given message and key
    public static byte[] hmacSha256(byte[] message, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA256");
            mac.init(secretKey);
            return mac.doFinal(message);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
/**
 RC4加密是一种流密码，它是由Ron Rivest在1987年为RSA Security设计的。
 1它的全称是Rivest Cipher 4，也可以理解为Ron’s Code，与RC2，RC5和RC6类似。
 RC4加密的特点是简单和快速，它可以使用不同长度的密钥（从8位到2048位），并且可以在软件和硬件中实现。
 RC4加密被广泛应用在各种网络协议和应用中，比如SSL，TLS，WEP和WPA等。

 RC4加密的算法分为两个部分：密钥安排算法（KSA）和伪随机生成算法（PRGA）。

 KSA使用一个用户选择的变长密钥（K）来初始化一个256字节的状态向量（S），S中的每个字节都与K中的一个字节进行异或运算，
 并且进行多次交换操作。
 PRGA使用S来生成一个与明文长度相同的密钥流（Z），Z中的每个字节都是S中的两个字节相加后再查找S中对应的值。
 加密或解密时，只需要将明文或密文与Z进行异或运算即可得到结果。
 RC4加密的优点是简单和快速，它可以在软件和硬件中高效地实现，并且可以使用不同长度的密钥来适应不同的安全需求。

 RC4加密的缺点是存在多种安全漏洞，它不是完全随机的，而是有很多偏差和相关性，这使得它容易受到密码分析和攻击，
 比如FMS攻击，Klein攻击，Bar mitzvah攻击等。 它也不能提供数据的完整性和认证性，需要配合其他机制来防止篡改或伪造。
 它也不能适用于小数据流的加密，因为会导致密钥流的重复或预测。
 */