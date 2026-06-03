package com.org.jzprinter.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.org.jzprinter.R;
import com.org.jzprinter.network.AuthManager;
import com.org.jzprinter.print.MaterialPathBuilder;
import com.org.jzprinter.utils.Storage.PreferencesUtils;

import java.io.File;

public class DeepLinkActivity extends BaseActivity {

    private static final String SCHEME = "jzprint";
    private static final String HOST = "share";

    public static Uri buildDeepLinkUri(String schoolId, String editionId,
                                        String targetId, String businessId,
                                        int editionType) {
        Uri.Builder builder = new Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST)
            .appendQueryParameter("schoolId", schoolId);
        if (editionId != null && !editionId.isEmpty()) {
            builder.appendQueryParameter("editionId", editionId);
        }
        if (targetId != null && !targetId.isEmpty()) {
            builder.appendQueryParameter("targetId", targetId);
        }
        if (businessId != null && !businessId.isEmpty()) {
            builder.appendQueryParameter("businessId", businessId);
        }
        if (editionType > 0) {
            builder.appendQueryParameter("editionType", String.valueOf(editionType));
        }
        return builder.build();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data == null) {
            finish();
            return;
        }

        String schoolId = data.getQueryParameter("schoolId");
        if (schoolId == null || schoolId.isEmpty()) {
            showToast("无效的分享链接");
            finish();
            return;
        }

        PreferencesUtils.putString(this, "schoolId", schoolId);

        String editionId = data.getQueryParameter("editionId");
        String targetId = data.getQueryParameter("targetId");
        String businessId = data.getQueryParameter("businessId");
        String editionTypeStr = data.getQueryParameter("editionType");
        int editionType = 1;
        if (editionTypeStr != null) {
            try {
                editionType = Integer.parseInt(editionTypeStr);
            } catch (NumberFormatException e) {
                editionType = 1;
            }
            if (editionType < 1 || editionType > 2) editionType = 1;
        }
        int resolvedEditionType = editionType;

        if (!AuthManager.getInstance(this).isTokenValid()) {
            AuthManager.getInstance(this).login(new AuthManager.LoginCallback() {
                @Override
                public void onSuccess() {
                    navigateAfterAuth(schoolId, editionId, targetId, businessId, resolvedEditionType);
                }

                @Override
                public void onError(String error) {
                    showToast(getString(R.string.error_load_failed, error));
                    finish();
                }
            });
            return;
        }

        navigateAfterAuth(schoolId, editionId, targetId, businessId, resolvedEditionType);
    }

    private void navigateAfterAuth(String schoolId, String editionId,
                                    String targetId, String businessId,
                                    int editionType) {
        if (targetId != null && !targetId.isEmpty() && editionId != null && !editionId.isEmpty()) {
            String pagesPath = MaterialPathBuilder.getPagesPath(
                this, schoolId, editionId, editionType, targetId);
            File pagesDir = new File(pagesPath);
            boolean materialReady = pagesDir.exists() && pagesDir.isDirectory()
                && pagesDir.list() != null && pagesDir.list().length > 0;

            if (materialReady) {
                startActivity(PrintModeSelectActivity.newIntent(this,
                    schoolId, editionId, targetId, targetId, editionType, pagesPath, businessId, ""));
            } else {
                startActivity(StudentListActivity.newIntent(this, schoolId, editionId, editionType, ""));
            }
        } else if (editionId != null && !editionId.isEmpty()) {
            startActivity(StudentListActivity.newIntent(this, schoolId, editionId, editionType, ""));
        } else {
            startActivity(SchoolHomeworkListActivity.newIntent(this, schoolId));
        }
        finish();
    }
}
