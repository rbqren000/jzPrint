package com.org.jzprinter.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class SignUtil {

    public static String generateSign(String creditCode, long time,
                                      String accessKey, String accessSecret,
                                      String regionalismCode, String functionCode) {
        List<String> paramList = new ArrayList<>();
        paramList.add("creditCode=" + creditCode);
        paramList.add("time=" + time);
        paramList.add("regionalismCode=" + regionalismCode);
        paramList.add("functionCode=" + functionCode);
        Collections.sort(paramList);

        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("accessKey=").append(accessKey);
        keyBuilder.append(String.join("", paramList));

        String md5 = md5Hex(keyBuilder.toString());
        return hmacSha256Hex(md5, accessSecret);
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    private static String hmacSha256Hex(String data, String key) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("HmacSHA256 failed", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private SignUtil() {}
}
