package com.mx.mxSdk.Safe;

import android.util.Base64;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.Cipher;

public class RSAUtils {

    // 定义加密算法
    private static final String ALGORITHM = "RSA";
    // 定义密钥长度
    private static final int KEY_SIZE = 1024;
    // 定义分段大小
    private static final int SEGMENT_SIZE = KEY_SIZE / 8 - 11;

    // 生成密钥对，返回公钥和私钥
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        // 根据算法生成密钥对生成器
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        // 初始化密钥对生成器
        keyPairGenerator.initialize(KEY_SIZE);
        // 生成密钥对
        return keyPairGenerator.generateKeyPair();
    }

    // 获取公钥的模和指数，用于传输或者保存
    public static String[] getPublicKeyModulusAndExponent(PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // 根据算法获取密钥工厂
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        // 获取公钥规范
        RSAPublicKeySpec publicKeySpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);
        // 获取公钥的模和指数
        BigInteger modulus = publicKeySpec.getModulus();
        BigInteger exponent = publicKeySpec.getPublicExponent();
        // 将模和指数转换为字符串并返回
        return new String[]{modulus.toString(), exponent.toString()};
    }

    // 获取私钥的模和指数，用于传输或者保存
    public static String[] getPrivateKeyModulusAndExponent(PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // 根据算法获取密钥工厂
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        // 获取私钥规范
        RSAPrivateKeySpec privateKeySpec = keyFactory.getKeySpec(privateKey, RSAPrivateKeySpec.class);
        // 获取私钥的模和指数
        BigInteger modulus = privateKeySpec.getModulus();
        BigInteger exponent = privateKeySpec.getPrivateExponent();
        // 将模和指数转换为字符串并返回
        return new String[]{modulus.toString(), exponent.toString()};
    }

    // 根据公钥的模和指数还原公钥对象，用于接收或者加载
    public static PublicKey restorePublicKey(String modulus, String exponent) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // 根据算法获取密钥工厂
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        // 将字符串转换为大整数
        BigInteger modulusBigInteger = new BigInteger(modulus);
        BigInteger exponentBigInteger = new BigInteger(exponent);
        // 根据模和指数生成公钥规范
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulusBigInteger, exponentBigInteger);
        // 根据公钥规范还原公钥对象并返回
        return keyFactory.generatePublic(publicKeySpec);
    }

    // 根据私钥的模和指数还原私钥对象，用于接收或者加载
    public static PrivateKey restorePrivateKey(String modulus, String exponent) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // 根据算法获取密钥工厂
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        // 将字符串转换为大整数
        BigInteger modulusBigInteger = new BigInteger(modulus);
        BigInteger exponentBigInteger = new BigInteger(exponent);
        // 根据模和指数生成私钥规范
        RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(modulusBigInteger, exponentBigInteger);
        // 根据私钥规范还原私钥对象并返回
        return keyFactory.generatePrivate(privateKeySpec);
    }

    // 加密方法，参数为明文和公钥，返回Base64编码后的密文字符串
    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            throw new IllegalArgumentException("plainText cannot be null or empty");
        }
        if (publicKey == null) {
            throw new IllegalArgumentException("publicKey cannot be null");
        }
        // 根据算法获取密码器对象
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        // 初始化密码器为加密模式，传入公钥
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] plainBytes = plainText.getBytes();
        int inputLen = plainBytes.length;
        int offset = 0;
        byte[] buffer;
        byte[] cipherBytes = new byte[0];
        while (inputLen - offset > 0) {
            if (inputLen - offset > SEGMENT_SIZE) {
                buffer = cipher.doFinal(plainBytes, offset, SEGMENT_SIZE);
            } else {
                buffer = cipher.doFinal(plainBytes, offset, inputLen - offset);
            }
            cipherBytes = concat(cipherBytes, buffer);
            offset += SEGMENT_SIZE;
        }
        return Base64.encodeToString(cipherBytes, Base64.DEFAULT);

    }

    // 解密方法，参数为Base64编码后的密文字符串和私钥，返回明文字符串
    public static String decrypt(String cipherText, PrivateKey privateKey) throws Exception {
        if (cipherText == null || cipherText.isEmpty()) {
            throw new IllegalArgumentException("cipherText cannot be null or empty");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("privateKey cannot be null");
        }
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] cipherBytes = Base64.decode(cipherText, Base64.DEFAULT);
        int inputLen = cipherBytes.length;
        int offset = 0;
        byte[] buffer;
        byte[] plainBytes = new byte[0];
        while (inputLen - offset > 0) {
            if (inputLen - offset > SEGMENT_SIZE) {
                buffer = cipher.doFinal(cipherBytes, offset, SEGMENT_SIZE);
            } else {
                buffer = cipher.doFinal(cipherBytes, offset, inputLen - offset);
            }
            plainBytes = concat(plainBytes, buffer);
            offset += SEGMENT_SIZE;
        }
        return new String(plainBytes);

    }

    private static byte[] concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] c= new byte[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }
}

/**
 RSA加密是一种非对称加密算法，即使用一对密钥来进行加密和解密，其中一个称为公钥，
 可以公开给任何人使用，另一个称为私钥，必须保密。
 RSA加密的原理是基于数论的一个难题：给出两个大素数（即只能被1和自身整除的数），
 很容易计算出它们的乘积，但是给出它们的乘积，想要分解出这两个素数就非常困难。
 RSA加密的过程如下：
 首先，选择两个大素数 p 和 q ，计算它们的乘积 n ，并计算 n 的欧拉函数 \phi (n) ，
 即小于等于 n 且与 n 互素的正整数的个数；
 然后，选择一个小于 \phi (n) 的正整数 e ，使得 e 和 \phi (n) 互素，即最大公约数为1；
 接着，计算 e 在模 \phi (n) 下的逆元 d ，即满足 ed \ mod \ \phi (n) = 1 的正整数 d ；
 最后，将 (n, e) 作为公钥，将 (n, d) 作为私钥；
 加密时，将明文转换为一个小于 n 的正整数 m ，然后计算 c = m^e \ mod \ n ，得到的 c 就是密文；
 解密时，将密文 c 用私钥 d 进行解密，即计算 m = c^d \ mod \ n ，得到的 m 就是明文；

 */
/*
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // 生成密钥对
            KeyPair keyPair = RSAUtils.generateKeyPair();
            // 获取公钥和私钥
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            // 定义明文
            String plainText = "Hello RSA";
            // 使用公钥加密明文，得到密文
            String cipherText = RSAUtils.encrypt(plainText, publicKey);
            // 使用私钥解密密文，得到明文
            String decryptedText = RSAUtils.decrypt(cipherText, privateKey);
            // 打印结果
            System.out.println("plainText: " + plainText);
            System.out.println("cipherText: " + cipherText);
            System.out.println("decryptedText: " + decryptedText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
*/