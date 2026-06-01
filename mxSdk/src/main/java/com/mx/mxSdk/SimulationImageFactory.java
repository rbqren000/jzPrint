package com.mx.mxSdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;

import com.mx.mxSdk.Utils.MxSdkStore;

/**
 * ========================================
 * 模拟图像生成工厂类
 * ========================================
 * 负责处理异步图像生成相关的功能：
 * - 异步模拟图像生成
 * - 灰度类型图像处理
 * - 图像保存和回调管理
 * ========================================
 */
public class SimulationImageFactory {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    //////////////////////////////////////////////////////////////////
    //                    回调接口定义
    //////////////////////////////////////////////////////////////////

    /**
     * 模拟图像创建监听器
     */
    public interface OnCreateSimulationImageListener {
        void onCreateSimulationImageStart();
        void onCreateSimulationImageComplete(String imagePath);
        void onCreateSimulationImageError(int code);
    }

    /**
     * 灰度类型模拟图像创建监听器
     */
    public interface OnCreateGrayTypeSimulationImageListener {
        void onCreateGrayTypeSimulationImageStart();
        void onCreateGrayTypeSimulationImageComplete(Bitmap bitmap);
        void onCreateGrayTypeSimulationImageError(int code);
    }

    /**
     * 灰度类型模拟图像创建并保存监听器
     */
    public interface OnCreateGrayTypeSimulationImageWithSaveListener {
        void onCreateGrayTypeSimulationImageStart();
        void onCreateGrayTypeSimulationImageComplete(String imagePath);
        void onCreateGrayTypeSimulationImageError(int code);
    }

    //////////////////////////////////////////////////////////////////
    //                    异步图像生成方法
    //////////////////////////////////////////////////////////////////

