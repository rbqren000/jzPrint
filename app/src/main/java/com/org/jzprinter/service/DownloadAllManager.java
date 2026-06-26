package com.org.jzprinter.service;

import android.content.Context;
import android.util.Log;

import com.org.jzprinter.database.entity.StudentEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadAllManager {
    private static final String TAG = "DownloadAllManager";
    private static final int MAX_CONCURRENT = 3;
    private static final int MAX_RETRY = 3;

    private final ExecutorService executor;
    private final AtomicInteger nextIndex = new AtomicInteger(0);
    private final AtomicInteger activeCount = new AtomicInteger(0);
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private final List<DownloadTask> failedList = new ArrayList<>();
    private final AtomicBoolean completionCalled = new AtomicBoolean(false);
    private volatile boolean isCancelled = false;
    private volatile int totalTasks = 0;

    public interface DownloadTask {
        String getTargetId();
        String getBusinessId();
        String getDisplayName();
    }

    public static class StudentTask implements DownloadTask {
        private final StudentEntity student;

        public StudentTask(StudentEntity student) {
            this.student = student;
        }

        @Override public String getTargetId() { return student.getStudentId(); }
        @Override public String getBusinessId() { return student.getBusinessId(); }
        @Override public String getDisplayName() { return student.getStudentName(); }

        public StudentEntity getStudent() { return student; }
    }

    public static class PrepareCodeTask implements DownloadTask {
        private final String prepareCode;
        private final String businessId;

        public PrepareCodeTask(String prepareCode, String businessId) {
            this.prepareCode = prepareCode;
            this.businessId = businessId;
        }

        @Override public String getTargetId() { return prepareCode; }
        @Override public String getBusinessId() { return businessId; }
        @Override public String getDisplayName() { return prepareCode; }
    }

    public DownloadAllManager() {
        this.executor = Executors.newFixedThreadPool(MAX_CONCURRENT);
    }

    public void downloadAll(Context context, String schoolId, String editionId,
                           int editionType, List<DownloadTask> tasks,
                           DownloadAllCallback callback) {
        if (tasks == null || tasks.isEmpty()) {
            if (callback != null) callback.onComplete(0, 0);
            return;
        }

        totalTasks = tasks.size();
        nextIndex.set(0);
        completedCount.set(0);
        failedCount.set(0);
        failedList.clear();
        completionCalled.set(false);
        isCancelled = false;

        Log.d(TAG, "start batch download, total=" + totalTasks);

        int batchSize = Math.min(MAX_CONCURRENT, tasks.size());
        for (int i = 0; i < batchSize; i++) {
            startNextDownload(context, schoolId, editionId, editionType, tasks, callback);
        }
    }

    private void startNextDownload(Context context, String schoolId, String editionId,
                                   int editionType, List<DownloadTask> tasks,
                                   DownloadAllCallback callback) {
        int index = nextIndex.getAndIncrement();
        if (isCancelled || index >= tasks.size()) {
            checkCompletion(callback);
            return;
        }

        DownloadTask task = tasks.get(index);
        activeCount.incrementAndGet();

        downloadWithRetry(context, schoolId, editionId, editionType, task, 0,
            new RetryCallback() {
                @Override
                public void onSuccess(String path) {
                    activeCount.decrementAndGet();
                    int completed = completedCount.incrementAndGet();
                    if (callback != null) {
                        callback.onProgress(completed, totalTasks, task.getDisplayName());
                    }
                    startNextDownload(context, schoolId, editionId, editionType, tasks, callback);
                }

                @Override
                public void onFailed() {
                    activeCount.decrementAndGet();
                    int failed = failedCount.incrementAndGet();
                    synchronized (failedList) {
                        failedList.add(task);
                    }
                    if (callback != null) {
                        callback.onFailed(task, "download failed");
                    }
                    startNextDownload(context, schoolId, editionId, editionType, tasks, callback);
                }
            });
    }

    private void downloadWithRetry(Context context, String schoolId, String editionId,
                                   int editionType, DownloadTask task, int retryCount,
                                   RetryCallback retryCallback) {
        if (isCancelled) {
            retryCallback.onFailed();
            return;
        }

        DownloadService.downloadAndExtract(context, schoolId, task.getBusinessId(),
            editionType, editionId, task.getTargetId(),
            new DownloadService.DownloadAndExtractCallback() {
                @Override
                public void onDownloadProgress(int percentage) {}

                @Override
                public void onComplete(String path) {
                    Log.d(TAG, "success: " + task.getDisplayName());
                    retryCallback.onSuccess(path);
                }

                @Override
                public void onAlreadyExists(String path) {
                    Log.d(TAG, "already exists: " + task.getDisplayName());
                    retryCallback.onSuccess(path);
                }

                @Override
                public void onError(String errorMsg) {
                    Log.e(TAG, "failed(retry=" + retryCount + "): " + task.getDisplayName());
                    if (retryCount < MAX_RETRY - 1 && !isCancelled) {
                        executor.submit(() -> {
                            try {
                                Thread.sleep(1000L * (retryCount + 1));
                                downloadWithRetry(context, schoolId, editionId, editionType,
                                    task, retryCount + 1, retryCallback);
                            } catch (InterruptedException e) {
                                retryCallback.onFailed();
                            }
                        });
                    } else {
                        retryCallback.onFailed();
                    }
                }
            });
    }

    private void checkCompletion(DownloadAllCallback callback) {
        int completed = completedCount.get();
        int failed = failedCount.get();
        if (completed + failed == totalTasks) {
            if (completionCalled.compareAndSet(false, true)) {
                Log.d(TAG, "batch download complete, success=" + completed + ", failed=" + failed);
                if (callback != null) {
                    callback.onComplete(completed, failed);
                }
            }
        }
    }

    public void cancel() {
        Log.d(TAG, "cancel batch download");
        isCancelled = true;
    }

    public List<DownloadTask> getFailedList() {
        synchronized (failedList) {
            return new ArrayList<>(failedList);
        }
    }

    private interface RetryCallback {
        void onSuccess(String path);
        void onFailed();
    }

    public interface DownloadAllCallback {
        void onProgress(int completed, int total, String itemName);
        void onFailed(DownloadTask task, String error);
        void onComplete(int successCount, int failedCount);
    }
}
