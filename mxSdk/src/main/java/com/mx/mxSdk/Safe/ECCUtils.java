package com.mx.mxSdk.Safe;

import com.mx.mxSdk.Utils.RBQLog;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
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

    // 使用AES算法加密数据
    public static byte[] encryptData(byte[] data, byte[] symmetricKey, byte[] iv) {
        Cipher cipher = null; // declare cipher outside try block
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(symmetricKey, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv); // use IvParameterSpec instead of GCMParameterSpec
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC"); // use Bouncy Castle provider
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(data);
        } catch (NoSuchProviderException|NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 使用AES算法解密数据
    public static byte[] decryptData(byte[] data, byte[] symmetricKey, byte[] iv) {
        Cipher cipher = null; // declare cipher outside try block
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(symmetricKey, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv); // use IvParameterSpec instead of GCMParameterSpec
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC"); // use Bouncy Castle provider
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            return cipher.doFinal(data);
        } catch (NoSuchProviderException|NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
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