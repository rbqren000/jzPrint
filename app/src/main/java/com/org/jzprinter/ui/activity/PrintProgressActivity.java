package com.org.jzprinter.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.org.jzprinter.R;
import com.org.jzprinter.database.AppDatabase;
import com.org.jzprinter.database.converter.IntegerListConverter;
import com.org.jzprinter.database.entity.PrintTaskEntity;
import com.org.jzprinter.databinding.ActivityPrintProgressBinding;
import com.org.jzprinter.print.PrintEngine;
import com.org.jzprinter.print.PrintMode;
import com.org.jzprinter.print.PrintPhaseCallback;
import com.org.jzprinter.print.TaskStatus;

import java.util.ArrayList;
import java.util.List;

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

    private ActivityPrintProgressBinding binding;
    private long currentTaskId = -1;
    private PrintTaskEntity currentTask;

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
            rbqRunOnUiThread(() -> updateRestartButton());
        }

        @Override
        public void onPrepareStart(int totalPages) {
            prepareTotal = totalPages;
            rbqRunOnUiThread(() -> {
                binding.pbPrepare.setMax(totalPages);
                binding.pbPrepare.setProgress(0);
                binding.tvPrepareStatus.setText("0/" + totalPages + " 页");
                binding.tvStatus.setText("准备数据中...");
                setPhaseActive(0);
            });
        }

        @Override
        public void onPreparePageProgress(int currentPage, int totalPages, int pageIndex) {
            rbqRunOnUiThread(() -> {
                binding.pbPrepare.setProgress(currentPage);
                binding.tvPrepareStatus.setText(currentPage + "/" + totalPages + " 页 (page_" + pageIndex + ")");
            });
        }

        @Override
        public void onPrepareComplete() {
            rbqRunOnUiThread(() -> {
                binding.pbPrepare.setProgress(prepareTotal);
                binding.tvPrepareStatus.setText("完成 ✓");
                binding.tvStatus.setText("正在发送 " + prepareTotal + " 页数据...");
                setPhaseActive(1);
            });
        }

        @Override
        public void onDataTransferStart(float totalSize) {
            String sizeStr = formatSize(totalSize);
            rbqRunOnUiThread(() -> {
                binding.pbTransfer.setProgress(0);
                binding.tvTransferStatus.setText("0% (" + sizeStr + ")");
            });
        }

        @Override
        public void onDataTransferProgress(int percentage) {
            rbqRunOnUiThread(() -> {
                binding.pbTransfer.setProgress(percentage);
                binding.tvTransferStatus.setText(percentage + "%");
            });
        }

        @Override
        public void onDataTransferComplete() {
            rbqRunOnUiThread(() -> {
                binding.pbTransfer.setProgress(100);
                binding.tvTransferStatus.setText("完成 ✓");
                binding.tvStatus.setText("请在打印机上操作...");
                setPhaseActive(2);
            });
        }

        @Override
        public void onPhysicalPrintStart(int totalPages) {
            printTotal = totalPages;
            rbqRunOnUiThread(() -> {
                binding.pbPrint.setMax(totalPages);
                binding.pbPrint.setProgress(0);
                binding.tvPrintStatus.setText("0/" + totalPages + " 页");
            });
        }

        @Override
        public void onPhysicalPrintPageProgress(int printedPages, int totalPages, int pageIndex) {
            rbqRunOnUiThread(() -> {
                binding.pbPrint.setProgress(printedPages);
                binding.tvPrintStatus.setText(printedPages + "/" + totalPages + " 页");
            });
        }

        @Override
        public void onPhysicalPrintComplete() {
            rbqRunOnUiThread(() -> {
                binding.pbPrint.setProgress(printTotal);
                binding.tvPrintStatus.setText(printTotal + "/" + printTotal + " 页 ✓");
                binding.tvStatus.setText("打印完成");
                binding.btnRestart.setVisibility(View.GONE);
                binding.btnCancel.setVisibility(View.GONE);
                binding.btnViewDetail.setVisibility(View.VISIBLE);
            });
        }

        @Override
        public void onPhaseError(String phase, String error) {
            rbqRunOnUiThread(() -> {
                binding.tvStatus.setText("失败: " + error);
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

        binding.commonAppBar.titleTextView.setText("打印进度");
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

        /**
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

        binding.btnCancel.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("取消打印")
                .setMessage("确定取消本次打印？取消后需重新开始。")
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
        });

        binding.btnViewDetail.setOnClickListener(v -> {
            if (currentTaskId > 0) {
                startActivity(TaskDetailActivity.newIntent(this, currentTaskId));
                finish();
            }
        });

        // 重打指定页按钮：仅 PRINT 阶段可用，弹出页面选择弹窗
        binding.btnReprintSpecifiedPage.setOnClickListener(v -> showReprintPageSelector());

        PrintEngine.getInstance().setPhaseCallback(phaseCallback);
        startOrResumePrint();
    }

    private void startOrResumePrint() {
        boolean isReprint = getIntent().getBooleanExtra(EXTRA_IS_REPRINT, false);
        boolean isResume = getIntent().getBooleanExtra(EXTRA_IS_RESUME, false);
        long existingTaskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);

        if (isReprint && existingTaskId > 0) {
            currentTaskId = existingTaskId;
            List<Integer> reprintPages = getIntent().getIntegerArrayListExtra(EXTRA_REPRINT_PAGES);
            binding.tvStatus.setText("准备重打...");
            PrintEngine engine = PrintEngine.getInstance();
            engine.getDbExecutor().execute(() -> {
                try {
                    PrintTaskEntity task = AppDatabase.getInstance(PrintProgressActivity.this)
                        .printTaskRepository().getById(existingTaskId);
                    if (task == null) {
                        rbqRunOnUiThread(() -> {
                            binding.tvStatus.setText("任务不存在");
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
                    rbqRunOnUiThread(() -> startProgressPolling());
                } catch (Exception e) {
                    rbqRunOnUiThread(() -> {
                        binding.tvStatus.setText("重打失败: " + e.getMessage());
                        binding.btnRestart.setEnabled(false);
                    });
                }
            });
            return;
        }

        if (isResume && existingTaskId > 0) {
            currentTaskId = existingTaskId;
            binding.tvStatus.setText("准备继续...");
            PrintEngine engine = PrintEngine.getInstance();
            engine.getDbExecutor().execute(() -> {
                try {
                    PrintTaskEntity task = AppDatabase.getInstance(PrintProgressActivity.this)
                        .printTaskRepository().getById(existingTaskId);
                    if (task == null) {
                        rbqRunOnUiThread(() -> {
                            binding.tvStatus.setText("任务不存在");
                            binding.btnRestart.setEnabled(false);
                        });
                        return;
                    }
                    currentTask = task;
                    engine.resumeFromBreakpoint(task);
                    rbqRunOnUiThread(() -> startProgressPolling());
                } catch (Exception e) {
                    rbqRunOnUiThread(() -> {
                        binding.tvStatus.setText("继续失败: " + e.getMessage());
                        binding.btnRestart.setEnabled(false);
                    });
                }
            });
            return;
        }

        if (existingTaskId > 0) {
            currentTaskId = existingTaskId;
            binding.tvStatus.setText("查看进度...");
            binding.pbPrepare.setProgress(binding.pbPrepare.getMax());
            binding.tvPrepareStatus.setText("已完成");
            binding.pbTransfer.setProgress(binding.pbTransfer.getMax());
            binding.tvTransferStatus.setText("已完成");
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

        binding.tvStatus.setText("准备中...");
        PrintEngine engine = PrintEngine.getInstance();
        engine.getDbExecutor().execute(() -> {
            try {
                PrintTaskEntity task =
                    engine.startNewTask(schoolId, editionId, targetId, targetName,
                        editionType, printMode, pagesPath, businessId, editionName);
                currentTaskId = task.getTaskId();
                currentTask = task;
                engine.execute(task);
                rbqRunOnUiThread(() -> startProgressPolling());
            } catch (Exception e) {
                rbqRunOnUiThread(() -> {
                    binding.tvStatus.setText("打印失败: " + e.getMessage());
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
                binding.btnRestart.setText("重新发送");
                binding.btnRestart.setEnabled(false);
                binding.btnReprintSpecifiedPage.setVisibility(View.GONE);
                break;
            case TRANSFER:
                binding.btnRestart.setText("停止发送");
                binding.btnRestart.setEnabled(true);
                binding.btnReprintSpecifiedPage.setVisibility(View.GONE);
                break;
            case STOPPED:
                binding.btnRestart.setText("重新发送");
                binding.btnRestart.setEnabled(false);
                binding.btnReprintSpecifiedPage.setVisibility(View.GONE);
                binding.tvStatus.setText("发送已停止");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (currentPhase == PrintPhaseCallback.Phase.STOPPED) {
                        binding.btnRestart.setEnabled(true);
                    }
                }, 5000);
                break;
            case PRINT:
                binding.btnRestart.setText("重新发送");
                binding.btnRestart.setEnabled(false);
                binding.btnReprintSpecifiedPage.setVisibility(View.VISIBLE);
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
        binding.tvStatus.setText("准备中...");
        setPhaseActive(0);

        PrintEngine engine = PrintEngine.getInstance();
        engine.getDbExecutor().execute(() -> {
            try {
                Log.d(TAG, "[restartPrint] dbExecutor: loading task from DB...");
                PrintTaskEntity task = AppDatabase.getInstance(PrintProgressActivity.this)
                    .printTaskRepository().getById(currentTaskId);
                if (task == null) {
                    Log.e(TAG, "[restartPrint] task not found in DB!");
                    rbqRunOnUiThread(() -> binding.tvStatus.setText("任务不存在"));
                    return;
                }
                Log.d(TAG, "[restartPrint] task loaded, status=" + task.getStatus()
                    + " targetPages=" + task.getTargetPages()
                    + " printedPages=" + task.getPrintedPages());
                currentTask = task;
                engine.resumeFromBreakpoint(task);
                Log.d(TAG, "[restartPrint] resumeFromBreakpoint returned, starting polling");
                rbqRunOnUiThread(() -> startProgressPolling());
            } catch (Exception e) {
                Log.e(TAG, "[restartPrint] exception: " + e.getMessage(), e);
                rbqRunOnUiThread(() -> binding.tvStatus.setText("重启失败: " + e.getMessage()));
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

        List<Integer> targetPages = IntegerListConverter.fromString(task.getTargetPages());
        List<Integer> printedPages = IntegerListConverter.fromString(task.getPrintedPages());
        if (targetPages.isEmpty()) return;

        // 构建选项列表：✅ page_85 / ⬜ page_87 ...
        String[] items = new String[targetPages.size()];
        int defaultSelectedIndex = -1;
        // 从后往前找最后打印的页，作为默认选中
        for (int i = targetPages.size() - 1; i >= 0; i--) {
            if (printedPages.contains(targetPages.get(i))) {
                defaultSelectedIndex = i;
                break;
            }
        }
        if (defaultSelectedIndex < 0) defaultSelectedIndex = 0;

        for (int i = 0; i < targetPages.size(); i++) {
            int page = targetPages.get(i);
            items[i] = (printedPages.contains(page) ? "✅ 第 " : "⬜ 第 ") + page + " 页";
        }

        final int[] selectedIndex = {defaultSelectedIndex};

        new android.app.AlertDialog.Builder(this)
            .setTitle("重打指定页")
            .setSingleChoiceItems(items, defaultSelectedIndex, (dialog, which) -> {
                selectedIndex[0] = which;
            })
            .setNegativeButton("取消", null)
            .setPositiveButton("确认重打", (dialog, which) -> {
                int puzzleIndex = selectedIndex[0];
                int page = targetPages.get(puzzleIndex);
                engine.reprintSpecifiedPage(puzzleIndex);
                android.widget.Toast.makeText(PrintProgressActivity.this,
                    "已发送重打指令，请按打印机按钮重打第 " + page + " 页",
                    android.widget.Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    private void startProgressPolling() {
        final long pollTaskId = currentTaskId;
        PrintEngine.getInstance().getDbExecutor().execute(() -> {
            try {
                while (true) {
                    Thread.sleep(500);
                    if (isFinishing() || isDestroyed()) break;

                    // 关键检查：cancelTransfer()/pause() 会同步设置 isPrinting=false，
                    // 但 DB 的 PAUSED 更新可能排在 dbExecutor 队列后面尚未执行。
                    // 此处提前用 AtomicBoolean 判断，避免因 dbExecutor 单线程导致死锁：
                    //   polling 等 DB PAUSED → DB 更新在队列中等 polling 退出 → 互等。
                    if (!PrintEngine.getInstance().isPrinting()) {
                        Log.d(TAG, "[polling] isPrinting=false, breaking loop");
                        break;
                    }

                    AppDatabase db = AppDatabase.getInstance(PrintProgressActivity.this);
                    PrintTaskEntity task = db.printTaskRepository().getById(pollTaskId);
                    if (task == null) break;

                    TaskStatus status = TaskStatus.fromCode(task.getStatus());
                    List<Integer> printed = IntegerListConverter.fromString(task.getPrintedPages());

                    rbqRunOnUiThread(() -> {
                        if (!printed.isEmpty()) {
                            binding.tvPrintedPages.setVisibility(View.VISIBLE);
                            StringBuilder sb = new StringBuilder("已打印：");
                            for (int i = 0; i < printed.size(); i++) {
                                if (i > 0) sb.append(",");
                                sb.append("page_").append(printed.get(i));
                            }
                            binding.tvPrintedPages.setText(sb.toString());
                        }

                        if (PrintEngine.getInstance().getPhaseCallback() == null) {
                            switch (status) {
                                case COMPLETED:
                                    binding.tvStatus.setText("打印完成");
                                    binding.btnRestart.setVisibility(View.GONE);
                                    binding.btnCancel.setVisibility(View.GONE);
                                    binding.btnViewDetail.setVisibility(View.VISIBLE);
                                    break;
                                case PAUSED:
                                    binding.tvStatus.setText("已暂停");
                                    binding.btnRestart.setEnabled(false);
                                    binding.btnViewDetail.setVisibility(View.VISIBLE);
                                    break;
                                case INTERRUPTED:
                                    binding.tvStatus.setText("中断: " + (task.getLastError() != null ? task.getLastError() : ""));
                                    binding.btnRestart.setEnabled(false);
                                    binding.btnViewDetail.setVisibility(View.VISIBLE);
                                    break;
                                case CANCELLED:
                                    binding.tvStatus.setText("已取消");
                                    binding.btnRestart.setVisibility(View.GONE);
                                    binding.btnCancel.setVisibility(View.GONE);
                                    break;
                                default:
                                    break;
                            }
                        }
                    });

                    if (status == TaskStatus.COMPLETED || status == TaskStatus.PAUSED
                        || status == TaskStatus.INTERRUPTED || status == TaskStatus.CANCELLED) {
                        break;
                    }
                }
            } catch (InterruptedException ignored) {
            }
        });
    }

    private static String formatSize(float bytes) {
        if (bytes < 1024) return String.format("%.0f B", bytes);
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024);
        return String.format("%.1f MB", bytes / (1024 * 1024));
    }

    @Override
    public void onBackPressed() {
        if (PrintEngine.getInstance().isPrinting()) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("打印正在进行")
                .setMessage("当前正在打印，退出将暂停任务。下次可继续打印。")
                .setPositiveButton("暂停并退出", (d, w) -> {
                    PrintEngine.getInstance().pause();
                    finish();
                })
                .setNegativeButton("继续打印", null)
                .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
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
