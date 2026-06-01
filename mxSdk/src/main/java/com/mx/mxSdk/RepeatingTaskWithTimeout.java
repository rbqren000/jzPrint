package com.mx.mxSdk;

import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReentrantLock;

public class RepeatingTaskWithTimeout {
    private final Handler handler;
    private final Runnable task;
    private final long intervalMillis;
    private final long startTimeMillis;
    private final long timeoutMillis;
    private boolean isRunning;
    private final ReentrantLock lock = new ReentrantLock();
    private final WeakReference<Callback> callbackWeakReference;

    public interface Callback {
        void onTaskStarted();
        void onTaskStopped();
        void onTaskError(Exception e);
        void onTaskTimeout(); // 添加超时事件回调
    }

    /**
     * 构造方法，接收以秒为单位的时间参数。
     *
     * @param task       需要执行的重复任务
     * @param startTime  初始延迟时间（秒）
     * @param interval   重复间隔时间（秒）
     * @param timeout    超时时间（秒）
     * @param callback   任务状态的回调
     */
    public RepeatingTaskWithTimeout(Runnable task, double startTime, double interval, double timeout, Callback callback) {
        this.handler = new Handler(Looper.getMainLooper());
        this.task = task;
        this.intervalMillis = (long) (interval * 1000); // 转换为毫秒
        this.startTimeMillis = (long) (startTime * 1000); // 转换为毫秒
        this.timeoutMillis = (long) (timeout * 1000); // 转换为毫秒
        this.isRunning = false;
        this.callbackWeakReference = new WeakReference<>(callback);
    }

    private final Runnable internalRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                try {
                    task.run();
                } catch (Exception e) {
                    Callback callback = callbackWeakReference.get();
                    if (callback != null) {
                        callback.onTaskError(e);
                    }
                }
                handler.postDelayed(this, intervalMillis);
            }
        }
    };

    private final Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {
            lock.lock();
            try {
                if (isRunning) {
                    isRunning = false;
                    handler.removeCallbacks(internalRunnable);
                    Callback callback = callbackWeakReference.get();
                    if (callback != null) {
                        callback.onTaskTimeout(); // 调用超时事件回调
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    };

    public void start() {
        lock.lock();
        try {
            if (!isRunning) {
                isRunning = true;
                handler.postDelayed(internalRunnable, startTimeMillis);
                handler.postDelayed(timeoutRunnable, timeoutMillis);
                Callback callback = callbackWeakReference.get();
                if (callback != null) {
                    callback.onTaskStarted();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        lock.lock();
        try {
            if (isRunning) {
                isRunning = false;
                handler.removeCallbacks(internalRunnable);
                handler.removeCallbacks(timeoutRunnable);
                Callback callback = callbackWeakReference.get();
                if (callback != null) {
                    callback.onTaskStopped();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isRunning() {
        lock.lock();
        try {
            return isRunning;
        } finally {
            lock.unlock();
        }
    }
}


