package com.org.jzprinter.ui.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.org.jzprinter.R;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.mx.mxSdk.Conditions.ConditionAction;
import com.mx.mxSdk.Conditions.ConditionCallback;
import com.mx.mxSdk.Conditions.ConditionCheckerImpl;
import com.mx.mxSdk.Conditions.ConditionManager;
import com.mx.mxSdk.ConnectManager;
import com.org.jzprinter.XXPermissions.PermissionInterceptor;
import com.org.jzprinter.utils.location.GPSUtils;

import java.util.List;

public class BaseBluetoothPermissionActivity extends BaseActivity{

     public ConditionManager bluetoothPermissionConditionManager = new ConditionManager();
     ActivityResultLauncher<Intent> gpsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    // 在返回后检查GPS状态
                    if (GPSUtils.isLocationEnabled(BaseBluetoothPermissionActivity.this)){
                        bluetoothPermissionConditionManager.onConditionResult(BaseBluetoothPermissionActivity.this,gpsAction.getKey(),true);
                    }else {
//						show("");
                    }
                }
            }
    );

    // 定义蓝牙权限操作
    ConditionAction bluetoothAction = new ConditionAction() {
        @Override
        public String getKey() {
            return "bluetoothAction";
        }

        @Override
        public boolean isConditionMet(Activity activity) {
            return XXPermissions.isGranted(activity, Permission.ACCESS_COARSE_LOCATION,
                    Permission.ACCESS_FINE_LOCATION,
                    Permission.BLUETOOTH_CONNECT,
                    Permission.BLUETOOTH_SCAN);
        }

        @Override
        public void onConditionMet() {

        }

        @Override
        public void requestCondition(Activity activity, ConditionManager manager) {
            XXPermissions.with(activity)
                    .permission(Permission.ACCESS_COARSE_LOCATION,
                            Permission.ACCESS_FINE_LOCATION,
                            Permission.BLUETOOTH_CONNECT,
                            Permission.BLUETOOTH_SCAN)
                    .interceptor(new PermissionInterceptor(getString(R.string.permissionPosition)))
                    .request(new OnPermissionCallback() {
                        @Override
                        public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                            if (!allGranted) {
                                showToast(R.string.bluetooth_reject);
                                return;
                            }
                            manager.onConditionResult(activity,getKey(),true);
                        }
                    });
        }
    };

    ConditionAction bluetoothIsOpenAction = new ConditionAction() {
        @Override
        public String getKey() {
            return "bluetoothIsOpenAction";
        }

        @Override
        public boolean isConditionMet(Activity activity) {
            return ConnectManager.share().isEnable();
        }

        @Override
        public void onConditionMet() {

        }

        @Override
        public void requestCondition(Activity activity, ConditionManager manager) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.mAlertDialog);
            builder.setMessage(getResources().getString(R.string.turnOnBluetoothHint))
                    .setCancelable(false)
                    .setNegativeButton(R.string.custom_dialog_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showToast("蓝牙未打开可能无使用打印机");
                        }
                    })
                    .setPositiveButton(R.string.custom_dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ConnectManager.share().enable();
                            manager.onConditionResult(activity,getKey(),true);
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
        }
    };

    // 定义GPS权限操作
    ConditionAction gpsAction = new ConditionAction() {
        @Override
        public String getKey() {
            return "gpsAction";
        }

        @Override
        public boolean isConditionMet(Activity activity) {
            return GPSUtils.isLocationEnabled(activity);
        }

        @Override
        public void onConditionMet() {

        }

        @Override
        public void requestCondition(Activity activity, ConditionManager manager) {

            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.mAlertDialog);
            builder.setMessage(R.string.locationPermission_wifi)
                    .setCancelable(false)
                    .setNegativeButton(R.string.custom_dialog_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setPositiveButton(R.string.custom_dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            gpsActivityResultLauncher.launch(intent);
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
        }
    };

    public void checkScanPermission(){
        bluetoothPermissionConditionManager.checkConditions(this, new ConditionCallback() {
            @Override
            public void onAllConditionsMet() {
            }

            @Override
            public void onConditionsUnmet(List<String> unmetConditions) {

            }
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addBluetoothPermissionCondition();

    }

    private synchronized void  addBluetoothPermissionCondition(){
        bluetoothPermissionConditionManager.addChecker(new ConditionCheckerImpl(bluetoothPermissionConditionManager, bluetoothIsOpenAction));
        bluetoothPermissionConditionManager.addChecker(new ConditionCheckerImpl(bluetoothPermissionConditionManager, bluetoothAction));
        bluetoothPermissionConditionManager.addChecker(new ConditionCheckerImpl(bluetoothPermissionConditionManager, gpsAction));
    }
}
