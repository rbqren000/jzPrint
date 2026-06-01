package com.org.jzprinter.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.mx.mxSdk.ConnectManager;
import com.org.jzprinter.R;
import com.org.jzprinter.database.AppDatabase;
import com.org.jzprinter.database.entity.PrintTaskEntity;
import com.org.jzprinter.databinding.ActivityMainBinding;
import com.org.jzprinter.network.AuthConfig;
import com.org.jzprinter.network.AuthManager;
import com.org.jzprinter.print.PrintEngine;
import com.org.jzprinter.print.TaskStatus;
import com.org.jzprinter.print.PrintMode;
import com.org.jzprinter.print.StorageManager;
import com.org.jzprinter.ui.adapter.TaskCardAdapter;
import com.org.jzprinter.utils.Storage.PreferencesUtils;

import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_DEVICE_CONNECT = 1002;
    private static final String PREFS_KEY_SCHOOL_ID = "schoolId";
    private static final String PREFS_KEY_LANGUAGE = "app_language";

    private ActivityMainBinding binding;
    private TaskCardAdapter taskCardAdapter;
    private boolean hasCheckedResumableOnStartup = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        ensureDevSchoolId();
        initContentViews();
        updateDeviceState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTokenIfNeeded();
        updateDeviceState();
        updateTaskCards();
    }

    /** 每次回到 MainActivity 异步刷新 token，保证不会使用过期凭证 */
    private void refreshTokenIfNeeded() {
        AuthManager.getInstance(this).login(new AuthManager.LoginCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("MainActivity", "token refreshed");
            }

            @Override
            public void onError(String error) {
                android.util.Log.w("MainActivity", "token refresh failed: " + error);
                // 静默忽略，旧 token 仍在 SP 中作为 fallback
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void ensureDevSchoolId() {
        String current = PreferencesUtils.getString(this, PREFS_KEY_SCHOOL_ID, "");
        if (current.isEmpty()) {
            PreferencesUtils.putString(this, PREFS_KEY_SCHOOL_ID, AuthConfig.DEV_SCHOOL_ID);
        }
    }

    private void initViews() {
        binding.ivMenu.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));

        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_language) {
                showLanguageDialog();
            } else if (id == R.id.nav_about) {
                showAboutDialog();
            } else if (id == R.id.nav_storage) {
                showStorageInfo();
            } else if (id == R.id.nav_print_settings) {
                startActivity(PrintSettingsActivity.newIntent(this));
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        binding.tvDeviceState.setOnClickListener(v -> openDeviceSelect());
    }

    private void initContentViews() {
        binding.tvEditionEntry.setOnClickListener(v -> {
            String schoolId = PreferencesUtils.getString(this, PREFS_KEY_SCHOOL_ID, "");
            android.util.Log.d("MainActivity", "tvEditionEntry click: schoolId=" + schoolId);
            if (schoolId.isEmpty()) {
                showNoSchoolIdHint();
                return;
            }
            startActivity(EditionListActivity.newIntent(this, schoolId));
        });

        binding.llUnfinishedEntry.setOnClickListener(v -> {
            binding.rvTaskCards.setVisibility(
                binding.rvTaskCards.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        binding.rvTaskCards.setLayoutManager(new LinearLayoutManager(this));

        taskCardAdapter = new TaskCardAdapter();
        taskCardAdapter.setOnTaskActionListener(new TaskCardAdapter.OnTaskActionListener() {
            @Override
            public void onContinue(PrintTaskEntity task) {
                if (!checkPrinterConnection()) return;
                if (!checkMaterialExists(task)) return;

                List<Integer> printed = com.org.jzprinter.database.converter.IntegerListConverter
                    .fromString(task.getPrintedPages());
                List<Integer> target = com.org.jzprinter.database.converter.IntegerListConverter
                    .fromString(task.getTargetPages());
                int remaining = target.size() - printed.size();

                new AlertDialog.Builder(MainActivity.this, R.style.mAlertDialog)
                    .setTitle("继续打印")
                    .setMessage(String.format("将发送剩余 %d 页数据给打印机，是否继续？", remaining))
                    .setPositiveButton("继续", (d, w) -> {
                        PrintEngine.getInstance().switchToNewTarget();
                        startActivity(PrintProgressActivity.newResumeIntent(
                            MainActivity.this, task.getTaskId()));
                    })
                    .setNegativeButton("取消", null)
                    .show();
            }

            @Override
            public void onViewDetail(PrintTaskEntity task) {
                startActivity(TaskDetailActivity.newIntent(MainActivity.this, task.getTaskId()));
            }

            @Override
            public void onCancel(PrintTaskEntity task) {
                new AlertDialog.Builder(MainActivity.this, R.style.mAlertDialog)
                    .setTitle("取消打印")
                    .setMessage("确定取消此打印任务？取消后不可恢复。")
                    .setPositiveButton("取消任务", (d, w) -> {
                        task.setStatus(TaskStatus.CANCELLED.getCode());
                        task.setUpdatedAt(System.currentTimeMillis());
                        PrintEngine.getInstance().getDbExecutor().execute(() ->
                            AppDatabase.getInstance(MainActivity.this).printTaskDao().update(task));
                        updateTaskCards();
                    })
                    .setNegativeButton("返回", null)
                    .show();
            }
        });
        binding.rvTaskCards.setAdapter(taskCardAdapter);
    }

    private void showNoSchoolIdHint() {
        new AlertDialog.Builder(this, R.style.mAlertDialog)
            .setTitle("尚未关联学校")
            .setMessage("请通过微信小程序分享学校信息进入本应用，即可使用校本作业打印功能。")
            .setPositiveButton("知道了", null)
            .show();
    }

    private void updateTaskCards() {
        PrintEngine.getInstance().getDbExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<PrintTaskEntity> allTasks = db.printTaskDao().findRecent(50);
            List<PrintTaskEntity> resumable = new java.util.ArrayList<>();
            if (allTasks != null) {
                for (PrintTaskEntity t : allTasks) {
                    int s = t.getStatus();
                    if (s != TaskStatus.COMPLETED.getCode()
                        && s != TaskStatus.CANCELLED.getCode()) {
                        resumable.add(t);
                    }
                }
            }
            int resumableCount = resumable.size();
            int allCount = allTasks != null ? allTasks.size() : 0;

            rbqRunOnUiThread(() -> {
                boolean hasTasks = allCount > 0;
                binding.llUnfinishedEntry.setVisibility(hasTasks ? View.VISIBLE : View.GONE);
                binding.dividerUnfinished.setVisibility(hasTasks ? View.VISIBLE : View.GONE);

                if (resumableCount > 0) {
                    binding.tvUnfinishedBadge.setText(String.valueOf(resumableCount));
                    binding.tvUnfinishedBadge.setVisibility(View.VISIBLE);
                } else {
                    binding.tvUnfinishedBadge.setVisibility(View.GONE);
                }

                taskCardAdapter.setTasks(allTasks);

                if (hasTasks) {
                    binding.rvTaskCards.setVisibility(View.VISIBLE);
                } else {
                    binding.rvTaskCards.setVisibility(View.GONE);
                }

                if (!hasCheckedResumableOnStartup && resumableCount > 0) {
                    hasCheckedResumableOnStartup = true;
                    showStartupResumeDialog(resumable);
                }
            });
        });
    }

    private void showStartupResumeDialog(List<PrintTaskEntity> resumable) {
        StringBuilder message = new StringBuilder("以下任务尚未完成，是否继续打印？\n\n");
        for (PrintTaskEntity task : resumable) {
            List<Integer> printed = com.org.jzprinter.database.converter.IntegerListConverter
                .fromString(task.getPrintedPages());
            List<Integer> target = com.org.jzprinter.database.converter.IntegerListConverter
                .fromString(task.getTargetPages());
            String modeLabel = PrintMode.fromCode(task.getPrintMode()).getLabel();
            message.append(String.format(Locale.getDefault(),
                "● %s - %s (%d/%d页)\n", task.getTargetId(), modeLabel, printed.size(), target.size()));
        }

        new AlertDialog.Builder(this, R.style.mAlertDialog)
            .setTitle("发现未完成的打印任务")
            .setMessage(message.toString())
            .setPositiveButton("继续打印", (d, w) -> {
                PrintTaskEntity latest = resumable.get(0);
                PrintEngine.getInstance().switchToNewTarget();
                startActivity(TaskDetailActivity.newIntent(this, latest.getTaskId()));
            })
            .setNegativeButton("稍后处理", null)
            .show();
    }

    private void showLanguageDialog() {
        String[] items = {getString(R.string.language_chinese), getString(R.string.language_english)};
        int currentIndex = "en".equals(PreferencesUtils.getString(this, PREFS_KEY_LANGUAGE, "zh")) ? 1 : 0;

        new AlertDialog.Builder(this, R.style.mAlertDialog)
            .setTitle(R.string.select_language)
            .setSingleChoiceItems(items, currentIndex, (dialog, which) -> {
                String lang = which == 1 ? "en" : "zh";
                PreferencesUtils.putString(this, PREFS_KEY_LANGUAGE, lang);
                Locale locale = which == 1 ? Locale.ENGLISH : Locale.SIMPLIFIED_CHINESE;
                Locale.setDefault(locale);
                android.content.res.Configuration config = new android.content.res.Configuration();
                config.setLocale(locale);
                getResources().updateConfiguration(config, getResources().getDisplayMetrics());
                dialog.dismiss();
                recreate();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this, R.style.mAlertDialog)
            .setTitle(R.string.menu_about)
            .setMessage(R.string.about_message)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private void showStorageInfo() {
        StorageManager storageManager =
            new StorageManager(this,
                AppDatabase.getInstance(this).printTaskDao());
        long totalSize = storageManager.getMaterialTotalSize();
        String sizeStr = android.text.format.Formatter.formatFileSize(this, totalSize);

        new AlertDialog.Builder(this, R.style.mAlertDialog)
            .setTitle("存储管理")
            .setMessage(String.format(Locale.getDefault(),
                "素材占用空间：%s\n\n清理已完成任务的素材可释放空间。", sizeStr))
            .setPositiveButton("清理已完成", (d, w) -> {
                PrintEngine.getInstance().getDbExecutor().execute(() -> {
                    storageManager.cleanupCompletedMaterials();
                    rbqRunOnUiThread(() -> showToast("清理完成"));
                });
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void openDeviceSelect() {
        Intent intent = new Intent(this, DeviceSelectActivity.class);
        intent.putExtra(com.org.jzprinter.constant.Constant.KEY_INTENT_EXTRA_BACK_CLASS_NAME, MainActivity.class.getName());
        startActivityForResult(intent, REQUEST_DEVICE_CONNECT);
    }

    private void updateDeviceState() {
        ConnectManager connectManager = ConnectManager.share();
        boolean isConnected = Boolean.TRUE.equals(connectManager.isConnected());

        if (isConnected) {
            binding.tvDeviceState.setText(R.string.status_connected);
            binding.tvDeviceState.setTextColor(getResources().getColor(R.color.status_success));
        } else {
            binding.tvDeviceState.setText(R.string.status_disconnected);
            binding.tvDeviceState.setTextColor(getResources().getColor(R.color.status_error));
        }
    }

    private boolean checkPrinterConnection() {
        if (!Boolean.TRUE.equals(ConnectManager.share().isConnected())) {
            new AlertDialog.Builder(this, R.style.mAlertDialog)
                .setTitle("打印机未连接")
                .setMessage("请先连接打印机")
                .setPositiveButton("去连接", (d, w) -> openDeviceSelect())
                .setNegativeButton("取消", null)
                .show();
            return false;
        }
        return true;
    }

    private boolean checkMaterialExists(PrintTaskEntity task) {
        String pagesPath = task.getMaterialPath();
        if (pagesPath == null || !new java.io.File(pagesPath).exists()) {
            new AlertDialog.Builder(this, R.style.mAlertDialog)
                .setTitle("素材不存在")
                .setMessage("素材文件已丢失，请重新下载素材后再打印")
                .setPositiveButton("确定", null)
                .show();
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DEVICE_CONNECT) {
            updateDeviceState();
        }
    }
}
