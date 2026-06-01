package com.mx.mxSdk.Utils;

import java.nio.charset.Charset;

public final class Strings {

    private Strings() {
    }

    public static byte[] stringToBytes(String str, int length) {

        byte[] srcBytes;

        if (length <= 0) {
            return str.getBytes(Charset.defaultCharset());
        }

        byte[] result = new byte[length];

        srcBytes = str.getBytes(Charset.defaultCharset());

        System.arraycopy(srcBytes, 0, result, 0, Math.min(srcBytes.length, length));

        return result;
    }

    public static byte[] stringToBytes(String str) {
        return stringToBytes(str, 0);
    }

    public static String bytesToString(byte[] data) {
        return data == null || data.length == 0 ? null : new String(data, Charset.defaultCharset()).trim();
    }

    public static String bytesToStringOfInt(byte[] data) {
        if(data == null || data.length <= 0 ){

           return null;
        }

        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i<data.length;i++){

            buffer.append(data[i]&0xff);

            if (i<data.length-1){

                buffer.append(":");
            }

        }

        return buffer.toString();
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
