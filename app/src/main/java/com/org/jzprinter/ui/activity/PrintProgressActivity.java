package com.org.jzprinter.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.org.jzprinter.R;
import com.org.jzprinter.database.AppDatabase;
import com.org.jzprinter.database.converter.IntegerListConverter;
import com.org.jzprinter.database.entity.PrintTaskEntity;
import com.org.jzprinter.databinding.ActivityPrintProgressBinding;
import com.org.jzprinter.print.PrintEngine;
import com.org.jzprinter.print.PrintMode;
import com.org.jzprinter.print.PrintConfig;
import com.org.jzprinter.print.PrintPhaseCallback;
import com.org.jzprinter.print.TaskStatus;
import com.org.jzprinter.ui.adapter.PageChipAdapter;
import com.org.jzprinter.ui.adapter.PageChipAdapter.ChipItem;
import com.org.jzprinter.ui.adapter.PageChipAdapter.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.activity.OnBackPressedCallback;

public class PrintProgressActivity extends BaseActivity {

    private static final String TAG = "PrintProgress";

    private static final String EXTRA_SCHOOL_ID = "schoolId";
    private static final String EXTRA_EDITION_ID = "editionId";
    private static final String EXTRA_TARGET_ID = "targetId";
    private static final String EXTRA_TARGET_NAME = "targetName";
    private static final String EXTRA_EDITION_TYPE = "editionType";
    private static final String EXTRA_PRINT_MODE = "printMode";
    private static final String EXTRA_PAGES_PATH = "pagesPath";
    private static final String EXTRA_TASK_ID = "taskId";
    private static final String EXTRA_IS_REPRINT = "isReprint";
    private static final String EXTRA_IS_RESUME = "isResume";
    private static final String EXTRA_REPRINT_PAGES = "reprintPages";
    private static final String EXTRA_BUSINESS_ID = "businessId";
    private static final String EXTRA_EDITION_NAME = "editionName";

    private static final long POLL_INTERVAL_MS = 500L;

    private ActivityPrintProgressBinding binding;
    private PageChipAdapter pageChipAdapter;
    private long currentTaskId = -1;
    private PrintTaskEntity currentTask;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private boolean isPollingActive = false;
    private boolean isPollRequestInFlight = false;
    private final Runnable pollRunnable = this::pollProgressSnapshot;

    private int prepareTotal = 0;
    private int printTotal = 0;

    public static Intent newIntent(Context context, String schoolId, String editionId,
                                   String targetId, String targetName, int editionType,
                                   int printMode, String pagesPath, long taskId,
                                   String businessId, String editionName) {
        Intent intent = new Intent(context, PrintProgressActivity.class);
        intent.putExtra(EXTRA_SCHOOL_ID, schoolId);
        intent.putExtra(EXTRA_EDITION_ID, editionId);
        intent.putExtra(EXTRA_TARGET_ID, targetId);
        intent.putExtra(EXTRA_TARGET_NAME, targetName);
        intent.putExtra(EXTRA_EDITION_TYPE, editionType);
        intent.putExtra(EXTRA_PRINT_MODE, printMode);
        intent.putExtra(EXTRA_PAGES_PATH, pagesPath);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        intent.putExtra(EXTRA_BUSINESS_ID, businessId != null ? businessId : "");
        intent.putExtra(EXTRA_EDITION_NAME, editionName != null ? editionName : "");
        return intent;
    }

    public static Intent newReprintIntent(Context context, long taskId) {
        Intent intent = new Intent(context, PrintProgressActivity.class);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        intent.putExtra(EXTRA_IS_REPRINT, true);
        return intent;
    }

    public static Intent newResumeIntent(Context context, long taskId) {
        Intent intent = new Intent(context, PrintProgressActivity.class);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        intent.putExtra(EXTRA_IS_RESUME, true);
        return intent;
    }

    public static Intent newReprintPagesIntent(Context context, long taskId,
                                                 ArrayList<Integer> pages) {
        Intent intent = new Intent(context, PrintProgressActivity.class);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        intent.putExtra(EXTRA_IS_REPRINT, true);
        intent.putIntegerArrayListExtra(EXTRA_REPRINT_PAGES, pages);
        return intent;
    }

    private PrintPhaseCallback.Phase currentPhase = null;

