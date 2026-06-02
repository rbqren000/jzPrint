package com.org.jzprinter.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mx.mxSdk.ConnectManager;
import com.org.jzprinter.R;
import com.org.jzprinter.database.AppDatabase;
import com.org.jzprinter.database.converter.IntegerListConverter;
import com.org.jzprinter.database.entity.PrintTaskEntity;
import com.org.jzprinter.databinding.ActivityTaskDetailBinding;
import com.org.jzprinter.print.PrintEngine;
import com.org.jzprinter.print.PrintMode;
import com.org.jzprinter.print.TaskStatus;
import com.org.jzprinter.ui.adapter.PageAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaskDetailActivity extends BaseActivity {

    private static final String EXTRA_TASK_ID = "taskId";

    public static Intent newIntent(Context context, long taskId) {
        Intent intent = new Intent(context, TaskDetailActivity.class);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        return intent;
    }

    private ActivityTaskDetailBinding binding;
    private PrintTaskEntity task;
    private final Set<Integer> selectedPages = new HashSet<>();
    private PageAdapter pageAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupStatusBarWithCustomColorResId(R.color.primary_blue);

        binding.commonAppBar.titleTextView.setText("任务详情");
        binding.commonAppBar.leftMenuLayout.setOnClickListener(v -> finish());

        long taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);

        binding.rvPageList.setLayoutManager(new LinearLayoutManager(this));
        pageAdapter = new PageAdapter(selectedPages);
        binding.rvPageList.setAdapter(pageAdapter);

        loadTask(taskId);

        binding.btnContinue.setOnClickListener(v -> onContinuePrint());
        binding.btnReprintSelected.setOnClickListener(v -> onReprintSelected());
        binding.btnReprintAll.setOnClickListener(v -> onReprintAll());
        binding.btnViewProgress.setOnClickListener(v -> onViewProgress());
    }

    private void loadTask(long taskId) {
        AppDatabase db = AppDatabase.getInstance(this);
        PrintEngine.getInstance().getDbExecutor().execute(() -> {
            PrintTaskEntity loaded = db.printTaskRepository().getById(taskId);
            if (loaded != null) {
                task = loaded;
                rbqRunOnUiThread(() -> bindTask());
            }
        });
    }

    private void bindTask() {
        if (task == null) return;

        TaskStatus status = TaskStatus.fromCode(task.getStatus());
        PrintMode mode = PrintMode.fromCode(task.getPrintMode());

        List<Integer> targetPages = IntegerListConverter.fromString(task.getTargetPages());
        List<Integer> printedPages = IntegerListConverter.fromString(task.getPrintedPages());

        binding.tvTaskInfo.setText(String.format("目标：%s\n校本：%s\n模式：%s (%d页)\n状态：%s",
            task.getTargetId(), task.getEditionId(),
            mode.getLabel(), targetPages.size(), status.getLabel()));

        pageAdapter.setPages(targetPages, printedPages);

        binding.btnContinue.setVisibility(View.GONE);
        binding.btnReprintSelected.setVisibility(View.GONE);
        binding.btnReprintAll.setVisibility(View.GONE);
        binding.btnViewProgress.setVisibility(View.GONE);

        switch (status) {
            case COMPLETED:
                binding.btnReprintSelected.setVisibility(View.VISIBLE);
                binding.btnReprintAll.setVisibility(View.VISIBLE);
                break;
            case PAUSED:
            case INTERRUPTED:
                binding.btnContinue.setVisibility(View.VISIBLE);
                binding.btnReprintSelected.setVisibility(View.VISIBLE);
                binding.btnReprintAll.setVisibility(View.VISIBLE);
                break;
            case PENDING:
                binding.btnContinue.setVisibility(View.VISIBLE);
                binding.btnContinue.setText("开始打印");
                binding.btnReprintAll.setVisibility(View.VISIBLE);
                break;
            case IN_PROGRESS:
                binding.btnViewProgress.setVisibility(View.VISIBLE);
                break;
            case CANCELLED:
                binding.btnReprintAll.setVisibility(View.VISIBLE);
                break;
        }

        boolean hasPrintedPages = !printedPages.isEmpty();
        binding.btnReprintSelected.setEnabled(hasPrintedPages);
        binding.btnReprintSelected.setAlpha(hasPrintedPages ? 1.0f : 0.4f);
    }

    private boolean checkPrinterConnection() {
        if (!Boolean.TRUE.equals(ConnectManager.share().isConnected())) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("打印机未连接")
                .setMessage("请先连接打印机")
                .setPositiveButton("去连接", (d, w) ->
                    startActivity(new Intent(this, DeviceSelectActivity.class)))
                .setNegativeButton("取消", null)
                .show();
            return false;
        }
        return true;
    }

    private boolean checkMaterialExists() {
        String pagesPath = task.getMaterialPath();
        if (pagesPath == null || !new File(pagesPath).exists()) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("素材不存在")
                .setMessage("素材文件已丢失，请重新下载素材后再打印")
                .setPositiveButton("确定", null)
                .show();
            return false;
        }
        return true;
    }

    private void onContinuePrint() {
        if (task == null) return;
        if (!checkPrinterConnection()) return;
        if (!checkMaterialExists()) return;

        TaskStatus status = TaskStatus.fromCode(task.getStatus());
        if (status == TaskStatus.PENDING) {
            List<Integer> target = IntegerListConverter.fromString(task.getTargetPages());
            new android.app.AlertDialog.Builder(this)
                .setTitle("开始打印")
                .setMessage(String.format("将发送 %d 页数据给打印机，是否开始？", target.size()))
                .setPositiveButton("开始", (d, w) -> {
                    startActivity(PrintProgressActivity.newResumeIntent(this, task.getTaskId()));
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
        } else {
            List<Integer> printed = IntegerListConverter.fromString(task.getPrintedPages());
            List<Integer> target = IntegerListConverter.fromString(task.getTargetPages());
            int remaining = target.size() - printed.size();

            new android.app.AlertDialog.Builder(this)
                .setTitle("继续打印")
                .setMessage(String.format("将重新发送剩余 %d 页数据给打印机，是否继续？", remaining))
                .setPositiveButton("继续", (d, w) -> {
                    startActivity(PrintProgressActivity.newResumeIntent(this, task.getTaskId()));
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
        }
    }

    private void onReprintSelected() {
        if (selectedPages.isEmpty()) {
            showToast("请选择需要重打的已完成页面");
            return;
        }
        if (task == null) return;
        if (!checkPrinterConnection()) return;
        if (!checkMaterialExists()) return;

        List<Integer> pagesToReprint = new ArrayList<>(selectedPages);
        Collections.sort(pagesToReprint);

        new android.app.AlertDialog.Builder(this)
            .setTitle("确认重打")
            .setMessage(String.format("将重新发送 %d 页数据给打印机（耗时与正常打印相同），是否继续？\n\n重打页码：%s",
                pagesToReprint.size(), pagesToReprint))
            .setPositiveButton("重打", (d, w) -> {
                PrintEngine.getInstance().switchToNewTarget();
                startActivity(PrintProgressActivity.newReprintPagesIntent(
                    this, task.getTaskId(), new ArrayList<>(pagesToReprint)));
                finish();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void onReprintAll() {
        if (task == null) return;
        if (!checkPrinterConnection()) return;
        if (!checkMaterialExists()) return;

        List<Integer> allTarget = IntegerListConverter.fromString(task.getTargetPages());

        new android.app.AlertDialog.Builder(this)
            .setTitle("确认全部重打")
            .setMessage(String.format("将重新发送全部 %d 页数据给打印机（耗时与正常打印相同），是否继续？",
                allTarget.size()))
            .setPositiveButton("全部重打", (d, w) -> {
                PrintEngine.getInstance().switchToNewTarget();
                startActivity(PrintProgressActivity.newReprintIntent(this, task.getTaskId()));
                finish();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void onViewProgress() {
        if (task == null) return;
        startActivity(PrintProgressActivity.newIntent(this,
            task.getSchoolId(), task.getEditionId(), task.getTargetId(),
            task.getTargetName(), task.getEditionType(), task.getPrintMode(),
            task.getMaterialPath(), task.getTaskId(), task.getBusinessId(),
            task.getEditionName()));
        finish();
    }
}
