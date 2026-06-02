package com.org.jzprinter.print;

import com.org.jzprinter.database.entity.PrintProgressEntity;
import com.org.jzprinter.database.entity.PrintTaskEntity;
import com.org.jzprinter.repository.PrintProgressRepository;
import com.org.jzprinter.repository.PrintTaskRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class PrintProgressManager {
    private final PrintTaskRepository taskRepo;
    private final PrintProgressRepository progressRepo;
    private final ExecutorService dbExecutor;
    private PrintTaskEntity currentTask;
    private List<Integer> targetPages;

    public PrintProgressManager(PrintTaskRepository taskRepo, PrintProgressRepository progressRepo,
                                 ExecutorService dbExecutor) {
        this.taskRepo = taskRepo;
        this.progressRepo = progressRepo;
        this.dbExecutor = dbExecutor;
    }

    public void setTargetPages(List<Integer> targetPages) {
        this.targetPages = targetPages;
    }

    public int getPageByPuzzleIndex(int puzzleIndex) {
        if (targetPages != null && puzzleIndex >= 0 && puzzleIndex < targetPages.size()) {
            return targetPages.get(puzzleIndex);
        }
        return -1;
    }

    public void setCurrentTask(PrintTaskEntity task) {
        this.currentTask = task;
    }

    public void onSdkPrintStart(int beginIndex, int endIndex, int currentIndex) {
        if (currentTask == null) return;
        int pageIndex = getPageByPuzzleIndex(currentIndex);

        PrintProgressEntity progress = new PrintProgressEntity();
        progress.setTaskId(currentTask.getTaskId());
        progress.setPageIndex(pageIndex);
        progress.setPuzzleIndex(currentIndex);
        progress.setTotalPuzzles(endIndex + 1);
        progress.setStatus(TaskStatus.PENDING.getCode());
        progress.setTimestamp(System.currentTimeMillis());
        dbExecutor.execute(() -> progressRepo.insert(progress));
    }

    public void onSdkPrintComplete(int beginIndex, int endIndex,
                                    int currentIndex, String cartridgeId) {
        if (currentTask == null) return;
        int pageIndex = getPageByPuzzleIndex(currentIndex);

        PrintProgressEntity progress = new PrintProgressEntity();
        progress.setTaskId(currentTask.getTaskId());
        progress.setPageIndex(pageIndex);
        progress.setPuzzleIndex(currentIndex);
        progress.setTotalPuzzles(endIndex + 1);
        progress.setStatus(TaskStatus.COMPLETED.getCode());
        progress.setCartridgeId(cartridgeId);
        progress.setTimestamp(System.currentTimeMillis());
        dbExecutor.execute(() -> progressRepo.insert(progress));
    }

    /**
     * 物理打印完成，更新 printedPages
     * OnPrintListener 回调在主线程，DB 写操作必须在子线程执行
     */
    public void onPageComplete(PrintTaskEntity task, int pageIndex) {
        taskRepo.addPrintedPage(task, pageIndex);
        dbExecutor.execute(() -> taskRepo.update(task));
    }

    public void onPageError(PrintTaskEntity task, int pageIndex, String error) {
        task.setStatus(TaskStatus.INTERRUPTED.getCode());
        task.setLastError("页面 " + pageIndex + ": " + error);
        task.setUpdatedAt(System.currentTimeMillis());
        dbExecutor.execute(() -> taskRepo.update(task));
    }

    public void onDataTransferStart(float size) {}
    public void onDataTransferProgress(int percentage) {}
    public void onDataTransferComplete() {}
}
