package com.org.jzprinter.manager;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class RBQAppService extends Service {

    private static RBQAppService mThis;

    private final IBinder mBinder = new LocalBinder();

    public static class LocalBinder extends Binder{

        public RBQAppService getService() {

            return mThis;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mThis = this;
    }

}
