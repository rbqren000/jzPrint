package com.mx.mxSdk;

import static com.mx.mxSdk.FactoryErrorCodes.Context_NULL_ERROR;
import static com.mx.mxSdk.FactoryErrorCodes.MULTI_ROW_IMAGE_NULL_ERROR;
import static com.mx.mxSdk.FactoryErrorCodes.ROW_IMAGE_NULL_ERROR;
import static com.mx.mxSdk.RowLayoutDirection.RowLayoutDirectionHorizontal;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import com.mx.mxSdk.OpencvUtils.OpenCVUtils;
import com.mx.mxSdk.Utils.MxSdkStore;
import java.util.ArrayList;
import java.util.concurrent.Executors;

public class MultiRowDataFactory {

    /**
     *
     * @param context  上下文对象
     * @param multiRowImage 多拼图像对象
     * @param threshold  二值化阈值
     * @param clearBackground 是否移除背景色
     * @param dithering 是否开启抖动算法
     * @param compress 是否启用压缩
     * @param flipHorizontally 是否横向翻转图像
     * @param transparentToWhiteAuto 是否将透明图片转为白色背景图
     * @param thumbToSimulation 缩略图是否转为可预览的打印效果图
     * @param onCreateMultiRowDataListener 图像转化事件
     */
    public static void bitmap2MultiRowData(Context context, MultiRowImage multiRowImage, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean flipHorizontally, boolean transparentToWhiteAuto, boolean thumbToSimulation, OnCreateMultiRowDataListener onCreateMultiRowDataListener) {

        Handler mainHandler = new Handler(Looper.getMainLooper());

        if (multiRowImage == null) {
            mainHandler.post(() -> {
                if (onCreateMultiRowDataListener != null) {
                    onCreateMultiRowDataListener.onCreateMultiRowDataError(MULTI_ROW_IMAGE_NULL_ERROR);
                }
            });
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {

            mainHandler.post(() -> {
                if (onCreateMultiRowDataListener != null) {
                    onCreateMultiRowDataListener.onCreateMultiRowDataStart();
                }
            });

            if (context == null) {
                mainHandler.post(() -> {
                    if (onCreateMultiRowDataListener != null) {
                        onCreateMultiRowDataListener.onCreateMultiRowDataError(Context_NULL_ERROR);
                    }
                });
                return;
            }

            MultiRowData multiRowData = bitmap2MultiRowData(context, multiRowImage, threshold, clearBackground, dithering, compress, flipHorizontally, transparentToWhiteAuto, thumbToSimulation);

            if (multiRowData == null) {
                mainHandler.post(() -> {
                    if (onCreateMultiRowDataListener != null) {
                        onCreateMultiRowDataListener.onCreateMultiRowDataError(ROW_IMAGE_NULL_ERROR);
                    }
                });
                return;
            }

            mainHandler.post(() -> {
                if (onCreateMultiRowDataListener != null) {
                    onCreateMultiRowDataListener.onCreateMultiRowDataComplete(multiRowData);
                }
            });

        });
    }

