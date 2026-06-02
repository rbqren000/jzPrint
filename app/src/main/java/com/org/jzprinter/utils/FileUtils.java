package com.org.jzprinter.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.mx.mxSdk.Utils.RBQLog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public final static String FILE_EXTENSION_SEPARATOR = ".";//文件扩展名分割器
    public final static String FILE_CHARSET_NAME = "UTF-8";//编码集


    public static boolean  isExternalStorageLegacy() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return Environment.isExternalStorageLegacy();
        }
        return true;
    }
    public static File getSaveFile(Context context) {
        File file = new File(context.getFilesDir(), "pic.jpg");
        return file;
    }
    public static File getSaveFileIDBack(Context context) {
        File file = new File(context.getFilesDir(), "picBack.jpg");
        return file;
    }
    // 已移除 getSDPath() 和 getBasePath()：使用 Environment.getExternalStorageDirectory()
    // 不符合分区存储政策，且无任何外部调用

    /**
     * read file【读取文件内容】
     *
     * @param filePath：文件路径
     * @param charsetName：The name of a supported {@link java.nio.charset.Charset </code>charset<code>}指定编码集
     * @return if file not exist, return null, else return content of file
     * @throws RuntimeException if an error occurs while operator BufferedReader
     */
    public static StringBuilder readFile(String filePath, String charsetName) {
        File file = new File(filePath);
        StringBuilder fileContent = new StringBuilder("");
        if (!file.isFile()) {
            return null;
        }

        BufferedReader reader = null;
        try {
            InputStreamReader is = new InputStreamReader(new FileInputStream(file), charsetName);//构造一个指定编码集的InputStreamReader类
            reader = new BufferedReader(is);
            String line;
            while ((line = reader.readLine()) != null) {
                //如果已读取的文本内容不为空，则每读取一行则在本行的末尾添加一个换行符
                if (!fileContent.toString().equals("")) {
                    fileContent.append("\r\n");
                }
                fileContent.append(line);
            }
            reader.close();
            return fileContent;
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException("IOException occurred. ", e);
                }
            }
        }
    }

    /**读取文件内容赋值给字符串【固定字符集为utf-8】*/
    /*
    public static String readFile(String filePath)
    {
        String str = "";
        if (StringUtils.isEmpty(filePath))
            return str;
        try
        {
            FileInputStream fileIn = new FileInputStream(filePath);
            byte[] arrayOfByte = new byte[fileIn.available()];
            fileIn.read(arrayOfByte);
            str = EncodingUtils.getString(arrayOfByte, FILE_CHARSET_NAME);
            fileIn.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return str;
    }
    */
    /**
     * read file to string list, a element of list is a line
     *
     * @param filePath
     * @param charsetName The name of a supported {@link java.nio.charset.Charset </code>charset<code>}
     * @return if file not exist, return null, else return content of file
     * @throws RuntimeException if an error occurs while operator BufferedReader
     */
    public static List<String> readFileToList(String filePath, String charsetName) {
        File file = new File(filePath);
        List<String> fileContent = new ArrayList<String>();
        if (!file.isFile()) {
            return null;
        }

        BufferedReader reader = null;
        try {
            InputStreamReader is = new InputStreamReader(new FileInputStream(file), charsetName);
            reader = new BufferedReader(is);
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.add(line);
            }
            reader.close();
            return fileContent;
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException("IOException occurred. ", e);
                }
            }
        }
    }

    /**
     * write file【写文件：字符串】
     *
     * @param filePath
     * @param content
     * @param append is append, if true, write to the end of file, else clear content of file and write into it
     * @return return false if content is transparency, true otherwise
     * @throws RuntimeException if an error occurs while operator FileWriter
     */
    public static boolean writeFile(Context context,String filePath, String content, boolean append) {
        //字符串判空
        if (StringUtils.isEmpty(content)) {
            return false;
        }

        FileWriter fileWriter = null;
        try {
            makeDirs(filePath);
            fileWriter = new FileWriter(filePath, append);
            fileWriter.write(content);
            fileWriter.close();
            updateGallery(context,filePath);//媒体库数据更新
            return true;
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    throw new RuntimeException("IOException occurred. ", e);
                }
            }
        }
    }

    /**
     * write file【写入文件：字符串集合】
     *
     * @param filePath
     * @param contentList
     * @param append is append, if true, write to the end of file, else clear content of file and write into it
     * @return return false if contentList is transparency, true otherwise
     * @throws RuntimeException if an error occurs while operator FileWriter
     */
    public static boolean writeFile(Context context,String filePath, List<String> contentList, boolean append) {
        if (ListUtils.isEmpty(contentList)) {
            return false;
        }

        FileWriter fileWriter = null;
        try {
            makeDirs(filePath);
            fileWriter = new FileWriter(filePath, append);
            int i = 0;
            for (String line : contentList) {
                if (i++ > 0) {
                    fileWriter.write("\r\n");
                }
                fileWriter.write(line);
            }
            fileWriter.close();
            updateGallery(context,filePath);//媒体库数据更新
            return true;
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    throw new RuntimeException("IOException occurred. ", e);
                }
            }
        }
    }

    /**
     * write file, the string will be written to the begin of the file【写入文件：字符串 且重新写入】
     *
     * @param filePath
     * @param content
     * @return
     */
    public static boolean writeFile(Context context,String filePath, String content) {
        return writeFile(context,filePath, content, false);
    }

    /**
     * write file, the string list will be written to the begin of the file【写入文件：字符串集合 且 重新写入】
     *
     * @param filePath
     * @param contentList
     * @return
     */
    public static boolean writeFile(Context context,String filePath, List<String> contentList) {
        return writeFile(context,filePath, contentList, false);
    }

    /**
     * write file, the bytes will be written to the begin of the file【写入文件：输入流 且 重新写入】
     *
     * @param filePath
     * @param stream
     * @return
     * @see {@link #writeFile(Context context,String, InputStream, boolean)}
     */
    public static boolean writeFile(Context context,String filePath, InputStream stream) {
        return writeFile(context,filePath, stream, false);
    }

    /**
     * write file【写入文件 输入流】
     *
     * @param filePath the file to be opened for writing.
     * @param stream the input stream
     * @param append if <code>true</code>, then bytes will be written to the end of the file rather than the beginning
     * @return return true
     * @throws RuntimeException if an error occurs while operator FileOutputStream
     */
    public static boolean writeFile(Context context,String filePath, InputStream stream, boolean append) {
        return writeFile(context,filePath != null ? new File(filePath) : null, stream, append);
    }

    /**
     * write file, the bytes will be written to the begin of the file
     *
     * @param file
     * @param stream
     * @return
     * @see {@link #writeFile(Context context,File file, InputStream stream)}
     */
    public static boolean writeFile(Context context,File file, InputStream stream) {
        return writeFile(context,file, stream, false);
    }

    /**
     * write file
     *
     * @param file the file to be opened for writing.
     * @param stream the input stream
     * @param append if <code>true</code>, then bytes will be written to the end of the file rather than the beginning
     * @return return true
     * @throws RuntimeException if an error occurs while operator FileOutputStream
     */
    public static boolean writeFile(Context context,File file, InputStream stream, boolean append) {
        OutputStream o = null;
        try {
            makeDirs(file.getAbsolutePath());
            o = new FileOutputStream(file, append);
            byte[] data = new byte[1024];
            int length = -1;
            while ((length = stream.read(data)) != -1) {
                o.write(data, 0, length);
            }
            o.flush();
            updateGallery(context,file.getAbsolutePath());//媒体库数据更新
            return true;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("FileNotFoundException occurred. ", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            if (o != null) {
                try {
                    o.close();
                    stream.close();
                } catch (IOException e) {
                    throw new RuntimeException("IOException occurred. ", e);
                }
            }
        }
    }

    /**
     * move file
     *
     * @param sourceFilePath
     * @param destFilePath
     */
    public static void moveFile(Context context,String sourceFilePath, String destFilePath) {
        if (TextUtils.isEmpty(sourceFilePath) || TextUtils.isEmpty(destFilePath)) {
            throw new RuntimeException("Both sourceFilePath and destFilePath cannot be null.");
        }
        moveFile(context,new File(sourceFilePath), new File(destFilePath));
    }

    /**
     * move file
     *
     * @param srcFile
     * @param destFile
     */
    public static void moveFile(Context context,File srcFile, File destFile) {
        boolean rename = srcFile.renameTo(destFile);
        if (!rename) {
            copyFile(context,srcFile.getAbsolutePath(), destFile.getAbsolutePath());
            deleteFile(context,srcFile.getAbsolutePath());
        }
    }

    /**
     * copy file
     *
     * @param sourceFilePath
     * @param destFilePath
     * @return
     * @throws RuntimeException if an error occurs while operator FileOutputStream
     */
    public static boolean copyFile(Context context,String sourceFilePath, String destFilePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(sourceFilePath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("FileNotFoundException occurred. ", e);
        }
        return writeFile(context,destFilePath, inputStream);
    }


    /**从raw中复制文件到sd卡中（如果存在，则先删除后添加）*/
    public static void copyFile(Context context, int resId, File targetFile)throws IOException
    {
        if (targetFile.exists())
            targetFile.delete();

        InputStream in = context.getResources().openRawResource(resId);
        BufferedInputStream bufferIn = new BufferedInputStream(in);
        FileOutputStream fileOut = new FileOutputStream(targetFile);
        BufferedOutputStream BufferOut = new BufferedOutputStream(fileOut);
        byte[] arrayOfByte = new byte[5120];
        while (true)
        {
            int i = bufferIn.read(arrayOfByte);
            if (i == -1)
                break;
            BufferOut.write(arrayOfByte, 0, i);
        }
        BufferOut.flush();

        bufferIn.close();
        BufferOut.close();
        fileOut.close();
        in.close();
    }

    /**
     * delete file
     *
     * @param filePath
     * @return
     */
    public static String getFileNameWithoutExtension(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return filePath;
        }

        int extenPosi = filePath.lastIndexOf(FILE_EXTENSION_SEPARATOR);
        int filePosi = filePath.lastIndexOf(File.separator);
        if (filePosi == -1) {
            return (extenPosi == -1 ? filePath : filePath.substring(0, extenPosi));
        }
        if (extenPosi == -1) {
            return filePath.substring(filePosi + 1);
        }
        return (filePosi < extenPosi ? filePath.substring(filePosi + 1, extenPosi) : filePath.substring(filePosi + 1));
    }

    /**
     * get file name from path, include suffix【获得文件路径中的文件目录字符串】
     *
     * <pre>
     *      getFileName(null)               =   null
     *      getFileName("")                 =   ""
     *      getFileName("   ")              =   "   "
     *      getFileName("a.mp3")            =   "a.mp3"
     *      getFileName("a.b.rmvb")         =   "a.b.rmvb"
     *      getFileName("abc")              =   "abc"
     *      getFileName("c:\\")              =   ""
     *      getFileName("c:\\a")             =   "a"
     *      getFileName("c:\\a.b")           =   "a.b"
     *      getFileName("c:a.txt\\a")        =   "a"
     *      getFileName("/home/admin")      =   "admin"
     *      getFileName("/home/admin/a.txt/b.mp3")  =   "b.mp3"
     * </pre>
     *
     * @param filePath
     * @return file name from path, include suffix
     */
    public static String getFileName(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return filePath;
        }

        int filePos = filePath.lastIndexOf(File.separator);
        return (filePos == -1) ? filePath : filePath.substring(filePos + 1);
    }

    /**
     * get folder name from path【获得文件路径中的父文件夹目录字符串】
     *
     * <pre>
     *      getFolderName(null)               =   null
     *      getFolderName("")                 =   ""
     *      getFolderName("   ")              =   ""
     *      getFolderName("a.mp3")            =   ""
     *      getFolderName("a.b.rmvb")         =   ""
     *      getFolderName("abc")              =   ""
     *      getFolderName("c:\\")              =   "c:"
     *      getFolderName("c:\\a")             =   "c:"
     *      getFolderName("c:\\a.b")           =   "c:"
     *      getFolderName("c:a.txt\\a")        =   "c:a.txt"
     *      getFolderName("c:a\\b\\c\\d.txt")    =   "c:a\\b\\c"
     *      getFolderName("/home/admin")      =   "/home"
     *      getFolderName("/home/admin/a.txt/b.mp3")  =   "/home/admin/a.txt"
     * </pre>
     *
     * @param filePath
     * @return
     */
    public static String getFolderName(String filePath) {

        if (StringUtils.isEmpty(filePath)) {
            return filePath;
        }

        int filePosi = filePath.lastIndexOf(File.separator);
        return (filePosi == -1) ? "" : filePath.substring(0, filePosi);
    }

    /**
     * get suffix of file from path
     *
     * <pre>
     *      getFileExtension(null)               =   ""
     *      getFileExtension("")                 =   ""
     *      getFileExtension("   ")              =   "   "
     *      getFileExtension("a.mp3")            =   "mp3"
     *      getFileExtension("a.b.rmvb")         =   "rmvb"
     *      getFileExtension("abc")              =   ""
     *      getFileExtension("c:\\")              =   ""
     *      getFileExtension("c:\\a")             =   ""
     *      getFileExtension("c:\\a.b")           =   "b"
     *      getFileExtension("c:a.txt\\a")        =   ""
     *      getFileExtension("/home/admin")      =   ""
     *      getFileExtension("/home/admin/a.txt/b")  =   ""
     *      getFileExtension("/home/admin/a.txt/b.mp3")  =   "mp3"
     * </pre>
     *
     * @param filePath
     * @return
     */
    public static String getFileExtension(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return filePath;
        }

        int extenPosi = filePath.lastIndexOf(FILE_EXTENSION_SEPARATOR);
        int filePosi = filePath.lastIndexOf(File.separator);
        if (extenPosi == -1) {
            return "";
        }
        return (filePosi >= extenPosi) ? "" : filePath.substring(extenPosi + 1);
    }


    /**
     * MD5加密
     *
     * @param info
     */
    public static String getMD5(String info) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(info.getBytes(StandardCharsets.UTF_8));
            byte[] encryption = md5.digest();
            StringBuilder strBuilder = new StringBuilder();
            for (byte b : encryption) {
                if (Integer.toHexString(0xff & b).length() == 1) {
                    strBuilder.append("0").append(Integer.toHexString(0xff & b));
                } else {
                    strBuilder.append(Integer.toHexString(0xff & b));
                }
            }

            return strBuilder.toString();
        } catch (Exception e) {
            return "";
        }
    }


    /**
     * Creates the directory named by the trailing filename of this file, including the complete directory path required
     * to create this directory. <br/>
     * 【创建目录】
     * <br/>
     * <ul>
     * <strong>Attentions:</strong>
     * <li>makeDirs("C:\\Users\\Trinea") can only create users folder</li>
     * <li>makeFolder("C:\\Users\\Trinea\\") can create Trinea folder</li>
     * </ul>
     *
     * @param filePath
     * @return true if the necessary directories have been created or the target directory already exists, false one of
     *         the directories can not be created.
     *         <ul>
     *         <li>if {@link FileUtils#getFolderName(String)} return null, return false</li>
     *         <li>if target directory already exists, return true</li>
     *         <li>return {@link File# makeFolder}</li>
     *         </ul>
     */
    public static boolean makeDirs(String filePath) {
        String folderName = getFolderName(filePath);
        if (StringUtils.isEmpty(folderName)) {
            return false;
        }

        File folder = new File(folderName);
        return (folder.exists() && folder.isDirectory()) || folder.mkdirs();
    }

    /**
     * 【创建父目录文件夹】
     * @param filePath
     * @return
     * @see #makeDirs(String)
     */
    public static boolean makeFolders(String filePath) {
        return makeDirs(filePath);
    }
    
    /**
     * Indicates if this file represents a file on the underlying file system.
     *
     * @param filePath
     * @return
     */
    public static boolean isFileExist(Context context,String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return false;
        }

        File file = new File(filePath);
        boolean isExist = (file.exists() && file.isFile());
        if(! isExist){
            updateGallery(context,filePath);//如果不存在，说明手动删除了，那么就需要更新媒体库
        }
        return isExist;
    }

    /**
     * Indicates if this file represents a directory on the underlying file system.
     *
     * @param directoryPath
     * @return
     */
    public static boolean isFolderExist(String directoryPath) {
        if (StringUtils.isBlank(directoryPath)) {
            return false;
        }

        File dire = new File(directoryPath);
        return (dire.exists() && dire.isDirectory());
    }

    /**删除所有文件*/
    public static void deleteAllFiles(Context context,File root)
    {
        File[] arrayOfFile = root.listFiles();
        if (arrayOfFile != null)
        {
            for(int j = 0;j < arrayOfFile.length;j++){
                File file = arrayOfFile[j];
                if (file.isDirectory())
                {
                    deleteAllFiles(context,file);
                    file.delete();
                }else if(file.exists()){
                    deleteAllFiles(context,file);
                    file.delete();
                    updateGallery(context,file.getAbsolutePath());//媒体库数据更新
                }
            }
        }
    }

    /**删除单个文件*/
    private static void deleteFile(Context context,File file)
    {
        if(file != null) {
            file.delete();
            updateGallery(context,file.getAbsolutePath());//媒体库数据更新
        }
    }
    /**删除单个文件*/
    public static void deleteFile(Context context,String filePath)
    {
        if(!StringUtils.isEmpty(filePath)) {
            deleteFile(context,new File(filePath));
        }
    }


    /**
     * get file size
     * <ul>
     * <li>if path is null or transparency, return -1</li>
     * <li>if path exist and it is a file, return file size, else return -1</li>
     * <ul>
     *
     * @param path
     * @return returns the length of this file in bytes. returns -1 if the file does not exist.
     */
    public static long getFileSize(String path) {
        if (StringUtils.isBlank(path)) {
            return -1;
        }

        File file = new File(path);
        return (file.exists() && file.isFile() ? file.length() : -1);
    }

    // 已移除 getAppFilePath()：使用 Environment.getExternalStorageDirectory()
    // 不符合分区存储政策，且无任何外部调用

    /**获取res目录中文件的内容*/
    public static String getContentFromRes(Context context, int id) throws IOException
    {
        BufferedReader bufferReader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(id)));
        StringBuilder stringBuffer = new StringBuilder();
        while (true)
        {
            String str = bufferReader.readLine();
            if (str == null)
                break;
            stringBuffer.append(str).append("\n");
        }
        return stringBuffer.toString();
    }

    //高效获取指定类型文件
    //https://blog.csdn.net/Programming2012/article/details/50060099
    public static List<String> getSpecificTypeOfFile(Context context,String[] extension)
    {
        List<String> filePathsList = new ArrayList<String>();
        //从外存中获取
        Uri fileUri = MediaStore.Files.getContentUri("external");
        //筛选列，这里只筛选了：文件路径和不含后缀的文件名、后缀名
        String[] projection=new String[]{
                MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.TITLE, MediaStore.Files.FileColumns.MIME_TYPE
        };
        //构造筛选语句
        StringBuilder selection= new StringBuilder();
        for(int i=0; i<extension.length; i++)
        {
            if(i!=0)
            {
                selection.append(" OR ");
            }
            selection.append(MediaStore.Files.FileColumns.DATA).append(" LIKE '%").append(extension[i]).append("'");
        }
        //按时间递增顺序对结果进行排序;待会从后往前移动游标就可实现时间递减
        String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED;
        //获取内容解析器对象
        ContentResolver resolver = context.getApplicationContext().getContentResolver();
        //获取游标
        Cursor cursor = resolver.query(fileUri, projection, selection.toString(), null, sortOrder);
        if(cursor==null)
            return filePathsList;
        //游标从最后开始往前递减，以此实现时间递减顺序（最近访问的文件，优先显示）
        if(cursor.moveToLast())
        {
            do{
                //输出文件的完整路径
                String filePath = cursor.getString(0);
                filePathsList.add(filePath);
                Log.d("filePath=", filePath);
            }while(cursor.moveToPrevious());
        }
        cursor.close();

        return filePathsList;
    }

    //媒体库数据更新【添加文件、删除文件的时候需要执行】
    //filePath：是我们的文件全名，包括后缀哦
    //https://blog.csdn.net/trent1985/article/details/23907093
    private static void updateGallery(Context context,String filePath)
    {
        MediaScannerConnection.scanFile(context.getApplicationContext(),
                new String[] { filePath }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    /**
     * 获取文件名称，不带后缀
     * @param filePath
     * @return
     */
    public static String getFileNameNoSuffix(String filePath) {

        if (StringUtils.isEmpty(filePath)) {
            return filePath;
        }

        int filePos = filePath.lastIndexOf(File.separator);
        int dotPos = filePath.lastIndexOf(".");

        if (filePos!=-1&&dotPos!=-1){

            return filePath.substring(filePos + 1,dotPos);
        }

        if (filePos==-1&&dotPos!=-1){

            return filePath.substring(0,dotPos);
        }

        if (filePos != -1){

            return filePath.substring(filePos);
        }

        return filePath;
    }

    /**
     * 获取文件后缀名
     * @param filePath
     * @return
     */
    public static String getSuffix(String filePath) {

        if (StringUtils.isEmpty(filePath)) {
            return "";
        }

        int extendPos = filePath.lastIndexOf(".");
//        int filePos = filePath.lastIndexOf(File.separator);

        if (extendPos==-1){

            return "";
        }
        return filePath.substring(extendPos+1);
    }

    /**
     * 删除单个文件
     * @param   filePath    被删除文件的文件名
     * @return 文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * 删除文件夹以及目录下的文件
     * @param   filePath 被删除目录的文件路径
     * @return  目录删除成功返回true，否则返回false
     */
    public static boolean deleteDirectory(String filePath) {
        boolean flag = false;
        //如果filePath不以文件分隔符结尾，自动添加文件分隔符
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        File dirFile = new File(filePath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        flag = true;
        File[] files = dirFile.listFiles();
        //遍历删除文件夹下的所有文件(包括子目录)
        for (int i = 0; files!=null&&i < files.length; i++) {
            if (files[i].isFile()) {
                //删除子文件
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) break;
            } else {
                //删除子目录
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) break;
            }
        }
        if (!flag) return false;
        //删除当前空目录
        return dirFile.delete();
    }

    /**
     *  根据路径删除指定的目录或文件，无论存在与否
     *@param filePath  要删除的目录或文件
     *@return 删除成功返回 true，否则返回 false。
     */
    public static boolean DeleteFolder(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        } else {
            if (file.isFile()) {
                // 为文件时调用删除文件方法
                return deleteFile(filePath);
            } else {
                // 为目录时调用删除目录方法
                return deleteDirectory(filePath);
            }
        }
    }

    /**
     * isFile()：判断是否文件，也许可能是文件或者目录
     * exists()：判断是否存在，可能不存在
     * isDirectory()是检查一个对象是否是文件夹。返回值是boolean类型的。如果是则返回true，否则返回false。
     * 调用方法为：对象.isDirectory()  无需指定参数。
     * @param file
     * @return
     */
    public static List<String> files(File file) {
        List<String> filesList = new ArrayList<String>();
        if (file != null && file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i=0;files!=null&&i<files.length;i++) {
                File f = files[i];
                if (f.isDirectory()){
                    files(f);
                }else {
                    String str = "文件名称:" + f.getName() + " 路径:" + f.getPath();
                    RBQLog.i(str);
                    filesList.add(f.getPath());
                }
            }
        } else {
            RBQLog.i("文件不存在......");
        }
        return filesList;
    }
}