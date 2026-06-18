package com.mx.mxSdk.OpencvUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;

import com.mx.mxSdk.MxImageUtils;
import com.mx.mxSdk.Utils.RBQLog;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

public class OpenCVUtils {

    public static void staticLoadCVLibraries(){
        boolean load = OpenCVLoader.initDebug();
        if(load) {
            RBQLog.i("CV", "Open CV Libraries loaded...");
        }
    }

    //灰度化方法
    public static Bitmap RGB2Gray(Bitmap photo) {
        Mat RGBMat = new Mat();
        Bitmap grayBitmap = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.RGB_565);
        Utils.bitmapToMat(photo, RGBMat);//convert original bitmap to Mat, R G B.
        Imgproc.cvtColor(RGBMat, RGBMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat
        Utils.matToBitmap(RGBMat, grayBitmap);
        return grayBitmap;
    }

    //二值化滤镜
    public static Bitmap threshold(Bitmap photo, int threshold) {
        Mat mat = new Mat();
        Bitmap bitmap = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.RGB_565);
        Utils.bitmapToMat(photo, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        Core.bitwise_not(mat, mat);
        Imgproc.threshold(mat, mat, threshold, 255, Imgproc.THRESH_BINARY_INV);
//        Imgproc.threshold(mat, mat, threshold, 255, Imgproc.THRESH_BINARY);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

    //边缘素描
    public static Bitmap edgeSketch(Bitmap photo) {
        Mat mat = new Mat();
        Mat Cmat = new Mat();
        Bitmap cartton = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.RGB_565);
        Utils.bitmapToMat(photo, mat);
        Imgproc.Canny(mat,Cmat,50,100);
        mat.release();  // mat only read by Canny above
        Core.bitwise_not(Cmat,Cmat);
        Utils.matToBitmap(Cmat, cartton);
        Cmat.release();
        return cartton;
    }

