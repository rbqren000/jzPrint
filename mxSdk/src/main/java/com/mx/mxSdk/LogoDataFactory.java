package com.mx.mxSdk;

import static com.mx.mxSdk.FactoryErrorCodes.Context_NULL_ERROR;
import static com.mx.mxSdk.FactoryErrorCodes.IMAGE_NULL_ERROR;
import static com.mx.mxSdk.FactoryErrorCodes.IMAGE_PATH_NULL_ERROR;
import static com.mx.mxSdk.RowLayoutDirection.RowLayoutDirectionVertical;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import com.mx.mxSdk.OpencvUtils.OpenCVUtils;
import com.mx.mxSdk.Utils.MxSdkStore;
import com.mx.mxSdk.Utils.RBQLog;

import java.util.concurrent.Executors;

public class LogoDataFactory {

    /**
     *
     * @param context  上下文对象
     * @param logoImage logo图片对象
     * @param threshold  二值化阈值
     * @param transparentToWhiteAuto 是否自动将透明背景图转化为白色背景图(通常在明确知道图片不为透明图片的情况下不建议打开，检测图片透明会降低运行效率)
     * @param onCreateLogoDataListener  logo图片对象处理事件
     */
    public static void LogoImage2Data(Context context, LogoImage logoImage,int threshold, boolean transparentToWhiteAuto, OnCreateLogoDataListener onCreateLogoDataListener) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        if (logoImage == null) {
            mainHandler.post(() -> {
                if (onCreateLogoDataListener != null) {
                    onCreateLogoDataListener.onCreateLogoDataError(IMAGE_NULL_ERROR);
                }
            });
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {

            mainHandler.post(() -> {
                if (onCreateLogoDataListener != null) {
                    onCreateLogoDataListener.onCreateLogoDataStart();
                }
            });

            if (context == null) {
                mainHandler.post(() -> {
                    if (onCreateLogoDataListener != null) {
                        onCreateLogoDataListener.onCreateLogoDataError(Context_NULL_ERROR);
                    }
                });
                return;
            }

            LogoData logoData = LogoImage2Data(context, logoImage, threshold, transparentToWhiteAuto);

            if (logoData == null) {
                mainHandler.post(() -> {
                    if (onCreateLogoDataListener != null) {
                        onCreateLogoDataListener.onCreateLogoDataError(IMAGE_PATH_NULL_ERROR);
                    }
                });
                return;
            }

            mainHandler.post(() -> {
                if (onCreateLogoDataListener != null) {
                    onCreateLogoDataListener.onCreateLogoDataComplete(logoData);
                }
            });

        });
    }
    /**
     *
     * @param context  上下文对象
     * @param logoImage logo图片对象
     * @param threshold  二值化阈值
     * @param transparentToWhiteAuto 是否自动将透明背景图转化为白色背景图(通常在明确知道图片不为透明图片的情况下不建议打开，检测图片透明会降低运行效率)
     */
    public static LogoData LogoImage2Data(Context context, LogoImage logoImage, int threshold, boolean transparentToWhiteAuto){

        if (logoImage == null || logoImage.getImagePath()==null){
            return null;
        }

        String path = logoImage.getImagePath();

        Bitmap bitmap = MxSdkStore.getImageFromPath(path,Bitmap.Config.ARGB_8888);

        if (bitmap == null){
            return null;
        }

        if (transparentToWhiteAuto && OpenCVUtils.isTransparent(bitmap)){
            bitmap = MxImageUtils.transparentBGtoWhite(bitmap);
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width!=2000 || height!=552){
            bitmap = processImage(bitmap);
            width = bitmap.getWidth();
            height = bitmap.getHeight();
        }
        //像素将画在这个数组
        int[] pixels = new int[width * height];	//通过位图的大小创建像素点数组
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int[] gray = new int[width * height];	//通过位图的大小创建像素点数组

        MxImageUtils.bitmapToGray(pixels,gray,width,height);

        MxImageUtils.formatGrayToFloydDithering(gray,width,height,threshold);

        int[] binaryPixels  = new int[width * height];    //通过位图的大小创建像素点数组

        //黑白图
        MxImageUtils.grayToBinary(gray,binaryPixels,width,height,threshold);

        byte[] d72 = MxImageUtils.formatBinary69ToData72(binaryPixels,width,height);

        String dataPath = MxSdkStore.writeByteArrToCacheDataFile(context,d72);

//        String imagePath = MxImageUtils.createBitmapFromBinaryPixelsWithSave(context,bitmap,threshold,false,true,false,true, RowLayoutDirectionVertical, transparentToWhiteAuto,null,null);
        String imagePath = MxImageUtils.createSimulationBitmapFromBinaryPixelsWithSave(context,binaryPixels,width,height,false, RowLayoutDirectionVertical);
        return LogoData.createInstance(dataPath,d72.length,imagePath);
    }


    public static void LogoImage2ExtLogoData(Context context, LogoImage logoImage,GrayType grayType,int threshold, boolean transparentToWhiteAuto, OnCreateExtLogoDataListener onCreateExtLogoDataListener) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        if (logoImage == null) {
            mainHandler.post(() -> {
                if (onCreateExtLogoDataListener != null) {
                    onCreateExtLogoDataListener.onCreateExtLogoDataError(IMAGE_NULL_ERROR);
                }
            });
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {

            mainHandler.post(() -> {
                if (onCreateExtLogoDataListener != null) {
                    onCreateExtLogoDataListener.onCreateExtLogoDataStart();
                }
            });

            if (context == null) {
                mainHandler.post(() -> {
                    if (onCreateExtLogoDataListener != null) {
                        onCreateExtLogoDataListener.onCreateExtLogoDataError(Context_NULL_ERROR);
                    }
                });
                return;
            }

            ExtLogoData extLogoData = LogoImage2ExtLogoData(context, logoImage,grayType, threshold, transparentToWhiteAuto);

            if (extLogoData == null) {
                mainHandler.post(() -> {
                    if (onCreateExtLogoDataListener != null) {
                        onCreateExtLogoDataListener.onCreateExtLogoDataError(IMAGE_PATH_NULL_ERROR);
                    }
                });
                return;
            }

            mainHandler.post(() -> {
                if (onCreateExtLogoDataListener != null) {
                    onCreateExtLogoDataListener.onCreateExtLogoDataComplete(extLogoData);
                }
            });

        });
    }


    public static ExtLogoData LogoImage2ExtLogoData(Context context, LogoImage logoImage,GrayType grayType, int threshold, boolean transparentToWhiteAuto){

        if (logoImage == null || logoImage.getImagePath()==null){
            return null;
        }

        String path = logoImage.getImagePath();

        Bitmap bitmap = MxSdkStore.getImageFromPath(path,Bitmap.Config.ARGB_8888);

        if (bitmap == null){
            return null;
        }

        if (transparentToWhiteAuto && OpenCVUtils.isTransparent(bitmap)){
            bitmap = MxImageUtils.transparentBGtoWhite(bitmap);
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width!=2000 || height!=552){
            bitmap = processImage(bitmap);
            width = bitmap.getWidth();
            height = bitmap.getHeight();
        }
        //像素将画在这个数组
        int[] pixels = new int[width * height];	//通过位图的大小创建像素点数组
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int[] gray = new int[width * height];	//通过位图的大小创建像素点数组

        MxImageUtils.bitmapToGray(pixels,gray,grayType,width,height);

        MxImageUtils.formatGrayToFloydDithering(gray,width,height,threshold);

        int[] binaryPixels = new int[width * height];    //通过位图的大小创建像素点数组

        //黑白图
        MxImageUtils.grayToBinary(gray,binaryPixels,width,height,threshold);

        D72Obj d72Obj = MxImageUtils.formatBinary69ToD72Obj(binaryPixels,false,width,height);

        byte[] d72 = d72Obj.getD72();
        int validValueCount = d72Obj.getValidValueCount();

        String dataPath = MxSdkStore.writeByteArrToCacheDataFile(context,d72);

        String imagePath = MxImageUtils.createSimulationBitmapFromBinaryPixelsWithSave(context,binaryPixels,width,height,false,RowLayoutDirectionVertical);

        return ExtLogoData.createInstance(LogoData.createInstance(dataPath,d72.length,imagePath),validValueCount);
    }


    /**
     * 处理Bitmap，将其调整为2000x552的尺寸
     * @param inputBitmap 输入的Bitmap
     * @return 处理后的Bitmap
     */
    public static Bitmap processImage(Bitmap inputBitmap) {
        int targetWidth = 2000;
        int targetHeight = 552;
        // 创建一个目标尺寸的白色背景
        Bitmap outputBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);
        canvas.drawColor(Color.WHITE);

        int inputWidth = inputBitmap.getWidth();
        int inputHeight = inputBitmap.getHeight();

        float scale;
        int scaledWidth;
        int scaledHeight;
        Bitmap scaledBitmap;

        if (inputWidth <= targetWidth && inputHeight <= targetHeight) {
            // 图片比目标尺寸小，居中显示
            RBQLog.i("图片比目标尺寸小，居中显示");
            scaledBitmap = inputBitmap;
            scaledWidth = inputWidth;
            scaledHeight = inputHeight;
        } else {
            // 图片比目标尺寸大，进行缩放
            RBQLog.i("图片比目标尺寸大，进行缩放");
            float widthRatio = (float) targetWidth / inputWidth;
            float heightRatio = (float) targetHeight / inputHeight;
            scale = Math.min(widthRatio, heightRatio); // 保持纵横比缩放
            scaledWidth = Math.round(inputWidth * scale);
            scaledHeight = Math.round(inputHeight * scale);
            scaledBitmap = Bitmap.createScaledBitmap(inputBitmap, scaledWidth, scaledHeight, true);
        }

        // 计算居中的位置
        int left = (targetWidth - scaledWidth) / 2;
        int top = (targetHeight - scaledHeight) / 2;

        // 将缩放后的图片绘制到白色背景的中心位置
        canvas.drawBitmap(scaledBitmap, left, top, null);

        return outputBitmap;
    }

    public interface OnCreateLogoDataListener {
        void onCreateLogoDataStart();
        void onCreateLogoDataComplete(LogoData logoData);
        void onCreateLogoDataError(int code);
    }

    public interface OnCreateExtLogoDataListener {
        void onCreateExtLogoDataStart();
        void onCreateExtLogoDataComplete(ExtLogoData extLogoData);
        void onCreateExtLogoDataError(int code);
    }

}
