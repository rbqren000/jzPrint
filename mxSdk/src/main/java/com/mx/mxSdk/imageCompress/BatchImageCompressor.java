package com.mx.mxSdk.imageCompress;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by RBQ on 2025/8/15.
 * <p>
 * 批量图片压缩器
 */
public class BatchImageCompressor {
    
    private static final String TAG = "BatchImageCompressor";

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 线程池管理
    private ExecutorService executorService;
    private static final int DEFAULT_BUFFER_SIZE = 8192; // 文件复制的缓冲区大小
    private final List<Future<?>> runningTasks = new ArrayList<>();
    
    // 状态控制
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final Object pauseLock = new Object(); // 专用于暂停/恢复的锁对象
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    // 统计信息
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    
    // 进度控制
    private long lastProgressUpdateTime = 0;
    private static final long MIN_PROGRESS_UPDATE_INTERVAL = 100; // 最小进度更新间隔(毫秒)
    
    // 内存监控
    private final Runtime runtime = Runtime.getRuntime();
    private static final long LOW_MEMORY_THRESHOLD = 20 * 1024 * 1024; // 20MB
    
    // 结果收集 - 使用线程安全的集合
    private final ConcurrentLinkedQueue<File> successFiles = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<BatchCompressError> errorList = new ConcurrentLinkedQueue<>();
    
    // CountDownLatch 用于精确控制完成时机
    private CountDownLatch completionLatch;
    
    // 当前任务
    private BatchCompressTask currentTask;
    
