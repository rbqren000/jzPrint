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

        binding.commonAppBar.titleTextView.setText(R.string.task_detail_title);
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

        binding.tvTaskInfo.setText(getString(R.string.task_detail_info_fmt,
            task.getTargetId(), task.getEditionId(),
            mode.getLabel(this), targetPages.size(), status.getLabel(this)));

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
                binding.btnContinue.setText(R.string.task_detail_start_print);
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
                .setTitle(R.string.main_no_printer_title)
                .setMessage(R.string.main_no_printer_msg)
                .setPositiveButton(R.string.btn_go_connect, (d, w) ->
                    startActivity(new Intent(this, DeviceSelectActivity.class)))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
            return false;
        }
        return true;
    }

    private boolean checkMaterialExists() {
        String pagesPath = task.getMaterialPath();
        if (pagesPath == null || !new File(pagesPath).exists()) {
            new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.main_no_material_title)
                .setMessage(R.string.main_no_material_msg)
                .setPositiveButton(R.string.dialog_ok, null)
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
                .setTitle(R.string.task_detail_start_print)
                .setMessage(getString(R.string.task_detail_start_pending_msg, target.size()))
                .setPositiveButton(R.string.task_detail_start_btn, (d, w) -> {
                    startActivity(PrintProgressActivity.newResumeIntent(this, task.getTaskId()));
                    finish();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        } else {
            List<Integer> printed = IntegerListConverter.fromString(task.getPrintedPages());
            List<Integer> target = IntegerListConverter.fromString(task.getTargetPages());
            int remaining = target.size() - printed.size();

            new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.main_continue_print)
                .setMessage(getString(R.string.main_resume_prompt, remaining))
                .setPositiveButton(R.string.main_continue_btn, (d, w) -> {
                    startActivity(PrintProgressActivity.newResumeIntent(this, task.getTaskId()));
                    finish();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        }
    }

    private void onReprintSelected() {
        if (selectedPages.isEmpty()) {
            showToast(getString(R.string.task_detail_select_reprint_pages));
            return;
        }
        if (task == null) return;
        if (!checkPrinterConnection()) return;
        if (!checkMaterialExists()) return;

        List<Integer> pagesToReprint = new ArrayList<>(selectedPages);
        Collections.sort(pagesToReprint);

        new android.app.AlertDialog.Builder(this)
            .setTitle(R.string.task_detail_reprint_confirm_title)
            .setMessage(getString(R.string.task_detail_reprint_confirm_msg,
                pagesToReprint.size(), pagesToReprint))
            .setPositiveButton(R.string.task_detail_reprint_btn, (d, w) -> {
                PrintEngine.getInstance().switchToNewTarget();
                startActivity(PrintProgressActivity.newReprintPagesIntent(
                    this, task.getTaskId(), new ArrayList<>(pagesToReprint)));
                finish();
            })
            .setNegativeButton(R.string.dialog_cancel, null)
            .show();
    }

    private void onReprintAll() {
        if (task == null) return;
        if (!checkPrinterConnection()) return;
        if (!checkMaterialExists()) return;

        List<Integer> allTarget = IntegerListConverter.fromString(task.getTargetPages());

        new android.app.AlertDialog.Builder(this)
            .setTitle(R.string.task_detail_reprint_all_title)
            .setMessage(getString(R.string.task_detail_reprint_all_msg, allTarget.size()))
            .setPositiveButton(R.string.task_detail_reprint_all_btn, (d, w) -> {
                PrintEngine.getInstance().switchToNewTarget();
                startActivity(PrintProgressActivity.newReprintIntent(this, task.getTaskId()));
                finish();
            })
            .setNegativeButton(R.string.dialog_cancel, null)
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
