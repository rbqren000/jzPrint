package com.mx.mxSdk.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import androidx.annotation.Nullable;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MxSdkStore {

    /**
     * 位于.nomedia文件夹下的图片，将被系统隐藏，不显示在系统相册和文件夹中，
     * 这里主要用于图片缓存，APP在打开的时候会自动删除3天前缓存的图片
     */
    //存储的基本路径
    public static final String basePath= ".nomedia"+File.separator;

    public static final String cacheImagePath = "mxSdk"+File.separator+"images"+File.separator;
    //存储打印数据
    public static final String cacheDataPath = "mxSdk"+File.separator+"data"+File.separator;

    public static final String sqlLiteDataPath = "mxSdk"+File.separator+"sqlLiteData"+File.separator+"image"+File.separator;

    /**
     * 这里注释掉的真正原因是因为在Android 11上面，getExternalStorageState()不再允许使用
     * @param context 上下文对象
     * @return 返回文件路径
     */
    public static String getBasePath(Context context){
//        return context.getCacheDir().getAbsolutePath();
//        return context.getExternalFilesDir("").getAbsolutePath();
        return context.getFilesDir().getAbsolutePath();
    }

    public static String cacheImageFile(Context context){

        String rootPath = getBasePath(context) + File.separator;
        String imagePath = rootPath + basePath + cacheImagePath;
        File baseImageFile = new File(imagePath);
        if (!baseImageFile.exists()){
            boolean mkdirs = baseImageFile.mkdirs();
            RBQLog.i("创建baseImageFile:"+baseImageFile+(mkdirs?"成功":"失败"));
        }
        return imagePath;
    }

    private static String sqlLiteDataFile(Context context) {

        String rootPath = getBasePath(context) + File.separator;
        String imagePath = rootPath + basePath + sqlLiteDataPath;
        File baseImageFile = new File(imagePath);
        if (!baseImageFile.exists()){
            boolean mkdirs = baseImageFile.mkdirs();
            RBQLog.i("创建dataImagePath:"+baseImageFile+(mkdirs?"成功":"失败"));
        }
        return imagePath;
    }

    private static String cacheDataFile(Context context) {

        String rootPath = getBasePath(context) + File.separator;
        String path = rootPath + basePath + cacheDataPath;
        File file = new File(path);
        if (!file.exists()){
            boolean mkdirs = file.mkdirs();
            RBQLog.i("创建dataPath:"+file+(mkdirs?"成功":"失败"));
        }
        return path;
    }

    /**
     * 保存图片
     * @param context 上下文对象
     * @param bitmap 要保存的图片
     */
    public static String saveImageToCache(Context context,Bitmap bitmap) {
        if (bitmap==null)return null;
        String fileName = create26LenStringByLetterAndNumber()+".jpg";
        String path = cacheImageFile(context);
        File mFile = new File(path, fileName);
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(mFile.getAbsolutePath()));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != out) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mFile.getPath();
    }

    public static String saveImageToSqlLiteFile(Context context,Bitmap bitmap) {

        if (bitmap==null)return null;
        String fileName = create26LenStringByLetterAndNumber()+".jpg";

        String path = sqlLiteDataFile(context);
        File mFile = new File(path, fileName);
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(mFile.getAbsolutePath()));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != out) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mFile.getPath();
    }

    public static String saveDrawableToCache(Context context,BitmapDrawable drawable) {

        if (drawable==null) return null;
        String fileName = create26LenStringByLetterAndNumber()+".jpg";

        Bitmap bitmap = drawable.getBitmap();

        String path = cacheImageFile(context);
        File mFile = new File(path, fileName);
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(mFile.getAbsolutePath()));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != out) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mFile.getPath();
    }

    public static String saveDrawableToSqlLiteFile(Context context,BitmapDrawable drawable) {

        if (drawable==null)return null;
        String fileName = create26LenStringByLetterAndNumber()+".jpg";

        Bitmap bitmap = drawable.getBitmap();

        String path = sqlLiteDataFile(context);
        File mFile = new File(path, fileName);
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(mFile.getAbsolutePath()));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != out) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mFile.getPath();
    }

    public static Bitmap getImageFromPath(String imagePath,Bitmap.Config config){

        if (TextUtils.isEmpty(imagePath)) return null;
        File mFile = new File(imagePath);
        if (!mFile.exists()) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;//这个参数设置为true才有效，
        options.inPreferredConfig = config;
        options.inSampleSize = 1;
        return BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
    }

    public static Bitmap getImageFromPath(String imagePath,Bitmap.Config config,boolean flipHorizontally){

        if (TextUtils.isEmpty(imagePath)) return null;
        File mFile = new File(imagePath);
        if (!mFile.exists()) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();

        if (!flipHorizontally){

            options.inJustDecodeBounds = false;//这个参数设置为true才有效，
            options.inPreferredConfig = config;
            options.inSampleSize = 1;
            return BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);

        }else {

            options.inJustDecodeBounds = true;//这个参数设置为true才有效，
            options.inPreferredConfig = config;
            options.inSampleSize = 1;
            BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
            int width = options.outWidth;
            int height = options.outHeight;
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
            Matrix matrix = new Matrix();
            matrix.postScale(-1, 1); // 镜像水平翻转
            return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        }
    }

    public static Drawable getDrawableFromPath(Context context, String imagePath,Bitmap.Config config){

        if (TextUtils.isEmpty(imagePath)) return null;
        File mFile = new File(imagePath);
        if (!mFile.exists()) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;//这个参数设置为true返回的是图片的轮廓，bitmap为null，设置为false，则返回真实的图片，
        options.inPreferredConfig = config;
        options.inSampleSize = 1;
        Bitmap bitmap = BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static Drawable getDrawableFromPath(Context context, String imagePath,Bitmap.Config config,boolean flipHorizontally){

        if (TextUtils.isEmpty(imagePath)) return null;
        File mFile = new File(imagePath);
        if (!mFile.exists()) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        if (!flipHorizontally){

            options.inJustDecodeBounds = false;//这个参数设置为true才有效，
            options.inPreferredConfig = config;
            options.inSampleSize = 1;
            Bitmap bitmap = BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
            return new BitmapDrawable(context.getResources(),bitmap);

        }else {

            options.inJustDecodeBounds = true;//这个参数设置为true才有效，
            options.inPreferredConfig = config;
            options.inSampleSize = 1;
            BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
            int width = options.outWidth;
            int height = options.outHeight;
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
            Matrix matrix = new Matrix();
            matrix.postScale(-1, 1); // 镜像水平翻转
            Bitmap convertBitmap =  Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            return new BitmapDrawable(context.getResources(),convertBitmap);
        }
    }

    public static Bitmap highThumbnailImageFromPath(String imagePath,Bitmap.Config config){

        if (TextUtils.isEmpty(imagePath)) return null;
        File mFile = new File(imagePath);
        if (!mFile.exists()) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        //这个参数设置为true才有效，获取到图片的尺寸，但是这个时候图片没加载到内存，设置为false的时候，图片会加载到内存
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);

        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        int max = Math.max(width,height);
        if (max>1200){
            inSampleSize = max/1200;
        }
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = config;
        options.inSampleSize = inSampleSize;
        return BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
    }

    public static Bitmap thumbnailImageFromPath(String imagePath,Bitmap.Config config){

        if (TextUtils.isEmpty(imagePath)) return null;
        File mFile = new File(imagePath);
        if (!mFile.exists()) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        //这个参数设置为true才有效，获取到图片的尺寸，但是这个时候图片没加载到内存，设置为false的时候，图片会加载到内存
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);

        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        int max = Math.max(width,height);
        if (max>600){
            inSampleSize = max/600;
        }
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = config;
        options.inSampleSize = inSampleSize;
        return BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
    }

    public static Bitmap lowThumbnailImageFromPath(String imagePath,Bitmap.Config config){

        if (TextUtils.isEmpty(imagePath)) return null;
        File mFile = new File(imagePath);
        if (!mFile.exists()) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        //这个参数设置为true才有效，获取到图片的尺寸，但是这个时候图片没加载到内存，设置为false的时候，图片会加载到内存
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);

        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        int max = Math.max(width,height);
        if (max>300){
            inSampleSize = max/300;
        }
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = config;
        options.inSampleSize = inSampleSize;
        return BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
    }

    public static boolean deleteImageFromPath(String imagePath){

        if (TextUtils.isEmpty(imagePath)) return false;
        File mFile = new File(imagePath);
        if (mFile.exists()) {
            return mFile.delete();
        }
        return false;
    }

    public static Uri getUriFormPath(String path){

        if (TextUtils.isEmpty(path)) return null;
        File mFile = new File(path);
        if (mFile.exists()) {
            return Uri.fromFile(mFile);
        }
        return null;
    }

    public static String writeByteArrToCacheDataFile(Context context, byte[] data) {
        if (data == null) return "";
        String fileName = create26LenStringByLetterAndNumber() + ".data";
        File file = new File(cacheDataFile(context), fileName);

        try {
            File pf = file.getParentFile();
            if (pf != null && !pf.exists()) {
                boolean created = pf.mkdirs();
                RBQLog.i("创建父目录: " + (created ? "成功" : "失败"));
            }

            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
                out.write(data);
                RBQLog.i("写入打印数据完成");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return file.getPath();
    }

    public static byte[] readByteArrToCacheDataFile(String dataPath) {
        if (dataPath == null) return null;
        File file = new File(dataPath);
        if (!file.exists()) return null;

        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            byte[] data = new byte[(int) file.length()];
            dis.readFully(data);
            RBQLog.i("读取到字节数 read:" + data.length);
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 删除缓存文件夹下，几天前的图片
     * @param context
     */
    public static void deleteBeforeThreeDataImageToCache(Context context){

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        long nowTime = System.currentTimeMillis();
        long beforeOneDayTimes = 24L * 60 * 60 * 1000;
        long beforeTwoDayTimes = 2 * 24L * 60 * 60 * 1000;
        long beforeThreeDayTimes = 3 * 24L * 60 * 60 * 1000;
        String beforeOneDayStart = sdf.format(new Date(nowTime+beforeOneDayTimes));
        String beforeTwoDayStart = sdf.format(new Date(nowTime+beforeTwoDayTimes));
        String beforeThreeDayStart = sdf.format(new Date(nowTime+beforeThreeDayTimes));

        String rootPath = getBasePath(context) + File.separator;
        String cacheImage = rootPath + basePath + cacheImagePath;

        File cacheImageFile = new File(cacheImage);

        if (cacheImageFile.exists() && cacheImageFile.isDirectory()) {
            File[] files = cacheImageFile.listFiles();
            for (int i=0;files!=null&&i<files.length;i++) {
                File f = files[i];
                if (f.isDirectory()){
                    deleteBeforeThreeDataImageToCache(context);
                }else {
                    String str = "文件名称:" + f.getName() + " 路径:" + f.getAbsolutePath();
                    RBQLog.i("app缓存图片:"+str);
                    //在这了删除不符合条件的图片，既3天前的图片
                    String fileName = f.getName().toLowerCase();
                    if ((!fileName.startsWith(beforeOneDayStart)
                            &&!fileName.startsWith(beforeTwoDayStart)
                            &&!fileName.startsWith(beforeThreeDayStart))
                            &&(fileName.endsWith(".png")
                            ||fileName.endsWith(".jpeg")
                            ||fileName.endsWith(".jpg")
                            ||fileName.endsWith(".bmp"))){
                        boolean delete = f.delete();
                        RBQLog.i("删除缓存的图片:"+f.getAbsolutePath()+(delete?"成功":"失败"));
                    }
                }
            }
        } else {
            RBQLog.i("文件不存在......");
        }
    }

    @SafeVarargs
    public static <T> ArrayList<T> asArrayLists(@Nullable T[]... arrays) {
        ArrayList<T> list = new ArrayList<>();
        if (arrays == null) {
            return list;
        }
        for (T[] ts : arrays) {
            list.addAll(asArrayList(ts));
        }
        return list;
    }

    @SafeVarargs
    public static <T> ArrayList<T> asArrayList(@Nullable T... array) {
        int initialCapacity = 0;
        if (array != null) {
            initialCapacity = array.length;
        }
        ArrayList<T> list = new ArrayList<>(initialCapacity);
        if (array == null) {
            return list;
        }
        Collections.addAll(list, array);
        return list;
    }

    public static void clearImageCacheWithSinglePath(Context context,@Nullable String protectedPath) {
        clearImageCache(context,asArrayList(protectedPath));
    }

    public static void clearImageCacheWithMultiplePaths(Context context,@Nullable String... protectedPaths) {
        clearImageCache(context,asArrayList(protectedPaths));
    }

    public static void clearImageCacheWithNestedPaths(Context context,@Nullable String[]... protectedPaths) {
        clearImageCache(context,asArrayLists(protectedPaths));
    }

    public static void clearImageCache(Context context,@Nullable List<String> protectedPaths) {

        String rootPath = getBasePath(context) + File.separator;
        String cacheImage = rootPath + basePath + cacheImagePath;

        File cacheImageFile = new File(cacheImage);

        if (cacheImageFile.exists() && cacheImageFile.isDirectory()) {
            File[] files = cacheImageFile.listFiles();
            for (int i=0;files!=null&&i<files.length;i++) {
                File f = files[i];

                if (protectedPaths != null && protectedPaths.contains(f.getAbsolutePath())) {
                    // 如果在保护路径列表中，跳过删除
                    RBQLog.i("保护的图片，跳过删除：" + f.getAbsolutePath());
                    continue;
                }

                if (f.isDirectory()){
                    clearImageCache(context,protectedPaths);
                }else {
                    boolean delete = f.delete();
                    RBQLog.i("删除缓存的图片:"+f.getAbsolutePath()+(delete?"成功":"失败"));
                }
            }
        } else {
            RBQLog.i("文件不存在......");
        }
    }

    public static void clearDataCacheWithSinglePath(Context context,@Nullable String protectedPath) {
        clearDataCache(context,asArrayList(protectedPath));
    }

    public static void clearDataCacheWithMultiplePaths(Context context,@Nullable String... protectedPaths) {
        clearDataCache(context,asArrayList(protectedPaths));
    }

    public static void clearDataCacheWithNestedPaths(Context context,@Nullable String[]... protectedPaths) {
        clearDataCache(context,asArrayLists(protectedPaths));
    }

    public static void clearDataCache(Context context, @Nullable List<String>  protectedPaths){

        String rootPath = getBasePath(context) + File.separator;
        String cacheData = rootPath + basePath + cacheDataPath;

        File cacheDataFile = new File(cacheData);

        if (cacheDataFile.exists() && cacheDataFile.isDirectory()) {
            File[] files = cacheDataFile.listFiles();
            for (int i=0;files!=null&&i<files.length;i++) {
                File f = files[i];

                if (protectedPaths != null && protectedPaths.contains(f.getAbsolutePath())) {
                    // 如果在保护路径列表中，跳过删除
                    RBQLog.i("保护的图片，跳过删除：" + f.getAbsolutePath());
                    continue;
                }

                if (f.isDirectory()){
                    clearDataCache(context,protectedPaths);
                }else {
                    boolean delete = f.delete();
                    RBQLog.i("删除缓存的数据:"+f.getAbsolutePath()+(delete?"成功":"失败"));
                }
            }
        } else {
            RBQLog.i("文件不存在......");
        }
    }

    public static String getFileName(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return filePath;
        }
        int start = filePath.lastIndexOf(File.separator);
        int end = filePath.lastIndexOf(".");
        if (start != -1 && end != -1) {
            return filePath.substring(start + 1, end);
        } else {
            return null;
        }
    }

    public static boolean copySqlLiteToCacheImageFile(Context context, String sourceName, String destName) {

        String oldFilePath = sqlLiteDataFile(context);
        String newFilePath = cacheImageFile(context);
        File oldFile = new File(oldFilePath, sourceName);
        File newFile = new File(newFilePath, destName);
        try {
            if (!newFile.exists()){
                boolean create = newFile.createNewFile();
                RBQLog.i("创建拷贝文件:"+(create?"YES":"NO"));
            }
            //获得原文件流
            FileInputStream inputStream = new FileInputStream(oldFile);
            byte[] data = new byte[1024];
            //输出流
            FileOutputStream outputStream =new FileOutputStream(newFile);
            //开始处理流
            while (inputStream.read(data) != -1) {
                outputStream.write(data);
            }
            inputStream.close();
            outputStream.close();

        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    public static Uri createCacheUri(Context context){
        String fileName = create26LenStringByLetterAndNumber()+".jpg";
        String path = cacheImageFile(context);
        File mFile = new File(path, fileName);
        return Uri.fromFile(mFile);
    }

    public static String create26LenStringByLetterAndNumber(){
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            int number = random.nextInt(str.length());    //从62个字符中随机取其中一个
            sb.append(str.charAt(number));  //用取到的数当索引取字符加到length个数的字符串
        }
        return sb.toString();
    }

    public static String createName(){

        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.CHINA).format(new Date());
    }

    public static String createJpgName(){

        return new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.CHINA).format(new Date())+".jpg";
    }

    // 横向分块保存
    private static void saveByHorizontalChunks(Bitmap src, File file, int chunkSize) throws IOException {
        int width = src.getWidth();
        int height = src.getHeight();

        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos, 1024 * 16);

        // 横向分块处理
        for (int x = 0; x < width; x += chunkSize) {
            int chunkWidth = Math.min(chunkSize, width - x);
            Bitmap chunk = Bitmap.createBitmap(src, x, 0, chunkWidth, height);
            chunk.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            chunk.recycle();
        }

        bos.flush();
        fos.getFD().sync();
        bos.close();
        fos.close();
    }

    // 纵向分块保存
    private static void saveByVerticalChunks(Bitmap src, File file, int chunkSize) throws IOException {
        int width = src.getWidth();
        int height = src.getHeight();

        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos, 1024 * 16);

        // 纵向分块处理
        for (int y = 0; y < height; y += chunkSize) {
            int chunkHeight = Math.min(chunkSize, height - y);
            Bitmap chunk = Bitmap.createBitmap(src, 0, y, width, chunkHeight);
            chunk.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            chunk.recycle();
        }

        bos.flush();
        fos.getFD().sync();
        bos.close();
        fos.close();
    }

    // 网格分块保存
    private static void saveByGridChunks(Bitmap src, File file, int chunkSize) throws IOException {
        int width = src.getWidth();
        int height = src.getHeight();

        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos, 1024 * 16);

        // 网格分块处理
        for (int y = 0; y < height; y += chunkSize) {
            for (int x = 0; x < width; x += chunkSize) {
                int chunkWidth = Math.min(chunkSize, width - x);
                int chunkHeight = Math.min(chunkSize, height - y);
                Bitmap chunk = Bitmap.createBitmap(src, x, y, chunkWidth, chunkHeight);
                chunk.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                chunk.recycle();
            }
        }

        bos.flush();
        fos.getFD().sync();
        bos.close();
        fos.close();
    }

}
