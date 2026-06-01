package com.mx.mxSdk.imageCompress;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by RBQ on 2025/8/15.
 * <p>
 * 核心压缩引擎
 */
class CompressEngine {
    
    private static final String TAG = "CompressEngine";

    /**
     * 进度监听接口
     */
    interface ProgressListener {
        /**
         * 进度回调
         * @param progress 进度值，范围0.0-1.0
         */
        void onProgress(float progress);
    }

    private final File sourceFile;
    private final File targetDir;
    private final CompressConfig config;
    
    // 缓存图片边界信息，避免重复解码
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    
    // 进度监听
    private ProgressListener progressListener;
    
    // 用于跟踪需要回收的Bitmap，确保资源正确释放
    private final List<Bitmap> bitmapsToRecycle = new ArrayList<>();

    CompressEngine(File sourceFile, File targetDir, CompressConfig config) {
        this.sourceFile = sourceFile;
        this.targetDir = targetDir;
        this.config = config;
    }
    
    /**
     * 设置进度监听器
     * @param listener 进度监听器
     */
    void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }
    
    /**
     * 更新进度
     * @param progress 进度值，范围0.0-1.0
     */
    private void updateProgress(float progress) {
        if (progressListener != null) {
            progressListener.onProgress(Math.max(0, Math.min(1, progress)));
        }
    }
    
    /**
     * 添加需要回收的Bitmap到跟踪列表
     */
    private void trackBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            synchronized (bitmapsToRecycle) {
                bitmapsToRecycle.add(bitmap);
            }
        }
    }
    
    /**
     * 回收所有跟踪的Bitmap
     */
    private void recycleAllBitmaps() {
        synchronized (bitmapsToRecycle) {
            for (Bitmap bitmap : bitmapsToRecycle) {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
            bitmapsToRecycle.clear();
        }
    }

    /**
     * 执行压缩
     *
     * @return 压缩后的文件
     * @throws IOException       文件读写异常
     * @throws CompressException 压缩异常
     */
    File compress() throws IOException, CompressException {
        try {
            // 更新进度 - 开始
            updateProgress(0.0f);
            
            // 步骤 0: 权限和文件检查
            checkFilePermissions();
            
            // 步骤 1: 预检，如果原始文件已经符合所有要求，复制到目标目录
            if (sourceFile.length() <= config.getMaxSize() && isOriginalSizeValid()) {
                // 更新进度 - 直接复制
                updateProgress(0.5f);
                File result = copyFileToTarget(sourceFile);
                updateProgress(1.0f);
                return result;
            }

            // 步骤 2: 获取原始尺寸（使用缓存避免重复解码）
            updateProgress(0.1f);
            ensureBoundsCached();
            int originalWidth = cachedWidth;
            int originalHeight = cachedHeight;

            if (originalWidth <= 0 || originalHeight <= 0) {
                throw new CompressException("无法解码图片边界，尺寸无效。");
            }

            // 步骤 3: 计算保持宽高比下的最大目标尺寸
            float widthRatio = (float) originalWidth / config.getMaxWidth();
            float heightRatio = (float) originalHeight / config.getMaxHeight();
            float ratio = Math.max(widthRatio, heightRatio);

            int targetWidth = (int) (originalWidth / ratio);
            int targetHeight = (int) (originalHeight / ratio);

            // 步骤 4: 使用inSampleSize加载初始Bitmap以提高内存效率
            updateProgress(0.2f);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateInSampleSize(originalWidth, originalHeight, targetWidth, targetHeight);
            options.inJustDecodeBounds = false;
            Bitmap sourceBitmap = BitmapFactory.decodeFile(sourceFile.getAbsolutePath(), options);

            if (sourceBitmap == null) {
                throw new CompressException("无法将文件解码为位图: " + sourceFile.getAbsolutePath());
            }
            
            // 跟踪源Bitmap
            trackBitmap(sourceBitmap);

            // 步骤 5: 多维度迭代搜索
            updateProgress(0.3f);
            double bestScore = 0;
            ByteArrayOutputStream bestCompressedData = null;

            // 从100%到配置的最小尺寸比例进行迭代搜索
            float totalScales = (1.0f - config.getMinScale()) / config.getScaleStep() + 1;
            float currentScaleIndex = 0;
            
            for (float scale = 1.0f; scale >= config.getMinScale(); scale -= config.getScaleStep()) {
                // 更新搜索进度
                currentScaleIndex++;
                float searchProgress = currentScaleIndex / totalScales;
                updateProgress(0.3f + searchProgress * 0.5f); // 搜索过程占总进度的50%
                
                int currentWidth = (int) (targetWidth * scale);
                int currentHeight = (int) (targetHeight * scale);

                // 配置化最小尺寸限制，避免硬编码
                int minDimension = 50; // 可以后续移到config中
                if (currentWidth < minDimension || currentHeight < minDimension) {
                    break; // 如果尺寸过小则停止
                }

                Bitmap scaledBitmap = null;
                try {
                    // 将源位图缩放到当前迭代的尺寸
                    Matrix matrix = new Matrix();
                    float sx = (float) currentWidth / sourceBitmap.getWidth();
                    float sy = (float) currentHeight / sourceBitmap.getHeight();
                    matrix.setScale(sx, sy);
                    scaledBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);
                    
                    // 跟踪缩放后的Bitmap
                    if (scaledBitmap != sourceBitmap) {
                        trackBitmap(scaledBitmap);
                    }

                    // 对当前尺寸，使用二分查找找到最佳质量和压缩数据
                    CompressResult result = binarySearchQuality(scaledBitmap, config.getMaxSize());

                    if (result != null) {
                        // 计算当前有效组合的得分
                        double currentScore = (double) currentWidth * currentHeight * Math.pow(result.quality, 2);

                        if (currentScore > bestScore) {
                            bestScore = currentScore;
                            // 关闭之前的最佳数据
                            if (bestCompressedData != null) {
                                try {
                                    bestCompressedData.close();
                                } catch (IOException e) {
                                    // 忽略关闭异常
                                }
                            }
                            // 使用新的最佳压缩数据
                            bestCompressedData = result.data;
                            Log.d(TAG, String.format("找到更好的压缩方案: %dx%d, 质量=%d, 得分=%.2f", 
                                    currentWidth, currentHeight, result.quality, currentScore));
                        } else {
                            // 如果不是最佳结果，关闭当前数据
                            try {
                                result.data.close();
                            } catch (IOException e) {
                                // 忽略关闭异常
                                Log.w(TAG, "无法关闭压缩数据", e);
                            }
                        }
                    } else {
                        // 记录无法找到合适质量的情况
                        Log.w(TAG, String.format("尺寸 %dx%d 无法找到满足大小限制(%d bytes)的压缩质量", 
                                currentWidth, currentHeight, config.getMaxSize()));
                    }
                } catch (OutOfMemoryError e) {
                    // 内存不足时，停止搜索并使用当前最佳结果
                    Log.w(TAG, "内存不足，停止尺寸搜索: scale=" + scale, e);
                    System.gc(); // 尝试回收内存
                    break;
                } catch (Exception e) {
                    // 记录异常但继续搜索其他尺寸
                    Log.w(TAG, "处理尺寸时发生异常: scale=" + scale, e);
                }
            }

            // 步骤 6: 与原图对比，选择更优的结果
            updateProgress(0.85f);
            if (bestCompressedData != null) {
                try {
                    long compressedSize = bestCompressedData.size();
                    long originalSize = sourceFile.length();
                    
                    // 如果压缩后文件更大，且原图已满足尺寸要求，则返回原图
                    if (compressedSize >= originalSize && isOriginalSizeValid()) {
                        updateProgress(0.95f);
                        File result = copyFileToTarget(sourceFile);
                        updateProgress(1.0f);
                        return result;
                    }
                    
                    // 否则保存压缩结果
                    updateProgress(0.95f);
                    File result = saveToFile(bestCompressedData);
                    updateProgress(1.0f);
                    return result;
                } finally {
                    try {
                        bestCompressedData.close();
                    } catch (IOException e) {
                        // 忽略关闭异常
                    }
                }
            } else {
                throw new CompressException("无法将图片压缩到要求的大小。未找到合适的尺寸和质量组合。");
            }
        } finally {
            // 确保所有Bitmap都被回收
            recycleAllBitmaps();
        }
    }
    
    /**
     * 检查文件权限
     */
    private void checkFilePermissions() throws CompressException {
        // 检查源文件
        if (!sourceFile.exists()) {
            throw new CompressException("源文件不存在: " + sourceFile.getAbsolutePath());
        }
        
        if (!sourceFile.canRead()) {
            throw new CompressException("无法读取源文件: " + sourceFile.getAbsolutePath());
        }
        
        if (sourceFile.length() == 0) {
            throw new CompressException("源文件为空: " + sourceFile.getAbsolutePath());
        }
        
        // 检查目标目录
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                throw new CompressException("无法创建目标目录: " + targetDir.getAbsolutePath());
            }
        }
        
        if (!targetDir.canWrite()) {
            throw new CompressException("无法写入目标目录: " + targetDir.getAbsolutePath());
        }
    }

    /**
     * 二分查找最佳压缩质量
     *
     * @param bitmap  需要压缩的位图
     * @param maxSize 最大文件大小限制
     * @return 压缩结果
     * @throws IOException
     */
    private CompressResult binarySearchQuality(Bitmap bitmap, long maxSize) throws IOException {
        int minQuality = config.getMinQuality();
        int maxQuality = 100;
        int bestQuality = -1;
        ByteArrayOutputStream bestData = null;

        while (minQuality <= maxQuality) {
            int midQuality = (minQuality + maxQuality) / 2;
            if (midQuality == 0) { // 避免质量为0的情况
                break;
            }
            
            // 使用 try-with-resources 确保 baos 总是被关闭
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                boolean compressSuccess = bitmap.compress(Bitmap.CompressFormat.JPEG, midQuality, baos);
                if (!compressSuccess) {
                    // 压缩失败，降低质量重试
                    maxQuality = midQuality - 1;
                    continue;
                }
                
                long size = baos.size();

                if (size <= maxSize) {
                    bestQuality = midQuality; // 这个质量是可行的
                    // 保存当前最佳结果
                    if (bestData != null) {
                        try {
                            bestData.close();
                        } catch (IOException e) {
                            // 忽略关闭异常
                        }
                    }
                    bestData = new ByteArrayOutputStream();
                    baos.writeTo(bestData);
                    minQuality = midQuality + 1; // 尝试寻找更高的质量
                } else {
                    maxQuality = midQuality - 1; // 质量太高，需要降低
                }
            } catch (OutOfMemoryError e) {
                // 内存不足，降低质量
                Log.w(TAG, "二分查找质量时内存不足: quality=" + midQuality, e);
                maxQuality = midQuality - 1;
                System.gc(); // 尝试回收内存
            } catch (Exception e) {
                // 其他异常，记录并降低质量重试
                Log.w(TAG, "二分查找质量时发生异常: quality=" + midQuality, e);
                maxQuality = midQuality - 1;
            }
        }
        
        if (bestQuality > 0 && bestData != null) {
            return new CompressResult(bestQuality, bestData);
        }
        
        // 如果没有找到合适的质量，尝试最低质量
        if (bestData == null) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                if (bitmap.compress(Bitmap.CompressFormat.JPEG, config.getMinQuality(), baos)) {
                    bestData = new ByteArrayOutputStream();
                    baos.writeTo(bestData);
                    return new CompressResult(config.getMinQuality(), bestData);
                }
            } catch (Exception e) {
                // 忽略异常
            }
        }
        
        return null;
    }

    /**
     * 压缩结果内部类
     */
    private static class CompressResult {
        final int quality;
        final ByteArrayOutputStream data;

        CompressResult(int quality, ByteArrayOutputStream data) {
            this.quality = quality;
            this.data = data;
        }
    }

    /**
     * 获取并缓存图片边界信息
     */
    private void ensureBoundsCached() throws CompressException {
        if (cachedWidth == -1 || cachedHeight == -1) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(sourceFile.getAbsolutePath(), options);
            cachedWidth = options.outWidth;
            cachedHeight = options.outHeight;
            
            // 检查是否成功获取尺寸信息
            if (cachedWidth <= 0 || cachedHeight <= 0) {
                throw new CompressException("无法获取图片尺寸信息，可能不是有效的图片文件: " + sourceFile.getAbsolutePath());
            }
        }
    }

    /**
     * 检查原始文件是否已经满足所有压缩条件
     */
    private boolean isAlreadyCompressed() {
        if (sourceFile.length() <= config.getMaxSize()) {
            try {
                ensureBoundsCached();
                return cachedWidth <= config.getMaxWidth() && cachedHeight <= config.getMaxHeight();
            } catch (CompressException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 检查原图尺寸是否满足要求（不考虑文件大小）
     */
    private boolean isOriginalSizeValid() {
        try {
            ensureBoundsCached();
            return cachedWidth <= config.getMaxWidth() && 
                   cachedHeight <= config.getMaxHeight();
        } catch (CompressException e) {
            return false;
        }
    }

    /**
     * 计算采样大小
     */
    private int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * 将原文件复制到目标目录，保持API一致性
     */
    private File copyFileToTarget(File sourceFile) throws IOException {
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                throw new IOException("无法创建目标目录: " + targetDir.getAbsolutePath());
            }
        }
        File targetFile = new File(targetDir, UUID.randomUUID().toString() + ".jpg");
        
        // 使用 try-with-resources 确保流总是被关闭
        try (java.io.FileInputStream fis = new java.io.FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(targetFile)) {
            
            byte[] buffer = new byte[8192]; // 使用更大的缓冲区提高性能
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.flush(); // 确保数据写入磁盘
        }
        return targetFile;
    }

    /**
     * 将压缩后的字节流保存到目标文件
     */
    private File saveToFile(ByteArrayOutputStream baos) throws IOException {
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                throw new IOException("无法创建目标目录: " + targetDir.getAbsolutePath());
            }
        }
        File targetFile = new File(targetDir, UUID.randomUUID().toString() + ".jpg");
        
        // 使用 try-with-resources 确保 fos 总是被关闭
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            baos.writeTo(fos);
            fos.flush(); // 确保数据写入磁盘
        }
        // baos 由调用者管理，此处不关闭
        return targetFile;
    }
}