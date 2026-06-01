package com.mx.mxSdk.Safe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class DES3Utils {

    // 定义默认的加密算法，有DES、DESede(即3DES)、Blowfish
    private static final String DEFAULT_ALGORITHM = "DESede";
    // 定义默认的加密模式，有ECB、CBC、CFB、OFB等
    private static final String DEFAULT_MODE = "CBC";
    // 定义默认的填充方式，有PKCS5Padding和NoPadding两种
    private static final String DEFAULT_PADDING = "PKCS5Padding";
    // 定义初始化向量的长度，必须为8字节
    private static final int IV_LENGTH = 8;

    // 定义密钥
    private byte[] key;
    // 定义加密算法
    private String algorithm;
    // 定义加密模式
    private String mode;
    // 定义填充方式
    private String padding;

    // 构造方法，使用默认的算法、模式和填充方式
    public DES3Utils(byte[] key) {
        this(key, DEFAULT_ALGORITHM, DEFAULT_MODE, DEFAULT_PADDING);
    }

    // 构造方法，使用指定的算法、模式和填充方式
    public DES3Utils(byte[] key, String algorithm, String mode, String padding) {
        this.key = key;
        this.algorithm = algorithm;
        this.mode = mode;
        this.padding = padding;
    }

    // 设置密钥
    public void setKey(byte[] key) {
        this.key = key;
    }

    // 获取密钥
    public byte[] getKey() {
        return this.key;
    }

    // 设置加密算法
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    // 获取加密算法
    public String getAlgorithm() {
        return this.algorithm;
    }

    // 设置加密模式
    public void setMode(String mode) {
        this.mode = mode;
    }
    // 获取加密模式
    public String getMode() {
        return this.mode;
    }

    // 设置填充方式
    public void setPadding(String padding) {
        this.padding = padding;
    }

    // 获取填充方式
    public String getPadding() {
        return this.padding;
    }

    // 根据密钥生成Key对象
    private Key generateKey(byte[] key) throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException {
        DESedeKeySpec dks = new DESedeKeySpec(key);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(algorithm);
        return keyFactory.generateSecret(dks);
    }

    // 生成随机的初始化向量
    private byte[] generateIV() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }

    // 根据初始化向量生成算法参数对象
    private AlgorithmParameterSpec generateIV(byte[] iv) {
        return new IvParameterSpec(iv);
    }

    // 创建Cipher对象
    private Cipher createCipher(int opmode, Key key, byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance(algorithm + "/" + mode + "/" + padding);
        cipher.init(opmode, key, generateIV(iv));
        return cipher;
    }

    // 加密数据，返回包含初始化向量和密文的字节数组
    public byte[] encrypt(byte[] data) throws Exception {
        Key k = generateKey(key);
        byte[] iv = generateIV();
        Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, k, iv);
        byte[] encrypted = cipher.doFinal(data);
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return result;
    }
    // 解密数据，输入包含初始化向量和密文的字节数组，返回明文
    public byte[] decrypt(byte[] data) throws Exception {
        Key k = generateKey(key);
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(data, 0, iv, 0, iv.length);
        Cipher cipher = createCipher(Cipher.DECRYPT_MODE, k, iv);
        byte[] encrypted = new byte[data.length - iv.length];
        System.arraycopy(data, iv.length, encrypted, 0, encrypted.length);
        return cipher.doFinal(encrypted);
    }

    // 加密字符串，返回Base64编码的字符串
    public String encryptString(String data) throws Exception {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encrypt(bytes);
        return android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT); // 使用android.util.Base64类
    }

    // 解密字符串，输入Base64编码的字符串，返回原始字符串
    public String decryptString(String data) throws Exception {
        byte[] bytes = android.util.Base64.decode(data, android.util.Base64.DEFAULT); // 使用android.util.Base64类
        byte[] decrypted = decrypt(bytes);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    // 加密文件，输入源文件和目标文件的路径，将源文件加密后写入目标文件
    public void encryptFile(String srcPath, String destPath) throws Exception {
        File srcFile = new File(srcPath);
        File destFile = new File(destPath);
        try (FileInputStream fis = new FileInputStream(srcFile);
             FileOutputStream fos = new FileOutputStream(destFile)) {
            Key k = generateKey(key);
            byte[] iv = generateIV();
            Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, k, iv);
            fos.write(iv); // 写入初始化向量
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, len); // 分段加密
                if (output != null) {
                    fos.write(output); // 写入加密数据
                }
            }
            byte[] output = cipher.doFinal(); // 结束加密
            if (output != null) {
                fos.write(output); // 写入加密数据
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 解密文件，输入源文件和目标文件的路径，将源文件解密后写入目标文件
    public void decryptFile(String srcPath, String destPath) throws Exception {
        File srcFile = new File(srcPath);
        File destFile = new File(destPath);
        try (FileInputStream fis = new FileInputStream(srcFile); FileOutputStream fos = new FileOutputStream(destFile)) {
            Key k = generateKey(key);
            byte[] iv = new byte[IV_LENGTH];
            fis.read(iv); // 读取初始化向量
            Cipher cipher = createCipher(Cipher.DECRYPT_MODE, k, iv);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, len); // 分段解密
                if (output != null) {
                    fos.write(output); // 写入解密数据
                }
            }
            byte[] output = cipher.doFinal(); // 结束解密
            if (output != null) {
                fos.write(output); // 写入解密数据
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/**
 3DES加密是一种对称密钥加密块密码，相当于是对每个数据块应用三次数据加密标准（DES）算法。
 它使用2个或者3个56位的密钥对数据进行三次加密。相比DES，3DES因密钥长度变长，安全性有所提高，但其处理速度不高。

 3DES加密算法的具体步骤如下：

 加密算法为： 密文 = E K3 (D K2 (E K1 (明文))) ，也就是说，使用K 1 为密钥进行DES加密，再用K 2 为密钥进行DES“解密”，
 最后以K 3 进行DES加密。
 解密算法为： 明文 = D K1 (E K2 (D K3 (密文))) ，即以K 3 解密，以K 2 “加密”，最后以K 1 解密。
 每次加密操作都只处理64位数据，称为一块。无论是加密还是解密，中间一步都是前后两步的逆。
 3DES加密算法有三种密钥选项：

 密钥选项1：三个密钥是独立的。这种选项的强度最高，拥有168个独立的密钥位。
 密钥选项2：K 1 和K 2 是独立的，而K 3 =K 1 。这种选项的安全性稍低，拥有112个独立的密钥位。
 密钥选项3：三个密钥均相等，即K 1 =K 2 =K 3 。这种选项等同于DES，只有56个密钥位。
 */
