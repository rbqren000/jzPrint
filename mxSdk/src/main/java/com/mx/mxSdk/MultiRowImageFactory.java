package com.mx.mxSdk;

import static com.mx.mxSdk.FactoryErrorCodes.MULTI_ROW_IMAGE_CREATION_FAILED;
import static com.mx.mxSdk.FactoryErrorCodes.Context_NULL_ERROR;
import static com.mx.mxSdk.FactoryErrorCodes.IOException_ERROR;
import static com.mx.mxSdk.FactoryErrorCodes.FILE_NOT_FOUND;
import static com.mx.mxSdk.FactoryErrorCodes.IMAGE_NULL_ERROR;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.mx.mxSdk.Utils.MxSdkStore;
import com.mx.mxSdk.Utils.RBQLog;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.Executors;

public class MultiRowImageFactory {
    /**
     *
     * @param context 上下文对象
     * @param uri 图片的uri
     * @param rowLayoutDirection 裁剪方向
     * @param onCreateMultiRowImageListener 事件
     */
    public static void image2MultiRowImage(Context context, Uri uri, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener) {
        image2MultiRowImage(context,uri,rowLayoutDirection,0,onCreateMultiRowImageListener);
    }

    /**
     *
     * @param context 上下文
     * @param uri 图片的uri
     * @param rowLayoutDirection 裁剪方向
     * @param ignoreLastRowIfHeightLess 忽略参数，最后一拼低于该值将直接被忽略
     * @param onCreateMultiRowImageListener 事件
     */
    public static void image2MultiRowImage(Context context, Uri uri, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener) {

        Handler mainHandler = new Handler(Looper.getMainLooper());

        if (context == null) {
            mainHandler.post(() -> {
                if (onCreateMultiRowImageListener != null) {
                    onCreateMultiRowImageListener.onCreateMultiRowImageError(Context_NULL_ERROR);
                }
            });
            return;
        }

        if (uri == null) {
            mainHandler.post(() -> {
                if (onCreateMultiRowImageListener != null) {
                    onCreateMultiRowImageListener.onCreateMultiRowImageError(IMAGE_NULL_ERROR);
                }
            });
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {

            mainHandler.post(() -> {
                if (onCreateMultiRowImageListener != null) {
                    onCreateMultiRowImageListener.onCreateMultiRowImageStart();
                }
            });

            try {

                MultiRowImage multiRowImage = image2MultiRowImage(context, uri, rowLayoutDirection, ignoreLastRowIfHeightLess);

                if (multiRowImage == null) {
                    mainHandler.post(() -> {
                        if (onCreateMultiRowImageListener != null) {
                            onCreateMultiRowImageListener.onCreateMultiRowImageError(MULTI_ROW_IMAGE_CREATION_FAILED);
                        }
                    });
                    return;
                }

                mainHandler.post(() -> {
                    if (onCreateMultiRowImageListener != null) {
                        onCreateMultiRowImageListener.onCreateMultiRowImageComplete(multiRowImage);
                    }
                });

            } catch (IOException e) {
                mainHandler.post(() -> {
                    if (onCreateMultiRowImageListener != null) {
                        onCreateMultiRowImageListener.onCreateMultiRowImageError(IOException_ERROR);
                    }
                });
            }

        });
    }

