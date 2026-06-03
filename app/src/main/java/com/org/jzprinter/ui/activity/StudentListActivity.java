package com.org.jzprinter.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.org.jzprinter.R;
import com.org.jzprinter.database.AppDatabase;
import com.org.jzprinter.database.converter.IntegerListConverter;
import com.org.jzprinter.database.entity.PrintTaskEntity;
import com.org.jzprinter.database.entity.StudentEntity;
import com.org.jzprinter.databinding.ActivityStudentListBinding;
import com.org.jzprinter.network.Api;
import com.org.jzprinter.network.ApiClientFactory;
import com.org.jzprinter.network.model.RosterGroup;
import com.org.jzprinter.network.model.Student;
import com.org.jzprinter.print.MaterialPathBuilder;
import com.org.jzprinter.print.PrintEngine;
import com.org.jzprinter.print.PrintMode;
import com.org.jzprinter.print.TaskStatus;
import com.org.jzprinter.repository.PrintTaskRepository;
import com.org.jzprinter.repository.StudentRepository;
import com.org.jzprinter.service.DownloadService;
import com.org.jzprinter.ui.adapter.PrepareCodeAdapter;
import com.org.jzprinter.ui.adapter.StudentAdapter;
import com.org.jzprinter.widget.CustomDialog.RBQProgressDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentListActivity extends BaseActivity {

    private static final String EXTRA_SCHOOL_ID = "schoolId";
    private static final String EXTRA_EDITION_ID = "editionId";
    private static final String EXTRA_EDITION_TYPE = "editionType";
    private static final String EXTRA_EDITION_NAME = "editionName";

    public static Intent newIntent(Context context, String schoolId,
                                   String editionId, int editionType,
                                   String editionName) {
        Intent intent = new Intent(context, StudentListActivity.class);
        intent.putExtra(EXTRA_SCHOOL_ID, schoolId);
        intent.putExtra(EXTRA_EDITION_ID, editionId);
        intent.putExtra(EXTRA_EDITION_TYPE, editionType);
        intent.putExtra(EXTRA_EDITION_NAME, editionName != null ? editionName : "");
        return intent;
    }

    private ActivityStudentListBinding binding;
    private Api apiClient;
    private String schoolId;
    private String editionId;
    private String editionName;
    private int editionType;
    private Map<String, String> prepareCodeBusinessIdMap = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupStatusBarWithCustomColorResId(R.color.primary_blue);

        schoolId = getIntent().getStringExtra(EXTRA_SCHOOL_ID);
        editionId = getIntent().getStringExtra(EXTRA_EDITION_ID);
        editionName = getIntent().getStringExtra(EXTRA_EDITION_NAME);
        editionType = getIntent().getIntExtra(EXTRA_EDITION_TYPE, 1);

        String title = editionType == 2 ? getString(R.string.title_prepare_code_list) : getString(R.string.title_student_list);
        binding.commonAppBar.titleTextView.setText(title);
        binding.commonAppBar.leftMenuLayout.setOnClickListener(v -> finish());

        binding.rvList.setLayoutManager(new LinearLayoutManager(this));
        // 添加分割线，与首页保持一致样式
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.divider_line, getTheme()));
        binding.rvList.addItemDecoration(divider);
        apiClient = ApiClientFactory.create(this);

        if (editionType == 2) {
            loadPrepareCodes();
        } else {
            loadStudents();
        }
    }

    private void showLoading() {
        binding.pbLoading.setVisibility(View.VISIBLE);
        binding.rvList.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(View.GONE);
    }

    private void showContent() {
        binding.pbLoading.setVisibility(View.GONE);
        binding.rvList.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);
    }

    private void showEmpty(String message) {
        binding.pbLoading.setVisibility(View.GONE);
        binding.rvList.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(View.VISIBLE);
        binding.tvEmpty.setText(message);
    }

    private void loadStudents() {
        showLoading();
        apiClient.fetchStudents(schoolId, editionId, editionType,
            new Api.StudentCallback() {
                @Override
                public void onSuccess(List<RosterGroup> classes) {
                    if (classes.isEmpty()) {
                        showEmpty(getString(R.string.empty_no_student));
                        return;
                    }

                    List<StudentEntity> entities = new ArrayList<>();
                    for (RosterGroup classInfo : classes) {
                        for (Student detail : classInfo.studentList) {
                            StudentEntity entity = new StudentEntity();
                            entity.setStudentId(detail.studentId);
                            entity.setSchoolId(schoolId);
                            entity.setEditionId(editionId);
                            entity.setStudentName(detail.studentName);
                            entity.setClassId(classInfo.classId);
                            entity.setClassName(classInfo.className);
                            entity.setBusinessId(detail.businessId);
                            entity.setCachedAt(System.currentTimeMillis());
                            entities.add(entity);
                        }
                    }

                    PrintEngine.getInstance().getDbExecutor().execute(() -> {
                        StudentRepository studentRepo = AppDatabase.getInstance(StudentListActivity.this)
                            .studentRepository();

                        for (StudentEntity entity : entities) {
                            checkMaterialReady(entity);
                            StudentEntity existing = studentRepo.getById(
                                entity.getStudentId(), entity.getSchoolId(), entity.getEditionId());
                            if (existing == null) {
                                studentRepo.insert(entity);
                            } else {
                                existing.setMaterialReady(entity.isMaterialReady());
                                existing.setMaterialPath(entity.getMaterialPath());
                                existing.setBusinessId(entity.getBusinessId());
                                studentRepo.update(existing);
                            }
                        }
                        List<StudentEntity> saved = studentRepo.getByEdition(schoolId, editionId);
                        rbqRunOnUiThread(() -> {
                            buildGroupedStudentList(saved);
                            showContent();
                        });
                    });
                }

                @Override
                public void onError(String error) {
                    rbqRunOnUiThread(() -> showEmpty(getString(R.string.error_load_failed, error)));
                }
            });
    }

    private void checkMaterialReady(StudentEntity entity) {
        String pagesPath = MaterialPathBuilder.getPagesPath(
            this, schoolId, editionId, editionType, entity.getStudentId());
        boolean ready = isPagesDirReady(pagesPath);
        entity.setMaterialReady(ready);
        entity.setMaterialPath(ready ? pagesPath : "");
    }

    private static boolean isPagesDirReady(String pagesPath) {
        File pagesDir = new File(pagesPath);
        return pagesDir.exists() && pagesDir.isDirectory()
            && pagesDir.list() != null && pagesDir.list().length > 0;
    }

    private void buildGroupedStudentList(List<StudentEntity> students) {
        List<StudentAdapter.ListItem> displayItems = new ArrayList<>();
        String lastClassName = null;
        for (StudentEntity student : students) {
            if (!student.getClassName().equals(lastClassName)) {
                lastClassName = student.getClassName();
                displayItems.add(StudentAdapter.ListItem.section(lastClassName));
            }
            displayItems.add(StudentAdapter.ListItem.item(student));
        }

        StudentAdapter adapter = new StudentAdapter();
        adapter.setItems(displayItems);
        adapter.setOnStudentClickListener(this::onStudentClick);
        adapter.setOnDownloadClickListener(this::onStudentDownload);
        binding.rvList.setAdapter(adapter);
    }

    private void loadPrepareCodes() {
        showLoading();
        apiClient.fetchStudents(schoolId, editionId, editionType,
            new Api.StudentCallback() {
                @Override
                public void onSuccess(List<RosterGroup> classes) {
                    prepareCodeBusinessIdMap.clear();
                    List<String> codes = new ArrayList<>();
                    for (RosterGroup classInfo : classes) {
                        if (classInfo.prepareCode != null && !classInfo.prepareCode.isEmpty()) {
                            codes.add(classInfo.prepareCode);
                            prepareCodeBusinessIdMap.put(classInfo.prepareCode, classInfo.businessId);
                        }
                    }
                    if (codes.isEmpty()) {
                        showEmpty(getString(R.string.empty_no_prepare_code));
                        return;
                    }

                    PrintEngine.getInstance().getDbExecutor().execute(() -> {
                        List<Boolean> readyStates = new ArrayList<>();
                        for (String code : codes) {
                            String pagesPath = MaterialPathBuilder.getPagesPath(
                                StudentListActivity.this, schoolId, editionId, 2, code);
                            readyStates.add(isPagesDirReady(pagesPath));
                        }
                        rbqRunOnUiThread(() -> {
                            PrepareCodeAdapter adapter = new PrepareCodeAdapter();
                            adapter.setItems(codes, readyStates);
                            adapter.setOnPrepareCodeClickListener(
                                StudentListActivity.this::onPrepareCodeClick);
                            adapter.setOnDownloadClickListener(
                                StudentListActivity.this::onPrepareCodeDownload);
                            binding.rvList.setAdapter(adapter);
                            showContent();
                        });
                    });
                }

                @Override
                public void onError(String error) {
                    rbqRunOnUiThread(() -> showEmpty(getString(R.string.error_load_failed, error)));
                }
            });
    }

    private void onStudentDownload(StudentEntity student) {
        Log.d("StudentList", "[onStudentDownload] studentId=" + student.getStudentId()
            + " businessId=" + student.getBusinessId() + " editionType=" + editionType);
        String businessId = student.getBusinessId();
        downloadAndThen(businessId, student.getStudentId(),
            pagesPath -> {
                student.setMaterialReady(true);
                student.setMaterialPath(pagesPath);
                onStudentClick(student);
            });
    }

    private void onPrepareCodeDownload(String prepareCode) {
        String businessId = prepareCodeBusinessIdMap.get(prepareCode);
        android.util.Log.d("StudentList", "[precodeDownload] prepareCode=" + prepareCode
            + " businessId=" + businessId + " editionType=" + editionType);
        if (businessId == null) {
            new android.app.AlertDialog.Builder(this, R.style.mAlertDialog)
                .setTitle(R.string.download_failed)
                .setMessage(R.string.download_no_material_info)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
            return;
        }
        downloadAndThen(businessId, prepareCode,
            pagesPath -> onPrepareCodeClick(prepareCode, true));
    }

    private RBQProgressDialog progressDialog;

    private void downloadAndThen(String businessId, String targetId,
                                 OnDownloadReadyCallback onReady) {
        Log.d("StudentList", "[downloadAndThen] targetId=" + targetId
            + " businessId=" + businessId + " editionType=" + editionType);
        progressDialog = new RBQProgressDialog();
        progressDialog.show(this, getString(R.string.downloading_material), getString(R.string.please_wait));

        DownloadService.downloadAndExtract(this, schoolId, businessId, editionType,
            editionId, targetId,
            new DownloadService.DownloadAndExtractCallback() {
                @Override
                public void onDownloadProgress(int percentage) {}

                @Override
                public void onComplete(String path) {
                    Log.d("StudentList", "[downloadComplete] targetId=" + targetId
                        + " pagesPath=" + path);
                    rbqRunOnUiThread(() -> {
                        dismissProgress();
                        refreshList();
                        onReady.onReady(path);
                    });
                }

                @Override
                public void onAlreadyExists(String path) {
                    Log.d("StudentList", "[downloadAlreadyExists] targetId=" + targetId
                        + " pagesPath=" + path);
                    rbqRunOnUiThread(() -> onReady.onReady(path));
                }

                @Override
                public void onError(String error) {
                    Log.e("StudentList", "[downloadError] targetId=" + targetId
                        + " error=" + error);
                    rbqRunOnUiThread(() -> {
                        dismissProgress();
                        if (!isFinishing()) {
                            new android.app.AlertDialog.Builder(StudentListActivity.this, R.style.mAlertDialog)
                                .setTitle(R.string.download_failed)
                                .setMessage(error)
                                .setPositiveButton(R.string.dialog_ok, null)
                                .show();
                        }
                    });
                }
            });
    }

    private void dismissProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private interface OnDownloadReadyCallback {
        void onReady(String pagesPath);
    }

    private void refreshList() {
        if (editionType == 2) {
            loadPrepareCodes();
        } else {
            loadStudents();
        }
    }

    private void onStudentClick(StudentEntity student) {
        String targetId = student.getStudentId();
        PrintEngine.getInstance().getDbExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(StudentListActivity.this);
            PrintTaskRepository taskRepo = db.printTaskRepository();
            List<PrintTaskEntity> allTasks = taskRepo.findByTargetId(targetId);
            List<PrintTaskEntity> resumable = taskRepo.findResumableByTargetId(targetId);

            if (allTasks == null || allTasks.isEmpty()) {
                rbqRunOnUiThread(() -> navigateToPrintModeSelect(
                    student.getStudentId(), student.getStudentName(), student.getMaterialPath(), student.getBusinessId()));
                return;
            }

            if (resumable != null && !resumable.isEmpty()) {
                if (resumable.size() == 1) {
                    PrintTaskEntity task = resumable.get(0);
                    rbqRunOnUiThread(() -> showSingleTaskResumeDialog(
                        student.getStudentName(), task,
                        () -> navigateToPrintModeSelect(student.getStudentId(), student.getStudentName(), student.getMaterialPath(), student.getBusinessId())));
                } else {
                    rbqRunOnUiThread(() -> showMultiTaskResumeDialog(
                        student.getStudentName(), resumable,
                        () -> navigateToPrintModeSelect(student.getStudentId(), student.getStudentName(), student.getMaterialPath(), student.getBusinessId())));
                }
                return;
            }

            rbqRunOnUiThread(() -> showExistingTasksDialog(
                student.getStudentName(), allTasks,
                () -> navigateToPrintModeSelect(student.getStudentId(), student.getStudentName(), student.getMaterialPath(), student.getBusinessId())));
        });
    }

    private void onPrepareCodeClick(String prepareCode, boolean materialReady) {
        String targetId = prepareCode;
        final String finalMaterialPath;
        if (materialReady) {
            finalMaterialPath = MaterialPathBuilder.getPagesPath(
                this, schoolId, editionId, 2, prepareCode);
        } else {
            finalMaterialPath = "";
        }
        String businessId = prepareCodeBusinessIdMap.get(prepareCode);

        PrintEngine.getInstance().getDbExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(StudentListActivity.this);
            PrintTaskRepository taskRepo = db.printTaskRepository();
            List<PrintTaskEntity> resumable = taskRepo.findResumableByTargetId(targetId);

            if (resumable == null || resumable.isEmpty()) {
                rbqRunOnUiThread(() -> navigateToPrintModeSelect(prepareCode, prepareCode, finalMaterialPath, businessId));
                return;
            }

            if (resumable.size() == 1) {
                PrintTaskEntity task = resumable.get(0);
                rbqRunOnUiThread(() -> showSingleTaskResumeDialog(
                    prepareCode, task,
                    () -> navigateToPrintModeSelect(prepareCode, prepareCode, finalMaterialPath, businessId)));
            } else {
                rbqRunOnUiThread(() -> showMultiTaskResumeDialog(
                    prepareCode, resumable,
                    () -> navigateToPrintModeSelect(prepareCode, prepareCode, finalMaterialPath, businessId)));
            }
        });
    }

    private void navigateToPrintModeSelect(String targetId, String targetName,
                                            String materialPath, String businessId) {
        if (materialPath == null) materialPath = "";
        startActivity(PrintModeSelectActivity.newIntent(this,
            schoolId, editionId, targetId, targetName, editionType, materialPath, businessId, editionName));
    }

    private void showSingleTaskResumeDialog(String name, PrintTaskEntity task,
                                             Runnable onRestart) {
        int printed = IntegerListConverter.fromString(task.getPrintedPages()).size();
        int total = IntegerListConverter.fromString(task.getTargetPages()).size();
        String modeLabel = PrintMode.fromCode(task.getPrintMode()).getLabel(this);

        new android.app.AlertDialog.Builder(this, R.style.mAlertDialog)
            .setTitle(getString(R.string.student_has_unfinished, name))
            .setMessage(getString(R.string.student_task_info, modeLabel, printed, total))
            .setPositiveButton(R.string.main_continue_print, (d, w) -> {
                PrintEngine.getInstance().switchToNewTarget();
                startActivity(TaskDetailActivity.newIntent(this, task.getTaskId()));
            })
            .setNegativeButton(R.string.dialog_resume_restart, (d, w) -> onRestart.run())
            .setNeutralButton(R.string.main_cancel_task_btn, (d, w) -> {
                task.setStatus(TaskStatus.CANCELLED.getCode());
                task.setUpdatedAt(System.currentTimeMillis());
                PrintEngine.getInstance().getDbExecutor().execute(() ->
                    AppDatabase.getInstance(this).printTaskRepository().update(task));
            })
            .show();
    }

    private void showMultiTaskResumeDialog(String name,
                                             List<PrintTaskEntity> tasks,
                                             Runnable onRestart) {
        String[] names = new String[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            PrintTaskEntity t = tasks.get(i);
            PrintMode mode = PrintMode.fromCode(t.getPrintMode());
            int printed = IntegerListConverter.fromString(t.getPrintedPages()).size();
            int total = IntegerListConverter.fromString(t.getTargetPages()).size();
            names[i] = getString(R.string.task_card_progress, printed, total, mode.getLabel(this));
        }

        new android.app.AlertDialog.Builder(this, R.style.mAlertDialog)
            .setTitle(getString(R.string.student_multi_unfinished, name))
            .setItems(names, (d, which) -> {
                PrintTaskEntity selected = tasks.get(which);
                PrintEngine.getInstance().switchToNewTarget();
                startActivity(TaskDetailActivity.newIntent(this, selected.getTaskId()));
            })
            .setNegativeButton(R.string.dialog_resume_restart, (d, w) -> onRestart.run())
            .show();
    }

    private void showExistingTasksDialog(String name,
                                          List<PrintTaskEntity> tasks,
                                          Runnable onRestart) {
        String[] items = new String[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            PrintTaskEntity t = tasks.get(i);
            PrintMode mode = PrintMode.fromCode(t.getPrintMode());
            TaskStatus status = TaskStatus.fromCode(t.getStatus());
            int printed = IntegerListConverter.fromString(t.getPrintedPages()).size();
            int total = IntegerListConverter.fromString(t.getTargetPages()).size();
            items[i] = getString(R.string.task_card_progress, printed, total, mode.getLabel(this)) + " " + status.getLabel(this);
        }

        new android.app.AlertDialog.Builder(this, R.style.mAlertDialog)
            .setTitle(getString(R.string.label_print_records, name))
            .setItems(items, (d, which) -> {
                PrintTaskEntity selected = tasks.get(which);
                startActivity(TaskDetailActivity.newIntent(this, selected.getTaskId()));
            })
            .setNegativeButton("新打印", (d, w) -> onRestart.run())
            .show();
    }
}