    private final PrintPhaseCallback phaseCallback = new PrintPhaseCallback() {
        @Override
        public void onPhaseChanged(PrintPhaseCallback.Phase phase) {
            currentPhase = phase;
            rbqRunOnUiThread(PrintProgressActivity.this::updateRestartButton);
        }

        @Override
        public void onPrepareStart(int totalPages) {
            prepareTotal = totalPages;
            rbqRunOnUiThread(() -> {
                binding.pbPrepare.setMax(totalPages);
                binding.pbPrepare.setProgress(0);
                binding.tvPrepareStatus.setText(getString(R.string.progress_prepare_counter, 0, totalPages));
                binding.tvStatus.setText(R.string.progress_preparing);
                setPhaseActive(0);
            });
        }

        @Override
        public void onPreparePageProgress(int currentPage, int totalPages, int pageIndex) {
            rbqRunOnUiThread(() -> {
                binding.pbPrepare.setProgress(currentPage);
                binding.tvPrepareStatus.setText(getString(R.string.progress_prepare_page_detail, currentPage, totalPages, pageIndex));
            });
        }

        @Override
        public void onPrepareComplete() {
            rbqRunOnUiThread(() -> {
                binding.pbPrepare.setProgress(prepareTotal);
                binding.tvPrepareStatus.setText(R.string.progress_prepare_done);
                binding.tvStatus.setText(getString(R.string.progress_sending_pages, prepareTotal));
                setPhaseActive(1);
            });
        }

        @Override
        public void onDataTransferStart(float totalSize) {
            String sizeStr = formatSize(totalSize);
            rbqRunOnUiThread(() -> {
                binding.pbTransfer.setProgress(0);
                binding.tvTransferStatus.setText(getString(R.string.progress_transfer_with_size, sizeStr));
            });
        }

        @Override
        public void onDataTransferProgress(int percentage) {
            rbqRunOnUiThread(() -> {
                binding.pbTransfer.setProgress(percentage);
                binding.tvTransferStatus.setText(getString(R.string.progress_transfer_pct, percentage));
            });
        }

        @Override
        public void onDataTransferComplete() {
            rbqRunOnUiThread(() -> {
                binding.pbTransfer.setProgress(100);
                binding.tvTransferStatus.setText(R.string.progress_transfer_done);
                binding.tvStatus.setText(R.string.progress_wait_user);
                setPhaseActive(2);
            });
        }

        @Override
        public void onPhysicalPrintStart(int totalPages) {
            printTotal = totalPages;
            rbqRunOnUiThread(() -> {
                binding.pbPrint.setMax(totalPages);
                binding.pbPrint.setProgress(0);
                binding.tvPrintStatus.setText(getString(R.string.progress_prepare_counter, 0, totalPages));
            });
        }

        @Override
        public void onPhysicalPrintPageProgress(int printedPages, int totalPages, int pageIndex) {
            rbqRunOnUiThread(() -> {
                binding.pbPrint.setProgress(printedPages);
                binding.tvPrintStatus.setText(getString(R.string.progress_print_counter, printedPages, totalPages));
                refreshPageChipsFromMemory();
            });
        }

        @Override
        public void onPhysicalPrintComplete() {
            rbqRunOnUiThread(() -> {
                binding.pbPrint.setProgress(printTotal);
                binding.tvPrintStatus.setText(getString(R.string.progress_print_counter_done, printTotal, printTotal));
                binding.tvStatus.setText(R.string.progress_print_done);
                refreshPageChipsFromMemory();
                binding.btnRestart.setVisibility(View.GONE);
                binding.btnPauseOrCancel.setVisibility(View.GONE);
                binding.btnViewDetail.setVisibility(View.VISIBLE);
            });
        }

        @Override
        public void onPhaseError(String phase, String error) {
            rbqRunOnUiThread(() -> {
                binding.tvStatus.setText(getString(R.string.progress_error_fmt, error));
                binding.btnRestart.setEnabled(false);
                binding.btnViewDetail.setVisibility(View.VISIBLE);
            });
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrintProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupStatusBarWithCustomColorResId(R.color.primary_blue);

        // 打印过程中保持屏幕常亮，避免发送数据或等待按键时手机黑屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 显示当前打印设置摘要
        binding.tvSettingsSummary.setText(PrintConfig.getSettingsSummary(this));

        // 初始化页码 Chip RecyclerView
        binding.rvPageChips.setLayoutManager(
            new GridLayoutManager(this, 7, RecyclerView.VERTICAL, false));
        pageChipAdapter = new PageChipAdapter(binding.rvPageChips);
        binding.rvPageChips.setAdapter(pageChipAdapter);

        binding.commonAppBar.titleTextView.setText(R.string.progress_title);
        binding.commonAppBar.leftMenuLayout.setOnClickListener(v -> {
            if (PrintEngine.getInstance().isPrinting()) {
                new android.app.AlertDialog.Builder(this)
                    .setTitle("打印正在进行")
                    .setMessage("退出后任务将保留，可从任务详情重新开始。")
                    .setPositiveButton("退出", (d, w) -> {
                        PrintEngine.getInstance().pause();
                        finish();
                    })
                    .setNegativeButton("留在此页", null)
                    .show();
            } else {
                finish();
            }
        });

        /*
         * btnRestart 按钮点击逻辑：
         *
         * 本打印机为手持移动打印机，打印流程分三个阶段：
         *   1. 准备数据（本地图像处理，毫秒级，无可中断 API）
         *   2. 发送数据（蓝牙/WiFi 逐包传输，耗时较长，SDK 支持 cancelSend）
         *   3. 物理打印（数据已在打印机内存，用户按打印机实体按钮触发，App 仅监听进度）
         *
         * 对应按钮行为如下：
         *   - PREPARE 阶段："重新发送"不可点 — 数据尚未发送过，重发无意义
         *   - TRANSFER 阶段："停止发送"可点 — 唯一可安全中断的阶段
         *   - STOPPED 阶段："重新发送"可点 — 用户已停止发送，可重走 准备+发送 全流程
         *   - PRINT 阶段："重新发送"不可点 — 数据已在打印机，重发无意义
         *
         * 阶段切换由 PrintEngine 通过 PrintPhaseCallback.onPhaseChanged() 驱动，
         * updateRestartButton() 根据当前 phase 统一控制按钮文案与可点状态。
         */
        binding.btnRestart.setOnClickListener(v -> {
            Log.d(TAG, "[btnRestart] clicked, currentPhase=" + currentPhase);
            if (currentPhase == PrintPhaseCallback.Phase.STOPPED) {
                Log.d(TAG, "[btnRestart] STOPPED → calling restartPrint()");
                restartPrint();
            } else if (currentPhase == PrintPhaseCallback.Phase.TRANSFER) {
                Log.d(TAG, "[btnRestart] TRANSFER → calling cancelTransfer()");
                PrintEngine.getInstance().cancelTransfer();
            }
        });

        binding.btnPauseOrCancel.setOnClickListener(v -> {
            if (currentPhase == PrintPhaseCallback.Phase.PRINT) {
                // 物理打印阶段，提示暂存
                new android.app.AlertDialog.Builder(this)
                    .setTitle("暂存并退出")
                    .setMessage("数据已发送完毕，设备可在进行打印。\n\n退出将暂存此任务，后续可从详情页继续打印或重打。")
                    .setPositiveButton("暂存退出", (d, w) -> {
                        PrintEngine.getInstance().pause();
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            } else {
                // PREPARE / TRANSFER 阶段，彻底取消
                new android.app.AlertDialog.Builder(this)
                    .setTitle("取消打印")
                    .setMessage("确定取消本次打印？数据发送将终止，取消后需重新开始。")
                    .setPositiveButton("取消打印", (d, w) -> {
                        PrintEngine engine = PrintEngine.getInstance();
                        if (engine.isPrinting()) {
                            engine.pause();
                        }
                        if (currentTask != null) {
                            currentTask.setStatus(TaskStatus.CANCELLED.getCode());
                            currentTask.setUpdatedAt(System.currentTimeMillis());
                            engine.getDbExecutor().execute(() ->
                                AppDatabase.getInstance(PrintProgressActivity.this)
                                    .printTaskRepository().update(currentTask));
                        }
                        finish();
                    })
                    .setNegativeButton("继续打印", null)
                    .show();
            }
        });

        binding.btnViewDetail.setOnClickListener(v -> {
            if (currentTaskId > 0) {
                startActivity(TaskDetailActivity.newIntent(this, currentTaskId));
                finish();
            }
        });

        // 重打指定页按钮：仅 PRINT 阶段可用，弹出页面选择弹窗
        binding.btnReprintSpecifiedPage.setOnClickListener(v -> showReprintPageSelector());

        // 返回键处理：打印中弹出确认，否则直接退出
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.btnPauseOrCancel.getVisibility() == View.VISIBLE) {
                    binding.btnPauseOrCancel.performClick();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        PrintEngine.getInstance().setPhaseCallback(phaseCallback);
        startOrResumePrint();
    }

    private void startOrResumePrint() {
        boolean isReprint = getIntent().getBooleanExtra(EXTRA_IS_REPRINT, false);
        boolean isResume = getIntent().getBooleanExtra(EXTRA_IS_RESUME, false);
        long existingTaskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);

        // 重打/续打路径不会经过 PrintModeSelectActivity 设置引擎参数，
        // 在此统一从 PrintConfig 读取当前设置，确保设置页与进度页行为一致
        PrintEngine engine = PrintEngine.getInstance();
        engine.setOddPageOnRight(PrintConfig.isOddPageOnRight(this));
        engine.setLeftBottomToTop(PrintConfig.isLeftBottomToTop(this));
        engine.setRightBottomToTop(PrintConfig.isRightBottomToTop(this));

        if (isReprint && existingTaskId > 0) {
            currentTaskId = existingTaskId;
            List<Integer> reprintPages = getIntent().getIntegerArrayListExtra(EXTRA_REPRINT_PAGES);
            binding.tvStatus.setText(R.string.progress_preparing_reprint);
            engine.getDbExecutor().execute(() -> {
                try {
                    PrintTaskEntity task = AppDatabase.getInstance(PrintProgressActivity.this)
                        .printTaskRepository().getById(existingTaskId);
                    if (task == null) {
                        rbqRunOnUiThread(() -> {
                            binding.tvStatus.setText(R.string.progress_task_not_found);
                            binding.btnRestart.setEnabled(false);
                        });
                        return;
                    }
                    currentTask = task;
                    if (reprintPages != null && !reprintPages.isEmpty()) {
                        engine.reprintPages(task, reprintPages);
                    } else {
                        engine.reprintAll(task);
                    }
                    rbqRunOnUiThread(this::startProgressPolling);
                } catch (Exception e) {
                    rbqRunOnUiThread(() -> {
                        binding.tvStatus.setText(getString(R.string.progress_retry_failed, e.getMessage()));
                        binding.btnRestart.setEnabled(false);
                    });
                }
            });
            return;
        }

        if (isResume && existingTaskId > 0) {
            currentTaskId = existingTaskId;
            binding.tvStatus.setText(R.string.progress_preparing_resume);
            engine.getDbExecutor().execute(() -> {
                try {
                    PrintTaskEntity task = AppDatabase.getInstance(PrintProgressActivity.this)
                        .printTaskRepository().getById(existingTaskId);
                    if (task == null) {
                        rbqRunOnUiThread(() -> {
                            binding.tvStatus.setText(R.string.progress_task_not_found);
                            binding.btnRestart.setEnabled(false);
                        });
                        return;
                    }
                    currentTask = task;
                    engine.resumeFromBreakpoint(task);
                    rbqRunOnUiThread(this::startProgressPolling);
                } catch (Exception e) {
                    rbqRunOnUiThread(() -> {
                        binding.tvStatus.setText(getString(R.string.progress_resume_failed, e.getMessage()));
                        binding.btnRestart.setEnabled(false);
                    });
                }
            });
            return;
        }

        if (existingTaskId > 0) {
            currentTaskId = existingTaskId;
            binding.tvStatus.setText(R.string.progress_viewing);
            binding.pbPrepare.setProgress(binding.pbPrepare.getMax());
            binding.tvPrepareStatus.setText(R.string.progress_completed);
            binding.pbTransfer.setProgress(binding.pbTransfer.getMax());
            binding.tvTransferStatus.setText(R.string.progress_completed);
            setPhaseActive(2);
            startProgressPolling();
            return;
        }

        String schoolId = getIntent().getStringExtra(EXTRA_SCHOOL_ID);
        String editionId = getIntent().getStringExtra(EXTRA_EDITION_ID);
        String targetId = getIntent().getStringExtra(EXTRA_TARGET_ID);
        String targetName = getIntent().getStringExtra(EXTRA_TARGET_NAME);
        int editionType = getIntent().getIntExtra(EXTRA_EDITION_TYPE, 1);
        int printModeCode = getIntent().getIntExtra(EXTRA_PRINT_MODE, 1);
        String pagesPath = getIntent().getStringExtra(EXTRA_PAGES_PATH);
        String businessId = getIntent().getStringExtra(EXTRA_BUSINESS_ID);
        String editionName = getIntent().getStringExtra(EXTRA_EDITION_NAME);
        PrintMode printMode = PrintMode.fromCode(printModeCode);

        binding.tvStatus.setText(R.string.progress_generic_preparing);
        engine.getDbExecutor().execute(() -> {
            try {
                PrintTaskEntity task =
                    engine.startNewTask(schoolId, editionId, targetId, targetName,
                        editionType, printMode, pagesPath, businessId, editionName);
                currentTaskId = task.getTaskId();
                currentTask = task;
                engine.execute(task);
                rbqRunOnUiThread(this::startProgressPolling);
            } catch (Exception e) {
                rbqRunOnUiThread(() -> {
                    binding.tvStatus.setText(getString(R.string.progress_execute_failed, e.getMessage()));
                    binding.btnRestart.setEnabled(false);
                });
            }
        });
    }

    private void setPhaseActive(int phase) {
        int activeColor = ContextCompat.getColor(this, R.color.accent_blue);
        int doneColor = ContextCompat.getColor(this, R.color.status_success);
        int inactiveColor = ContextCompat.getColor(this, R.color.grey300);

        binding.pbPrepare.getProgressDrawable().setTint(
            phase > 0 ? doneColor : activeColor);
        binding.pbTransfer.getProgressDrawable().setTint(
            phase > 1 ? doneColor : (phase == 1 ? activeColor : inactiveColor));
        binding.pbPrint.getProgressDrawable().setTint(
            phase == 2 ? activeColor : inactiveColor);
    }

    /**
     * 根据当前打印阶段控制 btnRestart 的文案与可点状态。
     *
     * 阶段设计原理（结合打印机硬件特性）：
     *
     * PREPARE（准备数据）：
     *   本地图像加载、旋转、合并等处理，速度极快（毫秒级），
     *   无 SDK API 可中断，打断无实际意义 → 按钮不可点。
     *
     * TRANSFER（发送数据）：
     *   所有图片已打包为 MultiRowData，通过蓝牙/WiFi 逐包发送给打印机，
     *   耗时较长，SDK 提供 cancelSendMultiRowDataPacket() 可安全中断 →
     *   按钮可点，文案"停止发送"。
     *
     * STOPPED（发送已停止）：
     *   用户在 TRANSFER 阶段点击"停止发送"后进入此阶段，
     *   数据未完全发送到打印机，可重新开始 →
     *   按钮可点，文案"重新发送"，点击后重走 PREPARE + TRANSFER。
     *
     * PRINT（物理打印）：
     *   数据已完整传输到打印机内存，用户按打印机实体按钮触发打印，
     *   App 端通过 SDK OnPrintListener 监听每页打印完成，
     *   无法远程取消，重新发送也无意义 → 按钮不可点。
     */
    private void updateRestartButton() {
        Log.d(TAG, "[updateRestartButton] currentPhase=" + currentPhase);
        if (currentPhase == null) return;
        switch (currentPhase) {
            case PREPARE:
                binding.btnRestart.setText(R.string.progress_resend);
                binding.btnRestart.setEnabled(false);
                binding.btnReprintSpecifiedPage.setVisibility(View.GONE);
                binding.btnPauseOrCancel.setText(R.string.progress_cancel_print);
                break;
            case TRANSFER:
                binding.btnRestart.setText(R.string.progress_stop_send);
                binding.btnRestart.setEnabled(true);
                binding.btnReprintSpecifiedPage.setVisibility(View.GONE);
                binding.btnPauseOrCancel.setText(R.string.progress_cancel_print);
                break;
            case STOPPED:
                binding.btnRestart.setText(R.string.progress_resend);
                binding.btnRestart.setEnabled(false);
                binding.btnReprintSpecifiedPage.setVisibility(View.GONE);
                binding.btnPauseOrCancel.setText(R.string.progress_cancel_print);
                binding.tvStatus.setText(R.string.progress_send_stopped);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (currentPhase == PrintPhaseCallback.Phase.STOPPED) {
                        binding.btnRestart.setEnabled(true);
                    }
                }, 5000);
                break;
            case PRINT:
                binding.btnRestart.setText(R.string.progress_resend);
                binding.btnRestart.setEnabled(false);
                binding.btnReprintSpecifiedPage.setVisibility(View.VISIBLE);
                binding.btnPauseOrCancel.setText(R.string.progress_pause_exit);
                break;
        }
    }

