package com.mx.mxSdk;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.List;

public class Compress {
    /**
     * 压缩打印头为552的数据
     * @param data72
     * @return
     */
    public static List<byte[]> compressRowDataArr(List<byte[]> data72) {
        // 检查输入列表的有效性
        if (data72 == null || data72.isEmpty()) {
            return new ArrayList<>();
        }

        List<byte[]> compressedData72Arr = new ArrayList<>(data72.size());

        for (byte[] d72 : data72) {
            int d72Len = d72.length;
            int width = d72Len / 72;
            int cWidth = (width + 1) / 2; // 向上取整计算压缩后的宽度

            byte[] c72 = new byte[cWidth * 72];

            for (int c = 0; c < cWidth; c++) {
                for (int r = 0; r < 72; r++) {
                    if (c < cWidth - 1) {
                        // 正常合并两列像素
                        byte bt0 = d72[r + (c * 2) * 72];
                        byte bt1 = d72[r + (c * 2 + 1) * 72];
                        c72[r + c * 72] = (byte) (bt0 | bt1);
                    } else {
                        // 处理最后一列时仅拷贝一列
                        c72[r + c * 72] = d72[r + (c * 2) * 72];
                    }
                }
            }
            compressedData72Arr.add(c72);
        }

        return compressedData72Arr;
    }


    /**
     * 单行数据压缩
     * @param d72
     * @return
     */
    public static byte[] compressRowData(byte[] d72) {
        int width = d72.length / 72;
        int cWidth = (width + 1) / 2; // 向上取整计算压缩后的宽度

        byte[] c72 = new byte[cWidth * 72];

        for (int c = 0; c < cWidth; c++) {
            for (int r = 0; r < 72; r++) {
                int colIndex1 = c * 2;
                int colIndex2 = colIndex1 + 1;
                
                if (colIndex1 < width) {
                    byte bt0 = d72[r + colIndex1 * 72];
                    
                    if (colIndex2 < width) {
                        // 正常情况：合并两列像素
                        byte bt1 = d72[r + colIndex2 * 72];
                        c72[r + c * 72] = (byte) (bt0 | bt1);
                    } else {
                        // 最后一列且宽度为奇数：仅拷贝一列像素
                        c72[r + c * 72] = bt0;
                    }
                }
            }
        }

        return c72;
    }


