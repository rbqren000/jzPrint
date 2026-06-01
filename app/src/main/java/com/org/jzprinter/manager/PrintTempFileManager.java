package com.org.jzprinter.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 打印临时文件管理器
 * 负责管理打印过程中产生的临时文件（创建、清理、统计）
 * 
 * Created by RBQ on 2025/05/15
 */
public class PrintTempFileManager {
    
    private static final String TAG = "PrintTempFileManager";
    
    // 临时文件命名规则（公开常量，供外部使用）
    public static final String TEMP_FILE_PREFIX = "print_";
    public static final String TEMP_FILE_SUFFIX = ".png";
    
    private static volatile PrintTempFileManager instance;
    
    private PrintTempFileManager() {
        // 私有构造函数，防止外部实例化
    }
    
    /**
     * 获取单例实例
     * @return PrintTempFileManager实例
     */
    public static PrintTempFileManager getInstance() {
        if (instance == null) {
            synchronized (PrintTempFileManager.class) {
                if (instance == null) {
                    instance = new PrintTempFileManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 将Bitmap保存为临时PNG文件
     * 
     * @param context 应用上下文
     * @param bitmap 要保存的Bitmap对象
     * @return 创建的临时文件
     * @throws Exception 文件创建或写入失败时抛出异常
     */
    public File saveBitmapToTempFile(Context context, Bitmap bitmap) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("Context不能为null");
        }
        if (bitmap == null) {
            throw new IllegalArgumentException("Bitmap不能为null");
        }
        
        File cacheDir = context.getCacheDir();
        if (cacheDir == null || !cacheDir.exists()) {
            throw new IllegalStateException("缓存目录不存在");
        }
        
        // 创建临时文件（格式：print_随机字符串.png）
        File imageFile = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX, cacheDir);
        
        // 将Bitmap写入文件
        FileOutputStream fos = new FileOutputStream(imageFile);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        } finally {
            fos.close();
        }
        
        Log.d(TAG, "创建临时文件: " + imageFile.getAbsolutePath());
        return imageFile;
    }
    
    /**
     * 清理所有打印临时文件
     * 在App启动时调用
     * 
     * @param context 应用上下文
     * @return 清理的文件数量
     */
    public int cleanAllTempFiles(Context context) {
        if (context == null) {
            Log.w(TAG, "Context为null，无法清理临时文件");
            return 0;
        }
        
        try {
            File cacheDir = context.getCacheDir();
            if (cacheDir == null || !cacheDir.exists()) {
                Log.w(TAG, "缓存目录不存在");
                return 0;
            }
            
            // 查找所有匹配的临时文件
            File[] tempFiles = cacheDir.listFiles((dir, name) -> 
                name != null && name.startsWith(TEMP_FILE_PREFIX) && name.endsWith(TEMP_FILE_SUFFIX)
            );
            
            if (tempFiles == null || tempFiles.length == 0) {
                Log.i(TAG, "没有需要清理的打印临时文件");
                return 0;
            }
            
            int deletedCount = 0;
            long totalSize = 0;
            
            for (File file : tempFiles) {
                try {
                    long fileSize = file.length();
                    if (file.delete()) {
                        deletedCount++;
                        totalSize += fileSize;
                    } else {
                        Log.w(TAG, "删除临时文件失败: " + file.getAbsolutePath());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "删除临时文件异常: " + file.getAbsolutePath(), e);
                }
            }
            
            Log.i(TAG, String.format("清理打印临时文件完成: 删除 %d 个文件, 释放 %.2f KB 空间", 
                deletedCount, totalSize / 1024.0));
            
            return deletedCount;
            
        } catch (Exception e) {
            Log.e(TAG, "清理打印临时文件异常", e);
            return 0;
        }
    }
    
    /**
     * 清理指定的临时文件
     * 可用于打印完成后立即清理单个文件
     * 
     * @param tempFile 要清理的临时文件
     * @return 是否删除成功
     */
    public boolean cleanSingleTempFile(File tempFile) {
        if (tempFile == null || !tempFile.exists()) {
            return false;
        }
        
        try {
            // 验证文件是否符合临时文件命名规则
            String fileName = tempFile.getName();
            if (!fileName.startsWith(TEMP_FILE_PREFIX) || !fileName.endsWith(TEMP_FILE_SUFFIX)) {
                Log.w(TAG, "文件不符合临时文件命名规则，拒绝删除: " + fileName);
                return false;
            }
            
            boolean deleted = tempFile.delete();
            if (deleted) {
                Log.d(TAG, "成功删除临时文件: " + tempFile.getAbsolutePath());
            } else {
                Log.w(TAG, "删除临时文件失败: " + tempFile.getAbsolutePath());
            }
            return deleted;
            
        } catch (Exception e) {
            Log.e(TAG, "删除临时文件异常: " + tempFile.getAbsolutePath(), e);
            return false;
        }
    }
    
    /**
     * 获取当前临时文件数量和总大小
     * 
     * @param context 应用上下文
     * @return 包含文件数量和大小的数组 [count, sizeInBytes]
     */
    public long[] getTempFileStats(Context context) {
        if (context == null) {
            return new long[]{0, 0};
        }
        
        try {
            File cacheDir = context.getCacheDir();
            if (cacheDir == null || !cacheDir.exists()) {
                return new long[]{0, 0};
            }
            
            File[] tempFiles = cacheDir.listFiles((dir, name) -> 
                name != null && name.startsWith(TEMP_FILE_PREFIX) && name.endsWith(TEMP_FILE_SUFFIX)
            );
            
            if (tempFiles == null || tempFiles.length == 0) {
                return new long[]{0, 0};
            }
            
            long totalSize = 0;
            for (File file : tempFiles) {
                totalSize += file.length();
            }
            
            return new long[]{tempFiles.length, totalSize};
            
        } catch (Exception e) {
            Log.e(TAG, "获取临时文件统计信息异常", e);
            return new long[]{0, 0};
        }
    }
}
