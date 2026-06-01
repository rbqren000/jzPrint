package com.mx.mxSdk.imageCompress;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by RBQ on 2025/8/15.
 * <p>
 * 单张图片压缩器
 */
public class SingleImageCompressor {
    private static final String TAG = "SingleImageCompressor";
    
    // 线程池管理
    private static ExecutorService EXECUTOR;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 60;
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    
    // 引用计数，用于自动管理线程池生命周期
    private static final AtomicInteger ACTIVE_TASKS = new AtomicInteger(0);
    
    // 状态控制
    private final AtomicBoolean isCompressing = new AtomicBoolean(false);
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    
    // 内存监控
    private final Runtime runtime = Runtime.getRuntime();
    private static final long LOW_MEMORY_THRESHOLD = 20 * 1024 * 1024; // 20MB
    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final File sourceFile;
    private final CompressConfig config;
    private final CompressListener listener;
    private final File targetDir;
    
    // 进度控制
    private long lastProgressUpdateTime = 0;
    private static final long MIN_PROGRESS_UPDATE_INTERVAL = 100; // 最小进度更新间隔(毫秒)

    private SingleImageCompressor(Builder builder) {
        this.sourceFile = builder.sourceFile;
        this.config = builder.config;
        this.listener = builder.listener;
        this.targetDir = builder.targetDir;
        
        // 确保线程池已初始化
        ensureExecutorCreated();
        
        // 增加活跃任务计数
        ACTIVE_TASKS.incrementAndGet();
    }
    
    /**
     * 确保线程池已创建
     */
    private static synchronized void ensureExecutorCreated() {
        if (EXECUTOR == null || EXECUTOR.isShutdown()) {
            EXECUTOR = createIOOptimizedThreadPool();
            Log.d(TAG, "创建新的压缩线程池");
        }
    }
    
