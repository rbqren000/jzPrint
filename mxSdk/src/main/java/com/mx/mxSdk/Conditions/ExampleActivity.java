package com.mx.mxSdk.Conditions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.List;

public class ExampleActivity extends AppCompatActivity {
    private ConditionManager conditionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_example);

        conditionManager = new ConditionManager();

        // 添加单个权限
        conditionManager.addChecker(new ConditionCheckerImpl(conditionManager, gpsAction));
        conditionManager.addChecker(new ConditionCheckerImpl(conditionManager, storageAction));
        conditionManager.addChecker(new ConditionCheckerImpl(conditionManager, cameraAction));

        // 添加多权限组合
        conditionManager.addChecker(new ConditionCheckerImpl(conditionManager, multiConditionAction));
    }

    ConditionAction gpsAction = new ConditionAction() {
        @Override
        public String getKey() {
            return "gpsAction";
        }

        @Override
        public boolean isConditionMet(Activity activity) {
            LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }

        @Override
        public void onConditionMet() {

        }

        @Override
        public void requestCondition(Activity activity, ConditionManager conditionManager) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage("需要启用 GPS 定位服务")
                    .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("前往设置", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        activity.startActivity(intent);
                    })
                    .show();
        }
    };

    ConditionAction storageAction = new ConditionAction() {
        @Override
        public String getKey() {
            return "storageAction";
        }

        @Override
        public boolean isConditionMet(Activity activity) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public void onConditionMet() {

        }

        @Override
        public void requestCondition(Activity activity, ConditionManager conditionManager) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
    };

    ConditionAction cameraAction = new ConditionAction() {
        @Override
        public String getKey() {
            return "cameraAction";
        }

        @Override
        public boolean isConditionMet(Activity activity) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public void onConditionMet() {

        }

        @Override
        public void requestCondition(Activity activity, ConditionManager conditionManager) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, 102);
        }
    };

    ConditionAction multiConditionAction = new MultiConditionAction(Arrays.asList(
            gpsAction,
            storageAction,
            cameraAction
    ));


    @Override
    protected void onStart() {
        super.onStart();

        // 检查权限
        conditionManager.checkConditions(this, new ConditionCallback() {
            @Override
            public void onAllConditionsMet() {
                Toast.makeText(ExampleActivity.this, "所有权限已通过", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConditionsUnmet(List<String> unmetConditions) {
                Toast.makeText(ExampleActivity.this, "以下权限未通过：" + unmetConditions, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 处理权限请求的结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101 || requestCode == 102) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                conditionManager.onConditionResult(this,permissions[0], true);
            } else {
                conditionManager.onConditionResult(this,permissions[0], false);
            }
        }
    }
}