    public static MultiRowImage image2MultiRowImage(Context context, Uri uri,
                                                    RowLayoutDirection direction, int ignoreThreshold) throws IOException {

        // 创建临时文件并确保流关闭
        File cacheFile;
        InputStream uriInputStream = null;
        OutputStream fileOutputStream = null;
        try {
            uriInputStream = context.getContentResolver().openInputStream(uri);
            if (uriInputStream == null) throw new IOException("Cannot open URI stream");

            cacheFile = File.createTempFile("img_", ".tmp", context.getCacheDir());
            fileOutputStream = new FileOutputStream(cacheFile);

            // 高效复制数据（8KB缓冲区）
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = uriInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        } finally {
            // 确保关闭所有流
            closeQuietly(uriInputStream);
            closeQuietly(fileOutputStream);
        }

        // 处理图像分割
        BitmapRegionDecoder decoder = null;
        InputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(cacheFile);
            decoder = BitmapRegionDecoder.newInstance(fileInputStream, false);
            if (decoder == null) return null;

            final int width = decoder.getWidth();
            final int height = decoder.getHeight();
            ArrayList<RowImage> rows = new ArrayList<>();

            if (direction == RowLayoutDirection.RowLayoutDirectionVertical) {
                // 纵向处理逻辑
                final int rowHeight = 552;
                int totalRows = (int) Math.ceil(height / (double) rowHeight);

                for (int i = 0; i < totalRows; i++) {
                    int top = i * rowHeight;
                    int bottom = Math.min(top + rowHeight, height);
                    int actualHeight = bottom - top;

                    if (i == totalRows - 1 && actualHeight < ignoreThreshold) break;

                    // 创建固定尺寸图像
                    Bitmap bitmap = Bitmap.createBitmap(width, rowHeight, Bitmap.Config.ARGB_8888);
                    try {
                        Canvas canvas = new Canvas(bitmap);
                        canvas.drawColor(Color.WHITE);

                        // 解码图像区域
                        Rect srcRect = new Rect(0, top, width, bottom);
                        Bitmap region = decoder.decodeRegion(srcRect, null);
                        if (region != null) {
                            canvas.drawBitmap(region, 0, 0, null);
                            region.recycle();
                        }

                        // 保存结果
                        rows.add(RowImage.createInstance(MxSdkStore.saveImageToCache(context, bitmap)));
                    } finally {
                        bitmap.recycle(); // 确保回收
                    }
                }
            } else {
                // 横向处理逻辑
                final int colWidth = 552;
                int totalCols = (int) Math.ceil(width / (double) colWidth);

                for (int i = 0; i < totalCols; i++) {
                    int left = i * colWidth;
                    int right = Math.min(left + colWidth, width);
                    int actualWidth = right - left;

                    if (i == totalCols - 1 && actualWidth < ignoreThreshold) break;

                    Bitmap bitmap = Bitmap.createBitmap(colWidth, height, Bitmap.Config.ARGB_8888);
                    try {
                        Canvas canvas = new Canvas(bitmap);
                        canvas.drawColor(Color.WHITE);

                        Rect srcRect = new Rect(left, 0, right, height);
                        Bitmap region = decoder.decodeRegion(srcRect, null);
                        if (region != null) {
                            canvas.drawBitmap(region, 0, 0, null);
                            region.recycle();
                        }

                        rows.add(RowImage.createInstance(MxSdkStore.saveImageToCache(context, bitmap)));
                    } finally {
                        bitmap.recycle();
                    }
                }
            }

            return MultiRowImage.createInstance(rows, null, direction, true);
        } finally {
            // 最终资源清理
            closeQuietly(fileInputStream);
            if (decoder != null) decoder.recycle();
            if (cacheFile != null) cacheFile.delete();
        }
    }

    // 安全关闭流工具方法
    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @param context 上下文
     * @param imagePath 图片的路径
     * @param rowLayoutDirection 裁剪方向
     * @param onCreateMultiRowImageListener 事件
     */
    public static void image2MultiRowImage(Context context, String imagePath, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener) {
        image2MultiRowImage(context,imagePath,rowLayoutDirection,0,onCreateMultiRowImageListener);
    }

    public static void image2MultiRowImage(Context context, String imagePath, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener) {

        Handler mainHandler = new Handler(Looper.getMainLooper());

        if (context == null) {
            mainHandler.post(() -> {
                if (onCreateMultiRowImageListener != null) {
                    onCreateMultiRowImageListener.onCreateMultiRowImageError(Context_NULL_ERROR);
                }
            });
            return;
        }

        if (imagePath == null || imagePath.isEmpty()) {
            mainHandler.post(() -> {
                if (onCreateMultiRowImageListener != null) {
                    onCreateMultiRowImageListener.onCreateMultiRowImageError(IMAGE_NULL_ERROR);
                }
            });
            return;
        }

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            mainHandler.post(() -> {
                if (onCreateMultiRowImageListener != null) {
                    onCreateMultiRowImageListener.onCreateMultiRowImageError(FILE_NOT_FOUND);
                }
            });
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {

            mainHandler.post(() -> {
                if (onCreateMultiRowImageListener != null) {
                    onCreateMultiRowImageListener.onCreateMultiRowImageStart();
                }
            });

            try {

                MultiRowImage multiRowImage = image2MultiRowImage(context,imagePath,rowLayoutDirection,ignoreLastRowIfHeightLess);

                if (multiRowImage == null){

                    mainHandler.post(() -> {
                        if (onCreateMultiRowImageListener != null) {
                            onCreateMultiRowImageListener.onCreateMultiRowImageError(MULTI_ROW_IMAGE_CREATION_FAILED);
                        }
                    });
                    return;
                }

                mainHandler.post(() -> {
                    if (onCreateMultiRowImageListener != null) {
                        onCreateMultiRowImageListener.onCreateMultiRowImageComplete(multiRowImage);
                    }
                });

            } catch (IOException e) {
                mainHandler.post(() -> {
                    if (onCreateMultiRowImageListener != null) {
                        onCreateMultiRowImageListener.onCreateMultiRowImageError(IOException_ERROR);
                    }
                });
            }

        });
    }

    public static MultiRowImage image2MultiRowImage(Context context, @NonNull String imagePath,
                                                    RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess) throws IOException {

        File file = new File(imagePath);
        if (!file.exists()) {
            return null;
        }

        InputStream inputStream = new FileInputStream(file);
        BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(inputStream, false);

        if (decoder == null) {
            inputStream.close();
            return null;
        }

        int width = decoder.getWidth();
        int height = decoder.getHeight();

        ArrayList<RowImage> rowImages = new ArrayList<>();

        if (rowLayoutDirection == RowLayoutDirection.RowLayoutDirectionVertical) {
            int rowHeight = 552;
            int rows = (int) Math.ceil((double) height / rowHeight);

            for (int i = 0; i < rows; i++) {
                int top = i * rowHeight;
                int bottom = Math.min(top + rowHeight, height);
                int actualHeight = bottom - top;

                // 忽略条件判断
                if (i == rows - 1 && actualHeight < ignoreLastRowIfHeightLess) {
                    break;
                }

                // 创建固定尺寸的Bitmap并填充白色
                Bitmap rowBitmap = Bitmap.createBitmap(width, rowHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(rowBitmap);
                canvas.drawColor(Color.WHITE);

                // 解码原图区域
                Rect srcRect = new Rect(0, top, width, bottom);
                Bitmap regionBitmap = decoder.decodeRegion(srcRect, new BitmapFactory.Options());

                if (regionBitmap != null) {
                    // 绘制到正确位置（顶部对齐，不拉伸）
                    Rect destRect = new Rect(0, 0, regionBitmap.getWidth(), regionBitmap.getHeight());
                    canvas.drawBitmap(regionBitmap, null, destRect, null);
                    regionBitmap.recycle();
                }

                // 保存处理后的Bitmap
                String path = MxSdkStore.saveImageToCache(context, rowBitmap);
                rowImages.add(RowImage.createInstance(path));
                rowBitmap.recycle();
            }

        } else if (rowLayoutDirection == RowLayoutDirection.RowLayoutDirectionHorizontal) {
            int colWidth = 552;
            int cols = (int) Math.ceil((double) width / colWidth);

            for (int i = 0; i < cols; i++) {
                int left = i * colWidth;
                int right = Math.min(left + colWidth, width);
                int actualWidth = right - left;

                // 忽略条件判断
                if (i == cols - 1 && actualWidth < ignoreLastRowIfHeightLess) {
                    break;
                }

                // 创建固定尺寸的Bitmap并填充白色
                Bitmap colBitmap = Bitmap.createBitmap(colWidth, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(colBitmap);
                canvas.drawColor(Color.WHITE);

                // 解码原图区域
                Rect srcRect = new Rect(left, 0, right, height);
                Bitmap regionBitmap = decoder.decodeRegion(srcRect, new BitmapFactory.Options());

                if (regionBitmap != null) {
                    // 绘制到正确位置（左对齐，不拉伸）
                    Rect destRect = new Rect(0, 0, regionBitmap.getWidth(), regionBitmap.getHeight());
                    canvas.drawBitmap(regionBitmap, null, destRect, null);
                    regionBitmap.recycle();
                }

                // 保存处理后的Bitmap
                String path = MxSdkStore.saveImageToCache(context, colBitmap);
                rowImages.add(RowImage.createInstance(path));
                colBitmap.recycle();
            }
        }

        decoder.recycle();
        inputStream.close();
        return MultiRowImage.createInstance(rowImages, null, rowLayoutDirection, true);
    }

    /**
     *
     * @param context 上下文
     * @param bitmap 图片bitmap对象
     * @param rowLayoutDirection 裁剪方向
     * @param onCreateMultiRowImageListener 事件
     */
    public static void image2MultiRowImage(Context context,Bitmap bitmap, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener){
        image2MultiRowImage(context,bitmap,rowLayoutDirection,0,onCreateMultiRowImageListener);
    }

    public static void image2MultiRowImage(Context context,Bitmap bitmap, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener){

        Handler mainHandler = new Handler(Looper.getMainLooper());

        if (context == null){
            mainHandler.post(() -> {
                if (onCreateMultiRowImageListener != null) {
                    onCreateMultiRowImageListener.onCreateMultiRowImageError(Context_NULL_ERROR);
                }
            });
            return;
        }

        if (bitmap == null){
            mainHandler.post(() -> {
                if (onCreateMultiRowImageListener != null) {
                    onCreateMultiRowImageListener.onCreateMultiRowImageError(IMAGE_NULL_ERROR);
                }
            });
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {

            mainHandler.post(() -> {
                if (onCreateMultiRowImageListener != null) {
                    onCreateMultiRowImageListener.onCreateMultiRowImageStart();
                }
            });

            MultiRowImage multiRowImage = image2MultiRowImage(context,bitmap, rowLayoutDirection,ignoreLastRowIfHeightLess);

            mainHandler.post(() -> {
                if (onCreateMultiRowImageListener != null) {
                    onCreateMultiRowImageListener.onCreateMultiRowImageComplete(multiRowImage);
                }
            });
        });

    }

    public static MultiRowImage image2MultiRowImage(@NonNull Context context, @NonNull Bitmap bitmap, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        RBQLog.i("*****【image2MultiRowImage】原图 width:" + width + "; height:" + height);

        ArrayList<RowImage> rowImages = new ArrayList<>();

        if (rowLayoutDirection == RowLayoutDirection.RowLayoutDirectionVertical) {
            int rowHeight = 552;
            int rows = (int) Math.ceil((double) height / rowHeight);

            for (int i = 0; i < rows; i++) {
                int top = i * rowHeight;
                int bottom = Math.min(top + rowHeight, height);
                int actualHeight = bottom - top;

                // 检查最后一行是否应忽略
                if (i == rows - 1 && actualHeight < ignoreLastRowIfHeightLess) {
                    break;
                }

                // 创建固定高度的行Bitmap
                Bitmap rowBitmap = Bitmap.createBitmap(width, rowHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(rowBitmap);
                canvas.drawColor(Color.WHITE); // 填充白色背景

                Rect srcRect = new Rect(0, top, width, bottom);
                Rect destRect = new Rect(0, 0, width, actualHeight);
                canvas.drawBitmap(bitmap, srcRect, destRect, null); // 绘制原图部分

                // 保存并释放资源
                String rowImagePath = MxSdkStore.saveImageToCache(context, rowBitmap);
                rowImages.add(RowImage.createInstance(rowImagePath));
                rowBitmap.recycle();
            }
        } else if (rowLayoutDirection == RowLayoutDirection.RowLayoutDirectionHorizontal) {
            int colWidth = 552;
            int cols = (int) Math.ceil((double) width / colWidth);

            for (int i = 0; i < cols; i++) {
                int left = i * colWidth;
                int right = Math.min(left + colWidth, width);
                int actualWidth = right - left;

                // 检查最后一列是否应忽略
                if (i == cols - 1 && actualWidth < ignoreLastRowIfHeightLess) {
                    break;
                }

                // 创建固定宽度的列Bitmap
                Bitmap colBitmap = Bitmap.createBitmap(colWidth, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(colBitmap);
                canvas.drawColor(Color.WHITE); // 填充白色背景

                Rect srcRect = new Rect(left, 0, right, height);
                Rect destRect = new Rect(0, 0, actualWidth, height);
                canvas.drawBitmap(bitmap, srcRect, destRect, null); // 绘制原图部分

                // 保存并释放资源
                String colImagePath = MxSdkStore.saveImageToCache(context, colBitmap);
                rowImages.add(RowImage.createInstance(colImagePath));
                colBitmap.recycle();
            }
        }

        return MultiRowImage.createInstance(rowImages, null, rowLayoutDirection, true);
    }

    public interface OnCreateMultiRowImageListener{
        void onCreateMultiRowImageStart();
        void onCreateMultiRowImageComplete(MultiRowImage multiRowImage);
        void onCreateMultiRowImageError(int code);
    }
}