    /**
     * 从 STOPPED 状态重新开始打印。
     * 重置进度条，走"断点续打"流程（PrintEngine.resumeFromBreakpoint），
     * 实际会重新执行 准备数据(PREPARE) + 发送数据(TRANSFER) 全流程。
     */
    private void restartPrint() {
        if (currentTask == null) {
            Log.w(TAG, "[restartPrint] currentTask is null, abort");
            return;
        }
        Log.d(TAG, "[restartPrint] START taskId=" + currentTaskId
            + " isPrinting=" + PrintEngine.getInstance().isPrinting());
        binding.pbPrepare.setProgress(0);
        binding.tvPrepareStatus.setText("");
        binding.pbTransfer.setProgress(0);
        binding.tvTransferStatus.setText("");
        binding.tvStatus.setText(R.string.progress_generic_preparing);
        setPhaseActive(0);

        PrintEngine engine = PrintEngine.getInstance();
        engine.getDbExecutor().execute(() -> {
            try {
                Log.d(TAG, "[restartPrint] dbExecutor: loading task from DB...");
                PrintTaskEntity task = AppDatabase.getInstance(PrintProgressActivity.this)
                    .printTaskRepository().getById(currentTaskId);
                if (task == null) {
                    Log.e(TAG, "[restartPrint] task not found in DB!");
                    rbqRunOnUiThread(() -> binding.tvStatus.setText(R.string.progress_task_not_found));
                    return;
                }
                Log.d(TAG, "[restartPrint] task loaded, status=" + task.getStatus()
                    + " targetPages=" + task.getTargetPages()
                    + " printedPages=" + task.getPrintedPages());
                currentTask = task;
                engine.resumeFromBreakpoint(task);
                Log.d(TAG, "[restartPrint] resumeFromBreakpoint returned, starting polling");
                rbqRunOnUiThread(this::startProgressPolling);
            } catch (Exception e) {
                Log.e(TAG, "[restartPrint] exception: " + e.getMessage(), e);
                rbqRunOnUiThread(() -> binding.tvStatus.setText(getString(R.string.progress_restart_failed, e.getMessage())));
            }
        });
    }

