package com.org.jzprinter.print;

import android.content.Context;
import android.graphics.Bitmap;

import com.mx.mxSdk.ConnectManager;
import com.mx.mxSdk.MultiRowData;
import com.mx.mxSdk.MultiRowImage;
import com.mx.mxSdk.MultiRowImageFactory;
import com.mx.mxSdk.MultiRowDataFactory;
import com.mx.mxSdk.RowLayoutDirection;
import com.org.jzprinter.utils.ProcessingLogger;

public class PrintController {
    private final Context context;

    public PrintController(Context context) {
        this.context = context;
    }

    public void print(Bitmap bitmap, PrintCallback callback) {
        ProcessingLogger.startSession("print");

        try {
            ProcessingLogger.enterMethod("PrintController.print");

            ProcessingLogger.debug(String.format("生成Bitmap: %dpx × %dpx",
                bitmap.getWidth(), bitmap.getHeight()));

            ProcessingLogger.startStage("发送SDK");
            convertAndSend(bitmap, callback);
            ProcessingLogger.endStage("发送SDK");

            ProcessingLogger.exitMethod("打印任务已提交");
        } finally {
            ProcessingLogger.endSession();
        }
    }

    private void convertAndSend(Bitmap bitmap, PrintCallback callback) {
        if (callback != null) {
            callback.onStart();
        }

        MultiRowImageFactory.image2MultiRowImage(
            context,
            bitmap,
            RowLayoutDirection.RowLayoutDirectionVertical,
            0,
            new MultiRowImageFactory.OnCreateMultiRowImageListener() {
                @Override
                public void onCreateMultiRowImageStart() {
                }

                @Override
                public void onCreateMultiRowImageComplete(MultiRowImage multiRowImage) {
                    createMultiRowDataAndSend(multiRowImage, callback);
                }

                @Override
                public void onCreateMultiRowImageError(int code) {
                    if (callback != null) {
                        callback.onError(context.getString(com.org.jzprinter.R.string.error_split_image_failed) + code);
                    }
                }
            }
        );
    }

    private void createMultiRowDataAndSend(MultiRowImage multiRowImage, PrintCallback callback) {
        MultiRowDataFactory.bitmap2MultiRowData(
            context,
            multiRowImage,
            127,
            false,
            true,
            false,
            false,
            false,
            false,
            new MultiRowDataFactory.OnCreateMultiRowDataListener() {
                @Override
                public void onCreateMultiRowDataStart() {
                }

                @Override
                public void onCreateMultiRowDataComplete(MultiRowData multiRowData) {
                    sendToPrinter(multiRowData, callback);
                }

                @Override
                public void onCreateMultiRowDataError(int code) {
                    if (callback != null) {
                        callback.onError(context.getString(com.org.jzprinter.R.string.error_generate_print_data_failed) + code);
                    }
                }
            }
        );
    }

    private void sendToPrinter(MultiRowData data, PrintCallback callback) {
        ConnectManager connectManager = ConnectManager.share();

        connectManager.registerDataProgressListener(new ConnectManager.OnDataProgressListener() {
            @Override
            public void onDataProgressStart(float size, int progress, long startTime) {
                if (callback != null) {
                    callback.onProgress(0);
                }
            }

            @Override
            public void onDataProgress(float size, int progress, long startTime, long currentTime) {
                if (callback != null) {
                    callback.onProgress(progress);
                }
            }

            @Override
            public void onDataProgressFinish(float size, long startTime, long currentTime) {
                if (callback != null) {
                    callback.onComplete();
                }
            }

            @Override
            public void onDataProgressError(String error, int code) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });

        connectManager.setWithSendMultiRowDataPacket(data);
    }

    public interface PrintCallback {
        void onStart();
        void onProgress(int percentage);
        void onComplete();
        void onError(String error);
    }
}