    /**
     *
     * @param context  上下文对象
     * @param multiRowImage 多拼图像对象
     * @param threshold  二值化阈值
     * @param clearBackground 是否移除背景色
     * @param dithering 是否开启抖动算法
     * @param compress 是否启用压缩
     * @param flipHorizontally 是否横向翻转图像
     * @param transparentToWhiteAuto 是否将透明图片转为白色背景图
     * @param thumbToSimulation 缩略图是否转为可预览的打印效果图
     */
    public static MultiRowData bitmap2MultiRowData(Context context, MultiRowImage multiRowImage, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean flipHorizontally,boolean transparentToWhiteAuto,boolean thumbToSimulation) {

        if (multiRowImage == null){
            return null;
        }
        ArrayList<RowData> rowDataArr = new ArrayList<RowData>();
        ArrayList<String> imagePaths = new ArrayList<String>();

        ArrayList<RowImage> rowImages = multiRowImage.getRowImages();

        RowLayoutDirection rowLayoutDirection =  multiRowImage.getRowLayoutDirection();
        boolean isContiguousCroppedImages =  multiRowImage.isContiguousCroppedImages();

        Bitmap bitmap;
        RowData rowData;
        String imagePath;

        int topBeyondDistance;
        int bottomBeyondDistance;
        int [] rowData_initialErrors = null;
        int [] rowData_lastRowErrors = null;

        for (int sm = 0; sm < rowImages.size(); sm++){

            RowImage rowImage = rowImages.get(sm);
            if (rowLayoutDirection == RowLayoutDirectionHorizontal){
                bitmap = MxImageUtils.rotatedImageByRadians(MxSdkStore.getImageFromPath(rowImage.getImagePath(),Bitmap.Config.ARGB_8888,flipHorizontally),Math.PI*0.5f);
            }else {
                bitmap = MxSdkStore.getImageFromPath(rowImage.getImagePath(),Bitmap.Config.ARGB_8888,flipHorizontally);
            }
            if (bitmap==null){
                return null;
            }

            topBeyondDistance = rowImage.getTopBeyondDistance();
            bottomBeyondDistance = rowImage.getBottomBeyondDistance();

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            Bitmap newBitmap;
            int valid_height = height - topBeyondDistance - bottomBeyondDistance;
            int new_width;
            int new_height;
            int new_topBeyondDistance;
            int new_bottomBeyondDistance;

            //如果高度不为552，则缩放到552
            if(valid_height != 552.0f ){

                float scale = 552.0f/valid_height;

                new_topBeyondDistance = (int) ((float)topBeyondDistance * scale);
                new_bottomBeyondDistance = (int) ((float)bottomBeyondDistance * scale);

                int temp_width = (int)((float) width * scale);
                int temp_height = (int)(552.0f + new_topBeyondDistance + new_bottomBeyondDistance);
                newBitmap =  Bitmap.createScaledBitmap(bitmap, temp_width, temp_height, true);
                //这里valid_height不为552则进行缩放，那么缩放后valid_height的值则变为552了
                valid_height = 552;
                new_width = temp_width;
                new_height = temp_height;

            }else{

                newBitmap = bitmap;
                new_width = width;
                new_height = height;
                new_topBeyondDistance = topBeyondDistance;
                new_bottomBeyondDistance = bottomBeyondDistance;

            }

            //是否检测背景透明并自动转为白色
            if (transparentToWhiteAuto && OpenCVUtils.isTransparent(newBitmap)){
                newBitmap = MxImageUtils.transparentBGtoWhite(newBitmap);
            }

            if (clearBackground){
                newBitmap = OpenCVUtils.lightClearBackground(newBitmap);
            }

            // 计算有效区域的起始和结束行
            int startRow = new_topBeyondDistance;
            int endRow = new_height - new_bottomBeyondDistance;
            int numberOfRows = endRow - startRow;

            // 创建目标数组并直接从newBitmap中获取有效区域的像素
            int[] validPixels = new int[numberOfRows * new_width];
            newBitmap.getPixels(validPixels, 0, new_width, 0, startRow, new_width, numberOfRows);

            if(isContiguousCroppedImages){
                if (rowData_lastRowErrors != null){
                    rowData_initialErrors = rowData_lastRowErrors;
                }
                rowData_lastRowErrors = new int[new_width];
            }


            int[] gray = new int[numberOfRows * new_width];	//通过位图的大小创建像素点数组
            MxImageUtils.bitmapToGray(validPixels,gray,new_width,numberOfRows);
            if (dithering){
                MxImageUtils.formatGrayToFloydDithering(gray,new_width,numberOfRows,threshold,rowData_initialErrors,rowData_lastRowErrors);
            }

            int[] binaryPixels = new int[numberOfRows * new_width];
            //黑白图
            MxImageUtils.grayToBinary(gray,binaryPixels,new_width,numberOfRows,threshold);
            byte[] d72 = MxImageUtils.formatBinary69ToData72(binaryPixels,new_width,numberOfRows);
            if (compress){
                d72 = Compress.compressRowData(d72);
            }
            String path = MxSdkStore.writeByteArrToCacheDataFile(context,d72);

            rowData = RowData.createInstance(path,d72.length, compress);

            rowDataArr.add(rowData);

            imagePath = MxImageUtils.createSimulationBitmapFromBinaryPixelsWithSave(context,binaryPixels,new_width,numberOfRows,compress,rowLayoutDirection);
            imagePaths.add(imagePath);

        }

        String thumbPath = null;
        if (thumbToSimulation && multiRowImage.getThumbPath() != null){
            Bitmap originThumb = MxSdkStore.getImageFromPath(multiRowImage.getThumbPath(), Bitmap.Config.ARGB_8888);
            if (originThumb != null){
                thumbPath = MxImageUtils.createSimulationBitmapFromBitmapWithSave(context,originThumb,threshold,clearBackground,dithering,compress,false,rowLayoutDirection,transparentToWhiteAuto,null,null);
            }
        }
        return MultiRowData.createInstance(rowDataArr,imagePaths,thumbPath,compress,rowLayoutDirection);
    }