    private BatchImageCompressor(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * 使用上下文初始化构造器
     *
     * @param context Context
     * @return Builder
     */
    public static Builder with(Context context) {
        return new Builder(context);
    }
    
    
    /**
     * 开始批量压缩
     */
    public synchronized void compress(BatchCompressTask task) {
        if (task == null) {
            throw new IllegalArgumentException("批量压缩任务不能为空");
        }
        // 使用原子操作确保并发安全
        if (!isRunning.compareAndSet(false, true)) {
            mainHandler.post(() -> {
                if (task.getListener() != null) {
                    task.getListener().onBatchError(new IllegalStateException("批量压缩正在进行中，请先取消或等待完成"));
                }
            });
            return;
        }

        if (task.getTotalCount() == 0) {
            // 如果任务为空，重置运行状态并返回错误
            isRunning.set(false);
            mainHandler.post(() -> {
                if (task.getListener() != null) {
                    task.getListener().onBatchError(new IllegalArgumentException("压缩列表不能为空"));
                }
            });
            return;
        }

        // 先重置状态，再设置当前任务
        resetStateForNewRun();
        this.currentTask = task;

        // 将整个压缩流程放入后台线程，避免阻塞调用者线程（尤其是UI线程）
        new Thread(() -> {
            try {
                // 确保目标目录存在
                File targetDir = task.getTargetDir();
                if (!targetDir.exists() && !targetDir.mkdirs()) {
                    // 如果创建目录失败，回调批量错误
                    if (task.getListener() != null) {
                        Exception error = new CompressException("无法创建目标目录: " + targetDir.getAbsolutePath());
                        mainHandler.post(() -> task.getListener().onBatchError(error));
                    }
                    return;
                }

                // 创建线程池，使用更适合IO密集型任务的线程池配置
                executorService = createIOOptimizedThreadPool(task.getMaxConcurrency());
                isRunning.set(true);

                // 初始化 CountDownLatch
                completionLatch = new CountDownLatch(task.getTotalCount());

                // 回调开始
                if (task.getListener() != null) {
                    mainHandler.post(() -> task.getListener().onBatchStart(task.getTotalCount()));
                }

                // 提交所有压缩任务
                List<BatchCompressItem> items = task.getItems();
                int submittedCount = 0;
                
                for (int i = 0; i < items.size(); i++) {
                    // 如果在提交过程中被取消，则停止提交新任务
                    if (isCancelled.get()) {
                        Log.w(TAG, "Task submission cancelled at index " + i);
                        break;
                    }

                    final int index = i;
                    final BatchCompressItem item = items.get(i);
                    
                    try {
                        Future<?> future = executorService.submit(() -> compressSingleItem(index, item, task));
                        synchronized (runningTasks) {
                            runningTasks.add(future);
                        }
                        submittedCount++;
                        Log.d(TAG, "Task submitted for index " + index + ", total submitted: " + submittedCount);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to submit task for index " + index, e);
                        // 提交失败时需要减少CountDownLatch的计数
                        if (completionLatch != null) {
                            completionLatch.countDown();
                        }
                    }
                }

                Log.i(TAG, "All tasks submitted. Total: " + submittedCount + "/" + items.size());

                // 如果在提交任务循环中被取消，需要调整CountDownLatch
                if (isCancelled.get()) {
                    // 减少未提交任务的CountDownLatch计数
                    int unsubmittedCount = items.size() - submittedCount;
                    for (int i = 0; i < unsubmittedCount; i++) {
                        if (completionLatch != null) {
                            completionLatch.countDown();
                        }
                    }
                    cleanupAndReset();
                    return;
                }

                try {
                    // 等待所有任务完成，添加超时机制防止无限等待
                    Log.d(TAG, "Waiting for all tasks to complete...");
                    boolean completed = completionLatch.await(30, TimeUnit.MINUTES); // 最多等待30分钟
                    
                    if (!completed) {
                        Log.e(TAG, "Batch compression timeout after 30 minutes");
                        // 避免递归调用cancel()，直接设置取消状态并清理
                        isCancelled.set(true);
                        
                        // 唤醒可能在 pauseLock.wait() 中等待的线程
                        synchronized (pauseLock) {
                            pauseLock.notifyAll();
                        }
                        
                        // 取消所有运行中的任务
                        synchronized (runningTasks) {
                            for (Future<?> future : runningTasks) {
                                future.cancel(true);
                            }
                        }
                        
                        // 回调超时错误
                        final BatchCompressListener listener = currentTask != null ? currentTask.getListener() : null;
                        if (listener != null) {
                            mainHandler.post(() -> listener.onBatchError(new RuntimeException("批量压缩超时")));
                        }
                        
                        cleanupAndReset();
                        return;
                    }
                    
                    Log.d(TAG, "All tasks completed successfully");
                } catch (InterruptedException e) {
                    // 如果等待被中断，说明任务可能被外部取消
                    Thread.currentThread().interrupt(); // 恢复中断状态
                    Log.w(TAG, "Batch compression interrupted", e);
                    
                    if (!isCancelled.get()) {
                        // 如果不是主动取消，则作为错误处理
                        mainHandler.post(() -> {
                            if (currentTask != null && currentTask.getListener() != null) {
                                currentTask.getListener().onBatchError(e);
                            }
                        });
                    }
                    // 清理并返回
                    cleanupAndReset();
                    return;
                }

                // 任务正常完成，将 finishBatch 提交到主线程消息队列，以确保它在所有 onSingleSuccess/onProgress 回调之后执行
                mainHandler.post(this::finishBatch);

            } catch (Exception e) {
                // 如果在启动或提交任务时发生异常（如RejectedExecutionException），回调批量错误
                if (!isCancelled.get()) {
                    final BatchCompressListener listener = currentTask != null ? currentTask.getListener() : null;
                    if (listener != null) {
                        mainHandler.post(() -> listener.onBatchError(e));
                    }
                }
                // 统一清理
                cleanupAndReset();
            }
        }).start();
    }
    
    /**
     * 启动批量压缩任务
     */
    public void launch() {
        if (currentTask == null) {
            throw new IllegalStateException("没有设置批量压缩任务，请先调用build()");
        }
        if (isRunning.get()) {
            throw new IllegalStateException("批量压缩正在进行中，请先取消或等待完成");
        }
        compress(currentTask);
    }
    
    /**
     * 暂停批量压缩
     */
    public synchronized void pause() {
        if (!isRunning.get()) {
            return;
        }
        isPaused.set(true);
    }
    
    /**
     * 恢复批量压缩
     */
    public synchronized void resume() {
        if (!isRunning.get()) {
            return;
        }
        isPaused.set(false);
        synchronized (pauseLock) {
            pauseLock.notifyAll(); // 唤醒所有等待的线程
        }
    }
    
    /**
     * 取消批量压缩
     */
    public synchronized void cancel() {
        if (!isRunning.get() || isCancelled.get()) {
            return;
        }
        
        Log.d(TAG, "Cancelling batch compression...");
        isCancelled.set(true);

        // 唤醒可能在 pauseLock.wait() 中等待的线程
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }

        // 取消所有运行中的任务
        synchronized (runningTasks) {
            for (Future<?> future : runningTasks) {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            }
        }

        // 安全地处理 CountDownLatch，避免无限循环
        if (completionLatch != null) {
            long remainingCount = completionLatch.getCount();
            Log.d(TAG, "Forcing completion of " + remainingCount + " remaining tasks");
            
            // 使用循环计数器防止无限循环
            int maxIterations = (int) remainingCount + 10; // 添加安全边界
            int iterations = 0;
            
            while (completionLatch.getCount() > 0 && iterations < maxIterations) {
                completionLatch.countDown();
                iterations++;
            }
            
            if (iterations >= maxIterations) {
                Log.w(TAG, "CountDownLatch force completion reached max iterations");
            }
        }

        final BatchCompressListener listener = currentTask != null ? currentTask.getListener() : null;
        final int completed = completedCount.get();

        // 回调取消
        if (listener != null) {
            mainHandler.post(() -> listener.onBatchCancelled(completed));
        }

        // 使用新方法统一清理
        cleanupAndReset();
    }
    
    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * 是否已暂停
     */
    public boolean isPaused() {
        return isPaused.get();
    }
    
