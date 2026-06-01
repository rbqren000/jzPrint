package com.mx.mxSdk.imageCompress;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UriFileUtils {

    /**
     * 将URI转换为临时文件
     * @param uri 输入URI
     * @return 临时文件
     * @throws IOException IO异常
     */
    private File convertUriToFile(Context context,Uri uri) throws IOException {
        if (uri == null) {
            throw new IllegalArgumentException("URI cannot be null");
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("URI scheme cannot be null");
        }

        switch (scheme.toLowerCase()) {
            case "file":
                // file://路径直接转换
                return new File(uri.getPath());

            case "content":
                // content://需要通过ContentResolver读取
                return createTempFileFromContentUri(context,uri);

            default:
                throw new IllegalArgumentException("Unsupported URI scheme: " + scheme);
        }
    }

    /**
     * 从content URI创建临时文件
     * @param uri content URI
     * @return 临时文件
     * @throws IOException IO异常
     */
    private File createTempFileFromContentUri(Context context, Uri uri) throws IOException {
        // 创建临时文件
        File tempFile = File.createTempFile("compress_temp_", ".jpg", context.getCacheDir());

        // 从URI读取数据并写入临时文件
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {

            if (inputStream == null) {
                throw new IOException("Cannot open input stream for URI: " + uri);
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return tempFile;
    }
}
