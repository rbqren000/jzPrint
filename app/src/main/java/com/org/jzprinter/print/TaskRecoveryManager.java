package com.org.jzprinter.print;

import com.org.jzprinter.database.dao.PrintTaskDao;
import com.org.jzprinter.database.entity.PrintTaskEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRecoveryManager {
    private final PrintTaskDao taskDao;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    public TaskRecoveryManager(PrintTaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public void recoverOnStartup() {
        dbExecutor.execute(() -> {
            List<PrintTaskEntity> inProgress = taskDao.findByStatus(TaskStatus.IN_PROGRESS.getCode());
            for (PrintTaskEntity task : inProgress) {
                task.setStatus(TaskStatus.INTERRUPTED.getCode());
                task.setUpdatedAt(System.currentTimeMillis());
                task.setLastError("App异常退出，任务中断");
                taskDao.update(task);
            }
        });
    }

    public void getResumableTasks(ResumableTasksCallback callback) {
        dbExecutor.execute(() -> {
            List<PrintTaskEntity> result = new ArrayList<>();
            result.addAll(taskDao.findByStatus(TaskStatus.INTERRUPTED.getCode()));
            result.addAll(taskDao.findByStatus(TaskStatus.PAUSED.getCode()));
            result.addAll(taskDao.findByStatus(TaskStatus.PENDING.getCode()));
            result.sort((a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
            if (callback != null) {
                callback.onResult(result);
            }
        });
    }

    public void getResumableTasks(String targetId, ResumableTasksCallback callback) {
        dbExecutor.execute(() -> {
            List<PrintTaskEntity> result = taskDao.findResumableByTargetId(targetId);
            if (callback != null) {
                callback.onResult(result);
            }
        });
    }

    public interface ResumableTasksCallback {
        void onResult(List<PrintTaskEntity> tasks);
    }
}