    /**
     * 是否已取消
     */
    public boolean isCancelled() {
        return isCancelled.get();
    }
    
    /**
     * 压缩单个项目
     */
    private void compressSingleItem(int index, BatchCompressItem item, BatchCompressTask task) {
        boolean shouldCountDown = true;
        
        try {
            // 检查是否被取消
            if (isCancelled.get()) {
                return;
            }
            
            // 检查暂停状态
            waitIfPaused();
            
            // 再次检查取消状态（暂停期间可能被取消）
            if (isCancelled.get()) {
                return;
            }
            
            String inputPath = item.getInputPath();
            
            // 回调单个开始
            if (task.getListener() != null) {
                mainHandler.post(() -> task.getListener().onSingleStart(index, inputPath));
            }
            
            try {
                // 确定使用的配置
                CompressConfig config = item.hasCustomConfig() ? 
                    item.getCustomConfig() : task.getGlobalConfig();
                
                // 确定输出目录
                File targetDir = task.getTargetDir();
                if (item.hasCustomOutputPath()) {
                    File customOutput = new File(item.getOutputPath());
                    targetDir = customOutput.getParentFile();
                    if (targetDir == null) {
                        targetDir = task.getTargetDir();
                    }
                    // 确保自定义输出目录存在
                    if (!targetDir.exists() && !targetDir.mkdirs()) {
                        throw new CompressException("无法创建输出目录: " + targetDir.getAbsolutePath());
                    }
                }
                
                // 执行压缩前再次检查取消状态
                if (isCancelled.get()) {
                    return;
                }
                
                // 执行压缩
                File sourceFile = new File(inputPath);
                if (!sourceFile.exists()) {
                    throw new CompressException("源文件不存在: " + inputPath);
                }
                
                CompressEngine engine = new CompressEngine(sourceFile, targetDir, config);
                File resultFile = engine.compress();
                
                // 压缩完成后再次检查取消状态
                if (isCancelled.get()) {
                    // 如果被取消，删除已生成的文件
                    if (resultFile != null && resultFile.exists()) {
                        resultFile.delete();
                    }
                    return;
                }
                
                // 如果指定了自定义输出路径，需要重命名文件
                if (item.hasCustomOutputPath()) {
                    File customOutputFile = new File(item.getOutputPath());
                    if (!resultFile.renameTo(customOutputFile)) {
                        // 重命名失败，尝试复制
                        copyFile(resultFile, customOutputFile);
                        if (!resultFile.delete()) {
                            Log.w(TAG, "Failed to delete temp file: " + resultFile.getAbsolutePath());
                        }
                        resultFile = customOutputFile;
                    }
                }
                
                // 记录成功结果 - ConcurrentLinkedQueue是线程安全的，无需同步
                successFiles.add(resultFile);
                completedCount.incrementAndGet();

                // 回调单个成功
                if (task.getListener() != null) {
                    final File finalResultFile = resultFile;
                    mainHandler.post(() -> task.getListener().onSingleSuccess(index, inputPath, finalResultFile));
                }

                // 回调进度更新（添加频率控制，避免UI线程过载）
                updateProgress(task);
                
            } catch (Exception e) {
                Log.e(TAG, "Compression failed for item " + index + ": " + item.getInputPath(), e);
                
                // 记录错误 - ConcurrentLinkedQueue是线程安全的，无需同步
                BatchCompressError error = new BatchCompressError(index, inputPath, e);
                errorList.add(error);
                errorCount.incrementAndGet();
                
                // 回调单个失败
                if (task.getListener() != null) {
                    mainHandler.post(() -> task.getListener().onSingleError(index, inputPath, e));
                }
                
                // 如果设置了遇到错误就停止
                if (task.isStopOnError()) {
                    Log.w(TAG, "Stopping batch compression due to error");
                    cancel();
                }
            }
            
        } catch (Exception e) {
            // 捕获所有未预期的异常
            Log.e(TAG, "Unexpected error in compressSingleItem for index " + index, e);
        } finally {
            // 确保CountDownLatch总是被递减，防止死锁
            if (shouldCountDown && completionLatch != null) {
                completionLatch.countDown();
                Log.d(TAG, "CountDown executed for item " + index + ", remaining: " + completionLatch.getCount());
            }
        }
    }
    
