package com.org.jzprinter.repository;

import com.org.jzprinter.database.converter.IntegerListConverter;
import com.org.jzprinter.database.dao.PrintProgressDao;
import com.org.jzprinter.database.dao.PrintTaskDao;
import com.org.jzprinter.database.entity.PrintTaskEntity;
import com.org.jzprinter.print.TaskStatus;

import java.util.List;

public class PrintTaskRepository {
    private final PrintTaskDao taskDao;
    private final PrintProgressDao progressDao;

    public PrintTaskRepository(PrintTaskDao taskDao, PrintProgressDao progressDao) {
        this.taskDao = taskDao;
        this.progressDao = progressDao;
    }

    public long insert(PrintTaskEntity task) {
        return taskDao.insert(task);
    }

    public void update(PrintTaskEntity task) {
        task.setUpdatedAt(System.currentTimeMillis());
        taskDao.update(task);
    }

    public PrintTaskEntity getById(long taskId) {
        return taskDao.getById(taskId);
    }

    public PrintTaskEntity findUnfinished(String schoolId, String targetId) {
        return taskDao.findUnfinished(schoolId, targetId);
    }

    public PrintTaskEntity findUnfinishedByEdition(String schoolId, String targetId, String editionId) {
        return taskDao.findUnfinishedByEdition(schoolId, targetId, editionId);
    }

    public PrintTaskEntity findUnfinishedByEditionAndMode(String targetId, String editionId, int printMode) {
        return taskDao.findUnfinishedByEditionAndMode(targetId, editionId, printMode);
    }

    public List<PrintTaskEntity> findAllResumable() {
        return taskDao.findAllResumable();
    }

    public List<PrintTaskEntity> findResumableByTargetId(String targetId) {
        return taskDao.findResumableByTargetId(targetId);
    }

    public List<PrintTaskEntity> findByStatus(int status) {
        return taskDao.findByStatus(status);
    }

    public List<PrintTaskEntity> getAll() {
        return taskDao.getAll();
    }

    public void addPrintedPage(long taskId, int pageIndex) {
        PrintTaskEntity task = taskDao.getById(taskId);
        if (task == null) return;

        List<Integer> printed = IntegerListConverter.fromString(task.getPrintedPages());
        if (!printed.contains(pageIndex)) {
            printed.add(pageIndex);
            task.setPrintedPages(IntegerListConverter.fromList(printed));
        }
        task.setUpdatedAt(System.currentTimeMillis());

        List<Integer> target = IntegerListConverter.fromString(task.getTargetPages());
        if (printed.containsAll(target)) {
            task.setStatus(TaskStatus.COMPLETED.getCode());
            task.setCompletedAt(System.currentTimeMillis());
        }

        taskDao.update(task);
    }
}
