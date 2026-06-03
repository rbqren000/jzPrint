package com.org.jzprinter.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.mx.mxSdk.ConnectManager;
import com.org.jzprinter.R;
import com.org.jzprinter.databinding.ActivityPrintModeSelectBinding;
import com.org.jzprinter.print.MaterialLoader;
import com.org.jzprinter.print.PrintConfig;
import com.org.jzprinter.print.PrintEngine;
import com.org.jzprinter.print.PrintImagePreparer;
import com.org.jzprinter.print.PrintMode;
import com.org.jzprinter.ui.adapter.PageSelectAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PrintModeSelectActivity extends BaseActivity {

    private static final String EXTRA_SCHOOL_ID = "schoolId";
    private static final String EXTRA_EDITION_ID = "editionId";
    private static final String EXTRA_TARGET_ID = "targetId";
    private static final String EXTRA_EDITION_TYPE = "editionType";
    private static final String EXTRA_TARGET_NAME = "targetName";
    private static final String EXTRA_PAGES_PATH = "pagesPath";
    private static final String EXTRA_BUSINESS_ID = "businessId";
    private static final String EXTRA_EDITION_NAME = "editionName";

    public static Intent newIntent(Context context, String schoolId, String editionId,
                                   String targetId, String targetName, int editionType,
                                   String pagesPath, String businessId, String editionName) {
        Intent intent = new Intent(context, PrintModeSelectActivity.class);
        intent.putExtra(EXTRA_SCHOOL_ID, schoolId);
        intent.putExtra(EXTRA_EDITION_ID, editionId);
        intent.putExtra(EXTRA_TARGET_ID, targetId);
        intent.putExtra(EXTRA_TARGET_NAME, targetName);
        intent.putExtra(EXTRA_EDITION_TYPE, editionType);
        intent.putExtra(EXTRA_PAGES_PATH, pagesPath);
        intent.putExtra(EXTRA_BUSINESS_ID, businessId != null ? businessId : "");
        intent.putExtra(EXTRA_EDITION_NAME, editionName != null ? editionName : "");
        return intent;
    }

    private ActivityPrintModeSelectBinding binding;

    private String schoolId;
    private String editionId;
    private String editionName;
    private String targetId;
    private String targetName;
    private int editionType;
    private String pagesPath;
    private String businessId;

    private MaterialLoader materialLoader;
    private List<Integer> availablePages;

    private PageSelectAdapter pageAdapter;
    private int previewPageCode = -1;
    private Bitmap currentPreviewBitmap;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger previewVersion = new AtomicInteger(0);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrintModeSelectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupStatusBarWithCustomColorResId(R.color.primary_blue);

        schoolId = getIntent().getStringExtra(EXTRA_SCHOOL_ID);
        editionId = getIntent().getStringExtra(EXTRA_EDITION_ID);
        editionName = getIntent().getStringExtra(EXTRA_EDITION_NAME);
        targetId = getIntent().getStringExtra(EXTRA_TARGET_ID);
        targetName = getIntent().getStringExtra(EXTRA_TARGET_NAME);
        editionType = getIntent().getIntExtra(EXTRA_EDITION_TYPE, 1);
        pagesPath = getIntent().getStringExtra(EXTRA_PAGES_PATH);
        businessId = getIntent().getStringExtra(EXTRA_BUSINESS_ID);

        binding.commonAppBar.titleTextView.setText("选择打印页");
        binding.commonAppBar.leftMenuLayout.setOnClickListener(v -> finish());

        initViews();
        loadMaterialInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (previewPageCode >= 0) {
            loadPreviewPage(previewPageCode);
        }
    }

    private void initViews() {
        pageAdapter = new PageSelectAdapter();
        binding.rvPageList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPageList.setAdapter(pageAdapter);

        pageAdapter.setOnPageClickListener(new PageSelectAdapter.OnPageClickListener() {
            @Override
            public void onPageClick(PageSelectAdapter.PageItem item, int position) {
                previewPageCode = item.pageCode;
                loadPreviewPage(item.pageCode);
            }

            @Override
            public void onPageCheckChanged(PageSelectAdapter.PageItem item, int position, boolean checked) {
                updateSelectedCount();
            }
        });

        binding.tvFilterAll.setOnClickListener(v -> {
            pageAdapter.selectAll();
            updateFilterHighlight(0);
            updateSelectedCount();
        });

        binding.tvFilterOdd.setOnClickListener(v -> {
            pageAdapter.selectOdd();
            updateFilterHighlight(1);
            updateSelectedCount();
        });

        binding.tvFilterEven.setOnClickListener(v -> {
            pageAdapter.selectEven();
            updateFilterHighlight(2);
            updateSelectedCount();
        });

        binding.rgMaterialMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (previewPageCode >= 0) {
                loadPreviewPage(previewPageCode);
            }
        });

        binding.btnPrintSettings.setOnClickListener(v ->
            startActivity(PrintSettingsActivity.newIntent(this)));

        binding.btnStartPrint.setOnClickListener(v -> startPrint());
    }

    private void loadMaterialInfo() {
        materialLoader = new MaterialLoader();

        if (pagesPath == null || pagesPath.isEmpty()) {
            showMaterialError("素材路径为空，请重新下载素材");
            return;
        }

        new Thread(() -> {
            availablePages = materialLoader.getAvailablePages(pagesPath);
            uiHandler.post(() -> {
                if (availablePages == null || availablePages.isEmpty()) {
                    showMaterialError("素材未就绪，请先下载素材");
                    return;
                }
                buildPageList();
                showPreviewArea();
            });
        }).start();
    }

    private void showMaterialError(String message) {
        binding.tvPageRange.setText(message);
        binding.tvPageRange.setTextColor(ContextCompat.getColor(this, R.color.status_error));
        binding.btnStartPrint.setEnabled(false);
        binding.rgMaterialMode.setEnabled(false);
    }

    private void buildPageList() {
        int start = availablePages.get(0);
        int end = availablePages.get(availablePages.size() - 1);
        binding.tvPageRange.setText(String.format("页码 %d~%d，共 %d 页", start, end, availablePages.size()));
        binding.tvPageRange.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        List<PageSelectAdapter.PageItem> items = new ArrayList<>();
        for (int pageCode : availablePages) {
            int codeCount = materialLoader.getCodeCount(pagesPath, pageCode);
            items.add(new PageSelectAdapter.PageItem(pageCode, codeCount, true));
        }
        pageAdapter.setItems(items);
        updateFilterHighlight(0);
        updateSelectedCount();

        if (previewPageCode == -1 && !items.isEmpty()) {
            // 默认预览第一个页面，不管是否选中
            previewPageCode = items.get(0).pageCode;
            loadPreviewPage(previewPageCode);
        }
    }

    private void updateFilterHighlight(int mode) {
        int activeColor = ContextCompat.getColor(this, R.color.accent_blue);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary);
        binding.tvFilterAll.setTextColor(mode == 0 ? activeColor : inactiveColor);
        binding.tvFilterOdd.setTextColor(mode == 1 ? activeColor : inactiveColor);
        binding.tvFilterEven.setTextColor(mode == 2 ? activeColor : inactiveColor);
    }

    private void updateSelectedCount() {
        List<Integer> selected = pageAdapter.getSelectedPageCodes();
        int total = pageAdapter.getItemCount();
        if (selected.size() == total) {
            binding.tvSelectedCount.setText("全选");
        } else if (selected.isEmpty()) {
            binding.tvSelectedCount.setText("未选");
        } else {
            binding.tvSelectedCount.setText(String.format("已选 %d/%d", selected.size(), total));
        }
        binding.btnStartPrint.setEnabled(!selected.isEmpty());
    }

    private void showPreviewArea() {
        binding.tvPreviewLabel.setVisibility(View.VISIBLE);
        binding.previewContainer.setVisibility(View.VISIBLE);
    }

    private void loadPreviewPage(int pageCode) {
        int myVersion = previewVersion.incrementAndGet();
        binding.tvPreviewLoading.setVisibility(View.VISIBLE);
        binding.tvPreviewLoading.setText("加载预览...");
        binding.ivPreview.setImageBitmap(null);
        binding.tvPreviewPageInfo.setVisibility(View.VISIBLE);
        binding.tvPreviewPageInfo.setText(String.format("page_%d", pageCode));

        final boolean useCustomMerge = isCustomMergeSelected();
        new Thread(() -> {
            Bitmap page;
            if (useCustomMerge) {
                page = materialLoader.loadPageCustomMerge(pagesPath, pageCode);
            } else {
                page = materialLoader.loadPage(pagesPath, pageCode);
            }

            if (page == null) {
                uiHandler.post(() -> {
                    if (myVersion != previewVersion.get()) return;
                    binding.tvPreviewLoading.setText("页面加载失败");
                    binding.ivPreview.setImageBitmap(null);
                });
                return;
            }

            if (myVersion != previewVersion.get()) {
                page.recycle();
                return;
            }

            Bitmap preview = PrintImagePreparer.prepare(page,
                PrintConfig.getRotationForPage(this, pageCode),
                PrintConfig.getAlignmentForPage(this, pageCode));
            page.recycle();

            uiHandler.post(() -> {
                if (myVersion != previewVersion.get()) {
                    if (preview != null) preview.recycle();
                    return;
                }
                if (isFinishing() || isDestroyed()) {
                    if (preview != null) preview.recycle();
                    return;
                }
                if (currentPreviewBitmap != null && !currentPreviewBitmap.isRecycled()) {
                    currentPreviewBitmap.recycle();
                }
                currentPreviewBitmap = preview;
                binding.ivPreview.setImageBitmap(preview);
                binding.tvPreviewLoading.setVisibility(View.GONE);
            });
        }).start();
    }

    private boolean isCustomMergeSelected() {
        return binding.rgMaterialMode.getCheckedRadioButtonId() == R.id.rbCustomMerge;
    }

    private void startPrint() {
        if (!checkPrinterConnection()) return;
        if (!checkMaterialValid()) return;

        List<Integer> selectedPages = pageAdapter.getSelectedPageCodes();
        if (selectedPages.isEmpty()) {
            showToast("请至少选择一页");
            return;
        }

        PrintEngine engine = PrintEngine.getInstance();
        engine.setUseCustomMerge(isCustomMergeSelected());
        engine.setOddPageOnRight(PrintConfig.isOddPageOnRight(this));
        engine.setLeftBottomToTop(PrintConfig.isLeftBottomToTop(this));
        engine.setRightBottomToTop(PrintConfig.isRightBottomToTop(this));
        engine.setCustomTargetPages(selectedPages);

        PrintMode printMode = inferPrintMode(selectedPages);
        Intent intent = PrintProgressActivity.newIntent(this,
            schoolId, editionId, targetId, targetName,
            editionType, printMode.getCode(), pagesPath, -1L, businessId, editionName);
        startActivity(intent);
        finish();
    }

    private PrintMode inferPrintMode(List<Integer> pages) {
        boolean allOdd = true, allEven = true;
        for (int p : pages) {
            if (p % 2 == 0) allOdd = false;
            else allEven = false;
        }
        if (allOdd) return PrintMode.ODD;
        if (allEven) return PrintMode.EVEN;
        return PrintMode.ALL;
    }

    private boolean checkPrinterConnection() {
        if (!Boolean.TRUE.equals(ConnectManager.share().isConnected())) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("打印机未连接")
                .setMessage("请先连接打印机后再开始打印")
                .setPositiveButton("去连接", (d, w) ->
                    startActivity(new Intent(this, DeviceSelectActivity.class)))
                .setNegativeButton("取消", null)
                .show();
            return false;
        }
        return true;
    }

    private boolean checkMaterialValid() {
        if (pagesPath == null || pagesPath.isEmpty()) {
            showToast("素材路径无效，请重新下载");
            return false;
        }
        if (availablePages == null || availablePages.isEmpty()) {
            showToast("素材未就绪，请先下载");
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacksAndMessages(null);
        if (currentPreviewBitmap != null && !currentPreviewBitmap.isRecycled()) {
            currentPreviewBitmap.recycle();
            currentPreviewBitmap = null;
        }
    }
}