    /**
     * 更新进度信息
     */
    private void updateProgress(BatchCompressTask task) {
        int completed = completedCount.get();
        int total = task.getTotalCount();
        int error = errorCount.get();

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastProgressUpdateTime >= MIN_PROGRESS_UPDATE_INTERVAL) {
            lastProgressUpdateTime = currentTime;
            if (task.getListener() != null) {
                mainHandler.post(() -> task.getListener().onProgress(completed, total, error));
            }
            Log.d(TAG, "Progress update - completed: " + completed + ", total: " + total + ", errors: " + error);
            
            // 内存监控（降低频率，避免过度检查）
            if (completed % 5 == 0) { // 每5个任务检查一次内存
                checkMemory();
            }
        }
    }
    
    /**
     * 等待暂停状态结束
     */
    private void waitIfPaused() {
        while (isPaused.get() && !isCancelled.get()) {
            synchronized (pauseLock) {
                try {
                    // 使用超时等待，避免无限期阻塞
                    pauseLock.wait(5000); // 最多等待5秒
                    
                    // 超时后重新检查状态，防止死锁
                    if (isPaused.get() && !isCancelled.get()) {
                        Log.w(TAG, "Wait timeout, rechecking pause state");
                    }
                } catch (InterruptedException e) {
                    // 被中断（如取消任务时），恢复中断状态并退出
                    Thread.currentThread().interrupt();
                    Log.d(TAG, "Wait interrupted, exiting pause wait");
                    return;
                }
            }
        }
    }
    
    /**
     * 完成批量压缩
     */
    private void finishBatch() {
        Log.i(TAG, "finishBatch");
        if (isCancelled.get()) {
            return; // 如果已被取消，不执行完成回调
        }
        
        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        // 在resetState之前捕获监听器和数据
        final BatchCompressListener listener = currentTask != null ? currentTask.getListener() : null;
        final List<File> finalSuccessFiles = new ArrayList<>(successFiles);
        final List<BatchCompressError> finalErrorList = new ArrayList<>(errorList);

        // 确保最终进度更新
        if (listener != null && currentTask != null) {
            final int totalCount = currentTask.getTotalCount();
            final int successCount = finalSuccessFiles.size();
            final int errorCount = finalErrorList.size();
            mainHandler.post(() -> listener.onProgress(successCount, totalCount, errorCount));
        }

        // 回调完成
        if (listener != null) {
            Log.i(TAG, "---调用事件【onBatchComplete】----");
            mainHandler.post(() -> listener.onBatchComplete(finalSuccessFiles, finalErrorList));
        }
        
        // 重置状态，确保isRunning被正确设置
        resetState();
    }
    
    /**
     * 重置状态
     */
    private void resetState() {
        resetStateForNewRun();
        currentTask = null;
        // 确保isRunning状态被正确重置
        isRunning.set(false);
    }

    /**
     * 为新的运行重置状态，但不清除当前任务
     */
    private void resetStateForNewRun() {
        isPaused.set(false);
        isCancelled.set(false);
        completedCount.set(0);
        errorCount.set(0);
        lastProgressUpdateTime = 0;

        successFiles.clear();
        errorList.clear();
        synchronized (runningTasks) {
            runningTasks.clear();
        }
        // 清理 CountDownLatch 相关资源
        completionLatch = null;
    }

    /**
     * 检查内存状态，如果内存不足则暂停任务
     */
    private void checkMemory() {
        try {
            // 先尝试GC，获取更准确的内存状态
            System.gc();
            
            long freeMemory = runtime.freeMemory();
            long totalMemory = runtime.totalMemory();
            long maxMemory = runtime.maxMemory();
            long usedMemory = totalMemory - freeMemory;
            long availableMemory = maxMemory - usedMemory;
            
            Log.d(TAG, String.format("Memory status - Available: %dMB, Used: %dMB, Free: %dMB", 
                    availableMemory / 1024 / 1024, usedMemory / 1024 / 1024, freeMemory / 1024 / 1024));
            
            // 使用可用内存而不是空闲内存来判断
            if (availableMemory < LOW_MEMORY_THRESHOLD && !isPaused.get() && !isCancelled.get()) {
                Log.w(TAG, "内存不足，自动暂停压缩任务: 可用内存 " + (availableMemory / 1024 / 1024) + "MB");
                pause();
                
                // 启动一个延迟任务，稍后尝试恢复
                new Thread(() -> {
                    try {
                        // 等待一段时间让GC有机会回收内存
                        Thread.sleep(3000);
                        
                        // 如果任务已被取消，不再尝试恢复
                        if (isCancelled.get() || !isRunning.get()) {
                            return;
                        }
                        
                        // 再次检查内存
                        System.gc();
                        long newAvailableMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
                        
                        if (newAvailableMemory > LOW_MEMORY_THRESHOLD * 2) {
                            Log.i(TAG, "内存已恢复，继续压缩任务: 可用内存 " + (newAvailableMemory / 1024 / 1024) + "MB");
                            resume();
                        } else {
                            Log.w(TAG, "内存仍然不足，保持暂停状态: 可用内存 " + (newAvailableMemory / 1024 / 1024) + "MB");
                            // 可以考虑降低并发数或取消任务
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Log.d(TAG, "Memory recovery thread interrupted");
                    }
                }).start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking memory status", e);
        }
    }
    
    /**
     * 创建IO线程池
     */
    private ExecutorService createIOOptimizedThreadPool(int maxConcurrency) {
        // 对于IO密集型任务，线程数可以适当多一些
        int corePoolSize = Math.max(2, maxConcurrency);
        int maximumPoolSize = Math.max(4, maxConcurrency * 2);
        long keepAliveTime = 60L;
        
        return new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "ImageCompress-Worker");
                    // 设置为后台线程，不阻止应用退出
                    t.setDaemon(true);
                    return t;
                }
            }
        );
    }
    
    /**
     * 清理资源并重置状态
     */
    private void cleanupAndReset() {
        // 关闭线程池，先尝试优雅关闭，给予任务完成的机会
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                // 等待一段时间让任务完成
                if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                    // 如果超时，则强制关闭
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                // 如果等待被中断，直接强制关闭
                executorService.shutdownNow();
                // 恢复中断状态
                Thread.currentThread().interrupt();
            }
        }
        resetState();
    }
    
    /**
     * 复制文件
     */
    private void copyFile(File source, File target) throws IOException {
        // 使用缓冲流提高IO性能
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(source));
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target))) {
            
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            // 确保所有数据都写入磁盘
            bos.flush();
        }
    }
    
    /**
     * 链式调用构造器
     */
    public static class Builder {
        private final Context context;
        private BatchCompressTask.Builder taskBuilder;
        
        Builder(Context context) {
            this.context = context.getApplicationContext();
            this.taskBuilder = new BatchCompressTask.Builder(context);
        }
        
        /**
         * 加载单个文件路径
         */
        public Builder load(String path) {
            this.taskBuilder.addPath(path);
            return this;
        }
        
        /**
         * 加载单个文件
         */
        public Builder load(File file) {
            this.taskBuilder.addPath(file.getAbsolutePath());
            return this;
        }
        
        /**
         * 加载多个文件路径
         */
        public Builder loadPaths(List<String> paths) {
            this.taskBuilder.addPaths(paths);
            return this;
        }
        
        /**
         * 加载多个文件
         */
        public Builder loadFiles(List<File> files) {
            if (files != null) {
                for (File file : files) {
                    this.taskBuilder.addPath(file.getAbsolutePath());
                }
            }
            return this;
        }

        /**
         * 加载多个URI（支持file://和content://）
         * @param uris URI列表
         * @return Builder
         */
        public Builder loadUris(List<Uri> uris) {
            if (uris == null || uris.isEmpty()) {
                return this;
            }
            
            for (Uri uri : uris) {
                try {
                    File file = convertUriToFile(uri);
                    if (file != null && file.exists()) {
                        this.taskBuilder.addPath(file.getAbsolutePath());
                    } else {
                        Log.w(TAG, "URI转换的文件不存在或为空: " + uri);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "URI转换失败: " + uri, e);
                    // 继续处理其他URI，不因单个失败而中断
                }
            }
            return this;
        }

        /**
         * 将URI转换为临时文件
         * @param uri 输入URI
         * @return 临时文件
         * @throws IOException IO异常
         */
        private File convertUriToFile(Uri uri) throws IOException {
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
                    return createTempFileFromContentUri(uri);

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
        private File createTempFileFromContentUri(Uri uri) throws IOException {
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
        
        /**
         * 添加批量压缩项目
         */
        public Builder addItem(BatchCompressItem item) {
            this.taskBuilder.addItem(item);
            return this;
        }
        
        /**
         * 添加多个批量压缩项目
         */
        public Builder addItems(List<BatchCompressItem> items) {
            this.taskBuilder.addItems(items);
            return this;
        }
        
        /**
         * 设置全局压缩配置
         */
        public Builder setConfig(CompressConfig config) {
            this.taskBuilder.setGlobalConfig(config);
            return this;
        }
        
        /**
         * 设置批量压缩监听器
         */
        public Builder setListener(BatchCompressListener listener) {
            this.taskBuilder.setListener(listener);
            return this;
        }
        
        /**
         * 设置目标目录
         */
        public Builder setTargetDir(String path) {
            this.taskBuilder.setTargetDir(path);
            return this;
        }
        
        /**
         * 设置目标目录
         */
        public Builder setTargetDir(File dir) {
            this.taskBuilder.setTargetDir(dir);
            return this;
        }
        
        /**
         * 设置最大并发数
         */
        public Builder setMaxConcurrency(int maxConcurrency) {
            this.taskBuilder.setMaxConcurrency(maxConcurrency);
            return this;
        }
        
        /**
         * 设置是否遇到错误时停止
         */
        public Builder setStopOnError(boolean stopOnError) {
            this.taskBuilder.setStopOnError(stopOnError);
            return this;
        }
        
        /**
         * 构建批量压缩器
         */
        public BatchImageCompressor build() {
            BatchImageCompressor compressor = new BatchImageCompressor(context);
            compressor.currentTask = taskBuilder.build();
            return compressor;
        }
        
        /**
         * 直接启动批量压缩
         */
        public void launch() {
            build().launch();
        }
    }
}
