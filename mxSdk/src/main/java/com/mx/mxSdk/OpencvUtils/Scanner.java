package com.mx.mxSdk.OpencvUtils;

import android.graphics.Bitmap;

import com.mx.mxSdk.Utils.RBQLog;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dyman on 2017/8/12.
 *
 *  智能选区帮助类
 */

public class Scanner {

    public static int resizeThreshold = 500;

    /**
     * 扫描图片，并返回检测到的矩形的四个顶点的坐标
     */
    public static Point[] scanPoint(Bitmap srcBitmap) {
        if (srcBitmap == null || srcBitmap.isRecycled()) {
            RBQLog.e("scanPoint: 输入Bitmap为空或已回收");
            return getDefaultPoints(0, 0);
        }

        Mat srcMat = new Mat();
        Utils.bitmapToMat(srcBitmap, srcMat);
        
        // 图像缩放
        Mat image = resizeImage(srcMat);
        // 图像预处理 - 使用缩放后的图像
        Mat scanImage = preProcessImage(image);

        // 获取最大矩形
        Point[] resultArr = null;

        // 检测到的轮廓
        List<MatOfPoint> contours = new ArrayList<>();
        // 各轮廓的继承关系
        Mat hierarchy = new Mat();
        // 提取边框
        Imgproc.findContours(scanImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        // 按面积排序，最后只取面积最大的那个
        if (contours.isEmpty()) {
            RBQLog.d("scanPoint: 未检测到轮廓，返回默认边界");
            Point[] defaultPoints = getDefaultPoints(image.cols(), image.rows());
            releaseMats(srcMat, image, scanImage, hierarchy);
            return defaultPoints;
        }
        
        // 取面积最大的
        MatOfPoint maxMatOfPoint = contours.get(0);
        double maxArea = Math.abs(Imgproc.contourArea(maxMatOfPoint));
        int num = contours.size() - 1;
        while (num > 0) {
            double area = Math.abs(Imgproc.contourArea(contours.get(num)));
            if (area > maxArea) {
                maxMatOfPoint = contours.get(num);
                maxArea = area;
            }
            num--;
        }

        RBQLog.d("最大面积:"+maxArea);

        MatOfPoint2f maxMatOfPoint2f = new MatOfPoint2f(maxMatOfPoint.toArray());
        double arc = Imgproc.arcLength(maxMatOfPoint2f, true);
        MatOfPoint2f outDpMat = new MatOfPoint2f();
        Imgproc.approxPolyDP(maxMatOfPoint2f, outDpMat, 0.02 * arc, true);//  多边形逼近
        
        // 筛选去除相近的点
        MatOfPoint2f selectMat = selectPoint(outDpMat, 1);
        RBQLog.d("点数:"+selectMat.toArray().length);
        if (selectMat.toArray().length == 4) {
            resultArr = selectMat.toArray();
        }
        
        // 对最终检测出的四个点进行排序：左上、右上、右下、左下
        Point[] finalResult;
        if (resultArr != null) {
            Point[] result = sortPointClockwise(resultArr);
            // 将坐标转换回原始图像尺寸
            finalResult = convertToOriginalScale(result, srcMat, image);
        } else {
            RBQLog.d("scanPoint: 未检测到四边形，返回默认边界");
            finalResult = getDefaultPoints(srcMat.cols(), srcMat.rows());
        }
        
        // 释放所有Mat对象
        releaseMats(srcMat, image, scanImage, hierarchy, maxMatOfPoint2f, outDpMat);
        
        return finalResult;
    }

    /**
     *  为避免处理时间过长，先对图片进行压缩
     * @param image
     * @return
     */
    private static Mat resizeImage(Mat image) {
        int width = image.cols();
        int height = image.rows();
        int maxSize = Math.max(width, height);
        if (maxSize > resizeThreshold) {
            float resizeScale = 1.0f * maxSize / resizeThreshold;
            width = (int) (width / resizeScale);
            height = (int) (height / resizeScale);
            Size size = new Size(width, height);
            Mat resizeMat = new Mat();
            Imgproc.resize(image, resizeMat, size);
            return resizeMat;
        }
        return image;
    }

    /**
     * 对图像进行预处理：灰度化、高斯模糊、Canny边缘检测
     */
    public static Mat preProcessImage(Mat image) {

        Mat grayMat = new Mat();
        Imgproc.cvtColor(image, grayMat, Imgproc.COLOR_RGB2GRAY);   //  注意RGB和BGR，影响很大

        Mat blurMat = new Mat();
        Imgproc.GaussianBlur(grayMat, blurMat, new Size(5, 5), 0);

        Mat cannyMat = new Mat();
        Imgproc.Canny(blurMat, cannyMat, 0, 5);

        Mat thresholdMat = new Mat();
        Imgproc.threshold(cannyMat, thresholdMat, 0, 255, Imgproc.THRESH_OTSU);

        return thresholdMat;
    }

    /**
     * 过滤掉距离相近的点
     */
    private static MatOfPoint2f selectPoint(MatOfPoint2f outDpMat, int selectTimes) {
        // 添加递归深度限制，防止栈溢出
        if (selectTimes > 10) {
            RBQLog.w("selectPoint: 递归深度超过限制，返回当前结果");
            return outDpMat;
        }
        
        List<Point> pointList = new ArrayList<Point>(outDpMat.toList());
        if (pointList.size() > 4) {
            double arc = Imgproc.arcLength(outDpMat, true);
            for (int i = pointList.size() - 1; i >= 0; i--) {
                if (pointList.size() == 4) {
                    Point[] resultPoints = new Point[pointList.size()];
                    for (int j = 0; j < pointList.size(); j++) {
                        resultPoints[j] = pointList.get(j);
                    }
                    return new MatOfPoint2f(resultPoints);
                }

                if (i != pointList.size() - 1) {
                    Point itor = pointList.get(i);
                    Point lastP = pointList.get(i + 1);

                    double pointLength = Math.sqrt(Math.pow(itor.x - lastP.x, 2) + Math.pow(itor.y - lastP.y, 2));
                    if (pointLength < arc * 0.01 * selectTimes && pointList.size() > 4) {
                        pointList.remove(i);
                    }
                }
            }

            if (pointList.size() > 4) {
                //  要手动逐个强转
                Point[] againPoints = new Point[pointList.size()];
                for (int i = 0; i < pointList.size(); i++) {
                    againPoints[i] = pointList.get(i);
                }
                return selectPoint(new MatOfPoint2f(againPoints), selectTimes + 1);
            }
        }

        return outDpMat;
    }

    /**
     * 对顶点进行排序
     */
    private static Point[] sortPointClockwise(Point[] points) {
        if (points.length != 4) {
            return points;
        }

        Point unFoundPoint = new Point();
        Point[] result = {unFoundPoint, unFoundPoint, unFoundPoint, unFoundPoint};

        long minDistance = -1;
        for (Point point : points) {
            long distance = (long) (point.x * point.x + point.y * point.y);
            if (minDistance == -1 || distance < minDistance) {
                result[0] = point;
                minDistance = distance;
            }
        }

        if (result[0] != unFoundPoint) {
            Point leftTop = result[0];
            Point[] p1 = new Point[3];
            int i = 0;
            for (Point point : points) {
                if (point.x == leftTop.x && point.y == leftTop.y) {
                    continue;
                }
                p1[i] = point;
                i++;
            }
            if ((pointSideLine(leftTop, p1[0], p1[1]) * pointSideLine(leftTop, p1[0], p1[2])) < 0) {
                result[2] = p1[0];
            } else if ((pointSideLine(leftTop, p1[1], p1[0]) * pointSideLine(leftTop, p1[1], p1[2])) < 0) {
                result[2] = p1[1];
            } else if ((pointSideLine(leftTop, p1[2], p1[0]) * pointSideLine(leftTop, p1[2], p1[1])) < 0) {
                result[2] = p1[2];
            }
        }

        if (result[0] != unFoundPoint && result[2] != unFoundPoint) {
            Point leftTop = result[0];
            Point rightBottom = result[2];
            Point[] p1 = new Point[2];
            int i = 0;
            for (Point point : points) {
                if (point.x == leftTop.x && point.y == leftTop.y) {
                    continue;
                }
                if (point.x == rightBottom.x && point.y == rightBottom.y) {
                    continue;
                }
                if (i < 2) {
                    p1[i] = point;
                    i++;
                }
            }
            // 添加空指针检查
            if (p1[0] != null && p1[1] != null) {
                if (pointSideLine(leftTop, rightBottom, p1[0]) > 0) {
                    result[1] = p1[0];
                    result[3] = p1[1];
                } else {
                    result[1] = p1[1];
                    result[3] = p1[0];
                }
            } else {
                RBQLog.w("sortPointClockwise: 无法确定剩余两个点的位置");
            }
        }

//        if (result[0] != unFoundPoint && result[1] != unFoundPoint && result[2] != unFoundPoint
//                && result[3] != unFoundPoint) {
//            return result;
//        }
        if (result[0] != unFoundPoint && result[1] != unFoundPoint && result[3] != unFoundPoint) {
            return result;
        }

        return points;
    }

    private static double pointSideLine(Point lineP1, Point lineP2, Point point) {
        double x1 = lineP1.x;
        double y1 = lineP1.y;
        double x2 = lineP2.x;
        double y2 = lineP2.y;
        double x = point.x;
        double y = point.y;
        return (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1);
    }

    /**
     * 获取默认边界点
     */
    private static Point[] getDefaultPoints(int width, int height) {
        Point[] resultArr = new Point[4];
        resultArr[0] = new Point(0, 0);
        resultArr[1] = new Point(width, 0);
        resultArr[2] = new Point(width, height);
        resultArr[3] = new Point(0, height);
        return resultArr;
    }

    /**
     * 将缩放后的坐标转换回原始图像尺寸
     */
    private static Point[] convertToOriginalScale(Point[] scaledPoints, Mat originalMat, Mat scaledMat) {
        if (scaledPoints == null || originalMat == null || scaledMat == null) {
            return scaledPoints;
        }
        
        // 如果图像没有缩放，直接返回
        if (originalMat.cols() == scaledMat.cols() && originalMat.rows() == scaledMat.rows()) {
            return scaledPoints;
        }
        
        double scaleX = (double) originalMat.cols() / scaledMat.cols();
        double scaleY = (double) originalMat.rows() / scaledMat.rows();
        
        Point[] originalPoints = new Point[scaledPoints.length];
        for (int i = 0; i < scaledPoints.length; i++) {
            Point scaledPoint = scaledPoints[i];
            originalPoints[i] = new Point(scaledPoint.x * scaleX, scaledPoint.y * scaleY);
        }
        
        return originalPoints;
    }

    /**
     * 释放Mat对象资源
     */
    private static void releaseMats(Mat... mats) {
        for (Mat mat : mats) {
            if (mat != null && !mat.empty()) {
                mat.release();
            }
        }
    }

}
