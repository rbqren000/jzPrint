package com.org.jzprinter.ui.activity;

import static com.org.jzprinter.constant.Constant.KEY_INTENT_EXTRA_BACK_CLASS_NAME;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import com.org.jzprinter.R;
import com.org.jzprinter.XXPermissions.PermissionInterceptor;
import com.org.jzprinter.databinding.ActivityApBinding;
import com.org.jzprinter.databinding.DialogInstructionApLayoutBinding;
import com.org.jzprinter.manager.ParameterManager;
import com.org.jzprinter.manager.RBQAppManager;
import com.org.jzprinter.utils.Storage.PreferencesUtils;
import com.org.jzprinter.utils.location.GPSUtils;
import com.org.jzprinter.widget.CustomDialog.DialogFactory;
import com.org.jzprinter.widget.CustomDialog.RBQProgressDialog;
import com.org.jzprinter.utils.WifiUtils;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.mx.mxSdk.Conditions.ConditionAction;
import com.mx.mxSdk.Conditions.ConditionCallback;
import com.mx.mxSdk.Conditions.ConditionCheckerImpl;
import com.mx.mxSdk.Conditions.ConditionManager;
import com.mx.mxSdk.ConnModel;
import com.mx.mxSdk.ConnectManager;
import com.mx.mxSdk.Device;
import com.mx.mxSdk.MxUtils;
import com.mx.mxSdk.RepeatingTaskWithTimeout;
import com.mx.mxSdk.Utils.RBQLog;
import java.util.List;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class ApActivity extends BaseActivity{

	private static final String TAG = ApActivity.class.getSimpleName();

	private final Handler mHandler = new Handler(Looper.getMainLooper());

	private static final float scanTimeOut = 60;

    private String ssid;
	//尝试连接次数
	private long startTime = 0;
	private static final long maxTryTime = 60*1000;

	String backClassName;

	private final RBQProgressDialog progressDialog = new RBQProgressDialog();

	private final RepeatingTaskWithTimeout fetchSsidTask = new RepeatingTaskWithTimeout(new Runnable() {
		@Override
		public void run() {
			fetchSSID();
		}
	}, 0, 0.2,scanTimeOut, new RepeatingTaskWithTimeout.Callback() {
		@Override
		public void onTaskStarted() {
			RBQLog.i("-----开始获取ssid-----");
		}

		@Override
		public void onTaskStopped() {
			RBQLog.i("-----停止获取ssid-----");
		}

		@Override
		public void onTaskError(Exception e) {

		}

		@Override
		public void onTaskTimeout() {

		}
	});

	private final ConditionManager conditionManager = new ConditionManager();
	private final ActivityResultLauncher<Intent> gpsActivityResultLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			new ActivityResultCallback<ActivityResult>() {
				@Override
				public void onActivityResult(ActivityResult result) {
					if (GPSUtils.isLocationEnabled(ApActivity.this)){
						conditionManager.onConditionResult(ApActivity.this,gpsAction.getKey(),true);
					}
				}
			}
	);

	private final ActivityResultLauncher<Intent> wifiSettingsLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			new ActivityResultCallback<ActivityResult>() {
				@Override
				public void onActivityResult(ActivityResult result) {
//					if (result.getResultCode() == Activity.RESULT_OK) {
//						// 处理结果
//					}
					ssid = WifiUtils.getCurrentSsid(ApActivity.this).replace("\"", "").replace("\"", "");
				}
			}
	);


	private void checkPermission(){
		conditionManager.checkConditions(this, new ConditionCallback() {
			@Override
			public void onAllConditionsMet() {
				ssid = WifiUtils.getCurrentSsid(ApActivity.this).replace("\"", "").replace("\"", "");
			}

			@Override
			public void onConditionsUnmet(List<String> unmetConditions) {

			}
		});
	}

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
		public void requestCondition(Activity activity, ConditionManager conditionManager) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.mAlertDialog);
			builder.setMessage(getResources().getString(R.string.turnOnBluetoothHint))
					.setCancelable(false)
					.setNegativeButton(R.string.custom_dialog_cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					})
					.setPositiveButton(R.string.custom_dialog_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							ConnectManager.share().enable();
							conditionManager.onConditionResult(ApActivity.this,getKey(),true);
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
			LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
			return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
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


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        ActivityApBinding binding = ActivityApBinding.inflate(LayoutInflater.from(this));
		setContentView(binding.getRoot());

		setupStatusBarWithCustomColorResId(R.color.primary_blue);

		backClassName = getIntent().getStringExtra(KEY_INTENT_EXTRA_BACK_CLASS_NAME);
		RBQLog.i(TAG,"backClassName:"+backClassName);

		binding.appBar.titleTextView.setText(R.string.title_ap);
		binding.appBar.tvRight.setText("");
		
		binding.appBar.leftMenuLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				RBQAppManager.share().killActivity(ApActivity.this);
			}
		});
		
		binding.btnAp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
				wifiSettingsLauncher.launch(intent);
			}
		});
		binding.btnNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				progressDialog.show(ApActivity.this,"",getString(R.string.connecting));
				startScan();
			}

		});

		ConnectManager.share().registerConnModelDiscoverListeners(onConnModelDiscoverListener);
		ConnectManager.share().registerDeviceConnectListener(onDeviceConnectListener);

		conditionManager.addChecker(new ConditionCheckerImpl(conditionManager, bluetoothIsOpenAction));
		conditionManager.addChecker(new ConditionCheckerImpl(conditionManager, bluetoothAction));
		conditionManager.addChecker(new ConditionCheckerImpl(conditionManager, gpsAction));

		checkPermission();

		if(!PreferencesUtils.getBoolean(ApActivity.this, ParameterManager.noMoreTipApInstructions,false)) {
			showInstructionApDialog();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		boolean isConnected = ConnectManager.share().isConnected();
		if (progressDialog.isShowing()&&!isConnected){
			//扫描60秒
			RBQLog.i("页面显示，重新开始扫描");
			startScan();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		stopScan();
	}

	private void startScan(){
		if (!conditionManager.allConditionsProcessed(this)){
			return;
		}
		startTime = System.currentTimeMillis();
		fetchSsidTask.start();
		ConnectManager.share().discoverConnModel(scanTimeOut);
	}

	private void stopScan(){
		fetchSsidTask.stop();
		ConnectManager.share().cancelDiscoverConnModel();
	}

	private final ConnectManager.OnDeviceConnectListener onDeviceConnectListener = new ConnectManager.OnDeviceConnectListener() {
		@Override
		public void onDeviceConnectStart(Device device) {

		}

		@Override
		public void onDeviceConnectSucceed(Device device) {
			//连接成功后关闭页面
			RBQLog.i("AP设备连接成功");

			progressDialog.dismiss();

			//显示连接成功提示
			showToast(getString(R.string.connect_success));

			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					RBQAppManager.share().killCurrentWithNavigateToActivity(backClassName);
				}
			},200);
		}

		@Override
		public void onDeviceDisconnect(Device device) {

			long currentTime = System.currentTimeMillis();
			long timeDifference = currentTime - startTime; // 计算时间差

			if (timeDifference < maxTryTime && device != null) {
				RBQLog.i("连接断开,尝试连接 startTime:" + startTime + "; currentTime:" + currentTime + "; maxTryTime:" + maxTryTime + "; timeDifference:" + timeDifference + "ms; --->继续开始尝试连接");
				ConnectManager.share().connect(device);
			} else {
				// 表示连接失败，不再尝试
				RBQLog.i("AP连接断开, timeDifference:" + timeDifference + "ms");
				progressDialog.dismiss();

				showToast(getString(R.string.connect_fail));
			}
		}

		@Override
		public void onDeviceConnectFail(Device device, String error) {

			long currentTime = System.currentTimeMillis();
			long timeDifference = currentTime - startTime; // 计算时间差

			if (timeDifference < maxTryTime && device != null) {
				RBQLog.i("连接失败，尝试连接 startTime:" + startTime + "; currentTime:" + currentTime + "; maxTryTime:" + maxTryTime + "; timeDifference:" + timeDifference + "ms; --->继续开始尝试连接");
				ConnectManager.share().connect(device);
			} else {
				// 表示连接失败，不再尝试
				RBQLog.i("AP连接失败, timeDifference:" + timeDifference + "ms");
				progressDialog.dismiss();

				showToast(getString(R.string.connect_fail));
			}
		}
	};

	private final ConnectManager.OnConnModelDiscoverListener onConnModelDiscoverListener = new ConnectManager.OnConnModelDiscoverListener() {
		@Override
		public void onConnModelStartDiscover() {
			RBQLog.i("--------开始扫描ConnModel--------");
		}

		@Override
		public void onConnModelDiscovered(ConnModel connModel) {

			RBQLog.i("ssid:"+ssid+";"+connModel.getMac());

			if (!TextUtils.isEmpty(ssid) && MxUtils.isSSIDConnModel(connModel,ssid)){

				stopScan();

				RBQLog.i("-------->>>开始连接AP设备>>>------>>> ssid:"+ssid + "; mac:"+connModel.getMac());
				Device device = Device.createApDevice(ssid,connModel.getBleDevice(),connModel.getMac(),connModel.getConnTypes(),connModel.getDeviceModel());
				ConnectManager.share().connect(device);
			}
		}

		@Override
		public void onConnModelStopDiscover() {
			RBQLog.i("--------停止扫描ConnModel--------");
		}
	};

	public void fetchSSID() {

		String currentSsid = WifiUtils.getCurrentSsid(this);
		// 连接的SSID有变化时，会赋值
		String tempSsid = MxUtils.isPrinterAp(currentSsid) ? currentSsid : null;

		if (tempSsid != null && !tempSsid.equals(ssid)) {
			Log.d(TAG, "【连接的WiFi发生变化】①ssid:" + ssid + "; currentSsid:" + currentSsid + "; tempSsid:" + tempSsid);
			ssid = tempSsid;
		} else {
			if (ssid != null && !ssid.equals(tempSsid)) {
				Log.d(TAG, "【连接的WiFi发生变化】②ssid为null");
				ssid = null;
			}
		}
	}

	//说明按钮
	private void showInstructionApDialog() {
		DialogInstructionApLayoutBinding dialogInstructionLayoutBinding = DialogInstructionApLayoutBinding.inflate(LayoutInflater.from(this));
		Dialog dialog = DialogFactory.share().showCustomDialog(this,dialogInstructionLayoutBinding.getRoot(),true);
		dialogInstructionLayoutBinding.cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
				PreferencesUtils.putBoolean(ApActivity.this, ParameterManager.noMoreTipApInstructions,isChecked);
			}
		});
		dialogInstructionLayoutBinding.tvKnow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		ConnectManager.share().unregisterConnModelDiscoverListeners(onConnModelDiscoverListener);
		ConnectManager.share().unregisterDeviceConnectListener(onDeviceConnectListener);
	}

}