package com.mx.mxSdk;

import static com.mx.mxSdk.RowLayoutDirection.RowLayoutDirectionHorizontal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import com.mx.mxSdk.OpencvUtils.OpenCVUtils;
import com.mx.mxSdk.Utils.MxSdkStore;
import com.mx.mxSdk.Utils.RBQLog;
import java.util.Arrays;

public class MxImageUtils {

    public static final int MX_IMAGE_SIMULATION_EMPTY_ERROR = 100;
    public static final int MX_IMAGE_EMPTY_ERROR = 200;
    public static final int MX_IMAGE_SAVE_ERROR = 200;



    public static int[] bitmap2Pixel(Bitmap bitmap){
        //获取位图的宽
        int width = bitmap.getWidth();
        //获取位图的高
        int height = bitmap.getHeight();
        int[] pixel = new int[width * height];
        //通过位图的大小创建像素点数组
        bitmap.getPixels(pixel, 0, width, 0, 0, width, height);
        return pixel;
    }

    public static Bitmap pixel2Bitmap(int[] pixel,int width,int height){

        return Bitmap.createBitmap(pixel,width,height,Bitmap.Config.ARGB_8888);
    }

    public static Bitmap bitmap2Gray(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        int index = 0;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int color = pixels[index];
                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;

                // 用移位操作代替浮点运算
                int grey = (red * 77 + green * 151 + blue * 28) >> 8;
                pixels[index] = alpha | (grey << 16) | (grey << 8) | grey;
                index++;
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565);
    }


    /*
    public static void bitmapToGray(int[] pixels,int[] gray,int width,int height){

        for(int i = 0; i < height; i++)	{
            for(int j = 0; j < width; j++) {

                int color = pixels[width * i + j];

                int red = ((color  & 0x00FF0000 ) >> 16);
                int green = ((color & 0x0000FF00) >> 8);
                int blue = (color & 0x000000FF);

                int grey = (int)((float) red * 0.3 + (float)green * 0.59 + (float)blue * 0.11);
                gray[width * i + j] = grey;
            }
        }
    }
    */

    /**
     * 改进版本
     * @param pixels
     * @param gray
     * @param width
     * @param height
     */
    public static void bitmapToGray(int[] pixels, int[] gray, int width, int height) {
        int index = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int color = pixels[index];

                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;

                // 用移位运算代替除法运算，8192 = 128 * 64，为了保持精度，计算前乘以128
                int grey = (red * 77 + green * 151 + blue * 28) >> 8;
                gray[index] = grey;

                index++;
            }
        }
    }

    public static void bitmapToGray(int[] pixels, int[] gray, GrayType toGray, int width, int height) {
        int index = 0;
        int type = toGray.getType();

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int color = pixels[index];

                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;
                int grey = 0;

                switch (type) {
                    case 0: // RGB
                        grey = (red * 77 + green * 151 + blue * 28) >> 8;
                        break;
                    case 1: // R
                        grey = red;
                        break;
                    case 2: // G
                        grey = green;
                        break;
                    case 3: // B
                        grey = blue;
                        break;
                }

                gray[index] = grey;
                index++;
            }
        }
    }

    public static void formatGrayToFloydDithering(int[] gray, int width, int height, int threshold) {
        int e;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int index = row * width + col;
                int oldPixel = gray[index];
                int newPixel = (oldPixel > threshold) ? 255 : 0;
                gray[index] = newPixel;

                e = oldPixel - newPixel;

                // Distribute error to neighboring pixels, according to Floyd-Steinberg algorithm weights
                if (col + 1 < width) gray[index + 1] += e * 7 / 16;
                if (row + 1 < height) {
                    if (col > 0) gray[index + width - 1] += e * 3 / 16;
                    gray[index + width] += e * 5 / 16;
                    if (col + 1 < width) gray[index + width + 1] += e * 1 / 16;
                }
            }
        }
    }

    public static void formatGrayToFloydDithering(int[] gray, int width, int height, int threshold,@Nullable int[] initialErrors,@Nullable int[] lastRowErrors) {

        if (lastRowErrors != null) {
            Arrays.fill(lastRowErrors, 0);
        }
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {

                int index = row * width + col;

                // 初始化当前像素的误差
                if (row == 0 && initialErrors != null && initialErrors.length == width) {
                    gray[index] += initialErrors[col];
                }
                // 应用量化
                int oldPixel = gray[index];
                int newPixel = (oldPixel > threshold) ? 255 : 0;
                gray[index] = newPixel;

                int e = oldPixel - newPixel;

                // 将误差分配给周围的像素
                if (col + 1 < width) gray[index + 1] += e * 7 / 16;
                if (row + 1 < height) {
                    if (col > 0) gray[index + width - 1] += e * 3 / 16;
                    gray[index + width] += e * 5 / 16;
                    if (col + 1 < width) gray[index + width + 1] += e * 1 / 16;
                }

                // 更新最后一行的误差
                if (row == height - 1 && lastRowErrors != null && lastRowErrors.length == width) {
                    if (col > 0) lastRowErrors[col - 1] += e * 3 / 16;
                    lastRowErrors[col] += e * 5 / 16;
                    if (col + 1 < width) lastRowErrors[col + 1] += e * 1 / 16;
                }
            }
        }
    }

    public static void formatGrayToAtkinsonDithering(int[] pixels, int width, int height, int threshold) {
        int error;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int oldPixel = pixels[index];
                int newPixel = (oldPixel > threshold) ? 255 : 0;
                pixels[index] = newPixel;

                error = oldPixel - newPixel;

                // Spread error to neighboring pixels
                if (x + 1 < width) pixels[index + 1] += error * 1/8;
                if (x + 2 < width) pixels[index + 2] += error * 1/8;
                if (y + 1 < height) {
                    if (x > 0) pixels[index + width - 1] += error * 1/8;
                    pixels[index + width] += error * 1/8;
                    if (x + 1 < width) pixels[index + width + 1] += error * 1/8;
                }
                if (y + 2 < height) pixels[index + 2*width] += error * 1/8;
            }
        }
    }

    public static void formatGrayToBurkesDithering(int[] gray, int width, int height, int threshold) {
        int e;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int index = row * width + col;
                int oldPixel = gray[index];
                // Convert pixel to black or white based on threshold
                int newPixel = (oldPixel > threshold) ? 255 : 0;
                gray[index] = newPixel;

                // Calculate error
                e = oldPixel - newPixel;

                // Distribute the error to neighboring pixels, according to Burkes algorithm weights
                if (col + 1 < width) gray[index + 1] += e * 8/32;
                if (col + 2 < width) gray[index + 2] += e * 4/32;
                if (row + 1 < height) {
                    if (col > 1) gray[index + width - 2] += e * 2/32;
                    if (col > 0) gray[index + width - 1] += e * 4/32;
                    gray[index + width] += e * 8/32;
                    if (col + 1 < width) gray[index + width + 1] += e * 4/32;
                    if (col + 2 < width) gray[index + width + 2] += e * 2/32;
                }
            }
        }
    }

    /**
     * @param gray
     * @param binaryPixels
     * @param width
     * @param height
     * @param threshold
     */
    public static void grayToBinary(int[] gray, int[] binaryPixels, int width, int height, int threshold) {
        int index = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int g = gray[index];
                //大于阈值的被转化为白色，小与阈值的被转化为黑色
                binaryPixels[index] = (g >= threshold) ? 0xFFFFFFFF : 0xFF000000;
                index++;
            }
        }
    }

    /*
        图像逻辑高度：552 行
        打印头物理高度：576 行（72 字节 × 8 位）
        每列打印数据大小：72 字节
        喷头物理结构：分为左右2段（inkvia），并行工作
        每段打印行数：276 行
        每段打印周期：6 个时钟周期
        每时钟周期数据块大小：12字节
        数据布局：每个12字节块内，前6字节为上段数据，后6字节为下段数据
        总数据大小：6个时钟周期 × 12字节/周期 = 72字节
     */
    public static byte[] formatBinary69ToData72(int[] binaryPixels, int width, int height){
        // 69*8 = 552
        byte[] d69 = new byte[width * 69];

        for (int col = 0; col < width; col++) {

            for (int row = 0; row < height; row++) {

                int pixel = binaryPixels[col + width * row];// 0xffffffff 或者0xff000000

                // 检查并处理黑色像素
                if (pixel == 0xff000000) {
                    int s = col * 69 + row / 8;
                    d69[s] = (byte) (d69[s] | (0x80 >> (row % 8)));
                }
            }
        }

        //打印头结构：左右2两个inkvia，每个inkvia有552个喷嘴，总计1104个喷嘴，每列72字节数据格式，分上下两段
        byte[] d72 = new byte[width * 72];

        //下面将69byte高的图片，转成72byte
        for (int col = 0; col < width; col++) { // 宽度不定

            for (int row = 0; row < height; row++) { // 高度552

                // 从69字节中读取当前像素的状态，0 表示白色，1 表示黑色
                int pixelState = (d69[col * 69 + row / 8] & (0x80 >> (row % 8)));
                // 6次时钟周期打印一列数据
                // 基于单色打印头硬件文档PDATA表格的映射关系
                switch (row % 6) {
                    case 0:
                        // 对应硬件表格中6,12,18,24... → EA2-A2区域 → 第1个时钟周期
                        if (pixelState!= 0 ) {
                            int index = col * 72 + (row / 276) * 6 + 0 + ((row % 276) / 6) / 8;
                            d72[index] = (byte) (d72[index] | (0x80 >> (((row % 276) / 6) % 8)));
                        }
                        break;
                    case 4:
                        // 对应硬件表格中4,10,16,22... → EA1-A2区域 → 第2个时钟周期
                        if (pixelState != 0 ) {
                            int index = col * 72 + (row / 276) * 6 + 12 + ((row % 276 - 4) / 6) / 8;
                            d72[index] = (byte) (d72[index] | (0x80 >> (((row % 276) / 6) % 8)));
                        }
                        break;
                    case 2:
                        // 对应硬件表格中2,8,14,20... → EA0-A2区域 → 第3个时钟周期
                        if (pixelState != 0 ) {
                            int index = col * 72 + (row / 276) * 6 + 24 + ((row % 276 - 2) / 6) / 8;
                            d72[index] = (byte) (d72[index] | (0x80 >> (((row % 276) / 6) % 8)));
                        }
                        break;
                    case 5:
                        // 对应硬件表格中5,11,17,23... → EA2-A1区域 → 第4个时钟周期
                        if (pixelState != 0 ) {
                            int index = col * 72 + (row / 276) * 6 + 36 + ((row % 276 - 5) / 6) / 8;
                            d72[index] = (byte) (d72[index] | (0x80 >> (((row % 276) / 6) % 8)));
                        }
                        break;
                    case 1:
                        // 对应硬件表格中1,7,13,19... → EA0-A1区域 → 第5个时钟周期
                        if (pixelState != 0 ) {
                            int index = col * 72 + (row / 276) * 6 + 48 + ((row % 276 - 1) / 6) / 8;
                            d72[index] = (byte) (d72[index] | (0x80 >> (((row % 276) / 6) % 8)));
                        }
                        break;
                    case 3:
                        // 对应硬件表格中3,9,15,21... → EA1-A1区域 → 第6个时钟周期
                        if (pixelState != 0 ) {
                            int index = col * 72 + (row / 276) * 6 + 60 + ((row % 276 - 3) / 6) / 8;
                            d72[index] = (byte) (d72[index] | (0x80 >> (((row % 276) / 6) % 8)));
                        }
                        break;
                }
            }
        }

        return d72;
    }

    public static int validValueCountAfterCompress(int[] pixel, int width, int height) {
        if (width <= 0 || height <= 0) return 0; // 检查宽度和高度的有效性

        int cWidth = (width + 1) / 2; // 向上取整计算压缩后的宽度
        int validValueCount = 0; // 用于统计黑色像素的数量

        // 同时进行压缩和解压操作
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < cWidth; c++) {
                int bt_0 = pixel[r * width + c * 2];
                int bt_1 = (c < cWidth - 1) ? pixel[r * width + c * 2 + 1] : 0xff000000; // 最后一列特殊处理

                // 模拟压缩
                int color = (bt_0 != 0xff000000 && bt_1 != 0xff000000) ? 0xffffffff : 0xff000000;

                // 统计黑色像素
                if (color == 0xff000000) { // 统计黑色像素
                    validValueCount++; // 压缩后的黑色像素至少对应一个解压后的黑色像素
                    if (c < cWidth - 1) {
                        validValueCount++; // 如果不是最后一列，则增加一个黑色像素
                    }
                }
            }
        }

        return validValueCount; // 返回黑色像素的数量
    }

    public static D72Obj formatBinary69ToD72Obj(int[] pixels,boolean compress, int width, int height){

        int validValueCount = 0;
        byte[] d69 = new byte[width * 69];

        for (int col = 0; col < width; col++) {

            for (int row = 0; row < height; row++) {

                int pixel = pixels[col + width * row];// 0xffffffff 或者0xff000000

                // 检查并处理黑色像素
                if (pixel == 0xff000000) {//有值
                    validValueCount = validValueCount + 1;
                    int s = col * 69 + row / 8;
                    d69[s] = (byte) (d69[s] | (0x80 >> (row % 8)));
                }
            }
        }

        if (compress){
            validValueCount = validValueCountAfterCompress(pixels,width,height);
        }

        byte[] d72 = new byte[width * 72];

        //下面将69byte高的图片，转成72byte
        for (int col = 0; col < width; col++) { // 宽度不定

            for (int row = 0; row < height; row++) { // 高度552

                // %6 --> which cycle
                int cycle = (d69[col * 69 + row / 8] & (0x80 >> (row % 8)));

                switch (row % 6) {
                    // cycle 1
                    case 0:
                        if (cycle!= 0 ) {
                            int index = col * 72 + (row / 276) * 6 + ((row % 276) / 6) / 8;
                            d72[index] = (byte) (d72[index] | (0x80 >> (((row % 276) / 6) % 8)));
                        }
                        break;
                    // cycle 2
                    case 4://注释掉没啥变化
                        if (cycle != 0 ) {
                            int index = col * 72 + (row / 276) * 6 + 12 + ((row % 276 - 4) / 6) / 8;
                            d72[index] = (byte) (d72[index] | (0x80 >> (((row % 276) / 6) % 8)));
                        }
                        break;
                    // cycle 3
                    case 2://注释掉没啥变化
                        if (cycle != 0 ) {
                            int index = col * 72 + (row / 276) * 6 + 24 + ((row % 276 - 2) / 6) / 8;
                            d72[index] = (byte) (d72[index] | (0x80 >> (((row % 276) / 6) % 8)));
                        }
                        break;
                    // cycle 4
                    case 5: //有变化
                        if (cycle != 0 ) {
                            int index = col * 72 + (row / 276) * 6 + 36 + ((row % 276 - 5) / 6) / 8;
                            d72[index] = (byte) (d72[index] | (0x80 >> (((row % 276) / 6) % 8)));
                        }
                        break;
                    // cycle 5
                    case 1://有变化
                        if (cycle != 0 ) {
                            int index = col * 72 + (row / 276) * 6 + 48 + ((row % 276 - 1) / 6) / 8;
                            d72[index] = (byte) (d72[index] | (0x80 >> (((row % 276) / 6) % 8)));
                        }
                        break;
//                         cycle 6
                    case 3://有变化
                        if (cycle != 0 ) {
                            int index = col * 72 + (row / 276) * 6 + 60 + ((row % 276 - 3) / 6) / 8;
                            d72[index] = (byte) (d72[index] | (0x80 >> (((row % 276) / 6) % 8)));
                        }
                        break;
                }
            }
        }
        return new D72Obj(d72,validValueCount);
    }

    //////////////////////////////////////////////////////////////////
    //                       CMYK 相关函数开始
    //////////////////////////////////////////////////////////////////
    


    /*
        图像逻辑高度：640 行
        打印头物理高度：640 行（80 字节 × 8 位）
        每列打印数据大小：80 字节
        喷头物理结构：采用单路非分段结构
        总打印周期：16 个时钟周期完成一列数据的打印
        每周期打印内容：一列数据的 1/16
        每周期数据大小：5个字节 (80 / 16 = 5)
        总周期数据大小：16 * 5 = 80字节
     */
    public static byte[][] formatCMYKBinaryToData(int[][] cmykBinary,int width, int height) {
        if (cmykBinary == null || cmykBinary.length != 4) {
            return null;
        }
        byte[][] d80 = new byte[4][width * 80];
        for (int channelIndex = 0; channelIndex < 4; channelIndex++) {
            int[] channelData = cmykBinary[channelIndex];
            for (int col = 0; col < width; col++) {
                for (int row = 0; row < height; row++) {
                    int index = row * width + col;
                    int channelDataValue = channelData[index];
                    if (channelDataValue == 1) {
                        // 计算要放置的字节位置
                        int s = col * 80 + row / 8;
                        // 计算当前像素要放到字节的位置
                        int t = row % 8;
                        // 将当前像素放到对应的字节中  0x80 既二进制的10000000
                        d80[channelIndex][s] = (byte) (d80[channelIndex][s] | (0x80 >> t));
                    }
                }
            }
        }
        //将数据映射成打印头驱动器需要的数据
        byte[][] data80 = new byte[4][width * 80];
        for (int channelIndex = 0; channelIndex < 4; channelIndex++) {

            byte[] channelD80 = d80[channelIndex];

            for (int col = 0; col < width; col++) { // 宽度不定

                for (int row = 0; row < height; row++) { // 高度640
                    // 计算要获取哪个字节
                    int s = col * 80 + row / 8;
                    // 计算当前像素在字节中的位置
                    int t = row % 8;
                    // 从 80 字节中读取当前像素的状态，结果为 0 或非零（位掩码）
                    int pixelState = (channelD80[s] & (0x80 >> t));
                    // 16次时钟周期打印一列数据，每组数据5个字节，一列数据有80字节，每个字节有8位，所以一个字节可以打印8个像素
                    // 基于硬件文档PDATA表格的映射关系
                    // 0x80 = 10000000（二进制）
                    switch (row % 16) {
                        case 0: // 序号 Cycle 1
                            /*
                             * [row % 16 == 0] 对应: Cycle 1 / Heater 1
                             * 硬件地址: 1, 17, 33, ... (共 40 个喷头)
                             * 硬件区域: EA0-A1
                             * 数据映射: 映射到当前列(col)数据块的第 0 组 (字节 0-4), baseOffset = 0
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 0 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 1: // 序号 Cycle 2
                            /*
                             * [row % 16 == 1] 对应: Cycle 2 / Heater 2
                             * 硬件地址: 2, 18, 34, ... (共 40 个喷头)
                             * 硬件区域: EA0-A2
                             * 数据映射: 映射到当前列(col)数据块的第 1 组 (字节 5-9), baseOffset = 5
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 5 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 2: // 序号 Cycle 3
                            /*
                             * [row % 16 == 2] 对应: Cycle 3 / Heater 11
                             * 硬件地址: 11, 27, 43, ... (共 40 个喷头)
                             * 硬件区域: EA1-A5
                             * 数据映射: 映射到当前列(col)数据块的第 2 组 (字节 10-14), baseOffset = 10
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 10 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 3: // 序号 Cycle 4
                            /*
                             * [row % 16 == 3] 对应: Cycle 4 / Heater 12
                             * 硬件地址: 12, 28, 44, ... (共 40 个喷头)
                             * 硬件区域: EA1-A6
                             * 数据映射: 映射到当前列(col)数据块的第 3 组 (字节 15-19), baseOffset = 15
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 15 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 4: // 序号 Cycle 5
                            /*
                             * [row % 16 == 4] 对应: Cycle 5 / Heater 7
                             * 硬件地址: 7, 23, 39, ... (共 40 个喷头)
                             * 硬件区域: EA1-A1
                             * 数据映射: 映射到当前列(col)数据块的第 4 组 (字节 20-24), baseOffset = 20
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 20 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 5: // 序号 Cycle 6
                            /*
                             * [row % 16 == 5] 对应: Cycle 6 / Heater 8
                             * 硬件地址: 8, 24, 40, ... (共 40 个喷头)
                             * 硬件区域: EA1-A2
                             * 数据映射: 映射到当前列(col)数据块的第 5 组 (字节 25-29), baseOffset = 25
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 25 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 6: // 序号 Cycle 7
                            /*
                             * [row % 16 == 6] 对应: Cycle 7 / Heater 13
                             * 硬件地址: 13, 29, 45, ... (共 40 个喷头)
                             * 硬件区域: EA2-A1
                             * 数据映射: 映射到当前列(col)数据块的第 6 组 (字节 30-34), baseOffset = 30
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 30 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 7: // 序号 Cycle 8
                            /*
                             * [row % 16 == 7] 对应: Cycle 8 / Heater 14
                             * 硬件地址: 14, 30, 46, ... (共 40 个喷头)
                             * 硬件区域: EA2-A2
                             * 数据映射: 映射到当前列(col)数据块的第 7 组 (字节 35-39), baseOffset = 35
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 35 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 8: // 序号 Cycle 9
                            /*
                             * [row % 16 == 8] 对应: Cycle 9 / Heater 5
                             * 硬件地址: 5, 21, 37, ... (共 40 个喷头)
                             * 硬件区域: EA0-A5
                             * 数据映射: 映射到当前列(col)数据块的第 8 组 (字节 40-44), baseOffset = 40
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 40 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 9: // 序号 Cycle 10
                            /*
                             * [row % 16 == 9] 对应: Cycle 10 / Heater 6
                             * 硬件地址: 6, 22, 38, ... (共 40 个喷头)
                             * 硬件区域: EA0-A6
                             * 数据映射: 映射到当前列(col)数据块的第 9 组 (字节 45-49), baseOffset = 45
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 45 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 10: // 序号 Cycle 11
                            /*
                             * [row % 16 == 10] 对应: Cycle 11 / Heater 15
                             * 硬件地址: 15, 31, 47, ... (共 40 个喷头)
                             * 硬件区域: EA2-A3
                             * 数据映射: 映射到当前列(col)数据块的第 10 组 (字节 50-54), baseOffset = 50
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 50 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 11: // 序号 Cycle 12
                            /*
                             * [row % 16 == 11] 对应: Cycle 12 / Heater 16
                             * 硬件地址: 16, 32, 48, ... (共 40 个喷头)
                             * 硬件区域: EA2-A4
                             * 数据映射: 映射到当前列(col)数据块的第 11 组 (字节 55-59), baseOffset = 55
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 55 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 12: // 序号 Cycle 13
                            /*
                             * [row % 16 == 12] 对应: Cycle 13 / Heater 3
                             * 硬件地址: 3, 19, 35, ... (共 40 个喷头)
                             * 硬件区域: EA0-A3
                             * 数据映射: 映射到当前列(col)数据块的第 12 组 (字节 60-64), baseOffset = 60
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 60 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 13: // 序号 Cycle 14
                            /*
                             * [row % 16 == 13] 对应: Cycle 14 / Heater 4
                             * 硬件地址: 4, 20, 36, ... (共 40 个喷头)
                             * 硬件区域: EA0-A4
                             * 数据映射: 映射到当前列(col)数据块的第 13 组 (字节 65-69), baseOffset = 65
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 65 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 14: // 序号 Cycle 15
                            /*
                             * [row % 16 == 14] 对应: Cycle 15 / Heater 9
                             * 硬件地址: 9, 25, 41, ... (共 40 个喷头)
                             * 硬件区域: EA1-A3
                             * 数据映射: 映射到当前列(col)数据块的第 14 组 (字节 70-74), baseOffset = 70
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 70 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                        case 15: // 序号 Cycle 16
                            /*
                             * [row % 16 == 15] 对应: Cycle 16 / Heater 10
                             * 硬件地址: 10, 26, 42, ... (共 40 个喷头)
                             * 硬件区域: EA1-A4
                             * 数据映射: 映射到当前列(col)数据块的第 15 组 (字节 75-79), baseOffset = 75
                             * 字节偏移: (row / 16) / 8 计算在 5 字节组内的字节偏移 (0-4)
                             */
                            if (pixelState != 0) {
                                int index = col * 80 + 75 + (row / 16) / 8;
                                data80[channelIndex][index] = (byte) (data80[channelIndex][index] | (0x80 >> ((row / 16) % 8)));
                            }
                            break;
                    }

                }
            }
        }
        return data80;
    }


    public static String createSimulationBitmapFromBitmapWithSave(Context context, Bitmap bitmap, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean isZoomTo552, RowLayoutDirection rowLayoutDirection, boolean transparentToWhiteAuto,@Nullable int[] initialErrors, @Nullable int[] lastRowErrors){

        Bitmap binaryBitmap = createSimulationBitmapFromBitmap(bitmap,threshold,clearBackground,dithering,compress, isZoomTo552,rowLayoutDirection,transparentToWhiteAuto,initialErrors,lastRowErrors);
        return MxSdkStore.saveImageToCache(context,binaryBitmap);
    }

    public static Bitmap createSimulationBitmapFromBitmap(Bitmap bitmap, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean isZoomTo552, RowLayoutDirection rowLayoutDirection, boolean transparentToWhiteAuto,@Nullable int[] initialErrors, @Nullable int[] lastRowErrors){
        if (transparentToWhiteAuto && OpenCVUtils.isTransparent(bitmap)){
            bitmap = transparentBGtoWhite(bitmap);
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if(height != 552 && isZoomTo552){

            float scale = 552.0f/(float) height;

            height = 552;
            width = (int)((float)width * scale);

            bitmap  = MxImageUtils.resizeBitmapToHeight552(bitmap);

        }

        if (clearBackground){
            bitmap = OpenCVUtils.lightClearBackground(bitmap);
        }

        int[] thumbnailPixels = new int[width * height];	//通过位图的大小创建像素点数组
        bitmap.getPixels(thumbnailPixels, 0, width, 0, 0, width, height);

        return createSimulationBitmapFromPixels(thumbnailPixels,width,height,threshold,dithering,compress,rowLayoutDirection,initialErrors,lastRowErrors);
    }

    public static String createSimulationBitmapFromPixelsWithSave(Context context, int[] pixels, int width, int height, int threshold, boolean dithering, boolean compress, RowLayoutDirection rowLayoutDirection,@Nullable int[] initialErrors, @Nullable int[] lastRowErrors){
        Bitmap binaryBitmap = createSimulationBitmapFromPixels(pixels,width,height,threshold,dithering,compress,rowLayoutDirection,initialErrors,lastRowErrors);
        return MxSdkStore.saveImageToCache(context,binaryBitmap);
    }

    public static Bitmap createSimulationBitmapFromPixels(int[] pixels, int width, int height, int threshold, boolean dithering, boolean compress, RowLayoutDirection rowLayoutDirection, @Nullable int[] initialErrors, @Nullable int[] lastRowErrors){

        int[] thumbnailGray = new int[width * height];	//通过位图的大小创建像素点数组

        bitmapToGray(pixels,thumbnailGray, width, height);
        if (dithering) {
            formatGrayToFloydDithering(thumbnailGray, width, height,threshold,initialErrors,lastRowErrors);
        }
        //黑白图
        grayToBinary(thumbnailGray,pixels, width, height,threshold);
        Bitmap binaryBitmap;
        if (compress){
            //生成预览图
            int[] uncompress = new int[width*height];
            Compress.mergeSimulationCompressWithUncompress(pixels,uncompress, width, height);
            binaryBitmap = Bitmap.createBitmap(uncompress, width, height,Bitmap.Config.ARGB_8888);
        }else {
            //生成预览图
            binaryBitmap = Bitmap.createBitmap(pixels, width, height,Bitmap.Config.ARGB_8888);
        }
        if(rowLayoutDirection == RowLayoutDirectionHorizontal){
            binaryBitmap = rotatedImageByRadians(binaryBitmap,-Math.PI*0.5f);
        }
        return binaryBitmap;
    }

    public static String createSimulationBitmapFromBinaryPixelsWithSave(Context context, int[] binaryPixels, int width, int height,boolean compress, RowLayoutDirection rowLayoutDirection){
        Bitmap binaryBitmap = createSimulationBitmapFromBinaryPixels(binaryPixels,width,height,compress,rowLayoutDirection);
        return MxSdkStore.saveImageToCache(context,binaryBitmap);
    }

    public static Bitmap createSimulationBitmapFromBinaryPixels(int[] binaryPixels, int width, int height,boolean compress, RowLayoutDirection rowLayoutDirection){

        Bitmap binaryBitmap;
        if (compress){
            //生成预览图
            int[] uncompress = new int[width*height];
            Compress.mergeSimulationCompressWithUncompress(binaryPixels,uncompress, width, height);
            binaryBitmap = Bitmap.createBitmap(uncompress, width, height,Bitmap.Config.ARGB_8888);
        }else {
            //生成预览图
            binaryBitmap = Bitmap.createBitmap(binaryPixels, width, height,Bitmap.Config.ARGB_8888);
        }
        if(rowLayoutDirection == RowLayoutDirectionHorizontal){
            binaryBitmap = rotatedImageByRadians(binaryBitmap,-Math.PI*0.5f);
        }
        return binaryBitmap;
    }

    public static String createSimulationBitmapFromCmykBinaryPixelsWithSave(Context context, int[][] cmykBinaryPixels, int width, int height, boolean compress, RowLayoutDirection rowLayoutDirection){
        Bitmap binaryBitmap = createSimulationBitmapFromCmykBinaryPixels(cmykBinaryPixels,width,height,rowLayoutDirection);
        return MxSdkStore.saveImageToCache(context,binaryBitmap);
    }

    public static Bitmap createSimulationBitmapFromCmykBinaryPixels(int[][] cmykBinaryPixels, int width, int height,RowLayoutDirection rowLayoutDirection){
        //将CMYK数据转成RGB数据
        int[] thumbnailPixels = new int[width * height];    //通过位图的大小创建像素点数组
        cmykToRgb(cmykBinaryPixels, thumbnailPixels, width, height);
        Bitmap binaryBitmap = Bitmap.createBitmap(thumbnailPixels, width, height,Bitmap.Config.ARGB_8888);
        if(rowLayoutDirection == RowLayoutDirectionHorizontal){
            binaryBitmap = rotatedImageByRadians(binaryBitmap,-Math.PI*0.5f);
        }
        return binaryBitmap;
    }

    /**
     * 将CMYK二值化数据转换为RGB格式（用于模拟显示）
     * 
     * @param cmykBinaryPixels CMYK四通道二值化数据，按CYAN、MAGENTA、YELLOW、BLACK顺序排列
     *                        每个通道的值只有100（有墨）或0（无墨）
     * @param thumbnailPixels 输出的RGB像素数组，每个元素是ARGB格式的整数
     * @param width 图像宽度
     * @param height 图像高度
     */
    public static void cmykToRgb(int[][] cmykBinaryPixels, int[] thumbnailPixels, int width, int height){
        if (cmykBinaryPixels == null || thumbnailPixels == null || cmykBinaryPixels.length != 4) {
            return;
        }
        
        int totalPixels = width * height;
        
        // 确保thumbnailPixels数组大小正确
        if (thumbnailPixels.length != totalPixels) {
            return;
        }
        
        // 确保每个CMYK通道的数组大小正确
        for (int i = 0; i < 4; i++) {
            if (cmykBinaryPixels[i] == null || cmykBinaryPixels[i].length != totalPixels) {
                return;
            }
        }
        
        // alpha通道固定为255（不透明）
        int alpha = 0xFF << 24;
        
        // 遍历所有像素
        for (int i = 0; i < totalPixels; i++) {
            // 获取CMYK二值化值（只有100或0）
            boolean hasCyan = cmykBinaryPixels[0][i] == 100;    // Cyan
            boolean hasMagenta = cmykBinaryPixels[1][i] == 100; // Magenta
            boolean hasYellow = cmykBinaryPixels[2][i] == 100;  // Yellow
            boolean hasBlack = cmykBinaryPixels[3][i] == 100;   // Black
            
            // 计算RGB值 - 模拟CMYK油墨叠加效果
            int r = 255, g = 255, b = 255; // 从白色开始
            
            // 如果有黑色墨水，直接设为黑色
            if (hasBlack) {
                r = g = b = 0;
            } else {
                // 否则根据CMY墨水计算颜色
                // Cyan减少红色分量
                if (hasCyan) {
                    r = 0;
                }
                // Magenta减少绿色分量
                if (hasMagenta) {
                    g = 0;
                }
                // Yellow减少蓝色分量
                if (hasYellow) {
                    b = 0;
                }
            }
            
            // 组合成ARGB格式
            thumbnailPixels[i] = alpha | (r << 16) | (g << 8) | b;
        }
    }

    //////////////////////////////////////////////////////////////////
    //                       支持传入转灰度图时使用RGB、R、G、B 开始位置
    //////////////////////////////////////////////////////////////////

    public static String createSimulationBitmapFormBitmapGrayTypeWithSave(Context context, Bitmap bitmap, GrayType grayType, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean isZoomTo552, RowLayoutDirection rowLayoutDirection, boolean transparentToWhiteAuto,@Nullable int[] initialErrors, @Nullable int[] lastRowErrors){

        Bitmap binaryBitmap = createSimulationBitmapFormBitmapGrayType(bitmap,grayType,threshold,clearBackground,dithering,compress, isZoomTo552,rowLayoutDirection,transparentToWhiteAuto,initialErrors,lastRowErrors);
        return MxSdkStore.saveImageToCache(context,binaryBitmap);
    }

    public static Bitmap createSimulationBitmapFormBitmapGrayType(Bitmap bitmap, GrayType grayType, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean isZoomTo552, RowLayoutDirection rowLayoutDirection, boolean transparentToWhiteAuto,@Nullable int[] initialErrors, @Nullable int[] lastRowErrors){
        if (transparentToWhiteAuto && OpenCVUtils.isTransparent(bitmap)){
            bitmap = transparentBGtoWhite(bitmap);
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if(height != 552 && isZoomTo552){

            float scale = 552.0f/(float) height;

            height = 552;
            width = (int)((float)width * scale);

            bitmap  = MxImageUtils.resizeBitmapToHeight552(bitmap);

        }

        if (clearBackground){
            bitmap = OpenCVUtils.lightClearBackground(bitmap);
        }

        int[] thumbnailPixels = new int[width * height];	//通过位图的大小创建像素点数组
        bitmap.getPixels(thumbnailPixels, 0, width, 0, 0, width, height);

        return createSimulationBitmapFormPixelsGrayType(thumbnailPixels,grayType,width,height,threshold,dithering,compress,rowLayoutDirection,initialErrors,lastRowErrors);
    }


    public static String createSimulationBitmapFormPixelsGrayTypeWithSave(Context context, int[] pixels, GrayType grayType, int width, int height, int threshold, boolean dithering, boolean compress, RowLayoutDirection rowLayoutDirection,@Nullable int[] initialErrors, @Nullable int[] lastRowErrors){
        Bitmap binaryBitmap = createSimulationBitmapFormPixelsGrayType(pixels,grayType,width,height,threshold,dithering,compress,rowLayoutDirection,initialErrors,lastRowErrors);
        return MxSdkStore.saveImageToCache(context,binaryBitmap);
    }

    public static Bitmap createSimulationBitmapFormPixelsGrayType(int[] pixels,GrayType grayType, int width, int height, int threshold, boolean dithering, boolean compress, RowLayoutDirection rowLayoutDirection, @Nullable int[] initialErrors, @Nullable int[] lastRowErrors){

        int[] thumbnailGray = new int[width * height];	//通过位图的大小创建像素点数组

        bitmapToGray(pixels,thumbnailGray,grayType, width, height);
        if (dithering) {
            formatGrayToFloydDithering(thumbnailGray, width, height,threshold,initialErrors,lastRowErrors);
        }
        //黑白图
        grayToBinary(thumbnailGray,pixels, width, height,threshold);
        Bitmap binaryBitmap;
        if (compress){
            //生成预览图
            int[] uncompress = new int[width*height];
            Compress.mergeSimulationCompressWithUncompress(pixels,uncompress, width, height);
            binaryBitmap = Bitmap.createBitmap(uncompress, width, height,Bitmap.Config.ARGB_8888);
        }else {
            //生成预览图
            binaryBitmap = Bitmap.createBitmap(pixels, width, height,Bitmap.Config.ARGB_8888);
        }
        if(rowLayoutDirection == RowLayoutDirectionHorizontal){
            binaryBitmap = rotatedImageByRadians(binaryBitmap,-Math.PI*0.5f);
        }
        return binaryBitmap;
    }
    //////////////////////////////////////////////////////////////////
    //               支持传入转灰度图时使用RGB、R、G、B 结束位置
    //////////////////////////////////////////////////////////////////

    /**
     *
     * @param pixels // 输入像素数组 (ARGB)
     * @param channel // 输出的单通道 CMYK 数据 (C、M、Y 或 K)
     * @param width  // 图像宽度
     * @param height  // 图像高度
     * @param cmykType // 指定通道 (1: C, 2: M, 3: Y, 4: K)
     */
    public static void bitmapToCMYKChannel(int[] pixels, float[] channel, int width, int height, CMYKType cmykType) {

        int index = 0;
        int type = cmykType.getType();

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int color = pixels[index];

                // 提取 RGB 分量
                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;

                // 计算 CMYK 值
                float rf = red / 255f;
                float gf = green / 255f;
                float bf = blue / 255f;

                float k = 1 - Math.max(rf, Math.max(gf, bf));
                
                float c, m, y;
                if (k < 1.0f) {
                    // 避免除以0
                    float denominator = 1 - k;
                    c = (1 - rf - k) / denominator;
                    m = (1 - gf - k) / denominator;
                    y = (1 - bf - k) / denominator;
                } else {
                    // 当k=1时，其他通道都为0
                    c = 0;
                    m = 0;
                    y = 0;
                }

                // 选择指定通道的数据
                switch (type) {
                    case 1:
                        channel[index] = c;
                        break;
                    case 2:
                        channel[index] = m;
                        break;
                    case 3:
                        channel[index] = y;
                        break;
                    case 4:
                        channel[index] = k;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid channel type");
                }
                index++;
            }
        }
    }

    /**
     *
     * @param channel // 输入/输出的单通道 CMYK 数据
     * @param width  // 图像宽度
     * @param height // 图像高度
     * @param threshold // 阈值 (0.5)
     * @param initialErrors // 初始误差
     * @param lastRowErrors  // 最后一行误差
     */
    public static void floydSteinbergDitheringCMYKChannel(float[] channel, int width, int height, float threshold, @Nullable float[] initialErrors, @Nullable float[] lastRowErrors) {
        // 初始化最后一行误差
        if (lastRowErrors != null) {
            Arrays.fill(lastRowErrors, 0);
        }

        // 遍历每个像素
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int index = row * width + col;

                // 初始化当前像素的误差
                if (row == 0 && initialErrors != null && initialErrors.length == width) {
                    channel[index] += initialErrors[col];
                }

                // 应用量化
                float oldPixel = channel[index];
                float newPixel = (oldPixel > threshold) ? 1.0f : 0.0f;
                channel[index] = newPixel;

                // 计算误差
                float error = oldPixel - newPixel;

                // 扩散误差
                if (col + 1 < width) channel[index + 1] += error * 7 / 16f;
                if (row + 1 < height) {
                    if (col > 0) channel[index + width - 1] += error * 3 / 16f;
                    channel[index + width] += error * 5 / 16f;
                    if (col + 1 < width) channel[index + width + 1] += error * 1 / 16f;
                }

                // 更新最后一行的误差
                if (row == height - 1 && lastRowErrors != null && lastRowErrors.length == width) {
                    if (col > 0) lastRowErrors[col - 1] += error * 3 / 16f;
                    lastRowErrors[col] += error * 5 / 16f;
                    if (col + 1 < width) lastRowErrors[col + 1] += error * 1 / 16f;
                }
            }
        }
    }

    /**
     *
     * @param channels  // 输入/输出的 CMYK 通道数据，二维数组 [4][width * height]
     * @param width // 图像宽度
     * @param height // 图像高度
     * @param thresholds // 每个通道的阈值数组 [C, M, Y, K]
     * @param initialErrors // 初始误差数组 [4][width]
     * @param lastRowErrors // 最后一行误差数组 [4][width]
     */
    public static void floydSteinbergDitheringCMYK(float[][] channels, int width, int height, float[] thresholds, @Nullable float[][] initialErrors, @Nullable float[][] lastRowErrors) {

        if (channels.length != 4 || thresholds.length != 4) {
            throw new IllegalArgumentException("Channels and thresholds must have a length of 4 for CMYK");
        }

        for (int t = 0; t < 4; t++) { // 对每个通道分别处理
            float[] channel = channels[t];
            float threshold = thresholds[t];

            // 初始化误差
            if (lastRowErrors != null && lastRowErrors[t] != null) {
                Arrays.fill(lastRowErrors[t], 0);
            }

            // 遍历每个像素
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    int index = row * width + col;

                    // 初始化当前像素误差
                    if (row == 0 && initialErrors != null && initialErrors[t] != null && initialErrors[t].length == width) {
                        channel[index] += initialErrors[t][col];
                    }

                    // 应用量化
                    float oldPixel = channel[index];
                    float newPixel = (oldPixel > threshold) ? 1.0f : 0.0f;
                    channel[index] = newPixel;

                    // 计算误差
                    float error = oldPixel - newPixel;

                    // 扩散误差
                    if (col + 1 < width) channel[index + 1] += error * 7 / 16f;
                    if (row + 1 < height) {
                        if (col > 0) channel[index + width - 1] += error * 3 / 16f;
                        channel[index + width] += error * 5 / 16f;
                        if (col + 1 < width) channel[index + width + 1] += error * 1 / 16f;
                    }

                    // 更新最后一行的误差
                    if (row == height - 1 && lastRowErrors != null && lastRowErrors[t] != null) {
                        if (col > 0) lastRowErrors[t][col - 1] += error * 3 / 16f;
                        lastRowErrors[t][col] += error * 5 / 16f;
                        if (col + 1 < width) lastRowErrors[t][col + 1] += error * 1 / 16f;
                    }
                }
            }
        }
    }


    //////////////////////////////////////////////////////////////////
    //                    图像处理工具函数开始
    //////////////////////////////////////////////////////////////////
    
    /**
     * ========================================
     * 图像处理工具相关函数
     * ========================================
     * 包含以下功能：
     * - 透明度检测和处理
     * - 图像缩放和旋转
     * - 缩略图生成
     * - 图像预处理工具
     * ========================================
     */

    /**
     * 检查图片是否为透明图片
     * @param bitmap 要检查的Bitmap
     * @param sampleRate 采样率，值越大采样率越低，处理速度越快
     * @param transparencyThreshold 透明像素比例阈值
     * @return 如果图片透明则返回true，否则返回false
     */
    public static boolean isTransparent(Bitmap bitmap, int sampleRate, float transparencyThreshold) {
        if (bitmap == null) {
            return false;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int totalPixels = (width / sampleRate) * (height / sampleRate);
        int transparentPixels = 0;

        boolean checkedTransparentPixel = false;
        for (int y = 0; y < height&&!checkedTransparentPixel; y += sampleRate) {
            for (int x = 0; x < width; x += sampleRate) {
                int pixel = bitmap.getPixel(x, y);
                if (Color.alpha(pixel) < 255) {
                    transparentPixels++;
                    // 如果检测到透明点，立即停止遍历
                    checkedTransparentPixel = true;
                    break;
                }
            }
        }

        float transparencyRatio = (float) transparentPixels / (float) totalPixels;
        RBQLog.i("当前测量点的数量:" + totalPixels + "; 透明点的数量:" + transparentPixels);
        RBQLog.i("当前的透明度比率为:" + transparencyRatio);

        return transparencyRatio > transparencyThreshold;
    }

    /**
     * 默认采样率和透明度阈值
     * @param bitmap 要检查的Bitmap
     * @return 如果图片透明则返回true，否则返回false
     */
    public static boolean isTransparent(Bitmap bitmap) {
        float defaultTransparencyThreshold = 0.1f; // 默认透明度阈值

        // 计算采样率，根据图片的大小动态调整
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int imageSize = width * height;
        int defaultSampleRate = Math.max(1, imageSize / 1000); // 适当调整分母的值

        return isTransparent(bitmap, defaultSampleRate, defaultTransparencyThreshold);
    }


    /**
     * 将透明背景的Bitmap转换为白色背景的Bitmap
     * @param originalBitmap 原始Bitmap
     * @return 带白色背景的Bitmap
     */
    public static Bitmap transparentBGtoWhite(Bitmap originalBitmap) {
        if (originalBitmap == null) {
            return null;
        }
        // 创建一个新的Bitmap，宽高与原图一致
        Bitmap newBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        // 创建一个Canvas，以新Bitmap为画布
        Canvas canvas = new Canvas(newBitmap);
        // 绘制白色背景
        canvas.drawColor(Color.WHITE);
        // 绘制原始Bitmap
        Paint paint = new Paint();
        canvas.drawBitmap(originalBitmap, 0, 0, paint);
        return newBitmap;
    }

    /**
     * 将Bitmap缩放到高度为552，并转换为ARGB_8888格式
     */
    public static Bitmap resizeBitmapToHeight552(Bitmap bitmap) {
        // 如果图片包含透明像素并需要转换为白色背景
        // 获取原始宽高
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();

        // 计算缩放比例
        float scaleFactor =  552.0f / originalHeight;
        int targetWidth = Math.round(originalWidth * scaleFactor);

        return Bitmap.createScaledBitmap(bitmap, targetWidth, 552, true);
    }

    public static Bitmap resizeBitmap(Bitmap bitmap,int width,int height) {

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    public static Bitmap rotatedImageByRadians(Bitmap image, double radians) {

        Matrix matrix = new Matrix();
        // 旋转图片
        matrix.postRotate((float) Math.toDegrees(radians));

        return Bitmap.createBitmap(
                image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }


    /**
     * 获取图片的缩略图
     *
     * @param imagePath 图片文件路径
     * @param maxWidth  缩略图的最大宽度
     * @param maxHeight 缩略图的最大高度
     * @return 缩略图 Bitmap 对象
     */
    public static Bitmap getThumbnailFromPath(String imagePath, int maxWidth, int maxHeight) {
        // 首先获取图片的尺寸
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        // 计算缩放比例
        int scale = calculateInSampleSize(options, maxWidth, maxHeight);

        // 使用计算出的缩放比例加载缩略图
        options.inJustDecodeBounds = false;
        options.inSampleSize = scale;

        return BitmapFactory.decodeFile(imagePath, options);
    }

    /**
     * 计算图片的缩放比例
     *
     * @param options   BitmapFactory.Options 对象
     * @param targetWidth  期望的宽度
     * @param targetHeight 期望的高度
     * @return 缩放比例
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int targetWidth, int targetHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > targetHeight || width > targetWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= targetHeight
                    && (halfWidth / inSampleSize) >= targetWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}
