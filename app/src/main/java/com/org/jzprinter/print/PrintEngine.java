package com.org.jzprinter.print;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.mx.mxSdk.ConnectManager;
import com.mx.mxSdk.MultiRowData;
import com.mx.mxSdk.MultiRowDataFactory;
import com.mx.mxSdk.MultiRowImage;
import com.mx.mxSdk.MultiRowImageFactory;
import com.mx.mxSdk.RowLayoutDirection;
import com.org.jzprinter.database.converter.IntegerListConverter;
import com.org.jzprinter.database.entity.PrintTaskEntity;
import com.org.jzprinter.repository.PrintTaskRepository;
import com.org.jzprinter.service.PrintService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrintEngine {
    private static final String TAG = "PrintEngine";
    private static PrintEngine instance;

    /**
     * jzPrint 扩展指令：重打指定拼。
     * 参数 1 byte（puzzleIndex），发送后用户按打印机按钮即打印该拼，
     * 完成后恢复原进度。
     */
    private static final int OPCODE_REPRINT_PAGE = 0x030A;

    private final Context context;
    private final PrintTaskRepository taskRepo;
    private final MaterialLoader materialLoader;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private PrintProgressManager progressManager;

    private PrintTaskEntity currentTask;
    private final AtomicBoolean isPrinting = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private boolean oddPageOnRight = true;
    private boolean useCustomMerge = false;
    private PrintPhaseCallback phaseCallback;

    // 重打模式：sendCommand(0x030A) 发送后，下次 onPhysicalPrintStart/Complete 为本次重打
    private volatile boolean isReprintMode = false;
    private volatile int reprintTargetPuzzleIndex = -1;

    private PrintEngine(Context context, PrintTaskRepository taskRepo,
                         MaterialLoader materialLoader) {
        this.context = context.getApplicationContext();
        this.taskRepo = taskRepo;
        this.materialLoader = materialLoader;
    }

    private static volatile boolean listenersRegistered = false;

    public static PrintEngine init(Context context, PrintTaskRepository taskRepo,
                                    MaterialLoader materialLoader) {
        if (instance == null) {
            instance = new PrintEngine(context, taskRepo, materialLoader);
        }

        if (!listenersRegistered) {
            listenersRegistered = true;
            ConnectManager cm = ConnectManager.share();

            cm.registerPrintListener(new ConnectManager.OnPrintListener() {
                @Override
                public void onPrintStart(int beginIndex, int endIndex, int currentIndex) {
                    instance.onPhysicalPrintStart(beginIndex, endIndex, currentIndex);
                }

                @Override
                public void onPrintComplete(int beginIndex, int endIndex,
                                            int currentIndex, String cartridgeId) {
                    instance.onPhysicalPrintComplete(beginIndex, endIndex, currentIndex, cartridgeId);
                }
            });

            cm.registerDeviceConnectListener(
                new ConnectManager.OnDeviceConnectListener() {
                    @Override
                    public void onDeviceConnectStart(com.mx.mxSdk.Device device) {}

                    @Override
                    public void onDeviceConnectSucceed(com.mx.mxSdk.Device device) {}

                    @Override
                    public void onDeviceDisconnect(com.mx.mxSdk.Device device) {
                        instance.onDeviceDisconnected();
                    }

                    @Override
                    public void onDeviceConnectFail(com.mx.mxSdk.Device device, String error) {}
                }
            );
        }
        return instance;
    }

    public static PrintEngine getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PrintEngine 未初始化，请先调用 init()");
        }
        return instance;
    }

    public void setProgressManager(PrintProgressManager progressManager) {
        this.progressManager = progressManager;
    }

    public void setOddPageOnRight(boolean oddPageOnRight) {
        this.oddPageOnRight = oddPageOnRight;
    }

    public void setUseCustomMerge(boolean useCustomMerge) {
        this.useCustomMerge = useCustomMerge;
    }

    public void setPhaseCallback(PrintPhaseCallback callback) {
        this.phaseCallback = callback;
    }

    public PrintPhaseCallback getPhaseCallback() {
        return phaseCallback;
    }

    public boolean isPrinting() {
        return isPrinting.get();
    }

    public PrintTaskEntity getCurrentTask() {
        return currentTask;
    }

    public boolean hasLiveTaskState(long taskId) {
        return currentTask != null
            && currentTask.getTaskId() == taskId
            && (isPrinting.get() || isReprintMode);
    }

    public ExecutorService getDbExecutor() {
        return dbExecutor;
    }

    public PrintTaskRepository getTaskRepo() {
        return taskRepo;
    }

    /**
     * 开始新任务
     */
    private List<Integer> customTargetPages = null;

    public void setCustomTargetPages(List<Integer> pages) {
        this.customTargetPages = pages;
    }

    public PrintTaskEntity startNewTask(String schoolId, String editionId,
                                         String targetId, String targetName,
                                         int editionType,
                                         PrintMode printMode, String pagesPath,
                                         String businessId, String editionName) {
        PrintTaskEntity existing = taskRepo.findUnfinishedByEditionAndMode(
            targetId, editionId, printMode.getCode());
        if (existing != null) {
            throw new IllegalStateException("该目标此校本此模式已有未完成的打印任务");
        }

        List<Integer> availablePages = materialLoader.getAvailablePages(pagesPath);
        int totalPages = availablePages.size();
        List<Integer> targetPages;
        if (customTargetPages != null && !customTargetPages.isEmpty()) {
            targetPages = customTargetPages;
            customTargetPages = null;
        } else {
            targetPages = PageSelector.select(availablePages, printMode);
        }

        PrintTaskEntity task = new PrintTaskEntity();
        task.setSchoolId(schoolId);
        task.setEditionId(editionId);
        task.setEditionName(editionName != null ? editionName : editionId);
        task.setTargetId(targetId);
        task.setTargetName(targetName != null ? targetName : targetId);
        task.setEditionType(editionType);
        task.setMaterialPath(pagesPath);
        task.setTotalPages(totalPages);
        task.setPrintMode(printMode.getCode());
        task.setTargetPages(IntegerListConverter.fromList(targetPages));
        task.setPrintedPages("[]");
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setCreatedAt(System.currentTimeMillis());
        task.setUpdatedAt(System.currentTimeMillis());
        task.setBusinessId(businessId);
        long taskId = taskRepo.insert(task);
        task.setTaskId(taskId);

        return task;
    }

    /**
     * 执行打印：将所有剩余页组织成一个 MultiRowData 一次发送
     */
    public void execute(PrintTaskEntity task) {
        List<Integer> remaining = getRemainingPages(task);
        if (remaining.isEmpty()) {
            task.setStatus(TaskStatus.COMPLETED.getCode());
            task.setCompletedAt(System.currentTimeMillis());
            dbExecutor.execute(() -> taskRepo.update(task));
            return;
        }

        if (isPrinting.get()) {
            throw new IllegalStateException("当前有打印任务正在进行中");
        }

        clearReprintState();
        task.setStatus(TaskStatus.IN_PROGRESS.getCode());
        dbExecutor.execute(() -> taskRepo.update(task));
        currentTask = task;
        isPrinting.set(true);
        cancelled.set(false);

        Log.d(TAG, "[execute] taskId=" + task.getTaskId()
            + " remainingPages=" + remaining.size()
            + " isPrinting=" + isPrinting.get()
            + " cancelled=" + cancelled.get());

        if (progressManager != null) {
            progressManager.setCurrentTask(task);
            progressManager.setTargetPages(remaining);
        }

        PrintService.start(context);

        buildAndSendMultiRow(task.getMaterialPath(), remaining);
    }

    private void buildAndSendMultiRow(String pagesPath, List<Integer> remainingPages) {
        int totalPages = remainingPages.size();
        Log.d(TAG, "[buildAndSendMultiRow] START totalPages=" + totalPages
            + " cancelled=" + cancelled.get());
        if (phaseCallback != null) {
            phaseCallback.onPhaseChanged(PrintPhaseCallback.Phase.PREPARE);
            phaseCallback.onPrepareStart(totalPages);
        }

        ArrayList<com.mx.mxSdk.RowImage> allRowImages = new ArrayList<>();

        for (int i = 0; i < totalPages; i++) {
            int pageIndex = remainingPages.get(i);

            Bitmap page;
            if (useCustomMerge) {
                page = materialLoader.loadPageCustomMerge(pagesPath, pageIndex);
            } else {
                page = materialLoader.loadPage(pagesPath, pageIndex);
            }

            if (page == null) {
                String error = "页面 " + pageIndex + " 加载失败";
                currentTask.setStatus(TaskStatus.INTERRUPTED.getCode());
                currentTask.setLastError(error);
                dbExecutor.execute(() -> taskRepo.update(currentTask));
                isPrinting.set(false);
                if (phaseCallback != null) phaseCallback.onPhaseError("prepare", error);
                return;
            }

            if (phaseCallback != null) phaseCallback.onPreparePageProgress(i + 1, totalPages, pageIndex);

            PrintImagePreparer.RotationDirection rotation = PrintImagePreparer.getRotation(pageIndex, oddPageOnRight);
            PrintImagePreparer.VerticalAlignment alignment = PrintImagePreparer.getAlignment(pageIndex, oddPageOnRight);
            Bitmap prepared = PrintImagePreparer.prepare(page, rotation, alignment);
            page.recycle();

            MultiRowImage pageImage = MultiRowImageFactory.image2MultiRowImage(
                context, prepared,
                RowLayoutDirection.RowLayoutDirectionVertical, 0);
            prepared.recycle();

            allRowImages.addAll(pageImage.getRowImages());
        }

        if (phaseCallback != null) {
            phaseCallback.onPhaseChanged(PrintPhaseCallback.Phase.TRANSFER);
            phaseCallback.onPrepareComplete();
        }

        // PREPARE 阶段不可中断（同步图像处理），此处检查取消标志：
        // 若用户在 PREPARE 期间点了返回/暂停，跳过数据发送，避免在后台偷偷传输
        if (cancelled.get()) {
            Log.d(TAG, "[buildAndSendMultiRow] CANCELLED after PREPARE, skip sendToPrinter");
            isPrinting.set(false);
            return;
        }

        Log.d(TAG, "[buildAndSendMultiRow] PREPARE done, calling bitmap2MultiRowData...");
        MultiRowImage combinedImage = MultiRowImage.createInstance(
            allRowImages, null,
            RowLayoutDirection.RowLayoutDirectionVertical, false);

        MultiRowDataFactory.bitmap2MultiRowData(
            context, combinedImage,
            127, false, true, false, false, false, false,
            new MultiRowDataFactory.OnCreateMultiRowDataListener() {
                @Override
                public void onCreateMultiRowDataStart() {
                    Log.d(TAG, "[buildAndSendMultiRow] bitmap2MultiRowData START callback");
                }

                @Override
                public void onCreateMultiRowDataComplete(MultiRowData multiRowData) {
                    Log.d(TAG, "[buildAndSendMultiRow] bitmap2MultiRowData COMPLETE, calling sendToPrinter");
                    sendToPrinter(multiRowData);
                }

                @Override
                public void onCreateMultiRowDataError(int code) {
                    Log.e(TAG, "[buildAndSendMultiRow] bitmap2MultiRowData ERROR code=" + code);
                    String error = "生成打印数据失败: " + code;
                    currentTask.setStatus(TaskStatus.INTERRUPTED.getCode());
                    currentTask.setLastError(error);
                    dbExecutor.execute(() -> taskRepo.update(currentTask));
                    isPrinting.set(false);
                    if (phaseCallback != null) phaseCallback.onPhaseError("prepare", error);
                }
            });
    }

    private ConnectManager.OnDataProgressListener currentDataProgressListener;

    private void clearReprintState() {
        isReprintMode = false;
        reprintTargetPuzzleIndex = -1;
    }

    private void dispatchPhysicalPrintProgress(int pageIndex) {
        if (phaseCallback == null || currentTask == null) return;

        List<Integer> printed = IntegerListConverter.fromString(currentTask.getPrintedPages());
        List<Integer> target = IntegerListConverter.fromString(currentTask.getTargetPages());
        int done = 0;
        for (int p : printed) {
            if (target.contains(p)) done++;
        }
        phaseCallback.onPhysicalPrintPageProgress(done, target.size(), pageIndex);
    }

    private void sendToPrinter(MultiRowData data) {
        // 防御 Bug 2A：用户在 bitmap2MultiRowData 异步处理期间点了"停止发送"，
        // 此时 cancelled 已为 true，不应继续发送
        if (cancelled.get()) {
            Log.d(TAG, "[sendToPrinter] cancelled, abort");
            return;
        }

        ConnectManager cm = ConnectManager.share();
        boolean connected = Boolean.TRUE.equals(cm.isConnected());

        Log.d(TAG, "[sendToPrinter] ENTRY isConnected=" + connected
            + " isDataSending=" + cm.isDataSending()
            + " cancelled=" + cancelled.get()
            + " hasOldListener=" + (currentDataProgressListener != null));

        if (currentDataProgressListener != null) {
            Log.d(TAG, "[sendToPrinter] unregistering old dataProgressListener");
            cm.unregisterDataProgressListener(currentDataProgressListener);
        }

        currentDataProgressListener = new ConnectManager.OnDataProgressListener() {
            @Override
            public void onDataProgressStart(float size, int progress, long startTime) {
                Log.d(TAG, "[sendToPrinter] onDataProgressStart size=" + size + " progress=" + progress);
                if (progressManager != null) progressManager.onDataTransferStart(size);
                if (phaseCallback != null) phaseCallback.onDataTransferStart(size);
            }

            @Override
            public void onDataProgress(float size, int progress, long startTime, long currentTime) {
                if (progressManager != null) progressManager.onDataTransferProgress(progress);
                if (phaseCallback != null) phaseCallback.onDataTransferProgress(progress);
            }

            @Override
            public void onDataProgressFinish(float size, long startTime, long currentTime) {
                Log.d(TAG, "[sendToPrinter] onDataProgressFinish");
                if (progressManager != null) progressManager.onDataTransferComplete();
                if (phaseCallback != null) {
                    phaseCallback.onPhaseChanged(PrintPhaseCallback.Phase.PRINT);
                    phaseCallback.onDataTransferComplete();
                }
            }

            @Override
            public void onDataProgressError(String error, int code) {
                Log.w(TAG, "[sendToPrinter] onDataProgressError error=" + error
                    + " code=" + code + " cancelled=" + cancelled.get());
                // 若用户主动取消（cancelTransfer/pause/switchToNewTarget 已设置标志），
                // 忽略此错误回调，避免 INTERRUPTED 覆盖 PAUSED 状态
                // 以及避免 onPhaseError 将 UI 按钮误禁（与 onPhaseChanged(STOPPED) 竞态）
                if (cancelled.get()) {
                    Log.d(TAG, "[sendToPrinter] onDataProgressError IGNORED (user cancelled)");
                    return;
                }

                String msg = "数据发送失败: " + error;
                currentTask.setStatus(TaskStatus.INTERRUPTED.getCode());
                currentTask.setLastError(msg);
                dbExecutor.execute(() -> taskRepo.update(currentTask));
                isPrinting.set(false);
                if (phaseCallback != null) phaseCallback.onPhaseError("transfer", msg);
            }
        };

        cm.registerDataProgressListener(currentDataProgressListener);
        Log.d(TAG, "[sendToPrinter] calling setWithSendMultiRowDataPacket...");
        cm.setWithSendMultiRowDataPacket(data);
        Log.d(TAG, "[sendToPrinter] setWithSendMultiRowDataPacket returned");
    }

    /**
     * SDK OnPrintListener.onPrintComplete 回调
     */
    public void onPhysicalPrintComplete(int beginIndex, int endIndex,
                                         int currentIndex, String cartridgeId) {
        if (currentTask == null) return;
        if (progressManager == null) return;

        int pageIndex = progressManager.getPageByPuzzleIndex(currentIndex);

        // 重打模式：不推进 printedPages，但必须发一次状态刷新，让 UI 从黄态回到绿态。
        // 注意：固件在重打时，回调上来的 currentIndex 可能是主线进度指针，而非 reprintTargetPuzzleIndex。
        // 因此只要 isReprintMode 为 true，我们就无条件认为本次按键对应的打印是重打动作的完成。
        if (isReprintMode) {
            int reprintPageIndex = progressManager.getPageByPuzzleIndex(reprintTargetPuzzleIndex);
            clearReprintState();
            dispatchPhysicalPrintProgress(reprintPageIndex);
            Log.d(TAG, "[onPhysicalPrintComplete] reprint done, resumed normal progress. Actual currentIndex=" + currentIndex);
            return;
        }

        progressManager.onPageComplete(currentTask, pageIndex);
        progressManager.onSdkPrintComplete(beginIndex, endIndex, currentIndex, cartridgeId);
        dispatchPhysicalPrintProgress(pageIndex);

        if (currentIndex == endIndex) {
            isPrinting.set(false);
            if (phaseCallback != null) phaseCallback.onPhysicalPrintComplete();
            PrintService.notifyComplete();
        }
    }

    public void onPhysicalPrintStart(int beginIndex, int endIndex, int currentIndex) {
        if (currentTask == null || progressManager == null) return;

        // 重打模式：跳过进度记录
        if (isReprintMode) {
            Log.d(TAG, "[onPhysicalPrintStart] reprint mode, skip progress. Actual currentIndex=" + currentIndex);
            return;
        }

        progressManager.onSdkPrintStart(beginIndex, endIndex, currentIndex);
        if (phaseCallback != null && currentIndex == beginIndex) {
            List<Integer> target = IntegerListConverter.fromString(currentTask.getTargetPages());
            phaseCallback.onPhysicalPrintStart(target.size());
        }
    }

    public void onDeviceDisconnected() {
        if (isPrinting.get() && currentTask != null) {
            // 设置 cancelled 标志，防止潜在的 onDataProgressError 竞态覆盖 INTERRUPTED 状态
            cancelled.set(true);
            clearReprintState();
            if (currentDataProgressListener != null) {
                ConnectManager.share().unregisterDataProgressListener(currentDataProgressListener);
                currentDataProgressListener = null;
            }
            currentTask.setStatus(TaskStatus.INTERRUPTED.getCode());
            currentTask.setLastError("打印机断开连接");
            currentTask.setUpdatedAt(System.currentTimeMillis());
            dbExecutor.execute(() -> taskRepo.update(currentTask));
            isPrinting.set(false);
            ConnectManager.share().cancelSendMultiRowDataPacket();
        }
    }

    /**
     * 暂停打印（用户主动退出页面 / 返回 / 切换目标）
     * 暂停 = 取消当前发送，任务保留为 PAUSED，可从任务详情恢复。
     *
     * 与 cancelTransfer() 同理，靠 cancelled 标志防御 SDK 回调竞态，
     * 不在此处 unregisterDataProgressListener。
     */
    public void pause() {
        if (isPrinting.get() && currentTask != null) {
            cancelled.set(true);
            clearReprintState();
            ConnectManager.share().cancelSendMultiRowDataPacket();
            currentTask.setStatus(TaskStatus.PAUSED.getCode());
            currentTask.setUpdatedAt(System.currentTimeMillis());
            dbExecutor.execute(() -> taskRepo.update(currentTask));
            isPrinting.set(false);
        }
    }

    /**
     * 取消数据传输（用户主动点"停止发送"按钮）
     *
     * 设计原因：
     * - "准备数据"阶段是本地图像处理，速度极快（毫秒级），无 SDK API 可中断，打断无意义
     * - "发送数据"阶段是蓝牙/WiFi 逐包传输，耗时长，SDK 提供 cancelSendMultiRowDataPacket() 可安全中断
     * - "物理打印"阶段数据已在打印机内存中，由用户按打印机实体按钮触发，App 端只能监听进度无法取消
     *
     * 因此只有 TRANSFER 阶段需要且可以实现"停止"操作。
     *
     * 实现注意事项：
     * - 不在此处 unregisterDataProgressListener，而是靠 onDataProgressError 内部检查 cancelled 标志
     * - 原因是 unregister 是异步的，SDK 可能在注销生效前就已分派回调，导致 onPhaseError 覆盖 STOPPED
     * - cancelled.set(true) 先于 cancelSend 执行，happens-before 保证 SDK 线程能看到
     * - 旧 listener 会在下一次 sendToPrinter() 时被自动清理
     */
    public void cancelTransfer() {
        Log.d(TAG, "[cancelTransfer] ENTER isPrinting=" + isPrinting.get()
            + " hasPacketStartSending=" + ConnectManager.share().hasPacketStartSending());
        // 必须在 cancelSend 之前设置标志，保证 SDK 回调线程看到 cancelled=true
        cancelled.set(true);
        clearReprintState();
        ConnectManager.share().cancelSendMultiRowDataPacket();
        Log.d(TAG, "[cancelTransfer] after cancelSend, isDataSending="
            + ConnectManager.share().isDataSending());

        if (currentTask != null) {
            currentTask.setStatus(TaskStatus.PAUSED.getCode());
            currentTask.setUpdatedAt(System.currentTimeMillis());
            dbExecutor.execute(() -> taskRepo.update(currentTask));
        }
        isPrinting.set(false);

        Log.d(TAG, "[cancelTransfer] notifying STOPPED phase");
        // 通知 UI 进入 STOPPED 阶段，按钮变为"重新发送"
        if (phaseCallback != null) {
            phaseCallback.onPhaseChanged(PrintPhaseCallback.Phase.STOPPED);
        }
    }

    /**
     * 切换到新目标时调用，先暂停当前任务（如有）
     */
    public void switchToNewTarget() {
        if (isPrinting.get() && currentTask != null) {
            cancelled.set(true);
            clearReprintState();
            currentTask.setStatus(TaskStatus.PAUSED.getCode());
            currentTask.setUpdatedAt(System.currentTimeMillis());
            dbExecutor.execute(() -> taskRepo.update(currentTask));
            isPrinting.set(false);
            ConnectManager.share().cancelSendMultiRowDataPacket();
        }
    }

    /**
     * 指令重打指定拼（jzPrint 扩展指令 0x030A）。
     * 数据已全部发送到打印机内存后，可通过此方法重打任意已发送页。
     * 发送指令后用户按打印机按钮时打印指定拼，完成后恢复原进度。
     *
     * @param puzzleIndex 要重打的拼索引（0-based，对应 targetPages 中的下标）
     */
    public void reprintSpecifiedPage(int puzzleIndex) {
        if (currentTask == null) {
            throw new IllegalStateException("当前没有可重打的任务");
        }
        if (!Boolean.TRUE.equals(ConnectManager.share().isConnected())) {
            throw new IllegalStateException("打印机未连接，请先连接打印机");
        }

        List<Integer> target = IntegerListConverter.fromString(currentTask.getTargetPages());
        if (puzzleIndex < 0 || puzzleIndex >= target.size()) {
            throw new IllegalArgumentException("重打页索引无效");
        }

        int page = target.get(puzzleIndex);
        List<Integer> printed = IntegerListConverter.fromString(currentTask.getPrintedPages());
        if (!printed.contains(page)) {
            throw new IllegalStateException("仅支持重打已完成页");
        }

        clearReprintState();
        isReprintMode = true;
        reprintTargetPuzzleIndex = puzzleIndex;

        ConnectManager.share().sendCommand(OPCODE_REPRINT_PAGE, new byte[]{(byte) puzzleIndex});
        Log.d(TAG, "[reprintSpecifiedPage] sent opcode=0x030A puzzleIndex=" + puzzleIndex
            + " page=" + page);
    }

    /**
     * 是否有重打指令待执行（用户已发送，等待按按钮）
     */
    public boolean isReprintPending() {
        return isReprintMode;
    }

    public int getReprintTargetPuzzleIndex() {
        return reprintTargetPuzzleIndex;
    }

    public void reprintPages(PrintTaskEntity task, List<Integer> pagesToReprint) {
        if (pagesToReprint == null || pagesToReprint.isEmpty()) return;

        if (!Boolean.TRUE.equals(ConnectManager.share().isConnected())) {
            throw new IllegalStateException("打印机未连接，请先连接打印机");
        }

        fallbackResend(task, pagesToReprint);
    }

    private void fallbackResend(PrintTaskEntity task, List<Integer> pagesToReprint) {
        if (!new File(task.getMaterialPath()).exists()) {
            throw new IllegalStateException("素材文件不存在，需重新下载");
        }

        clearReprintState();
        List<Integer> printed = IntegerListConverter.fromString(task.getPrintedPages());
        printed.removeAll(pagesToReprint);
        task.setPrintedPages(IntegerListConverter.fromList(printed));
        task.setStatus(TaskStatus.IN_PROGRESS.getCode());
        task.setUpdatedAt(System.currentTimeMillis());
        dbExecutor.execute(() -> taskRepo.update(task));

        currentTask = task;
        isPrinting.set(true);
        cancelled.set(false);

        if (progressManager != null) {
            progressManager.setCurrentTask(task);
            progressManager.setTargetPages(pagesToReprint);
        }

        PrintService.start(context);
        buildAndSendMultiRow(task.getMaterialPath(), pagesToReprint);
    }

    public void reprintAll(PrintTaskEntity task) {
        List<Integer> allTarget = IntegerListConverter.fromString(task.getTargetPages());
        reprintPages(task, allTarget);
    }

    /**
     * 断点续打
     */
    public void resumeFromBreakpoint(PrintTaskEntity task) {
        task.setStatus(TaskStatus.IN_PROGRESS.getCode());
        dbExecutor.execute(() -> taskRepo.update(task));
        execute(task);
    }

    private List<Integer> getRemainingPages(PrintTaskEntity task) {
        List<Integer> target = IntegerListConverter.fromString(task.getTargetPages());
        List<Integer> printed = IntegerListConverter.fromString(task.getPrintedPages());
        List<Integer> remaining = new ArrayList<>();
        for (int page : target) {
            if (!printed.contains(page)) {
                remaining.add(page);
            }
        }
        return remaining;
    }
}
