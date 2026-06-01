package com.mx.mxSdk.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BroadcastHelper {

    private static final Set<BroadcastReceiver> registeredReceivers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void registerReceiver(Context context, BroadcastReceiver receiver, IntentFilter filter, boolean isLocal) {
        if (context == null || receiver == null || filter == null) return;

        if (isLocal) {
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(receiver, filter);
            }
        }

        registeredReceivers.add(receiver);
    }

    public static void unregisterReceiver(Context context, BroadcastReceiver receiver, boolean isLocal) {
        if (context == null || receiver == null) return;

        if (!registeredReceivers.contains(receiver)) {
            RBQLog.w("BroadcastHelper", "Receiver not registered: " + receiver);
            return;
        }

        try {
            if (isLocal) {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
            } else {
                context.unregisterReceiver(receiver);
            }
        } catch (IllegalArgumentException e) {
            RBQLog.e("BroadcastHelper", "Failed to unregister receiver: " + receiver, e);
        } finally {
            registeredReceivers.remove(receiver);
        }
    }

    public static void sendBroadcast(Context context, Intent intent, boolean isLocal) {
        if (context == null || intent == null) return;

        if (isLocal) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } else {
            context.sendBroadcast(intent);
        }
    }
}