    public static void bitmap2ExtMultiRowData(Context context, MultiRowImage multiRowImage, GrayType grayType, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean flipHorizontally, boolean transparentToWhiteAuto, boolean thumbToSimulation, OnCreateExtMultiRowDataListener onCreateExtMultiRowDataListener) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        if (multiRowImage == null) {
            mainHandler.post(() -> {
                if (onCreateExtMultiRowDataListener != null) {
                    onCreateExtMultiRowDataListener.onCreateExtMultiRowDataError(MULTI_ROW_IMAGE_NULL_ERROR);
                }
            });
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {

            mainHandler.post(() -> {
                if (onCreateExtMultiRowDataListener != null) {
                    onCreateExtMultiRowDataListener.onCreateExtMultiRowDataStart();
                }
            });

            if (context == null) {
                mainHandler.post(() -> {
                    if (onCreateExtMultiRowDataListener != null) {
                        onCreateExtMultiRowDataListener.onCreateExtMultiRowDataError(Context_NULL_ERROR);
                    }
                });
                return;
            }

            ExtMultiRowData extMultiRowData = bitmap2ExtMultiRowData(context, multiRowImage, grayType, threshold, clearBackground, dithering, compress, flipHorizontally, transparentToWhiteAuto, thumbToSimulation);

            if (extMultiRowData == null) {
                mainHandler.post(() -> {
                    if (onCreateExtMultiRowDataListener != null) {
                        onCreateExtMultiRowDataListener.onCreateExtMultiRowDataError(ROW_IMAGE_NULL_ERROR);
                    }
                });
                return;
            }

            mainHandler.post(() -> {
                if (onCreateExtMultiRowDataListener != null) {
                    onCreateExtMultiRowDataListener.onCreateExtMultiRowDataComplete(extMultiRowData);
                }
            });
        });
    }


    public static ExtMultiRowData bitmap2ExtMultiRowData(Context context, MultiRowImage multiRowImage, GrayType grayType, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean flipHorizontally, boolean transparentToWhiteAuto, boolean thumbToSimulation) {

        if (multiRowImage == null){
            return null;
        }
        ArrayList<RowData> rowDataArr = new ArrayList<RowData>();
        ArrayList<String> imagePaths = new ArrayList<String>();

        ArrayList<RowImage> rowImages = multiRowImage.getRowImages();

        RowLayoutDirection rowLayoutDirection =  multiRowImage.getRowLayoutDirection();
        boolean isContiguousCroppedImages =  multiRowImage.isContiguousCroppedImages();

        Bitmap bitmap;
        ExtRowData extRowData;
        String imagePath;
        int validValueCount = 0;

        int topBeyondDistance;
        int bottomBeyondDistance;
        int [] rowData_initialErrors = null;
        int [] rowData_lastRowErrors = null;

        for (int sm = 0; sm < rowImages.size(); sm++){

            RowImage rowImage = rowImages.get(sm);
            if (rowLayoutDirection == RowLayoutDirectionHorizontal){
                bitmap = MxImageUtils.rotatedImageByRadians(MxSdkStore.getImageFromPath(rowImage.getImagePath(),Bitmap.Config.ARGB_8888,flipHorizontally),Math.PI*0.5f);
            }else {
                bitmap = MxSdkStore.getImageFromPath(rowImage.getImagePath(),Bitmap.Config.ARGB_8888,flipHorizontally);
            }
            if (bitmap==null){
                return null;
            }

            topBeyondDistance = rowImage.getTopBeyondDistance();
            bottomBeyondDistance = rowImage.getBottomBeyondDistance();

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            Bitmap newBitmap;
            int valid_height = height - topBeyondDistance - bottomBeyondDistance;
            int new_width;
            int new_height;
            int new_topBeyondDistance;
            int new_bottomBeyondDistance;

            //如果高度不为552，则缩放到552
            if(valid_height != 552.0f ){

                float scale = 552.0f/valid_height;

                new_topBeyondDistance = (int) ((float)topBeyondDistance * scale);
                new_bottomBeyondDistance = (int) ((float)bottomBeyondDistance * scale);

                int temp_width = (int)((float) width * scale);
                int temp_height = (int)(552.0f + new_topBeyondDistance + new_bottomBeyondDistance);
                newBitmap =  Bitmap.createScaledBitmap(bitmap, temp_width, temp_height, true);
                //这里valid_height不为552则进行缩放，那么缩放后valid_height的值则变为552了
                valid_height = 552;
                new_width = temp_width;
                new_height = temp_height;

            }else{

                newBitmap = bitmap;
                new_width = width;
                new_height = height;
                new_topBeyondDistance = topBeyondDistance;
                new_bottomBeyondDistance = bottomBeyondDistance;

            }

            //是否检测背景透明并自动转为白色
            if (transparentToWhiteAuto && OpenCVUtils.isTransparent(newBitmap)){
                newBitmap = MxImageUtils.transparentBGtoWhite(newBitmap);
            }

            if (clearBackground){
                newBitmap = OpenCVUtils.lightClearBackground(newBitmap);
            }

            // 计算有效区域的起始和结束行
            int startRow = new_topBeyondDistance;
            int endRow = new_height - new_bottomBeyondDistance;
            int numberOfRows = endRow - startRow;

            // 创建目标数组并直接从newBitmap中获取有效区域的像素
            int[] validPixels = new int[numberOfRows * new_width];
            newBitmap.getPixels(validPixels, 0, new_width, 0, startRow, new_width, numberOfRows);

            if(isContiguousCroppedImages){
                if (rowData_lastRowErrors != null){
                    rowData_initialErrors = rowData_lastRowErrors;
                }
                rowData_lastRowErrors = new int[new_width];
            }


            int[] gray = new int[numberOfRows * new_width];	//通过位图的大小创建像素点数组
            MxImageUtils.bitmapToGray(validPixels,gray,grayType,new_width,numberOfRows);
            if (dithering){
                MxImageUtils.formatGrayToFloydDithering(gray,new_width,numberOfRows,threshold,rowData_initialErrors,rowData_lastRowErrors);
            }
            int[] binaryPixels = new int[numberOfRows * new_width];    //通过位图的大小创建像素点数组
            //黑白图
            MxImageUtils.grayToBinary(gray,binaryPixels,new_width,numberOfRows,threshold);
            D72Obj d72Obj = MxImageUtils.formatBinary69ToD72Obj(binaryPixels,compress,new_width,numberOfRows);
            byte[] d72 = d72Obj.getD72();
            validValueCount = d72Obj.getValidValueCount();
            if (compress){
                d72 = Compress.compressRowData(d72);
            }
            String path = MxSdkStore.writeByteArrToCacheDataFile(context,d72);
            RowData rowData = RowData.createInstance(path,d72.length, compress);

            extRowData = new ExtRowData(rowData,validValueCount);

            rowDataArr.add(extRowData.getRowData());
            validValueCount = validValueCount + extRowData.getValidValueCount();

            imagePath = MxImageUtils.createSimulationBitmapFromBinaryPixelsWithSave(context,binaryPixels,new_width,numberOfRows,compress,rowLayoutDirection);
            imagePaths.add(imagePath);

        }

        String thumbPath = null;
        if (thumbToSimulation && multiRowImage.getThumbPath() != null){
            Bitmap originThumb = MxSdkStore.getImageFromPath(multiRowImage.getThumbPath(), Bitmap.Config.ARGB_8888);
            if (originThumb != null){
                thumbPath = MxImageUtils.createSimulationBitmapFormBitmapGrayTypeWithSave(context,originThumb,grayType,threshold,clearBackground,dithering,compress,false,rowLayoutDirection,transparentToWhiteAuto,null,null);
            }
        }
        return ExtMultiRowData.createInstance(MultiRowData.createInstance(rowDataArr,imagePaths,thumbPath,compress,rowLayoutDirection),validValueCount);
    }

    public interface OnCreateMultiRowDataListener{
        void onCreateMultiRowDataStart();
        void onCreateMultiRowDataComplete(MultiRowData multiRowData);
        void onCreateMultiRowDataError(int code);
    }

    public interface OnCreateExtMultiRowDataListener{
        void onCreateExtMultiRowDataStart();
        void onCreateExtMultiRowDataComplete(ExtMultiRowData extMultiRowData);
        void onCreateExtMultiRowDataError(int code);
    }

}