    //素描滤镜
    public static Bitmap pencilSketch(Bitmap photo) {
        Mat SM = new Mat();
        Mat SM1 = new Mat();
        Bitmap sketchBitmap = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.RGB_565);
        Bitmap SMB = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.RGB_565);
        Bitmap SMB1 = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.RGB_565);
        Utils.bitmapToMat(photo, SM);
        //灰度化
        Imgproc.cvtColor(SM, SM, Imgproc.COLOR_RGB2GRAY);
        //颜色取反
        Core.bitwise_not(SM, SM1);
        //高斯模糊
        Imgproc.GaussianBlur(SM1, SM1, new Size(13, 13), 0, 0);
        Utils.matToBitmap(SM, SMB);
        Utils.matToBitmap(SM1, SMB1);
        SM.release();
        SM1.release();
        for (int i = 0; i < SMB.getWidth(); i++) {
            for (int j = 0; j < SMB.getHeight(); j++) {
                int A = SMB.getPixel(i, j);
                int B = SMB1.getPixel(i, j);
                int CR = colorDodge(Color.red(A), Color.red(B));
                int CG = colorDodge(Color.green(A), Color.red(B));
                int CB = colorDodge(Color.blue(A), Color.blue(B));
                sketchBitmap.setPixel(i, j, Color.rgb(CR, CG, CB));
            }
        }
        return sketchBitmap;
    }

    public static int colorDodge(int A, int B) {
        return  Math.min(A+(A*B)/(255-B+1),255);
    }

    //怀旧色滤镜
    public static Bitmap vintageFilter(Bitmap photo) {
        Bitmap vintage = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.RGB_565);
        for (int i = 0; i < photo.getWidth(); i++) {
            for (int j = 0; j < photo.getHeight(); j++) {
                int A = photo.getPixel(i, j);
                int AR = (int) (0.393 * Color.red(A) + 0.769 * Color.green(A) + 0.189 * Color.blue(A));
                int AG = (int) (0.349 * Color.red(A) + 0.686 * Color.green(A) + 0.168 * Color.blue(A));
                int AB = (int) (0.272 * Color.red(A) + 0.534 * Color.green(A) + 0.131 * Color.blue(A));
                AR = Math.min(AR, 255);
                AG = Math.min(AG, 255);
                AB = Math.min(AB, 255);
                vintage.setPixel(i, j, Color.rgb(AR, AG, AB));
            }
        }
        return vintage;
    }

    //连环画：同样是对RGB色彩操作
    public static Bitmap comicFilter(Bitmap photo) {
        Bitmap comic = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.RGB_565);
        for (int i = 0; i < photo.getWidth(); i++) {
            for (int j = 0; j < photo.getHeight(); j++) {
                int A = photo.getPixel(i, j);
                int AR = Math.abs(Color.red(A) - Color.blue(A) + Color.green(A) + Color.green(A)) * Color.red(A) / 256;
                int AG = Math.abs(Color.red(A) - Color.green(A) + Color.blue(A) + Color.blue(A)) * Color.red(A) / 256;
                int AB = Math.abs(Color.red(A) - Color.blue(A) + Color.blue(A) + Color.blue(A)) * Color.green(A) / 256;
                AR = Math.min(AR, 255);
                AG = Math.min(AG, 255);
                AB = Math.min(AB, 255);
                comic.setPixel(i, j, Color.rgb(AR, AG, AB));
            }
        }
        return comic;

    }
    //熔铸滤镜
    public static Bitmap metalFilter(Bitmap photo){
        Bitmap metal  = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.RGB_565);
        for(int i = 0;i<photo.getWidth();i++){
            for( int j = 0;j<photo.getHeight();j++){
                int A = photo.getPixel(i,j);
                int AR =Color.red(A)*128/(Color.blue(A)+Color.green(A)+1);
                int AG =Color.green(A)*128/(Color.blue(A)+Color.red(A)+1);
                int AB =Color.blue(A)*128/(Color.red(A)+Color.green(A)+1);
                AR = Math.min(AR, 255);
                AG = Math.min(AG, 255);
                AB = Math.min(AB, 255);
                metal.setPixel(i,j,Color.rgb(AR,AG,AB));
            }
        }
        return metal;
    }

    //冰冻滤镜
    public static Bitmap frozenFilter(Bitmap photo) {
        Bitmap frozen = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.RGB_565);
        for (int i = 0; i < photo.getWidth(); i++) {
            for (int j = 0; j < photo.getHeight(); j++) {
                int A = photo.getPixel(i, j);
                int AR = (Color.red(A) - Color.blue(A) - Color.green(A)) * 3 / 2;
                int AG = (Color.green(A) - Color.blue(A) - Color.red(A)) * 3 / 2;
                int AB = (Color.blue(A) - Color.red(A) - Color.green(A)) * 3 / 2;
                AR = Math.max(0, Math.min(AR, 255));
                AG = Math.max(0, Math.min(AG, 255));
                AB = Math.max(0, Math.min(AB, 255));
                frozen.setPixel(i, j, Color.rgb(AR, AG, AB));
            }
        }
        return frozen;
    }
    //浮雕滤镜
    public static Bitmap embossFilter(Bitmap photo) {
        Bitmap emboss = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.RGB_565);
        for (int i = 1; i < photo.getWidth() - 1; i++) {
            for (int j = 1; j < photo.getHeight() - 1; j++) {
                int A = photo.getPixel(i - 1, j - 1);
                int B = photo.getPixel(i + 1, j + 1);
                int AR = Color.red(B) - Color.red(A) + 128;
                int AG = Color.green(B) - Color.green(A) + 128;
                int AB = Color.blue(B) - Color.blue(A) + 128;
                AR = Math.max(0, Math.min(AR, 255));
                AG = Math.max(0, Math.min(AG, 255));
                AB = Math.max(0, Math.min(AB, 255));
                emboss.setPixel(i, j, Color.rgb(AR, AG, AB));
            }
        }
        return emboss;
    }

    //去除背景色
    public static Bitmap lightClearBackground(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);

        Mat gray=new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);
        src.release();

        Mat fc1 = new Mat();
        gray.convertTo(fc1, CvType.CV_32FC1, 1.0 / 255);
        gray.release();

        Mat dst = reduceBackgroundAlgorithm(fc1);
        fc1.release();

        Mat dst3 = colorGradation(dst);
        dst.release();

        Bitmap newImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(dst3, newImage);
        dst3.release();
        return newImage;
    }

    public static Bitmap deepClearBackground(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);

        Mat gray=new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);
        src.release();

        Mat fc1 = new Mat();
        gray.convertTo(fc1, CvType.CV_32FC1, 1.0 / 255);
        gray.release();

        Mat dst = reduceBackgroundAlgorithm(fc1);
        fc1.release();

        Mat dst3 = colorGradation(dst);
        dst.release();

        Mat ts = new Mat();
        Imgproc.threshold(dst3,ts,1,255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        dst3.release();

        Bitmap newImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(ts, newImage);
        ts.release();
        return newImage;
    }

    public static Bitmap lightClearRedBackground(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);

        Mat src3channels = new Mat();
        Imgproc.cvtColor(src,src3channels,Imgproc.COLOR_RGBA2RGB);
        src.release();

        Mat hsv = new Mat();
        Imgproc.cvtColor(src3channels,hsv,Imgproc.COLOR_RGB2HSV);

        Scalar lowerb_1 = new Scalar(0,43,46);
        Scalar upperb_1 = new Scalar(10,255,255);

        Scalar lowerb_2 = new Scalar(156,43,46);
        Scalar upperb_2 = new Scalar(180,255,255);

        Mat mask1 = new Mat();
        Core.inRange(hsv,lowerb_1,upperb_1,mask1);

        Mat mask2 = new Mat();
        Core.inRange(hsv,lowerb_2,upperb_2,mask2);
        hsv.release();

        Mat maskImg = new Mat();
        Core.add(mask1,mask2,maskImg);
        mask1.release();
        mask2.release();

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(7, 7));
        Imgproc.dilate(maskImg,maskImg,kernel,new Point(-1,-1),1);
        kernel.release();

        Mat result = new Mat();
        Photo.inpaint(src3channels,maskImg,result,2,Photo.INPAINT_NS);
        maskImg.release();

        Mat gray=new Mat();
        Imgproc.cvtColor(result, gray, Imgproc.COLOR_RGB2GRAY);
        result.release();

        Mat fc1 = new Mat();
        gray.convertTo(fc1, CvType.CV_32FC1, 1.0 / 255);
        gray.release();

        Mat dst = reduceBackgroundAlgorithm(fc1);
        fc1.release();

        //增强文字颜色
        Mat dst3 = colorGradation(dst);
        dst.release();

        Bitmap newImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(dst3, newImage);
        dst3.release();

        return newImage;
    }

    public static Bitmap deepClearRedBackground(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);

        Mat src3channels = new Mat();
        Imgproc.cvtColor(src,src3channels,Imgproc.COLOR_RGBA2RGB);
        src.release();

        Mat hsv = new Mat();
        Imgproc.cvtColor(src3channels,hsv,Imgproc.COLOR_RGB2HSV);

        Scalar lowerb_1 = new Scalar(0,43,46);
        Scalar upperb_1 = new Scalar(10,255,255);

        Scalar lowerb_2 = new Scalar(156,43,46);
        Scalar upperb_2 = new Scalar(180,255,255);

        Mat mask1 = new Mat();
        Core.inRange(hsv,lowerb_1,upperb_1,mask1);

        Mat mask2 = new Mat();
        Core.inRange(hsv,lowerb_2,upperb_2,mask2);
        hsv.release();

        Mat maskImg = new Mat();
        Core.add(mask1,mask2,maskImg);
        mask1.release();
        mask2.release();

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(7, 7));
        Imgproc.dilate(maskImg,maskImg,kernel,new Point(-1,-1),1);
        kernel.release();

        Mat result = new Mat();
        Photo.inpaint(src3channels,maskImg,result,2,Photo.INPAINT_NS);
        maskImg.release();

        Mat gray=new Mat();
        Imgproc.cvtColor(result, gray, Imgproc.COLOR_RGB2GRAY);
        result.release();

        Mat fc1 = new Mat();
        gray.convertTo(fc1, CvType.CV_32FC1, 1.0 / 255);
        gray.release();

        Mat dst = reduceBackgroundAlgorithm(fc1);
        fc1.release();

        //增强文字颜色
        Mat dst3 = colorGradation(dst);
        dst.release();

        Mat ts = new Mat();
        Imgproc.threshold(dst3,ts,1,255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        dst3.release();

        Bitmap newImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(ts, newImage);
        ts.release();

        return newImage;
    }

    /**
     * 锐化
     * @param src
     * @param nAmount
     * @return
     */
    public static Mat ImageSharp(Mat src,int nAmount){
        Mat dst= new Mat();
        double sigma = 3;
        float amount = nAmount / 100.0f;
        Mat imgBlurred = new Mat();
        Imgproc.GaussianBlur(src, imgBlurred, new Size(7,7), sigma, sigma,4);
        Mat temp_sub= new Mat();
        Core.subtract(src,imgBlurred,temp_sub);
        imgBlurred.release();
        Core.addWeighted(src,1,temp_sub,amount,0,dst);
        temp_sub.release();
        return dst;
    }
    public static Mat reduceBackgroundAlgorithm(Mat src) {
        Mat gauss = new Mat();
        Mat dst2 = new Mat();
        Mat dst3 = new Mat();
        Imgproc.blur(src, gauss, new Size(101,101));
        Core.divide(src,gauss,dst2);
        gauss.release();

        Mat sharpened = ImageSharp(dst2, 101);
        dst2.release();
        sharpened.convertTo(dst3, CvType.CV_8UC1,255);
        sharpened.release();
        return dst3;
    }

    // 方法：检查Bitmap图像是否具有透明度
    public static boolean isTransparent(Bitmap bitmap) {
        // 将Bitmap转换为Mat
        Mat image = new Mat();
        Utils.bitmapToMat(bitmap, image);

        // 如果图像的通道数小于4，则没有Alpha通道
        if (image.channels() < 4) {
            image.release();
            return false; // 图像没有Alpha通道
        }

        Mat alphaChannel = new Mat();
        // 提取Alpha通道，第三个参数3表示提取第四个通道（Alpha通道）
        Core.extractChannel(image, alphaChannel, 3);
        image.release();

        // 计算Alpha通道中非零（不透明）像素的数量
        int nonZeroCount = Core.countNonZero(alphaChannel);

        // 如果非透明像素的数量不等于总像素数，则存在透明像素
        boolean result = nonZeroCount != alphaChannel.total();
        alphaChannel.release();
        return result;
    }

    public static Bitmap addBackShadow(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);
        Mat src_gray=new Mat();
        if (src.channels() == 3)
            Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_RGB2GRAY);
        else if(src.channels() == 4)
            Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_RGB2GRAY);
        else
            src.copyTo(src_gray);
        src.release();
        src_gray.convertTo(src_gray, CvType.CV_32FC1, 1.0 / 255);
        Mat src_reduback= reduceBackgroundAlgorithm(src_gray);
        Mat src_gauss = new Mat();
        Imgproc.GaussianBlur(src_gray, src_gauss, new Size(3,3), 0, 0,4);
        src_gauss.convertTo(src_gauss, CvType.CV_8UC1,255);
        Mat dst=new Mat();
        Mat bw=new Mat();
        int kernel_size = 3;
        int scale = 1;
        int delta = 0;
        Imgproc.adaptiveThreshold(src_gauss,bw,255,0,1,31,1);
        src_gauss.release();
        src_gray.release();
        Mat abs_dst=new Mat();
        Mat out=new Mat();
        Imgproc.Laplacian(src_reduback, dst, 5, kernel_size, scale, delta);
        Core.convertScaleAbs(dst, abs_dst);
        dst.release();
        Imgproc. threshold(abs_dst, out, 30, 255, 0);
        abs_dst.release();
        Mat kernel = Imgproc. getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7), new Point(-1,-1));
        Imgproc. morphologyEx(out, out, 1, kernel);
        kernel.release();
        Mat shadow = new Mat();
        Core.bitwise_and(bw,out,shadow);
        bw.release();
        out.release();
        Mat out_shadow = new Mat();
        Core.subtract(src_reduback,shadow,out_shadow);
        src_reduback.release();
        shadow.release();
        Bitmap newImg = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(out_shadow, newImg);
        out_shadow.release();
        return newImg;
    }
    public static Mat toneCurve(Mat src)
    {
        Mat dst = new Mat(src.size(), src.type());
        int clight=30;int cdarck=40;
        float h= Math.round (255.0*(100-clight)/100);
        float l=Math.round (255.0*(100-cdarck)/100);

        for (int i=0;i<src.rows();i++)
        {
            for (int j=0;j<src.cols();j++)
            {
               double data= src.get(i,j)[0];
               if (data>=h)
                   dst.put(i,j,255);
                else if (data<=l)
                  dst.put(i,j,0);
                else if (l<data&&data<h)
                    dst.put(i,j,255-(255.0/(h-l)*(h-data)));
            }
        }

        return dst;
    }
    public static Mat colorGradation(Mat src)
    {
        src.convertTo(src, CvType.CV_32FC1);
        int HighLight=255;
        int Shadow=120;
        int Diff=HighLight-Shadow;
        Mat rDiff=new Mat();
        Core.subtract(src,new Scalar(Shadow),rDiff);
        Mat temp1=new Mat();
        rDiff.convertTo(temp1, CvType.CV_32FC1, 255.0 / Diff);
        rDiff.release();
        Mat dst=new Mat();
        temp1.convertTo(dst, CvType.CV_8UC1);
        temp1.release();
        return dst;
    }
    public static Bitmap convertGray(Bitmap bitmap) {
        Mat src = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(bitmap, src);
        Imgproc.cvtColor(src,dst,Imgproc.COLOR_RGB2GRAY);
        Utils.matToBitmap(dst, bitmap);
        return bitmap;
    }

    public static Bitmap kayCanny(Bitmap bitmap) {
        Mat src = new Mat();
        Mat gray = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(bitmap, src);
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);

        Imgproc.Canny(gray, dst, 80, 90);

        Utils.matToBitmap(dst, bitmap);
        return bitmap;
    }

    public static Bitmap kayHsv(Bitmap bitmap) {

        Mat src = new Mat();
        Mat dst = new Mat();

        Utils.bitmapToMat(bitmap, src);
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_RGB2HSV);
        Utils.matToBitmap(dst, bitmap);
        return bitmap;
    }

    public static Bitmap adaptiveThresholdImage(Bitmap bitmap,int threshold){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Mat srcMat = new Mat();
        Utils.bitmapToMat(bitmap, srcMat);
        Mat greyMat = new Mat();
        Imgproc.cvtColor(srcMat, greyMat, Imgproc.COLOR_RGB2GRAY);

        Mat resMat = new Mat();
        Imgproc.adaptiveThreshold(greyMat,resMat,threshold,Imgproc.ADAPTIVE_THRESH_MEAN_C , Imgproc.THRESH_BINARY,13,5);

        Bitmap binaryImg = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        Utils.matToBitmap(resMat, binaryImg);

        return binaryImg;
    }

    public static Bitmap thresholdImage(Bitmap bitmap,int threshold){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Mat srcMat = new Mat();
        Utils.bitmapToMat(bitmap, srcMat);
        Mat greyMat = new Mat();
        Imgproc.cvtColor(srcMat, greyMat, Imgproc.COLOR_RGB2GRAY);

        Mat resMat = new Mat();
        Imgproc.threshold(greyMat, resMat, threshold, 255, Imgproc.THRESH_BINARY);

        Bitmap binaryImg = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(resMat, binaryImg);

        return binaryImg;
    }

    public static Bitmap resizeBitmap(Bitmap bitmap,int newWidth,int newHeight){

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        RBQLog.i("opencv 缩放前 width:"+width+"; height:"+height);
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap,src);
        Mat scaleMat = new Mat();
        Imgproc.resize(src,scaleMat,new Size(newWidth,newHeight));
        Bitmap newBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.RGB_565);
        RBQLog.i("opencv 缩放后 width:"+newBitmap.getWidth()+"; height:"+newBitmap.getHeight());
        Utils.matToBitmap(scaleMat,newBitmap);

        return newBitmap;
    }

    public static Bitmap resizeBitmapToHeight552AndRGB_565(Bitmap bitmap,boolean isAutoConvertWhiteBackground){
        //缩放时，如果为透明图片，则自动转为白色不透明背景的图片
        if (OpenCVUtils.isTransparent(bitmap)&&isAutoConvertWhiteBackground){
            bitmap = MxImageUtils.transparentBGtoWhite(bitmap);
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scale = 552.0f/height;
        int newWidth = (int) (scale*width);
        RBQLog.i("opencv 缩放前 width:"+width+"; height:"+height);
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap,src);
        Mat scaleMat = new Mat();
        Imgproc.resize(src,scaleMat,new Size(newWidth,552));
        Bitmap newBitmap = Bitmap.createBitmap(newWidth, 552, Bitmap.Config.RGB_565);
        RBQLog.i("opencv 缩放后 width:"+newBitmap.getWidth()+"; height:"+newBitmap.getHeight());
        Utils.matToBitmap(scaleMat,newBitmap);

        return newBitmap;
    }

    public static Bitmap resizeBitmapToHeight552AndARGB_8888(Bitmap bitmap,boolean isAutoConvertWhiteBackground){
        //缩放时，如果为透明图片，则自动转为白色不透明背景的图片
        if (OpenCVUtils.isTransparent(bitmap)&&isAutoConvertWhiteBackground){
            bitmap = MxImageUtils.transparentBGtoWhite(bitmap);
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scale = 552.0f/height;
        int newWidth = (int) (scale*width);
        RBQLog.i("opencv 缩放前 width:"+width+"; height:"+height);
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap,src);
        Mat scaleMat = new Mat();
        Imgproc.resize(src,scaleMat,new Size(newWidth,552));
        Bitmap newBitmap = Bitmap.createBitmap(newWidth, 552, Bitmap.Config.ARGB_8888);
        RBQLog.i("opencv 缩放后 width:"+newBitmap.getWidth()+"; height:"+newBitmap.getHeight());
        Utils.matToBitmap(scaleMat,newBitmap);

        return newBitmap;
    }

    /** 图片灰度化处理 */
    public Bitmap gray(Bitmap srcBitmap) {
        Mat rgbMat = new Mat();
        Mat grayMat = new Mat();
        Bitmap grayBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.RGB_565);

        Utils.bitmapToMat(srcBitmap, rgbMat);
        Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);
        Utils.matToBitmap(grayMat, grayBitmap);

        return grayBitmap;
    }


    /** 图像旋转 */
    public Bitmap rotate(Bitmap srcBitmap, int rotate) {
        Mat srcMat = new Mat();
        Mat dstMat = new Mat();

        Utils.bitmapToMat(srcBitmap, srcMat);

        rotate = rotate % 360;
        int dstWidth = srcBitmap.getWidth();
        int dstHeight = srcBitmap.getHeight();
        switch (rotate) {
            case 0:
                dstMat = srcMat;
                break;
            case 90:
                Core.transpose(srcMat, dstMat);
                dstWidth = srcBitmap.getHeight();
                dstHeight = srcBitmap.getWidth();
                break;
            case 180:
                Core.flip(srcMat, dstMat, -1);
                break;
            case 270:
                Core.transpose(srcMat, dstMat);
                Core.flip(dstMat, dstMat, 1);
                dstWidth = srcBitmap.getHeight();
                dstHeight = srcBitmap.getWidth();
                break;
            default:
                RBQLog.i( "rotate: 旋转的度数应该为90度的倍数");
                return null;
        }
        Bitmap dstBitmap = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.RGB_565);
        Utils.matToBitmap(dstMat, dstBitmap);
        return dstBitmap;
    }


    /** 图像裁剪 */
    public static Bitmap crop(Bitmap srcBitmap, Rect rect) {
        return crop(null, srcBitmap, rect);
    }


    /** 图像裁剪(复用cropBitmap, 节省内存) */
    public static Bitmap crop(Bitmap cropBitmap, Bitmap srcBitmap, Rect rect) {
        if (srcBitmap == null || rect == null) {
            RBQLog.e( "crop:   params is null!!!");
            return null;
        }

        if (rect.left < 0) {
            rect.left = 0;
        }
        if (rect.top < 0) {
            rect.top = 0;
        }
        if (rect.right > srcBitmap.getWidth()) {
            rect.right = srcBitmap.getWidth();
        }
        if (rect.bottom > srcBitmap.getHeight()) {
            rect.bottom = srcBitmap.getHeight();
        }

        if (cropBitmap == null || cropBitmap.getWidth() != rect.width() || cropBitmap.getHeight() != rect.height()) {
            cropBitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.RGB_565);
        }

        Mat srcMat = new Mat();
        Utils.bitmapToMat(srcBitmap, srcMat);
        Mat cropMat = new Mat(srcMat, new Range(rect.top, rect.bottom), new Range(rect.left, rect.right));
        Utils.matToBitmap(cropMat, cropBitmap);

        return cropBitmap;
    }


    /** 图像缩放 */
    public static Bitmap resize(Bitmap srcBitmap, float zoomScale) {

        Mat srcMat = new Mat();
        Mat dstMat = new Mat();
        Bitmap bitmap = Bitmap.createBitmap((int) (srcBitmap.getWidth() * zoomScale),
                (int) (srcBitmap.getHeight() * zoomScale), Bitmap.Config.RGB_565);

        Utils.bitmapToMat(srcBitmap, srcMat);
        Size dSize = new Size(srcBitmap.getWidth() * zoomScale, srcBitmap.getHeight() * zoomScale);
        Imgproc.resize(srcMat, dstMat, dSize);
        Utils.matToBitmap(dstMat, bitmap);

        return bitmap;
    }


    /** 方框滤波 */
    public Bitmap boxFilter(Bitmap srcBitmap, int depth, Size size) {
        Mat srcMat = new Mat();
        Mat dstMat = new Mat();
        Bitmap bitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.RGB_565);

        Utils.bitmapToMat(srcBitmap, srcMat);
        Imgproc.boxFilter(srcMat, dstMat, depth, size);

        Utils.matToBitmap(dstMat, bitmap);

        return bitmap;
    }


    /** 均值滤波 */
    public Bitmap blur(Bitmap srcBitmap, Size size) {
        Mat srcMat = new Mat();
        Mat dstMat = new Mat();
        Bitmap bitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.RGB_565);

        Utils.bitmapToMat(srcBitmap, srcMat);
        Imgproc.blur(srcMat, dstMat, size);

        Utils.matToBitmap(dstMat, bitmap);

        return bitmap;
    }


    /** 高斯滤波 */
    public Bitmap gaussianBlur(Bitmap srcBitmap, Size size) {
        Mat srcMat = new Mat();
        Mat dstMat = new Mat();
        Bitmap bitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.RGB_565);

        Utils.bitmapToMat(srcBitmap, srcMat);
        Imgproc.GaussianBlur(srcMat, dstMat, size, 0);
        Utils.matToBitmap(dstMat, bitmap);

        return bitmap;
    }


    /** 简单的Canny边缘检测 */
    public Bitmap simpleCanny(Bitmap srcBitmap) {
        Mat srcMat = new Mat();
        Mat dstMat = new Mat();
        Bitmap bitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.RGB_565);

        Utils.bitmapToMat(srcBitmap, srcMat);
        Imgproc.Canny(srcMat, dstMat, 100, 100);
        Utils.matToBitmap(dstMat, bitmap);

        return bitmap;
    }

    /** 高级的Canny边缘检测 */
    public Bitmap advantagedCanny(Bitmap srcBitmap) {
        Mat srcMat = new Mat();
        Mat edgeMat = new Mat();
        Mat grayMat = new Mat();
        Bitmap bitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.RGB_565);

        Utils.bitmapToMat(srcBitmap, srcMat);
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.blur(grayMat, edgeMat, new Size(3,3));
        Imgproc.Canny(srcMat, edgeMat, 100, 200);
        Utils.matToBitmap(edgeMat, bitmap);

        return bitmap;
    }


    public static Bitmap yuv2Bitmap(byte[] yuvData, int width, int height, int rotate) {
        Mat yuvMat = new Mat((int) (height * 1.5), width, CvType.CV_8UC1);
        Mat bgrMat = new Mat(height, width, CvType.CV_8UC3);
        yuvMat.put(0, 0, yuvData);

        Imgproc.cvtColor(yuvMat, bgrMat, Imgproc.COLOR_YUV2RGB_NV21);
        yuvMat.release();

        Mat dstMat = rotate(bgrMat, rotate);
        bgrMat.release();

        Bitmap bitmap = Bitmap.createBitmap(dstMat.cols(), dstMat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(dstMat, bitmap);
        dstMat.release();
        return bitmap;
    }


    private static Mat rotate(Mat srcMat, int rotate) {
        Mat dstMat;
        switch (rotate) {
            case 0:
                dstMat = new Mat(srcMat.rows(), srcMat.cols(), CvType.CV_8UC3);
                dstMat = srcMat;
                break;

            case 90:
                dstMat = new Mat(srcMat.cols(), srcMat.rows(), CvType.CV_8UC3);
                Core.transpose(srcMat, dstMat);
                Core.flip(dstMat, dstMat, 1);   //  手机后置摄像头需要再镜像翻转一次
                break;

            case 180:
                dstMat = new Mat(srcMat.rows(), srcMat.cols(), CvType.CV_8UC3);
                Core.flip(srcMat, dstMat, -1);
                break;

            case 270:
                dstMat = new Mat(srcMat.cols(), srcMat.rows(), CvType.CV_8UC3);
                Core.transpose(srcMat, dstMat);
                Core.flip(dstMat, dstMat, 1);
                break;

            default:
                throw new IllegalArgumentException("rotate: 旋转的度数应该为90度的倍数");
        }

        return dstMat;
    }

    /**
     * 获取最大矩形
     */
    public static MatOfPoint findRectangle(Mat source) {
        try {
            Mat src = new Mat();

            Imgproc.cvtColor(source, src, Imgproc.COLOR_BGR2RGB);

            Mat blurred = src.clone();
            Imgproc.medianBlur(src, blurred, 9);
            src.release();

            Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

            List<Mat> blurredChannel = new ArrayList<Mat>();
            blurredChannel.add(blurred);
            List<Mat> gray0Channel = new ArrayList<Mat>();
            gray0Channel.add(gray0);

            MatOfPoint2f approxCurve = null;

            double maxArea = 0;
            int maxId = -1;

            for (int c = 0; c < 3; c++) {
                int[] ch = {c, 0};
                Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

                int thresholdLevel = 1;
                for (int t = 0; t < thresholdLevel; t++) {
                    Imgproc.Canny(gray0, gray, 10, 20, 3, true);
                    Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1);

                    Imgproc.findContours(gray, contours, new Mat(),
                            Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                    for (MatOfPoint contour : contours) {
                        MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());

                        double area = Imgproc.contourArea(contour);
                        if (approxCurve != null) {
                            approxCurve.release();
                        }
                        approxCurve = new MatOfPoint2f();
                        Imgproc.approxPolyDP(temp, approxCurve,
                                Imgproc.arcLength(temp, true) * 0.02, true);
                        temp.release();

                        if (approxCurve.total() == 4 && area >= maxArea) {
                            double maxCosine = 0;

                            List<Point> curves = approxCurve.toList();
                            for (int j = 2; j < 5; j++) {
                                double cosine = Math.abs(angle(curves.get(j % 4),
                                        curves.get(j - 2), curves.get(j - 1)));
                                maxCosine = Math.max(maxCosine, cosine);
                            }

                            if (maxCosine < 0.3) {
                                maxArea = area;
                                maxId = contours.indexOf(contour);
                            }
                        }
                    }
                }
            }

            gray0.release();
            gray.release();
            blurred.release();
            if (approxCurve != null) {
                approxCurve.release();
            }

            if (maxId >= 0) {
                RBQLog.d("最大矩形");
                return contours.get(maxId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 判断清晰度
     *
     * @param image bitmap
     */
    public static boolean isBlurByOpenCV(Bitmap image) {
        int l = CvType.CV_8UC1;
        Mat matImage = new Mat();
        Utils.bitmapToMat(image, matImage);
        Mat matImageGrey = new Mat();
        Imgproc.cvtColor(matImage, matImageGrey, Imgproc.COLOR_RGB2GRAY); // 图像灰度化
        Bitmap destImage;
        destImage = Bitmap.createBitmap(image);
        Mat dst2 = new Mat();
        Utils.bitmapToMat(destImage, dst2);
        Mat laplacianImage = new Mat();
        dst2.convertTo(laplacianImage, l);
        Imgproc.Laplacian(matImageGrey, laplacianImage, CvType.CV_8U); // 拉普拉斯变换
        Mat laplacianImage8bit = new Mat();
        laplacianImage.convertTo(laplacianImage8bit, l);
        Bitmap bmp = Bitmap
                .createBitmap(laplacianImage8bit.cols(), laplacianImage8bit.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(laplacianImage8bit, bmp);
        int[] pixels = new int[bmp.getHeight() * bmp.getWidth()];
        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight()); // bmp为轮廓图
        int maxLap = -16777216;
        for (int pixel : pixels) {
            if (pixel > maxLap) {
                maxLap = pixel;
            }
        }
        int userOffset = -4881250; // 界线（严格性）降低一点
        int soglia = -6118750 + userOffset; // -6118750为广泛使用的经验值
        soglia += 6118750 + userOffset;
        maxLap += 6118750 + userOffset;
        return maxLap <= soglia;
    }

    /**
     * 判断清晰度
     *
     * @param picFilePath 地址
     */
    public static boolean isBlurByOpenCV(String picFilePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        // 通过path得到一个不超过2000*2000的Bitmap
        Bitmap image = decodeSampledBitmapFromFile(picFilePath, options, 2000, 2000);
        return isBlurByOpenCV(image);
    }

    /**
     * 图像透射变换
     */
    public static Bitmap imageRegulate(Bitmap bitmap) {
        Bitmap bitmapLast = null;
        try {
            Mat source = new Mat();
            Utils.bitmapToMat(bitmap, source);

            Mat src = source.clone();
            Mat bkup = source.clone();
            Mat img = source.clone();

            Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2GRAY);//二值化
            Imgproc.GaussianBlur(img, img, new Size(5, 5), 0, 0);

            //获取自定义核
            Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                    new Size(3, 3)); //第一个参数MORPH_RECT表示矩形的卷积核，当然还可以选择椭圆形的、交叉型的
            //膨胀操作
            Imgproc.dilate(img, img, element);  //实现过程中发现，适当的膨胀很重要
            Imgproc.Canny(img, img, 30, 120);   //边缘提取
            element.release();

            List<MatOfPoint> contours = new ArrayList<>();
            List<MatOfPoint> f_contours = new ArrayList<>();
            //注意第5个参数为CV_RETR_EXTERNAL，只检索外框
            Imgproc.findContours(img, f_contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE); //找轮廓

            //求出面积最大的轮廓
            int max_area = 0;
            int index = 0;
            for (int i = 0; i < f_contours.size(); i++) {
                double tmparea = Math.abs(Imgproc.contourArea(f_contours.get(i)));
                if (tmparea > max_area) {
                    index = i;
                    max_area = (int) tmparea;
                }

            }
            if (f_contours.isEmpty()) {
                source.release();
                src.release();
                bkup.release();
                img.release();
                return null;
            }
            contours.add(f_contours.get(index));

            for (int line_type = 1; line_type <= 3; line_type++) {
                Mat black = img.clone();
                black.setTo(new Scalar(0, 0, 0));
                Imgproc.drawContours(black, contours, 0, new Scalar(255, 255, 255), line_type);  //注意线的厚度，不要选择太细的

                Mat lines = new Mat();
                Mat lineNew = new Mat();
                List<Point> corners = new ArrayList<>();
                MatOfPoint2f approx = new MatOfPoint2f();

                int para = 10;
                int flag = 0;
                for (; para < 300; para++) {
                    lines.release();
                    lineNew.release();
                    corners.clear();
                    approx.release();

                    Imgproc.HoughLinesP(black, lines, 1, Math.PI / 180, para, 30, 10);

                    Set<Integer> ErasePt = new HashSet<Integer>();
                    for (int i = 0; i < lines.rows(); i++) {
                        for (int j = i + 1; j < lines.rows(); j++) {
                            if (isBadLine((int) Math.abs(lines.get(i, 0)[0] - lines.get(j, 0)[0]),
                                    (int) Math.abs(lines.get(i, 0)[1] - lines.get(j, 0)[1]))
                                    && isBadLine((int) Math.abs(lines.get(i, 0)[2] - lines.get(j, 0)[2]),
                                    (int) Math.abs(lines.get(i, 0)[3] - lines.get(j, 0)[3]))) {
                                ErasePt.add(j);//将该坏线加入集合
                            }
                        }
                    }

                    RBQLog.d( "坏线数---" + ErasePt.size());

                    for (int Num = 0; Num < lines.rows(); Num++) {
                        if (!ErasePt.contains(Num)) {
                            lineNew.push_back(lines.rowRange(Num, Num + 1));
                        }
                    }

                    RBQLog.d("好线数---" + lineNew.rows());

                    if (lineNew.rows() != 4) {
                        continue;
                    }
                    //计算直线的交点，保存在图像范围内的部分
                    for (int i = 0; i < lineNew.rows(); i++) {
                        for (int j = i + 1; j < lineNew.rows(); j++) {
                            Point pt = computeIntersect(lineNew.get(i, 0), lineNew.get(j, 0));
                            if (pt.x >= 0 && pt.y >= 0 && pt.x <= src.cols() && pt.y <= src
                                    .rows()) { //保证交点在图像的范围之内
                                corners.add(pt);
                            }
                        }
                    }
                    RBQLog.d("点数---" + corners.size());

                    if (corners.size() != 4) {
                        continue;
                    }

                    boolean IsGoodPoints = true;

                    // 保证点与点的距离足够大以排除错误点
                    for (int i = 0; i < corners.size(); i++) {
                        for (int j = i + 1; j < corners.size(); j++) {
                            double distance = Math
                                    .sqrt((corners.get(i).x - corners.get(j).x) * (corners.get(i).x - corners
                                            .get(j).x) + (corners.get(i).y - corners.get(j).y) * (corners.get(i).y
                                            - corners.get(j).y));
                            if (distance < 5) {
                                IsGoodPoints = false;
                            }
                        }
                    }
                    if (!IsGoodPoints) {
                        continue;
                    }

                    MatOfPoint2f corners_pts = new MatOfPoint2f(
                            corners.get(0),
                            corners.get(1),
                            corners.get(2),
                            corners.get(3)
                    );

                    Imgproc.approxPolyDP(corners_pts, approx, Imgproc.arcLength(corners_pts, true) * 0.02, true);

                    if (lineNew.rows() == 4 && corners.size() == 4 && approx.rows() == 4) {
                        flag = 1;
                        corners_pts.release();
                        break;
                    }
                }
                // 释放本 line_type 迭代在 para 循环中创建的临时 Mat（最后一次迭代末被循环开头释放）
                lines.release();
                lineNew.release();
                approx.release();
                black.release();

                RBQLog.d( "flag---" + flag);
                // Get mass center
                Point center = new Point(0, 0);
                for (int i = 0; i < corners.size(); i++) {
                    center.x = center.x + corners.get(i).x;
                    center.y = center.y + corners.get(i).y;
                }
                center.x = center.x / corners.size();
                center.y = center.y / corners.size();

                if (flag == 1) {

                    Imgproc.circle(bkup, corners.get(0), 3, new Scalar(255, 0, 0), -1);
                    Imgproc.circle(bkup, corners.get(1), 3, new Scalar(0, 255, 0), -1);
                    Imgproc.circle(bkup, corners.get(2), 3, new Scalar(0, 0, 255), -1);
                    Imgproc.circle(bkup, corners.get(3), 3, new Scalar(255, 255, 255), -1);
                    Imgproc.circle(bkup, center, 3, new Scalar(255, 0, 255), -1);

                    corners = sortCorners(corners, center);

                    double[] dstSize = calcDstSize(corners);
                    double g_dst_height = dstSize[0];
                    double g_dst_width = dstSize[1];

                    MatOfPoint2f corners_pts = new MatOfPoint2f(
                            corners.get(0),
                            corners.get(1),
                            corners.get(2),
                            corners.get(3)
                    );

                    Mat quad = Mat.zeros((int) g_dst_height, (int) g_dst_width, CvType.CV_8UC3);
                    MatOfPoint2f quad_pts = new MatOfPoint2f(
                            new Point(0, 0),
                            new Point(quad.cols(), 0),
                            new Point(0, quad.rows()),
                            new Point(quad.cols(), quad.rows())
                    );

                    Mat transmtx = Imgproc.getPerspectiveTransform(corners_pts, quad_pts);
                    Imgproc.warpPerspective(source, quad, transmtx, quad.size());

//                    Core.rotate(quad, quad, Core.ROTATE_180);
                    bitmapLast = Bitmap.createBitmap(quad.cols(), quad.rows(), Bitmap.Config.RGB_565);
                    Utils.matToBitmap(quad, bitmapLast);

                    corners_pts.release();
                    quad_pts.release();
                    transmtx.release();
                    quad.release();
                }
            }
            source.release();
            src.release();
            bkup.release();
            img.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmapLast;
    }

    /**
     * 图像透射变换 mat point
     */
    public static Bitmap imageRegulateMat(Mat source, List<Point> corners) {
        Bitmap bitmapLast = null;
        try {
            Point center = new Point(0, 0);
            for (int i = 0; i < corners.size(); i++) {
                center.x = center.x + corners.get(i).x;
                center.y = center.y + corners.get(i).y;
            }
            center.x = center.x / corners.size();
            center.y = center.y / corners.size();

            corners = sortCorners(corners, center);

            double h1 = Math.sqrt((corners.get(0).x - corners.get(3).x) * (corners.get(0).x - corners.get(3).x)
                    + (corners.get(0).y - corners.get(3).y) * (corners.get(0).y - corners.get(3).y));
            double h2 = Math.sqrt((corners.get(1).x - corners.get(2).x) * (corners.get(1).x - corners.get(2).x)
                    + (corners.get(1).y - corners.get(2).y) * (corners.get(1).y - corners.get(2).y));
            double g_dst_hight = Math.max(h1, h2);

            double w1 = Math.sqrt((corners.get(0).x - corners.get(1).x) * (corners.get(0).x - corners.get(1).x)
                    + (corners.get(0).y - corners.get(1).y) * (corners.get(0).y - corners.get(1).y));
            double w2 = Math.sqrt((corners.get(2).x - corners.get(3).x) * (corners.get(2).x - corners.get(3).x)
                    + (corners.get(2).y - corners.get(3).y) * (corners.get(2).y - corners.get(3).y));
            double g_dst_width = Math.max(w1, w2);

            MatOfPoint2f corners_pts = new MatOfPoint2f(
                    corners.get(0),
                    corners.get(1),
                    corners.get(2),
                    corners.get(3)
            );

            Mat quad = Mat.zeros((int) g_dst_hight, (int) g_dst_width, CvType.CV_8UC3);
            MatOfPoint2f quad_pts = new MatOfPoint2f(
                    new Point(0, 0),
                    new Point(quad.cols(), 0),
                    new Point(0, quad.rows()),
                    new Point(quad.cols(), quad.rows())
            );

            Mat transmtx = Imgproc.getPerspectiveTransform(corners_pts, quad_pts);
            Imgproc.warpPerspective(source, quad, transmtx, quad.size());
//            cvtColor(quad, quad, Imgproc.COLOR_BGR2GRAY);
            bitmapLast = Bitmap.createBitmap(quad.cols(), quad.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(quad, bitmapLast);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmapLast;
    }

    /**
     * 灰度 二值化 降噪 处理图像
     */
    public static Bitmap binaryZation(Bitmap bitmap) {
        Bitmap bitmapLast = null;
        try {
            Mat source = new Mat();
            Utils.bitmapToMat(bitmap, source);
            Mat mat = new Mat();
            Imgproc.cvtColor(source, mat, Imgproc.COLOR_RGB2GRAY);
            source.release();
            int BLACK = 0;
            int WHITE = 255;
            int ucThre = 0, ucThre_new = 127;
            int nBack_count, nData_count;
            int nBack_sum, nData_sum;
            int nValue;
            int i, j;

            int width = mat.width(), height = mat.height();
            //寻找最佳的阙值
            while (ucThre != ucThre_new) {
                nBack_sum = nData_sum = 0;
                nBack_count = nData_count = 0;

                for (j = 0; j < height; ++j) {
                    for (i = 0; i < width; i++) {
                        nValue = (int) mat.get(j, i)[0];

                        if (nValue > ucThre_new) {
                            nBack_sum += nValue;
                            nBack_count++;
                        } else {
                            nData_sum += nValue;
                            nData_count++;
                        }
                    }
                }

                nBack_sum = nBack_sum / nBack_count;
                nData_sum = nData_sum / nData_count;
                ucThre = ucThre_new;
                ucThre_new = (nBack_sum + nData_sum) / 2;
            }

            //二值化处理
            int nBlack = 0;
            int nWhite = 0;
            for (j = 0; j < height; ++j) {
                for (i = 0; i < width; ++i) {
                    nValue = (int) mat.get(j, i)[0];
                    if (nValue > ucThre_new) {
                        mat.put(j, i, WHITE);
                        nWhite++;
                    } else {
                        mat.put(j, i, BLACK);
                        nBlack++;
                    }
                }
            }

            // 确保白底黑字
            if (nBlack > nWhite) {
                for (j = 0; j < height; ++j) {
                    for (i = 0; i < width; ++i) {
                        nValue = (int) (mat.get(j, i)[0]);
                        if (nValue == 0) {
                            mat.put(j, i, WHITE);
                        } else {
                            mat.put(j, i, BLACK);
                        }
                    }
                }
            }
            Mat lastMat = eightRemoveNoise(mat, 1);
            bitmapLast = Bitmap.createBitmap(lastMat.cols(), lastMat.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(lastMat, bitmapLast);
            mat.release(); // lastMat == mat (eightRemoveNoise returns the same object)

        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmapLast;
    }

    /**
     * 8邻域降噪，又有点像9宫格降噪;即如果9宫格中心被异色包围，则同化 作用：降噪(默认白底黑字)
     *
     * @param src  Mat矩阵对象
     * @param pNum 阀值 默认取1即可
     */
    public static Mat eightRemoveNoise(Mat src, int pNum) {
        int i, j, m, n, nValue, nCount;
        int width = getImgWidth(src), height = getImgHeight(src);

        // 如果一个点的周围都是白色的，自己确实黑色的，同化
        for (j = 1; j < height - 1; j++) {
            for (i = 1; i < width - 1; i++) {
                nValue = getPixel(src, j, i);
                if (nValue == 0) {
                    nCount = 0;
                    // 比较(j , i)周围的9宫格，如果周围都是白色，同化
                    for (m = j - 1; m <= j + 1; m++) {
                        for (n = i - 1; n <= i + 1; n++) {
                            if (getPixel(src, m, n) == 0) {
                                nCount++;
                            }
                        }
                    }
                    if (nCount <= pNum) {
                        // 周围黑色点的个数小于阀值pNum,把自己设置成白色
                        setPixel(src, j, i, getWHITE());
                    }
                } else {
                    nCount = 0;
                    // 比较(j , i)周围的9宫格，如果周围都是黑色，同化
                    for (m = j - 1; m <= j + 1; m++) {
                        for (n = i - 1; n <= i + 1; n++) {
                            if (getPixel(src, m, n) == 0) {
                                nCount++;
                            }
                        }
                    }
                    if (nCount >= 8 - pNum) {
                        // 周围黑色点的个数大于等于(8 - pNum),把自己设置成黑色
                        setPixel(src, j, i, getBLACK());
                    }
                }
            }
        }
        return src;
    }

    /**
     * 作用：输入图像Mat矩阵对象，返回图像的宽度
     *
     * @param src Mat矩阵图像
     */
    private static int getImgWidth(Mat src) {
        return src.cols();
    }

    /**
     * 作用：输入图像Mat矩阵，返回图像的高度
     *
     * @param src Mat矩阵图像
     */
    private static int getImgHeight(Mat src) {
        return src.rows();
    }

    /**
     * 作用：获取图像(y,x)点的像素，我们只针对单通道(灰度图)
     *
     * @param src Mat矩阵图像
     * @param y   y坐标轴
     * @param x   x坐标轴
     */
    private static int getPixel(Mat src, int y, int x) {
        return (int) src.get(y, x)[0];
    }

    /**
     * 作用：设置图像(y,x)点的像素，我们只针对单通道(灰度图)
     *
     * @param src   Mat矩阵图像
     * @param y     y坐标轴
     * @param x     x坐标轴
     * @param color 颜色值[0-255]
     */
    private static void setPixel(Mat src, int y, int x, int color) {
        src.put(y, x, color);
    }

    private static int getBLACK() {
        return BLACK;
    }

    private static int getWHITE() {
        return WHITE;
    }

    private static double[] calcDstSize(List<Point> corners) {
        double h1 = Math.sqrt((corners.get(0).x - corners.get(3).x) * (corners.get(0).x - corners.get(3).x)
                + (corners.get(0).y - corners.get(3).y) * (corners.get(0).y - corners.get(3).y));
        double h2 = Math.sqrt((corners.get(1).x - corners.get(2).x) * (corners.get(1).x - corners.get(2).x)
                + (corners.get(1).y - corners.get(2).y) * (corners.get(1).y - corners.get(2).y));
        double g_dst_height = Math.max(h1, h2);

        double w1 = Math.sqrt((corners.get(0).x - corners.get(1).x) * (corners.get(0).x - corners.get(1).x)
                + (corners.get(0).y - corners.get(1).y) * (corners.get(0).y - corners.get(1).y));
        double w2 = Math.sqrt((corners.get(2).x - corners.get(3).x) * (corners.get(2).x - corners.get(3).x)
                + (corners.get(2).y - corners.get(3).y) * (corners.get(2).y - corners.get(3).y));
        double g_dst_width = Math.max(w1, w2);
        return new double[]{g_dst_height, g_dst_width};
    }

    private static List<Point> sortCorners(List<Point> corners, Point center) {
        List<Point> top = new ArrayList<>();
        List<Point> bot = new ArrayList<>();
//        List<Point> backup = corners;
        List<Point> backup = new ArrayList<>(corners);

        for (int i = 0; i < corners.size(); i++) {
            for (int j = i + 1; j < corners.size(); j++) {
                if (corners.get(i).x > corners.get(j).x) {
                    Point tmp = corners.get(i);
                    corners.set(i, corners.get(j));
                    corners.set(j, tmp);
                }
            }
        }

        for (int i = 0; i < corners.size(); i++) {
            if (corners.get(i).y < center.y && top.size() < 2) {
                top.add(corners.get(i));
            } else {
                bot.add(corners.get(i));
            }
        }

        corners.clear();

        if (top.size() == 2 && bot.size() == 2) {
            Point tl = top.get(0).x > top.get(1).x ? top.get(1) : top.get(0);
            Point tr = top.get(0).x > top.get(1).x ? top.get(0) : top.get(1);
            Point bl = bot.get(0).x > bot.get(1).x ? bot.get(1) : bot.get(0);
            Point br = bot.get(0).x > bot.get(1).x ? bot.get(0) : bot.get(1);

            corners.add(tl);
            corners.add(tr);
            corners.add(bl);
            corners.add(br);
        } else {
            corners = backup;
        }
        return corners;
    }

    private static Point computeIntersect(double[] a, double[] b) {
        double x1 = a[0], y1 = a[1], x2 = a[2], y2 = a[3];
        double x3 = b[0], y3 = b[1], x4 = b[2], y4 = b[3];
        double h1 = y2 - y1;
        double h2 = x2 * y1 - x1 * y2;
        double h3 = x2 - x1;
        double h4 = y4 - y3;
        double h5 = x4 * y3 - x3 * y4;
        double h6 = x4 - x3;

        double y = (h1 * h5 - h2 * h4) / (h1 * h6 - h3 * h4);
        double x = (y * h3 - h2) / h1;
        return new Point(x, y);
    }

    private static Boolean isBadLine(int a, int b) {
        return (a * a + b * b < 100);
    }

    private static Bitmap decodeSampledBitmapFromFile(String imgPath, BitmapFactory.Options options, int reqWidth,
                                                      int reqHeight) {
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath, options);
        // inSampleSize为缩放比例，举例：options.inSampleSize = 2表示缩小为原来的1/2，3则是1/3，以此类推
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imgPath, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
        RBQLog.i("inSampleSize=" + inSampleSize);
        return inSampleSize;
    }

    private static double angle(Point p1, Point p2, Point p0) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2)
                / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
                + 1e-10);
    }

    //去除前景文字字迹clearForeground
    public static Bitmap clearForeground(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);

        //转为三通道
        Mat src3channels = new Mat();
        Imgproc.cvtColor(src,src3channels,Imgproc.COLOR_RGBA2RGB);
        src.release();

        // 转换为灰度图像
        Mat gray = new Mat();
        Imgproc.cvtColor(src3channels, gray, Imgproc.COLOR_RGB2GRAY);

        // 使用高斯滤波器降噪
        Imgproc.GaussianBlur(gray, gray, new Size(3, 3), 0); // inplace reuse

        // 使用Canny边缘检测
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 50, 150);
        gray.release();

        // 使用膨胀操作填充字迹区域
        Mat dilated = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(edges, dilated, kernel);
        edges.release();

        // 使用原图像与膨胀后的图像进行按位与操作，去除字迹，得到掩码
        Mat result = new Mat();
        Core.bitwise_and(src3channels, src3channels, result, dilated);
        dilated.release();

        Imgproc.cvtColor(result,result, Imgproc.COLOR_RGBA2GRAY);//转为单通道，非黑既白

        Imgproc.dilate(result,result,kernel,new Point(-1,-1),2);//继续膨胀，不然还是有文字字体存在
        kernel.release();

        // 创建一个新的Mat对象，用于存储修复后的图像，应用inpaint函数修复图像
        Mat repaired = new Mat();
        Photo.inpaint(src3channels,result,repaired,3,Photo.INPAINT_TELEA);
        src3channels.release();
        result.release();

        Bitmap newImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(repaired, newImage);
        repaired.release();
        return newImage;
    }

    //    素描
    public static Bitmap sketchEffect(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);

        // 2. 将图像转换为灰度图像
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);
        src.release();

        // 3. 使用高斯滤波器平滑图像
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(3, 3), 0);
        gray.release();

        // 4. 计算图像的梯度值
        Mat gradientX = new Mat();
        Mat gradientY = new Mat();
        Imgproc.Sobel(blurred, gradientX, CvType.CV_16S, 1, 0);
        Imgproc.Sobel(blurred, gradientY, CvType.CV_16S, 0, 1);
        blurred.release();

        // 5. 应用阈值处理以创建二值图像
        Mat absGradientX = new Mat();
        Mat absGradientY = new Mat();
        //将梯度值转换为8位无符号整数
        Core.convertScaleAbs(gradientX, absGradientX);
        Core.convertScaleAbs(gradientY, absGradientY);
        gradientX.release();
        gradientY.release();

        Mat sketch = new Mat();
        //合并梯度值以获得最终的素描图像
        Core.addWeighted(absGradientX, 0.5, absGradientY, 0.5, 0, sketch);
        absGradientX.release();
        absGradientY.release();
        // 反转图像以获得白底的素描效果
        Core.bitwise_not(sketch, sketch);

        // 6. 显示或保存结果图像
        Bitmap newImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(sketch, newImage);
        sketch.release();
        return newImage;
    }

    //颜色反转
    public static Bitmap negativeEffect(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);
        // 创建一个与输入图像大小相同的空白图像
        Mat negativeImage = new Mat(src.size(), CvType.CV_8UC3, new Scalar(0, 0, 0));

        // 反转颜色
        Core.bitwise_not(src, negativeImage);

        // 显示或保存结果图像
        Bitmap newImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Utils.matToBitmap(negativeImage, newImage);
        return newImage;
    }

}
