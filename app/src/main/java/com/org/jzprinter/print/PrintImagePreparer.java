package com.org.jzprinter.print;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;

import com.org.jzprinter.printer.PrinterHeadManager;

public class PrintImagePreparer {

    public enum RotationDirection {
        CW_90(0),
        CCW_90(1);

        private final int value;

        RotationDirection(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static RotationDirection fromValue(int value) {
            for (RotationDirection dir : values()) {
                if (dir.value == value) return dir;
            }
            return CW_90;
        }
    }

    public enum VerticalAlignment {
        TOP(0),
        BOTTOM(1);

        private final int value;

        VerticalAlignment(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static VerticalAlignment fromValue(int value) {
            for (VerticalAlignment align : values()) {
                if (align.value == value) return align;
            }
            return TOP;
        }
    }

    /**
     * 根据页码和奇偶页设置获取旋转方向
     *
     * @param pageCode       页码
     * @param oddPageOnRight 奇页是否在右边（翻书方向）
     * @return 旋转方向
     */
    public static RotationDirection getRotation(int pageCode, boolean oddPageOnRight) {
        boolean isOdd = (pageCode % 2 == 1);
        if (isOdd) {
            return oddPageOnRight ? RotationDirection.CW_90 : RotationDirection.CCW_90;
        } else {
            return oddPageOnRight ? RotationDirection.CCW_90 : RotationDirection.CW_90;
        }
    }

    /**
     * 根据页码和奇偶页设置获取垂直对齐方式
     *
     * @param pageCode       页码
     * @param oddPageOnRight 奇页是否在右边（翻书方向）
     * @return 垂直对齐方式
     */
    public static VerticalAlignment getAlignment(int pageCode, boolean oddPageOnRight) {
        boolean isOdd = (pageCode % 2 == 1);
        if (isOdd) {
            return oddPageOnRight ? VerticalAlignment.TOP : VerticalAlignment.BOTTOM;
        } else {
            return oddPageOnRight ? VerticalAlignment.BOTTOM : VerticalAlignment.TOP;
        }
    }

    /**
     * 准备打印图片：旋转 + 补白到打印头高度
     *
     * @param source      原图（纵向窄长条，如 360×H）
     * @param rotation    旋转方向
     * @param alignment   垂直对齐方式
     * @return 处理后的 Bitmap（横向，宽度=原图高度，高度=打印头像素高度）
     */
    public static Bitmap prepare(Bitmap source, RotationDirection rotation,
                                  VerticalAlignment alignment) {
        if (source == null) return null;

        int printHeadHeight = PrinterHeadManager.getInstance().getParameters().getRowPixDistance();

        Matrix matrix = new Matrix();
        switch (rotation) {
            case CW_90:
                matrix.postRotate(90);
                break;
            case CCW_90:
                matrix.postRotate(-90);
                break;
        }
        Bitmap rotated = Bitmap.createBitmap(source, 0, 0,
            source.getWidth(), source.getHeight(), matrix, true);

        int width = rotated.getWidth();
        int height = rotated.getHeight();

        if (height == printHeadHeight) {
            return rotated;
        }

        if (height > printHeadHeight) {
            rotated.recycle();
            throw new IllegalArgumentException(
                "旋转后图片高度 " + height + "px 超过打印头高度 " + printHeadHeight + "px，" +
                "原图宽度应为360px，请检查素材");
        }

        Bitmap canvas = Bitmap.createBitmap(width, printHeadHeight,
            Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(canvas);
        c.drawColor(Color.WHITE);

        int y;
        switch (alignment) {
            case TOP:
                y = 0;
                break;
            case BOTTOM:
                y = printHeadHeight - height;
                break;
            default:
                y = 0;
                break;
        }
        c.drawBitmap(rotated, 0, y, null);

        rotated.recycle();
        return canvas;
    }
}