    /**
     * 弹出重打指定页选择弹窗。
     * 列出所有 targetPages，用 ✅/⬜ 标记已打印/未打印，
     * 默认选中最后打印的页，确认后发送指令重打。
     */
    private void showReprintPageSelector() {
        PrintEngine engine = PrintEngine.getInstance();
        PrintTaskEntity task = engine.getCurrentTask();
        if (task == null) return;

        // 获取本次会话实际下发给打印机的目标页面（断点续打时，这仅包含剩下的页，而不是全部 targetPages）
        List<Integer> sessionPages = engine.getCurrentSessionPages();
        List<Integer> printedPages = IntegerListConverter.fromString(task.getPrintedPages());
        if (sessionPages.isEmpty()) return;

        List<Integer> reprintablePages = new ArrayList<>();
        List<Integer> reprintablePuzzleIndexes = new ArrayList<>();
        // 只有本次实际下发给打印机（sessionPages）并且已经打印完成的页（printedPages）才能重打
        // i 就是在打印机内存中的 puzzleIndex (0-based)
        for (int i = 0; i < sessionPages.size(); i++) {
            int page = sessionPages.get(i);
            if (printedPages.contains(page)) {
                reprintablePages.add(page);
                reprintablePuzzleIndexes.add(i);
            }
        }

        if (reprintablePages.isEmpty()) {
            android.widget.Toast.makeText(this, "当前会话内没有可重打的已完成页",
                android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[reprintablePages.size()];
        for (int i = 0; i < reprintablePages.size(); i++) {
            items[i] = getString(R.string.reprint_page_item, reprintablePages.get(i));
        }

        final int[] selectedIndex = {reprintablePages.size() - 1};

        new android.app.AlertDialog.Builder(this)
            .setTitle("重打指定页")
            .setSingleChoiceItems(items, selectedIndex[0], (dialog, which) -> {
                selectedIndex[0] = which;
            })
            .setNegativeButton("取消", null)
            .setPositiveButton("确认重打", (dialog, which) -> {
                int index = selectedIndex[0];
                int puzzleIndex = reprintablePuzzleIndexes.get(index);
                int page = reprintablePages.get(index);
                try {
                    engine.reprintSpecifiedPage(puzzleIndex);
                    refreshPageChipsFromMemory();
                    android.widget.Toast.makeText(PrintProgressActivity.this,
                        getString(R.string.reprint_toast, page),
                        android.widget.Toast.LENGTH_SHORT).show();
                } catch (IllegalStateException | IllegalArgumentException e) {
                    android.widget.Toast.makeText(PrintProgressActivity.this,
                        e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    /**
     * 物理打印回调到达时，直接使用 PrintEngine 内存中的 currentTask 刷新页码 Chip。
     * `printedPages` 已在 `onPageComplete()` 中先同步写回任务对象，再异步持久化到 Room，
     * 因此这里应以内存态为准，而不是在主线程二次查询数据库。
     */
    private void refreshPageChipsFromMemory() {
        if (pageChipAdapter == null) return;

        PrintTaskEntity liveTask = PrintEngine.getInstance().getCurrentTask();
        if (liveTask == null) {
            liveTask = currentTask;
        }
        if (liveTask == null) return;
        if (currentTaskId > 0 && liveTask.getTaskId() != currentTaskId) return;

        currentTask = liveTask;
        refreshPageChips(liveTask);
        updatePrintProgress(liveTask);
    }

    /**
     * 刷新页码 Chip 视图。
     * 从 task 中读取 targetPages / printedPages，结合 PrintEngine 重打状态，
     * 为每个页码设置对应颜色：已打印=绿、即将打印=蓝、重打中=橙、待打印=灰。
     */
    private void refreshPageChips(PrintTaskEntity task) {
        if (task == null || pageChipAdapter == null) return;

        List<Integer> target = IntegerListConverter.fromString(task.getTargetPages());
        List<Integer> printed = IntegerListConverter.fromString(task.getPrintedPages());
        if (target.isEmpty()) {
            binding.rvPageChips.setVisibility(View.GONE);
            return;
        }

        List<Integer> remaining = new ArrayList<>();
        for (int p : target) {
            if (!printed.contains(p)) remaining.add(p);
        }
        int nextPage = remaining.isEmpty() ? -1 : remaining.get(0);

        PrintEngine engine = PrintEngine.getInstance();
        boolean reprintPending = engine.hasLiveTaskState(task.getTaskId()) && engine.isReprintPending();
        int reprintPuzzleIndex = reprintPending ? engine.getReprintTargetPuzzleIndex() : -1;
        
        List<Integer> sessionPages = engine.getCurrentSessionPages();
        int reprintPage = -1;
        if (reprintPuzzleIndex >= 0 && reprintPuzzleIndex < sessionPages.size()) {
            reprintPage = sessionPages.get(reprintPuzzleIndex);
        }

        List<ChipItem> chipItems = new ArrayList<>();
        for (int page : target) {
            ChipItem item = new ChipItem(page);
            if (reprintPending && page == reprintPage) {
                item.state = State.REPRINTING;
            } else if (printed.contains(page)) {
                item.state = State.PRINTED;
            } else if (page == nextPage) {
                item.state = State.NEXT;
            } else {
                item.state = State.PENDING;
            }
            chipItems.add(item);
        }

        binding.rvPageChips.setVisibility(View.VISIBLE);
        pageChipAdapter.setItems(chipItems);
    }

    private void startProgressPolling() {
        stopProgressPolling();
        if (currentTaskId < 0) return;
        isPollingActive = true;
        requestNextPoll(0L);
    }

    private void stopProgressPolling() {
        isPollingActive = false;
        isPollRequestInFlight = false;
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void requestNextPoll(long delayMs) {
        if (!isPollingActive) return;
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, delayMs);
    }

    private void pollProgressSnapshot() {
        if (!isPollingActive || currentTaskId < 0 || isFinishing() || isDestroyed()) {
            stopProgressPolling();
            return;
        }
        if (isPollRequestInFlight) {
            requestNextPoll(POLL_INTERVAL_MS);
            return;
        }

        isPollRequestInFlight = true;
        final long pollTaskId = currentTaskId;
        PrintEngine.getInstance().getDbExecutor().execute(() -> {
            PrintTaskEntity snapshot = null;
            Exception loadError = null;
            try {
                snapshot = AppDatabase.getInstance(PrintProgressActivity.this)
                    .printTaskRepository().getById(pollTaskId);
            } catch (Exception e) {
                loadError = e;
            }

            final PrintTaskEntity finalSnapshot = snapshot;
            final Exception finalLoadError = loadError;
            rbqRunOnUiThread(() -> {
                isPollRequestInFlight = false;
                if (!isPollingActive || isFinishing() || isDestroyed()) {
                    return;
                }
                if (finalLoadError != null) {
                    Log.e(TAG, "[polling] load task failed", finalLoadError);
                    requestNextPoll(POLL_INTERVAL_MS);
                    return;
                }
                if (finalSnapshot == null) {
                    stopProgressPolling();
                    binding.tvStatus.setText(R.string.progress_task_not_found);
                    binding.btnRestart.setEnabled(false);
                    return;
                }

                renderTaskSnapshot(finalSnapshot);
                if (shouldContinuePolling(finalSnapshot)) {
                    requestNextPoll(POLL_INTERVAL_MS);
                } else {
                    stopProgressPolling();
                }
            });
        });
    }

    private void renderTaskSnapshot(PrintTaskEntity task) {
        PrintTaskEntity renderTask = resolveRenderTask(task);
        currentTask = renderTask;
        refreshPageChips(renderTask);
        updatePrintProgress(renderTask);

        if (isUsingLiveTaskState(renderTask)) {
            return;
        }

        TaskStatus status = TaskStatus.fromCode(task.getStatus());
        applyTaskStatus(task, status);
    }

    private void updatePrintProgress(PrintTaskEntity task) {
        List<Integer> target = IntegerListConverter.fromString(task.getTargetPages());
        List<Integer> printed = IntegerListConverter.fromString(task.getPrintedPages());
        int total = target.size();
        int done = countPrintedPages(target, printed);

        binding.pbPrint.setMax(Math.max(total, 1));
        binding.pbPrint.setProgress(done);

        if (isTerminalStatus(TaskStatus.fromCode(task.getStatus())) && total > 0 && done >= total) {
            binding.tvPrintStatus.setText(getString(R.string.progress_print_counter_done, total, total));
        } else {
            binding.tvPrintStatus.setText(getString(R.string.progress_print_counter, done, total));
        }
    }

    private int countPrintedPages(List<Integer> target, List<Integer> printed) {
        int done = 0;
        for (int page : printed) {
            if (target.contains(page)) {
                done++;
            }
        }
        return done;
    }

    private boolean shouldContinuePolling(PrintTaskEntity task) {
        return !isTerminalStatus(TaskStatus.fromCode(task.getStatus()));
    }

    private PrintTaskEntity resolveRenderTask(PrintTaskEntity snapshot) {
        if (snapshot == null) return null;

        PrintEngine engine = PrintEngine.getInstance();
        if (engine.hasLiveTaskState(snapshot.getTaskId())) {
            PrintTaskEntity liveTask = engine.getCurrentTask();
            if (liveTask != null && liveTask.getTaskId() == snapshot.getTaskId()) {
                return liveTask;
            }
        }
        return snapshot;
    }

    private boolean isUsingLiveTaskState(PrintTaskEntity task) {
        return task != null && PrintEngine.getInstance().hasLiveTaskState(task.getTaskId());
    }

    private boolean isCurrentTaskNotActivelyPrinting(PrintTaskEntity task) {
        PrintEngine engine = PrintEngine.getInstance();
        PrintTaskEntity engineTask = engine.getCurrentTask();
        return !engine.isPrinting() || engineTask == null || task == null
            || engineTask.getTaskId() != task.getTaskId();
    }

    private boolean isTerminalStatus(TaskStatus status) {
        return status == TaskStatus.COMPLETED
            || status == TaskStatus.PAUSED
            || status == TaskStatus.INTERRUPTED
            || status == TaskStatus.CANCELLED;
    }

    private void applyTaskStatus(PrintTaskEntity task, TaskStatus status) {
        switch (status) {
            case COMPLETED:
                binding.tvStatus.setText(R.string.progress_print_done);
                binding.btnRestart.setVisibility(View.GONE);
                binding.btnPauseOrCancel.setVisibility(View.GONE);
                binding.btnViewDetail.setVisibility(View.VISIBLE);
                break;
            case PAUSED:
                binding.tvStatus.setText(R.string.progress_paused);
                binding.btnRestart.setEnabled(false);
                binding.btnViewDetail.setVisibility(View.VISIBLE);
                break;
            case INTERRUPTED:
                binding.tvStatus.setText(getString(R.string.progress_interrupted,
                    task.getLastError() != null ? task.getLastError() : ""));
                binding.btnRestart.setEnabled(false);
                binding.btnViewDetail.setVisibility(View.VISIBLE);
                break;
            case CANCELLED:
                binding.tvStatus.setText(R.string.progress_cancelled);
                binding.btnRestart.setVisibility(View.GONE);
                binding.btnPauseOrCancel.setVisibility(View.GONE);
                break;
            case IN_PROGRESS:
                if (isCurrentTaskNotActivelyPrinting(task)) {
                    binding.tvStatus.setText(R.string.progress_in_progress);
                }
                break;
            case PENDING:
                if (isCurrentTaskNotActivelyPrinting(task)) {
                    binding.tvStatus.setText(R.string.progress_pending);
                }
                break;
            default:
                break;
        }
    }

    private String formatSize(float bytes) {
        if (bytes < 1024) return String.format(Locale.US, getString(R.string.size_byte), bytes);
        if (bytes < 1024 * 1024) return String.format(Locale.US, getString(R.string.size_kb), bytes / 1024);
        return String.format(Locale.US, getString(R.string.size_mb), bytes / (1024 * 1024));
    }

    @Override
    protected void onDestroy() {
        stopProgressPolling();
        super.onDestroy();
        PrintEngine.getInstance().setPhaseCallback(null);
        // isChangingConfigurations() 为 true 表示 Activity 正在因配置变更重建（如旋转），
        // 此时不应中断打印 — 新 Activity 的 onCreate 会重新设置 phaseCallback 继续监听
        if (currentTask != null && PrintEngine.getInstance().isPrinting()
                && !isChangingConfigurations()) {
            PrintEngine.getInstance().pause();
            currentTask.setStatus(TaskStatus.INTERRUPTED.getCode());
            currentTask.setUpdatedAt(System.currentTimeMillis());
            PrintEngine.getInstance().getDbExecutor().execute(() ->
                AppDatabase.getInstance(this).printTaskRepository().update(currentTask));
        }
    }
}
