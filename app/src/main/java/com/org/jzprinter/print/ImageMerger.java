package com.org.jzprinter.print;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageMerger {

    /**
     * 将多张图片纵向拼接为一张长图
     *
     * @param imageFiles 图片文件数组
     * @return 拼接后的 Bitmap
     */
    public static Bitmap mergeVertically(File[] imageFiles) {
        Arrays.sort(imageFiles, ImageMerger::naturalCompare);

        List<Bitmap> bitmaps = new ArrayList<>();
        int width = 0;
        int totalHeight = 0;

        for (File file : imageFiles) {
            Bitmap bm = BitmapFactory.decodeFile(file.getPath());
            if (bm == null) continue;
            bitmaps.add(bm);
            width = Math.max(width, bm.getWidth());
            totalHeight += bm.getHeight();
        }

        if (bitmaps.isEmpty() || width == 0) return null;

        Bitmap result = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        int y = 0;
        for (Bitmap bm : bitmaps) {
            canvas.drawBitmap(bm, 0, y, null);
            y += bm.getHeight();
            bm.recycle();
        }

        return result;
    }

    /**
     * 自然排序比较器（Natural Sort）
     * 支持: 1.png, 2.png, 2-1.png, 2-2.png, 3.png, 10.png, 提交码.png, 订正码.png
     * 数字部分按数值比较，非数字部分按字符比较
     * 中文命名的文件排在数字命名之后
     */
    private static int naturalCompare(File f1, File f2) {
        String s1 = f1.getName();
        String s2 = f2.getName();
        int i1 = 0, i2 = 0;
        while (i1 < s1.length() && i2 < s2.length()) {
            char c1 = s1.charAt(i1);
            char c2 = s2.charAt(i2);
            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                int num1 = 0;
                while (i1 < s1.length() && Character.isDigit(s1.charAt(i1))) {
                    num1 = num1 * 10 + (s1.charAt(i1) - '0');
                    i1++;
                }
                int num2 = 0;
                while (i2 < s2.length() && Character.isDigit(s2.charAt(i2))) {
                    num2 = num2 * 10 + (s2.charAt(i2) - '0');
                    i2++;
                }
                if (num1 != num2) return num1 - num2;
            } else {
                if (c1 != c2) return c1 - c2;
                i1++;
                i2++;
            }
        }
        return (s1.length() - i1) - (s2.length() - i2);
    }
}