    /**
     * 异步创建模拟图像并保存
     */
    public static void asyncCreateSimulationBitmapFromBitmapWithSave(final Context context, final Bitmap bitmap, final int threshold, final boolean clearBackground, final boolean dithering, final boolean compress, final boolean isZoomTo552, final RowLayoutDirection rowLayoutDirection, @Nullable final int[] initialErrors, @Nullable final int[] lastRowErrors, final boolean transparentToWhiteAuto, final OnCreateSimulationImageListener onCreateSimulationImageListener) {

        if (bitmap == null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (onCreateSimulationImageListener != null) {
                        onCreateSimulationImageListener.onCreateSimulationImageError(MxImageUtils.MX_IMAGE_EMPTY_ERROR);
                    }
                }
            });
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 通知开始
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onCreateSimulationImageListener != null) {
                            onCreateSimulationImageListener.onCreateSimulationImageStart();
                        }
                    }
                });

                // 执行图像处理
                final Bitmap binaryBitmap = MxImageUtils.createSimulationBitmapFromBitmap(bitmap, threshold, clearBackground, dithering, compress, isZoomTo552, rowLayoutDirection, transparentToWhiteAuto, initialErrors, lastRowErrors);

                if (binaryBitmap == null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (onCreateSimulationImageListener != null) {
                                onCreateSimulationImageListener.onCreateSimulationImageError(MxImageUtils.MX_IMAGE_SIMULATION_EMPTY_ERROR);
                            }
                        }
                    });
                    return;
                }

                // 保存图像
                final String imagePath = MxSdkStore.saveImageToCache(context, binaryBitmap);

                if (imagePath == null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (onCreateSimulationImageListener != null) {
                                onCreateSimulationImageListener.onCreateSimulationImageError(MxImageUtils.MX_IMAGE_SAVE_ERROR);
                            }
                        }
                    });
                    return;
                }

                // 通知完成
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onCreateSimulationImageListener != null) {
                            onCreateSimulationImageListener.onCreateSimulationImageComplete(imagePath);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 异步创建灰度类型模拟图像
     */
    public static void asyncCreateSimulationBitmapFormBitmapGrayType(final Bitmap bitmap, final GrayType grayType, final int threshold, final boolean clearBackground, final boolean dithering, final boolean compress, final boolean isZoomTo552, final RowLayoutDirection rowLayoutDirection, @Nullable final int[] initialErrors, @Nullable final int[] lastRowErrors, final boolean transparentToWhiteAuto, final OnCreateGrayTypeSimulationImageListener onCreateGrayTypeSimulationImageListener) {

        if (bitmap == null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (onCreateGrayTypeSimulationImageListener != null) {
                        onCreateGrayTypeSimulationImageListener.onCreateGrayTypeSimulationImageError(MxImageUtils.MX_IMAGE_EMPTY_ERROR);
                    }
                }
            });
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 通知开始
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onCreateGrayTypeSimulationImageListener != null) {
                            onCreateGrayTypeSimulationImageListener.onCreateGrayTypeSimulationImageStart();
                        }
                    }
                });

                // 执行图像处理
                final Bitmap binaryBitmap = MxImageUtils.createSimulationBitmapFormBitmapGrayType(bitmap, grayType, threshold, clearBackground, dithering, compress, isZoomTo552, rowLayoutDirection, transparentToWhiteAuto, initialErrors, lastRowErrors);

                if (binaryBitmap == null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (onCreateGrayTypeSimulationImageListener != null) {
                                onCreateGrayTypeSimulationImageListener.onCreateGrayTypeSimulationImageError(MxImageUtils.MX_IMAGE_SIMULATION_EMPTY_ERROR);
                            }
                        }
                    });
                    return;
                }

                // 通知完成
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onCreateGrayTypeSimulationImageListener != null) {
                            onCreateGrayTypeSimulationImageListener.onCreateGrayTypeSimulationImageComplete(binaryBitmap);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 异步创建灰度类型模拟图像并保存
     */
    public static void createSimulationBitmapFormBitmapGrayTypeWithSave(final Context context, final Bitmap bitmap, GrayType grayType, final int threshold, final boolean clearBackground, final boolean dithering, final boolean compress, final boolean isZoomTo552, final RowLayoutDirection rowLayoutDirection, @Nullable final int[] initialErrors, @Nullable final int[] lastRowErrors, final boolean transparentToWhiteAuto, final OnCreateGrayTypeSimulationImageWithSaveListener onCreateGrayTypeSimulationImageWithSaveListener) {

        if (bitmap == null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (onCreateGrayTypeSimulationImageWithSaveListener != null) {
                        onCreateGrayTypeSimulationImageWithSaveListener.onCreateGrayTypeSimulationImageError(MxImageUtils.MX_IMAGE_EMPTY_ERROR);
                    }
                }
            });
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 通知开始
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onCreateGrayTypeSimulationImageWithSaveListener != null) {
                            onCreateGrayTypeSimulationImageWithSaveListener.onCreateGrayTypeSimulationImageStart();
                        }
                    }
                });

                // 执行图像处理
                final Bitmap binaryBitmap = MxImageUtils.createSimulationBitmapFormBitmapGrayType(bitmap, grayType, threshold, clearBackground, dithering, compress, isZoomTo552, rowLayoutDirection, transparentToWhiteAuto, initialErrors, lastRowErrors);

                if (binaryBitmap == null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (onCreateGrayTypeSimulationImageWithSaveListener != null) {
                                onCreateGrayTypeSimulationImageWithSaveListener.onCreateGrayTypeSimulationImageError(MxImageUtils.MX_IMAGE_SIMULATION_EMPTY_ERROR);
                            }
                        }
                    });
                    return;
                }

                // 保存图像
                final String imagePath = MxSdkStore.saveImageToCache(context, binaryBitmap);

                if (imagePath == null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (onCreateGrayTypeSimulationImageWithSaveListener != null) {
                                onCreateGrayTypeSimulationImageWithSaveListener.onCreateGrayTypeSimulationImageError(MxImageUtils.MX_IMAGE_SAVE_ERROR);
                            }
                        }
                    });
                    return;
                }

                // 通知完成
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onCreateGrayTypeSimulationImageWithSaveListener != null) {
                            onCreateGrayTypeSimulationImageWithSaveListener.onCreateGrayTypeSimulationImageComplete(imagePath);
                        }
                    }
                });
            }
        }).start();
    }
}