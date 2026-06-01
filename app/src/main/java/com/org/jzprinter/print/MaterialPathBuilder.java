package com.org.jzprinter.print;

import android.content.Context;

import java.io.File;

public class MaterialPathBuilder {

    private static final String MATERIALS_DIR = "materials";
    private static final String PAGES_DIR = "pages";
    private static final String INFO_FILE = "info.json";

    /**
     * 获取素材根目录
     */
    public static File getMaterialsRoot(Context context) {
        return new File(context.getFilesDir(), MATERIALS_DIR);
    }

    /**
     * 获取素材解压目录路径：materials/{schoolId}/{editionId}/{editionType}/{targetId}/pages/
     */
    public static String getPagesPath(Context context, String schoolId, String editionId,
                                       int editionType, String targetId) {
        return new File(getMaterialsRoot(context),
            schoolId + File.separator +
            editionId + File.separator +
            editionType + File.separator +
            targetId + File.separator +
            PAGES_DIR).getPath();
    }

    /**
     * 获取压缩包路径：materials/{schoolId}/{editionId}/{editionType}/{targetId}/package.zip
     */
    public static String getZipPath(Context context, String schoolId, String editionId,
                                     int editionType, String targetId) {
        return new File(getMaterialsRoot(context),
            schoolId + File.separator +
            editionId + File.separator +
            editionType + File.separator +
            targetId + File.separator +
            "package.zip").getPath();
    }

    /**
     * 获取信息文件路径：materials/{schoolId}/{editionId}/{editionType}/{targetId}/info.json
     */
    public static String getInfoPath(Context context, String schoolId, String editionId,
                                      int editionType, String targetId) {
        return new File(getMaterialsRoot(context),
            schoolId + File.separator +
            editionId + File.separator +
            editionType + File.separator +
            targetId + File.separator +
            INFO_FILE).getPath();
    }
}
