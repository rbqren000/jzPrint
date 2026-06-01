package com.org.jzprinter.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.org.jzprinter.R;
import com.org.jzprinter.network.Api;
import com.org.jzprinter.network.ApiClientFactory;
import com.org.jzprinter.print.MaterialPathBuilder;
import com.org.jzprinter.ui.activity.MainActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadService extends Service {
    private static final String TAG = "DownloadService";
    private static final String CHANNEL_ID = "download_service_channel";
    private static final int NOTIFICATION_ID = 1002;
    private static DownloadService instance;

    public static boolean isRunning() {
        return instance != null;
    }

    public static void start(Context context) {
        if (isRunning()) return;
        Intent intent = new Intent(context, DownloadService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop() {
        if (instance != null) {
            instance.stopSelf();
            instance = null;
        }
    }

    public static void downloadAndExtract(Context context, String schoolId, String businessId,
                                           int editionType, String editionId, String targetId,
                                           DownloadAndExtractCallback callback) {
        String pagesPath = MaterialPathBuilder.getPagesPath(
            context, schoolId, editionId, editionType, targetId);
        String zipPath = MaterialPathBuilder.getZipPath(
            context, schoolId, editionId, editionType, targetId);

        Log.d(TAG, "[downloadAndExtract] targetId=" + targetId
            + " zipPath=" + zipPath
            + " pagesPath=" + pagesPath);

        if (isAlreadyExtracted(pagesPath)) {
            Log.d(TAG, "[downloadAndExtract] already extracted, skip download. pagesPath=" + pagesPath);
            if (callback != null) {
                callback.onAlreadyExists(pagesPath);
            }
            return;
        }

        start(context);

        Api apiClient = ApiClientFactory.create(context);
        apiClient.downloadMaterial(schoolId, businessId, editionType, zipPath,
            new Api.DownloadCallback() {
                @Override
                public void onSuccess(String localPath) {
                    Log.d(TAG, "[downloadAndExtract] download success, zip saved to: " + localPath);
                    try {
                        unzip(localPath, pagesPath);
                        Log.d(TAG, "[downloadAndExtract] unzip complete, pages saved to: " + pagesPath);
                        new File(localPath).delete();
                        if (callback != null) callback.onComplete(pagesPath);
                    } catch (IOException e) {
                        Log.e(TAG, "[downloadAndExtract] unzip failed: " + e.getMessage());
                        if (callback != null) callback.onError("解压失败: " + e.getMessage());
                    }
                    stop();
                }

                @Override
                public void onProgress(int percentage) {
                    if (callback != null) callback.onDownloadProgress(percentage);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "[downloadAndExtract] download error: " + error);
                    if (callback != null) callback.onError(error);
                    stop();
                }
            });
    }

    private static boolean isAlreadyExtracted(String pagesPath) {
        File dir = new File(pagesPath);
        return dir.exists() && dir.isDirectory() && dir.list() != null && dir.list().length > 0;
    }

    private static void unzip(String zipFilePath, String destDirPath) throws IOException {
        File destDir = new File(destDirPath);
        destDir.mkdirs();

        byte[] buffer = new byte[8192];
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFilePath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(destDir, entry.getName());
                String canonicalDestPath = destDir.getCanonicalPath();
                String canonicalOutPath = outFile.getCanonicalPath();
                if (!canonicalOutPath.startsWith(canonicalDestPath + File.separator)) {
                    throw new IOException("Zip entry outside target dir: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    public interface DownloadAndExtractCallback {
        void onDownloadProgress(int percentage);
        void onComplete(String pagesPath);
        void onAlreadyExists(String pagesPath);
        void onError(String error);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        startForeground(NOTIFICATION_ID, createNotification("正在下载素材..."));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification createNotification(String text) {
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("简作下载")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "下载服务", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("素材下载进行中的前台通知");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
