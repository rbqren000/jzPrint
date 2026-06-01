package com.org.jzprinter.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.org.jzprinter.R;
import com.org.jzprinter.databinding.ActivityEditionListBinding;
import com.org.jzprinter.network.Api;
import com.org.jzprinter.network.ApiClientFactory;
import com.org.jzprinter.network.AuthManager;
import com.org.jzprinter.network.model.Edition;
import com.org.jzprinter.network.model.Semester;
import com.org.jzprinter.ui.adapter.EditionAdapter;

import java.util.List;

public class EditionListActivity extends BaseActivity {

    private static final String TAG = "EditionList";
    private static final String EXTRA_SCHOOL_ID = "schoolId";

    public static Intent newIntent(Context context, String schoolId) {
        Intent intent = new Intent(context, EditionListActivity.class);
        intent.putExtra(EXTRA_SCHOOL_ID, schoolId);
        return intent;
    }

    private ActivityEditionListBinding binding;
    private Api apiClient;
    private EditionAdapter adapter;
    private String schoolId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");
        binding = ActivityEditionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupStatusBarWithCustomColorResId(R.color.primary_blue);

        binding.commonAppBar.titleTextView.setText("校本作业");
        binding.commonAppBar.leftMenuLayout.setOnClickListener(v -> finish());

        schoolId = getIntent().getStringExtra(EXTRA_SCHOOL_ID);
        Log.d(TAG, "schoolId=" + schoolId);

        apiClient = ApiClientFactory.create(this);

        binding.rvEditionList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EditionAdapter();
        adapter.setOnEditionClickListener(edition -> {
            if (edition.supportsStudent() && edition.supportsPrepareCode()) {
                new android.app.AlertDialog.Builder(this, R.style.mAlertDialog)
                    .setTitle(edition.editionName)
                    .setItems(new String[]{"学生列表", "预铺码列表"}, (d, which) -> {
                        int type = which == 0 ? 1 : 2;
                        startActivity(StudentListActivity.newIntent(this,
                            schoolId, edition.editionId, type));
                    })
                    .show();
            } else if (edition.supportsStudent()) {
                startActivity(StudentListActivity.newIntent(this,
                    schoolId, edition.editionId, 1));
            } else {
                startActivity(StudentListActivity.newIntent(this,
                    schoolId, edition.editionId, 2));
            }
        });
        binding.rvEditionList.setAdapter(adapter);

        loadEditions(schoolId);
    }

    private void showLoading() {
        binding.pbLoading.setVisibility(View.VISIBLE);
        binding.rvEditionList.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(View.GONE);
    }

    private void showContent() {
        binding.pbLoading.setVisibility(View.GONE);
        binding.rvEditionList.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);
    }

    private void showEmpty(String message) {
        binding.pbLoading.setVisibility(View.GONE);
        binding.rvEditionList.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(View.VISIBLE);
        binding.tvEmpty.setText(message);
    }

    private void loadEditions(String schoolId) {
        showLoading();
        Log.d(TAG, "loadEditions: tokenValid=" + AuthManager.getInstance(this).isTokenValid());
        if (!AuthManager.getInstance(this).isTokenValid()) {
            AuthManager.getInstance(this).login(new AuthManager.LoginCallback() {
                @Override
                public void onSuccess() {
                    doFetchEditions(schoolId);
                }

                @Override
                public void onError(String error) {
                    rbqRunOnUiThread(() -> showEmpty("认证失败: " + error));
                }
            });
            return;
        }
        doFetchEditions(schoolId);
    }

    private void doFetchEditions(String schoolId) {
        apiClient.fetchSemesters(schoolId, new Api.SemesterCallback() {
            @Override
            public void onSuccess(List<Semester> semesters) {
                rbqRunOnUiThread(() -> {
                    boolean hasData = false;
                    for (Semester sem : semesters) {
                        if (sem.editionList != null && !sem.editionList.isEmpty()) {
                            hasData = true;
                            break;
                        }
                    }
                    if (hasData) {
                        adapter.setItems(semesters);
                        showContent();
                    } else {
                        showEmpty("暂无校本作业数据");
                    }
                });
            }

            @Override
            public void onError(String error) {
                rbqRunOnUiThread(() -> showEmpty("加载失败: " + error));
            }
        });
    }
}