    /**
     * 模拟压缩和解压图片
     * @param pixel
     * @param width
     * @param height
     * @return
     */
    public static void simulationCompressWithUncompress(int[] pixel, int[] uncompress, int width, int height) {
        if (width <= 0 || height <= 0) return; // 检查宽度和高度的有效性
        if (pixel == null || uncompress == null) return; // 检查数组有效性
        if (pixel.length < width * height || uncompress.length < width * height) return; // 检查数组长度

        int cWidth = (width + 1) / 2; // 向上取整计算压缩后的宽度
        int[] c72 = new int[cWidth * height]; // 装载压缩数据的数组

        // 模拟压缩
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < cWidth; c++) {
                int colIndex1 = c * 2;
                int colIndex2 = colIndex1 + 1;
                
                // 添加边界检查
                if (colIndex1 >= width) break;
                
                int bt_0 = pixel[r * width + colIndex1];
                int bt_1 = (colIndex2 < width) ? pixel[r * width + colIndex2] : bt_0; // 最后一列用第一列的值填充

                c72[r * cWidth + c] = (bt_0 != 0xff000000 && bt_1 != 0xff000000) ? 0xffffffff : 0xff000000;
            }
        }

        // 模拟展开
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < cWidth; c++) {
                int color = c72[r * cWidth + c];
                int colIndex1 = c * 2;
                int colIndex2 = colIndex1 + 1;
                
                // 添加边界检查
                if (colIndex1 < width) {
                    uncompress[r * width + colIndex1] = color;
                }
                if (colIndex2 < width) {
                    uncompress[r * width + colIndex2] = color;
                }
            }
        }
    }

    public static void mergeSimulationCompressWithUncompress(int[] pixel, int[] uncompress, int width, int height) {
        if (width <= 0 || height <= 0) return; // 检查宽度和高度的有效性
        if (pixel == null || uncompress == null) return; // 检查数组有效性
        if (pixel.length < width * height || uncompress.length < width * height) return; // 检查数组长度

        int cWidth = (width + 1) / 2; // 向上取整计算压缩后的宽度
        int[] c72 = new int[cWidth * height]; // 装载压缩数据的数组

        // 同时进行压缩和解压操作
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < cWidth; c++) {
                int colIndex1 = c * 2;
                int colIndex2 = colIndex1 + 1;
                
                // 添加边界检查
                if (colIndex1 >= width) break;
                
                int bt_0 = pixel[r * width + colIndex1];
                int bt_1 = (colIndex2 < width) ? pixel[r * width + colIndex2] : bt_0; // 最后一列用第一列的值填充

                // 模拟压缩：只有当两列都是白色时才显示白色，否则显示黑色
                c72[r * cWidth + c] = (bt_0 != 0xff000000 && bt_1 != 0xff000000) ? 0xffffffff : 0xff000000;

                // 模拟展开（解压）
                int color = c72[r * cWidth + c];
                if (colIndex1 < width) {
                    uncompress[r * width + colIndex1] = color;
                }
                if (colIndex2 < width) {
                    uncompress[r * width + colIndex2] = color;
                }
            }
        }
    }

    public static Bitmap simulationCompressWithUncompressBitmap(Bitmap binaryBitmap) {
        if (binaryBitmap == null) return null;

        // 获取位图的宽度和高度
        int width = binaryBitmap.getWidth();
        int height = binaryBitmap.getHeight();
        if (width <= 0 || height <= 0) return null;
        
        int[] pixel = new int[width * height];
        binaryBitmap.getPixels(pixel, 0, width, 0, 0, width, height);

        // 计算压缩后的宽度
        int cWidth = (width + 1) / 2;
        int[] c72 = new int[cWidth * height]; // 装载压缩数据的数组

        // 模拟压缩
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < cWidth; c++) {
                int colIndex1 = c * 2;
                int colIndex2 = colIndex1 + 1;
                
                // 添加边界检查
                if (colIndex1 >= width) break;
                
                int bt_0 = pixel[r * width + colIndex1];
                int bt_1 = (colIndex2 < width) ? pixel[r * width + colIndex2] : bt_0; // 最后一列用第一列的值填充

                c72[r * cWidth + c] = (bt_0 != 0xff000000 && bt_1 != 0xff000000) ? 0xffffffff : 0xff000000;
            }
        }

        // 模拟展开
        int[] d72 = new int[width * height]; // 装载解压缩后的数据
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < cWidth; c++) {
                int color = c72[r * cWidth + c];
                int colIndex1 = c * 2;
                int colIndex2 = colIndex1 + 1;
                
                // 添加边界检查
                if (colIndex1 < width) {
                    d72[r * width + colIndex1] = color;
                }
                if (colIndex2 < width) {
                    d72[r * width + colIndex2] = color;
                }
            }
        }

        return Bitmap.createBitmap(d72, width, height, Bitmap.Config.RGB_565);
    }

    public static Bitmap mergeSimulationCompressWithUncompressBitmap(Bitmap binaryBitmap) {
        if (binaryBitmap == null) return null;
        
        // 获取位图的宽度和高度
        int width = binaryBitmap.getWidth();
        int height = binaryBitmap.getHeight();
        if (width <= 0 || height <= 0) return null;
        
        int[] originalPixel = new int[width * height];
        binaryBitmap.getPixels(originalPixel, 0, width, 0, 0, width, height);

        // 计算压缩后的宽度
        int cWidth = (width + 1) / 2;
        int[] c72 = new int[cWidth * height]; // 装载压缩数据的数组
        int[] resultPixel = new int[width * height]; // 创建新数组存储结果，避免修改原始数据

        // 同时进行压缩和解压操作
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < cWidth; c++) {
                int colIndex1 = c * 2;
                int colIndex2 = colIndex1 + 1;
                
                // 添加边界检查
                if (colIndex1 >= width) break;
                
                int bt_0 = originalPixel[r * width + colIndex1];
                int bt_1 = (colIndex2 < width) ? originalPixel[r * width + colIndex2] : bt_0; // 最后一列用第一列的值填充

                // 模拟压缩
                c72[r * cWidth + c] = (bt_0 != 0xff000000 && bt_1 != 0xff000000) ? 0xffffffff : 0xff000000;

                // 模拟展开（解压）
                int color = c72[r * cWidth + c];
                resultPixel[r * width + colIndex1] = color;
                if (colIndex2 < width) {
                    resultPixel[r * width + colIndex2] = color;
                }
            }
        }
        
        // 创建并返回解压后的 Bitmap，使用新创建的 resultPixel 数组
        return Bitmap.createBitmap(resultPixel, width, height, Bitmap.Config.RGB_565);
    }

}
