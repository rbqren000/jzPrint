package com.mx.mxSdk.Safe.asymmetric;

import com.mx.mxSdk.Utils.RBQLog;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * ECC 非对称加密工具（ECDH 密钥协商 + AES-CBC 加解密）
 * 状态：工具类完备，待业务接入
 */
public class ECCUtils {

    // 生成ECC密钥对
    public static KeyPair generateECCKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(256, SecureRandom.getInstance("SHA1PRNG")); // use fixed key size and random algorithm
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 获取ECC公钥的字节数组
    public static byte[] getECCPublicKeyBytes(ECPublicKey publicKey) {
        return publicKey.getEncoded();
    }

    // 获取ECC私钥的字节数组
    public static byte[] getECCPrivateKeyBytes(ECPrivateKey privateKey) {
        return privateKey.getEncoded();
    }

    // 使用ECDH算法生成对称密钥
    public static byte[] generateSymmetricKey(byte[] publicKeyBytes, byte[] privateKeyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(x509EncodedKeySpec);
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            ECPrivateKey privateKey = (ECPrivateKey) keyFactory.generatePrivate(pkcs8EncodedKeySpec);
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(publicKey, true);
            return keyAgreement.generateSecret();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 使用 AES-CBC 加密数据。
     * ⚠️ IV 由调用方传入，不嵌入密文。双方须通过安全渠道约定 IV，切勿复用同一 IV。
     */
    public static byte[] encryptData(byte[] data, byte[] symmetricKey, byte[] iv) {
        Cipher cipher = null;
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(symmetricKey, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // Android 内置 provider
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 使用AES算法解密数据
    public static byte[] decryptData(byte[] data, byte[] symmetricKey, byte[] iv) {
        Cipher cipher = null;
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(symmetricKey, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // Android 内置 provider
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException |
                 BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
/**
 ECC加密是一种基于椭圆曲线数学的公开密钥加密算法。
 它的优势是可以使用更短的密钥来实现与RSA相当或更高的安全性。
 ECC加密的原理是利用椭圆曲线上的有理点构成阿贝尔加法群，以及椭圆曲线离散对数问题的计算困难性。
 ECC加密的常见应用有椭圆曲线迪菲-赫尔曼密钥交换（ECDH），椭圆曲线数字签名算法（ECDSA），以及国家密码管理局颁布的SM2算法。
 */