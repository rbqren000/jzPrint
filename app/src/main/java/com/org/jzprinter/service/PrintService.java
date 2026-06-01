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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.org.jzprinter.R;
import com.org.jzprinter.ui.activity.MainActivity;

public class PrintService extends Service {
    private static final String CHANNEL_ID = "print_service_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static PrintService instance;

    public static boolean isRunning() {
        return instance != null;
    }

    public static void start(Context context) {
        if (isRunning()) return;
        Intent intent = new Intent(context, PrintService.class);
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

    public static void notifyComplete() {
        stop();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        startForeground(NOTIFICATION_ID, createNotification("正在打印..."));
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
            .setContentTitle("简作打印")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "打印服务", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("打印进行中的前台通知");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