    /**
     * 创建IO线程池
     */
    private static ExecutorService createIOOptimizedThreadPool() {
        return new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE,
            KEEP_ALIVE_SECONDS,
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
     * 使用上下文初始化构造器
     *
     * @param context Context
     * @return Builder
     */
    public static Builder with(Context context) {
        return new Builder(context);
    }

    /**
     * 关闭线程池
     */
    public static void shutdown() {
        if (EXECUTOR != null && !EXECUTOR.isShutdown()) {
            EXECUTOR.shutdown();
            try {
                // 等待一段时间让任务完成
                if (!EXECUTOR.awaitTermination(3, TimeUnit.SECONDS)) {
                    // 如果超时，则强制关闭
                    EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                // 如果等待被中断，直接强制关闭
                EXECUTOR.shutdownNow();
                // 恢复中断状态
                Thread.currentThread().interrupt();
            }
            EXECUTOR = null;
        }
    }
    
    /**
     * 检查内存状态
     */
    private void checkMemory() {
        long freeMemory = runtime.freeMemory();
        if (freeMemory < LOW_MEMORY_THRESHOLD) {
            Log.w(TAG, "内存不足，可能影响压缩性能: " + (freeMemory / 1024 / 1024) + "MB");
            // 触发GC尝试释放内存
            System.gc();
        }
    }
    
    /**
     * 取消压缩任务
     */
    public void cancel() {
        isCancelled.set(true);
    }
    
    /**
     * 是否正在压缩
     */
    public boolean isCompressing() {
        return isCompressing.get();
    }
    
    /**
     * 是否已取消
     */
    public boolean isCancelled() {
        return isCancelled.get();
    }

    /**
     * 启动压缩任务
     */
    public void launch() {
        // 检查是否已经在压缩中
        if (!isCompressing.compareAndSet(false, true)) {
            if (listener != null) {
                mainHandler.post(() -> listener.onError(new IllegalStateException("压缩任务已在进行中")));
            }
            return;
        }
        
        // 重置取消状态
        isCancelled.set(false);
        
        // 1. 在主线程回调 onStart
        if (listener != null) {
            mainHandler.post(listener::onStart);
        }

        // 2. 在后台线程执行压缩
        EXECUTOR.execute(() -> {
            try {
                // 检查内存状态
                checkMemory();
                
                // 检查并创建目标目录
                if (!targetDir.exists() && !targetDir.mkdirs()) {
                    throw new CompressException("无法创建目标目录: " + targetDir.getAbsolutePath());
                }

                // 检查输入参数
                if (sourceFile == null || !sourceFile.exists()) {
                    throw new CompressException("源文件不存在或为空");
                }
                if (config.getMaxWidth() <= 0 || config.getMaxHeight() <= 0 || config.getMaxSize() <= 0) {
                    throw new CompressException("配置参数必须为正值");
                }
                
                // 检查是否已取消
                if (isCancelled.get()) {
                    Log.i(TAG, "压缩任务已被取消");
                    return;
                }

                // 创建压缩引擎
                CompressEngine engine = new CompressEngine(sourceFile, targetDir, config);
                
                // 添加进度监听
                engine.setProgressListener(new CompressEngine.ProgressListener() {
                    @Override
                    public void onProgress(float progress) {
                        // 控制回调频率，避免UI线程过载
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastProgressUpdateTime >= MIN_PROGRESS_UPDATE_INTERVAL) {
                            lastProgressUpdateTime = currentTime;
                            if (listener instanceof CompressProgressListener) {
                                mainHandler.post(() -> ((CompressProgressListener) listener).onProgress(progress));
                            }
                        }
                    }
                });

                // 3. 核心压缩逻辑
                File resultFile = engine.compress();
                
                // 检查是否已取消
                if (isCancelled.get()) {
                    Log.i(TAG, "压缩任务已被取消");
                    // 删除已生成的文件
                    if (resultFile != null && resultFile.exists()) {
                        resultFile.delete();
                    }
                    return;
                }

                // 4. 在主线程回调 onSuccess
                if (listener != null) {
                    final File finalResultFile = resultFile;
                    mainHandler.post(() -> listener.onSuccess(finalResultFile));
                }
            } catch (Exception e) {
                // 如果不是因为取消导致的错误，才回调错误
                if (!isCancelled.get()) {
                    // 5. 在主线程回调 onError
                    if (listener != null) {
                        mainHandler.post(() -> listener.onError(e));
                    }
                }
            } finally {
                // 重置状态
                isCompressing.set(false);
                
                // 减少活跃任务计数
                int remainingTasks = ACTIVE_TASKS.decrementAndGet();
                
                // 如果没有活跃任务，考虑关闭线程池
                if (remainingTasks == 0) {
                    considerShutdownExecutor();
                }
            }
        });
    }
    
    /**
     * 考虑关闭线程池
     */
    private static void considerShutdownExecutor() {
        // 创建一个弱引用的延迟任务，避免阻止GC
        final WeakReference<ExecutorService> executorRef = new WeakReference<>(EXECUTOR);
        
        // 延迟30秒后检查，如果仍然没有活跃任务，则关闭线程池
        Thread shutdownThread = new Thread(() -> {
            try {
                Thread.sleep(30000); // 30秒
                
                // 如果没有新任务被创建，且引用仍然有效
                if (ACTIVE_TASKS.get() == 0) {
                    ExecutorService executor = executorRef.get();
                    if (executor != null && executor == EXECUTOR && !executor.isShutdown()) {
                        synchronized (SingleImageCompressor.class) {
                            if (EXECUTOR == executor && !EXECUTOR.isShutdown()) {
                                Log.d(TAG, "自动关闭空闲的压缩线程池");
                                EXECUTOR.shutdown();
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "ImageCompress-Shutdown");
        
        // 设置为守护线程，防止阻止应用退出
        shutdownThread.setDaemon(true);
        shutdownThread.start();
    }

    /**
     * 链式调用构造器
     */
    public static class Builder {
        private final Context context;
        private File sourceFile;
        private CompressConfig config;
        private CompressListener listener;
        private File targetDir;

        Builder(Context context) {
            this.context = context.getApplicationContext();
            this.config = new CompressConfig(); // 使用默认配置
            this.targetDir = context.getCacheDir(); // 默认输出到缓存目录
        }

        public Builder load(File file) {
            this.sourceFile = file;
            return this;
        }

        public Builder load(String path) {
            this.sourceFile = new File(path);
            return this;
        }

        /**
         * 加载URI资源（支持content://、file://等）
         * @param uri 图片URI
         * @return Builder
         */
        public Builder loadUri(Uri uri) {
            try {
                this.sourceFile = convertUriToFile(uri);
                return this;
            } catch (IOException e) {
                throw new RuntimeException("Failed to convert URI to file: " + uri, e);
            }
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

        public Builder setConfig(CompressConfig config) {
            if (config != null) {
                this.config = new CompressConfig(config); // 创建副本，确保配置隔离
            } else {
                this.config = new CompressConfig(); // 如果传入null，则使用默认配置
            }
            return this;
        }

        public Builder setListener(CompressListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder setTargetDir(String path) {
            this.targetDir = new File(path);
            return this;
        }

        public SingleImageCompressor build() {
            return new SingleImageCompressor(this);
        }

        public void launch() {
            build().launch();
        }
    }
}