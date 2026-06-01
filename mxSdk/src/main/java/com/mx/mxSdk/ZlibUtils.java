package com.mx.mxSdk;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ZlibUtils {

    static {
        System.loadLibrary("mxSdk");
    }

    // 压缩数据
    public static byte[] compress(byte[] input) {
        Deflater deflater = new Deflater();
        deflater.setInput(input);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        deflater.end();

        return outputStream.toByteArray();
    }

    public static byte[] compressWithLevel(byte[] input,int level) {
        Deflater deflater = new Deflater(level);
        deflater.setInput(input);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        deflater.end();

        return outputStream.toByteArray();
    }

    // 解压数据
    public static byte[] decompress(byte[] input) {
        Inflater inflater = new Inflater();
        inflater.setInput(input);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            inflater.end();
        }

        return outputStream.toByteArray();
    }

    public static native byte[] nativeCompress(byte[] data);
    public static native byte[] nativeCompressWithLevel(byte[] data,int level);
    public static native byte[] nativeDecompress(byte[] data);

}

