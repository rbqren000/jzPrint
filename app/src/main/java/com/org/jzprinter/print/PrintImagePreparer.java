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
     * 根据页码、奇偶页设置和打印方向获取旋转方向。
     *
     * 决策逻辑：
     * 1. 先根据 pageCode + oddPageOnRight 确定该页所属侧（左/右）的基准旋转
     * 2. 再根据该侧的滑动方向决定是否取反（下→上时取反）
     *
     * @param pageCode         页码
     * @param oddPageOnRight   奇数页是否在右侧
     * @param leftBottomToTop  左侧页滑动方向是否为下→上
     * @param rightBottomToTop 右侧页滑动方向是否为下→上
     * @return 旋转方向
     */
    public static RotationDirection getRotation(int pageCode, boolean oddPageOnRight,
                                                 boolean leftBottomToTop, boolean rightBottomToTop) {
        boolean isOdd = (pageCode % 2 == 1);
        boolean isRightPage = isOdd == oddPageOnRight;

        // 基准旋转：实测确认两侧均为 CCW_90（仅对齐方式不同：右侧=TOP，左侧=BOTTOM）
        RotationDirection base = RotationDirection.CCW_90;

        // 该侧实际滑动方向
        boolean btoT = isRightPage ? rightBottomToTop : leftBottomToTop;

        return btoT ? opposite(base) : base;
    }

    /**
     * 根据页码、奇偶页设置和打印方向获取垂直对齐方式。
     *
     * 决策逻辑同 getRotation，基准对齐：右侧=TOP，左侧=BOTTOM。
     *
     * @param pageCode         页码
     * @param oddPageOnRight   奇数页是否在右侧
     * @param leftBottomToTop  左侧页滑动方向是否为下→上
     * @param rightBottomToTop 右侧页滑动方向是否为下→上
     * @return 垂直对齐方式
     */
    public static VerticalAlignment getAlignment(int pageCode, boolean oddPageOnRight,
                                                  boolean leftBottomToTop, boolean rightBottomToTop) {
        boolean isOdd = (pageCode % 2 == 1);
        boolean isRightPage = isOdd == oddPageOnRight;

        // 基准对齐：右侧=TOP，左侧=BOTTOM
        VerticalAlignment base = isRightPage ? VerticalAlignment.TOP : VerticalAlignment.BOTTOM;

        // 该侧实际滑动方向
        boolean btoT = isRightPage ? rightBottomToTop : leftBottomToTop;

        return btoT ? opposite(base) : base;
    }

    private static RotationDirection opposite(RotationDirection dir) {
        return dir == RotationDirection.CW_90 ? RotationDirection.CCW_90 : RotationDirection.CW_90;
    }

    private static VerticalAlignment opposite(VerticalAlignment align) {
        return align == VerticalAlignment.TOP ? VerticalAlignment.BOTTOM : VerticalAlignment.TOP;
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
