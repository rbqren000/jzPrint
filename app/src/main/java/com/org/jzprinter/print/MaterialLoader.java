package com.org.jzprinter.print;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MaterialLoader {

    public static final String PAGE_FILE_PREFIX = "page_";

    /**
     * 直接使用模式：加载 page_XX.png 完整页图片
     *
     * @param pagesPath 素材 pages 目录路径
     * @param pageIndex 页码
     * @return 该页的完整 Bitmap，不存在返回 null
     */
    public Bitmap loadPage(String pagesPath, int pageIndex) {
        String fileName = PAGE_FILE_PREFIX + pageIndex + ".png";
        File file = new File(pagesPath, fileName);
        if (!file.exists()) return null;
        return BitmapFactory.decodeFile(file.getPath());
    }

    /**
     * 自定义拼接模式：加载 page_XX/ 子目录中的所有图片并纵向拼接
     *
     * @param pagesPath 素材 pages 目录路径
     * @param pageIndex 页码
     * @return 拼接后的 Bitmap，不存在返回 null
     */
    public Bitmap loadPageCustomMerge(String pagesPath, int pageIndex) {
        String subDirName = PAGE_FILE_PREFIX + pageIndex;
        File subDir = new File(pagesPath, subDirName);
        if (!subDir.exists() || !subDir.isDirectory()) return null;

        File[] imageFiles = subDir.listFiles((dir, name) ->
            name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));
        if (imageFiles == null || imageFiles.length == 0) return null;

        return ImageMerger.mergeVertically(imageFiles);
    }

    /**
     * 获取素材中所有可用页码
     *
     * @param pagesPath 素材 pages 目录路径
     * @return 页码列表（已排序），如 [85,86,87,...,108]
     */
    public List<Integer> getAvailablePages(String pagesPath) {
        File dir = new File(pagesPath);
        if (!dir.exists() || !dir.isDirectory()) return new ArrayList<>();

        List<Integer> pages = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return pages;

        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(PAGE_FILE_PREFIX) && name.endsWith(".png")) {
                try {
                    String numStr = name.substring(PAGE_FILE_PREFIX.length(),
                                                   name.length() - 4);
                    pages.add(Integer.parseInt(numStr));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        Collections.sort(pages);
        return pages;
    }

    public int getCodeCount(String pagesPath, int pageIndex) {
        File subDir = new File(pagesPath, PAGE_FILE_PREFIX + pageIndex);
        if (subDir.exists() && subDir.isDirectory()) {
            File[] images = subDir.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg"));
            if (images != null && images.length > 0) return images.length;
        }
        File singleFile = new File(pagesPath, PAGE_FILE_PREFIX + pageIndex + ".png");
        if (singleFile.exists()) return 1;
        return 0;
    }
}
