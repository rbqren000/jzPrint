package com.org.jzprinter.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.mx.mxSdk.ConnectManager;
import com.org.jzprinter.BuildConfig;
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

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_DEVICE_CONNECT = 1002;
    private static final String PREFS_KEY_SCHOOL_ID = "schoolId";
    private static final String PREFS_KEY_LANGUAGE = "app_language";

    private ActivityMainBinding binding;
    private TaskCardAdapter taskCardAdapter;
    private boolean hasCheckedResumableOnStartup = false;

    private ActivityResultLauncher<Intent> scanCameraLauncher;
    private ActivityResultLauncher<String> scanGalleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        registerScanLaunchers();

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
        updateEditionEntry();
        updateTaskCards();
    }

    /** Release 模式下根据是否有 schoolId 切换入口 */
    private void updateEditionEntry() {
        if (BuildConfig.USE_DEV_SCHOOL) return;

        String schoolId = PreferencesUtils.getString(this, PREFS_KEY_SCHOOL_ID, "");
        boolean empty = schoolId.isEmpty();
        binding.tvEditionEntry.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.dividerEntry.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.llEmptyGuide.setVisibility(empty ? View.VISIBLE : View.GONE);
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

    private void ensureDevSchoolId() {
        if (!BuildConfig.USE_DEV_SCHOOL) return;
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

        binding.ivScan.setOnClickListener(v -> showScanOptions());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void initContentViews() {
        binding.tvEditionEntry.setOnClickListener(v -> {
            String schoolId = PreferencesUtils.getString(this, PREFS_KEY_SCHOOL_ID, "");
            if (schoolId.isEmpty()) {
                if (BuildConfig.USE_DEV_SCHOOL) {
                    showNoSchoolIdHint();
                } else {
                    showScanOptions();
                }
                return;
            }
            startActivity(SchoolHomeworkListActivity.newIntent(this, schoolId));
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
                    .setTitle(R.string.main_continue_print)
                    .setMessage(getString(R.string.main_resume_prompt, remaining))
                    .setPositiveButton(R.string.main_continue_btn, (d, w) -> {
                        PrintEngine.getInstance().switchToNewTarget();
                        startActivity(PrintProgressActivity.newResumeIntent(
                            MainActivity.this, task.getTaskId()));
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
            }

            @Override
            public void onViewDetail(PrintTaskEntity task) {
                startActivity(TaskDetailActivity.newIntent(MainActivity.this, task.getTaskId()));
            }

            @Override
            public void onCancel(PrintTaskEntity task) {
                new AlertDialog.Builder(MainActivity.this, R.style.mAlertDialog)
                    .setTitle(R.string.main_cancel_task_title)
                    .setMessage(R.string.main_cancel_task_confirm)
                    .setPositiveButton(R.string.main_cancel_task_btn, (d, w) -> {
                        task.setStatus(TaskStatus.CANCELLED.getCode());
                        task.setUpdatedAt(System.currentTimeMillis());
                        PrintEngine.getInstance().getDbExecutor().execute(() ->
                            AppDatabase.getInstance(MainActivity.this).printTaskRepository().update(task));
                        updateTaskCards();
                    })
                    .setNegativeButton(R.string.main_back, null)
                    .show();
            }

            @Override
            public void onDelete(PrintTaskEntity task) {
                new AlertDialog.Builder(MainActivity.this, R.style.mAlertDialog)
                    .setTitle(R.string.main_delete_task_title)
                    .setMessage(getString(R.string.main_delete_task_confirm,
                        task.getTargetName() != null ? task.getTargetName() : task.getTargetId()))
                    .setPositiveButton(R.string.main_delete_btn, (d, w) -> {
                        PrintEngine.getInstance().getDbExecutor().execute(() -> {
                            AppDatabase.getInstance(MainActivity.this).printTaskRepository().delete(task.getTaskId());
                            rbqRunOnUiThread(MainActivity.this::updateTaskCards);
                        });
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
            }
        });
        binding.rvTaskCards.setAdapter(taskCardAdapter);
    }

    private void showNoSchoolIdHint() {
        new AlertDialog.Builder(this, R.style.mAlertDialog)
            .setTitle(R.string.main_no_school_title)
            .setMessage(R.string.main_no_school_msg)
            .setPositiveButton(R.string.main_got_it, null)
            .show();
    }

    private void updateTaskCards() {
        PrintEngine.getInstance().getDbExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<PrintTaskEntity> allTasks = db.printTaskRepository().findRecent(50);
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
        StringBuilder message = new StringBuilder(getString(R.string.main_resume_tasks_prompt) + "\n\n");
        for (PrintTaskEntity task : resumable) {
            List<Integer> printed = com.org.jzprinter.database.converter.IntegerListConverter
                .fromString(task.getPrintedPages());
            List<Integer> target = com.org.jzprinter.database.converter.IntegerListConverter
                .fromString(task.getTargetPages());
            String modeLabel = PrintMode.fromCode(task.getPrintMode()).getLabel(MainActivity.this);
            message.append(getString(R.string.main_resume_task_item,
                task.getTargetId(), modeLabel, printed.size(), target.size()));
            message.append("\n");
        }

        new AlertDialog.Builder(this, R.style.mAlertDialog)
            .setTitle(R.string.main_resume_tasks_title)
            .setMessage(message.toString())
            .setPositiveButton(R.string.main_continue_print, (d, w) -> {
                PrintTaskEntity latest = resumable.get(0);
                PrintEngine.getInstance().switchToNewTarget();
                startActivity(TaskDetailActivity.newIntent(this, latest.getTaskId()));
            })
            .setNegativeButton(R.string.main_later, null)
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
                AppDatabase.getInstance(this).printTaskRepository());
        long totalSize = storageManager.getMaterialTotalSize();
        String sizeStr = android.text.format.Formatter.formatFileSize(this, totalSize);

        new AlertDialog.Builder(this, R.style.mAlertDialog)
            .setTitle(R.string.main_storage_title)
            .setMessage(getString(R.string.main_storage_info, sizeStr))
            .setPositiveButton(R.string.main_storage_cleanup, (d, w) -> {
                PrintEngine.getInstance().getDbExecutor().execute(() -> {
                    storageManager.cleanupCompletedMaterials();
                    rbqRunOnUiThread(() -> showToast(getString(R.string.main_storage_cleaned)));
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
                .setTitle(R.string.main_no_printer_title)
                .setMessage(R.string.main_no_printer_msg)
                .setPositiveButton(R.string.btn_go_connect, (d, w) -> openDeviceSelect())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
            return false;
        }
        return true;
    }

    private boolean checkMaterialExists(PrintTaskEntity task) {
        String pagesPath = task.getMaterialPath();
        if (pagesPath == null || !new java.io.File(pagesPath).exists()) {
            new AlertDialog.Builder(this, R.style.mAlertDialog)
                .setTitle(R.string.main_no_material_title)
                .setMessage(R.string.main_no_material_msg)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
            return false;
        }
        return true;
    }

    // ==================== 扫码功能 ====================

    private void registerScanLaunchers() {
        scanCameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String content = result.getData().getStringExtra(ScanActivity.EXTRA_RESULT);
                    handleScanResult(content);
                }
            });

        scanGalleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    decodeImageUri(uri);
                }
            });
    }

    private void showScanOptions() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_scan, null);

        view.findViewById(R.id.tvScanCamera).setOnClickListener(v -> {
            dialog.dismiss();
            startCameraScan();
        });
        view.findViewById(R.id.tvScanGallery).setOnClickListener(v -> {
            dialog.dismiss();
            scanGalleryLauncher.launch("image/*");
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void startCameraScan() {
        Intent intent = new Intent(this, ScanActivity.class);
        scanCameraLauncher.launch(intent);
    }

    private void decodeImageUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) {
                Toast.makeText(this, R.string.scan_decode_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();

            if (bitmap == null) {
                Toast.makeText(this, R.string.scan_decode_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            String result = decodeBitmap(bitmap);
            bitmap.recycle();
            handleScanResult(result);
        } catch (Exception e) {
            Toast.makeText(this, R.string.scan_decode_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private String decodeBitmap(Bitmap bitmap) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), pixels);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
            MultiFormatReader reader = new MultiFormatReader();
            reader.setHints(new java.util.EnumMap<>(java.util.Map.of(
                DecodeHintType.POSSIBLE_FORMATS, java.util.Collections.singletonList(
                    com.google.zxing.BarcodeFormat.QR_CODE))));
            Result result = reader.decode(binaryBitmap);
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    private void handleScanResult(@Nullable String content) {
        if (content == null || content.isEmpty()) {
            Toast.makeText(this, R.string.scan_invalid_code, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!content.startsWith("jzprint://share")) {
            Toast.makeText(this, R.string.scan_invalid_code, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(content));
        intent.setClass(this, DeepLinkActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DEVICE_CONNECT) {
            updateDeviceState();
        }
    }
}
