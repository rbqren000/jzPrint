package com.org.jzprinter.print;

import android.content.Context;

import com.org.jzprinter.database.entity.PrintTaskEntity;
import com.org.jzprinter.repository.PrintTaskRepository;

import java.io.File;
import java.util.List;

public class StorageManager {
    private final Context context;
    private final PrintTaskRepository taskRepo;

    public StorageManager(Context context, PrintTaskRepository taskRepo) {
        this.context = context.getApplicationContext();
        this.taskRepo = taskRepo;
    }

    /**
     * 获取素材总占用空间
     */
    public long getMaterialTotalSize() {
        File dir = MaterialPathBuilder.getMaterialsRoot(context);
        return calculateDirSize(dir);
    }

    /**
     * 清理已完成任务的压缩包
     */
    public void cleanupCompletedMaterials() {
        List<PrintTaskEntity> completed = taskRepo.findByStatus(TaskStatus.COMPLETED.getCode());
        for (PrintTaskEntity task : completed) {
            String zipPath = MaterialPathBuilder.getZipPath(context,
                task.getSchoolId(), task.getEditionId(),
                task.getEditionType(), task.getTargetId());
            new File(zipPath).delete();
        }
    }

    private long calculateDirSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File file : files) {
            if (file.isDirectory()) {
                size += calculateDirSize(file);
            } else {
                size += file.length();
            }
        }
        return size;
    }
}
