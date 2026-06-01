package com.org.jzprinter.ui.activity;

import static com.org.jzprinter.constant.Constant.KEY_INTENT_EXTRA_BACK_CLASS_NAME;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;

import com.org.jzprinter.R;
import com.org.jzprinter.XXPermissions.PermissionInterceptor;
import com.org.jzprinter.constant.Constant;
import com.org.jzprinter.databinding.ActivityWifiConfigurationBinding;
import com.org.jzprinter.databinding.DialogInstructionLayout0Binding;
import com.org.jzprinter.databinding.DialogStartConfigLayoutBinding;
import com.org.jzprinter.databinding.ToastViewBinding;
import com.org.jzprinter.manager.ParameterManager;
import com.org.jzprinter.manager.RBQAppManager;
import com.org.jzprinter.utils.WifiUtils;
import com.org.jzprinter.utils.location.GPSUtils;
import com.org.jzprinter.widget.CustomDialog.DialogFactory;
import com.org.jzprinter.widget.CustomDialog.RBQProgressDialog;
import com.org.jzprinter.widget.Toast.ToastView;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.mx.mxSdk.ConnectManager;
import com.mx.mxSdk.Device;
import com.mx.mxSdk.DistNetDevice;
import com.mx.mxSdk.Conditions.ConditionAction;
import com.mx.mxSdk.Conditions.ConditionCallback;
import com.mx.mxSdk.Conditions.ConditionCheckerImpl;
import com.mx.mxSdk.Conditions.ConditionManager;
import com.mx.mxSdk.Utils.RBQLog;
import java.util.List;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;

public class WifiConfigurationActivity extends BaseActivity{

	private static final String TAG = WifiConfigurationActivity.class.getSimpleName();

	private ActivityWifiConfigurationBinding binding;
	private DistNetDevice distNetDevice;

	String backClassName;

	private final Handler mainHandler = new Handler(Looper.getMainLooper());

	private Dialog dialog;
	private int reTryTime = 0;

	RBQProgressDialog progressDialog = new RBQProgressDialog();

	private final ConditionManager conditionManager = new ConditionManager();
	private final ActivityResultLauncher<Intent> gpsActivityResultLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			new ActivityResultCallback<ActivityResult>() {
				@Override
				public void onActivityResult(ActivityResult result) {
					if (GPSUtils.isLocationEnabled(WifiConfigurationActivity.this)){
						conditionManager.onConditionResult(WifiConfigurationActivity.this,gpsAction.getKey(),true);
					}
				}
			}
	);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityWifiConfigurationBinding.inflate(LayoutInflater.from(this));
		setContentView(binding.getRoot());

		setupStatusBarWithCustomColorResId(R.color.primary_blue);

		backClassName = getIntent().getStringExtra(KEY_INTENT_EXTRA_BACK_CLASS_NAME);
		RBQLog.i(TAG,"backClassName:"+backClassName);

		hideKeyboardOnTouch(binding.getRoot(),this);
		
		/*返回*/
		binding.appBar.leftMenuLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				RBQAppManager.share().killActivity(WifiConfigurationActivity.this);
			}
		});
		binding.appBar.tvRight.setText("");
		distNetDevice = getIntent().getParcelableExtra("distNetDevice");//携带有蓝牙信息的device
		RBQLog.i("收到的要配网的distNetDevice"+distNetDevice);
		initView();
		
		ConnectManager.share().registerDistributionNetworkListener(onDistributionNetworkListener);
		ConnectManager.share().registerDeviceConnectListener(onDeviceConnectListener);
		binding.etWifi.addCustomTextWatcher(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		binding.etPwd.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		conditionManager.addChecker(new ConditionCheckerImpl(conditionManager, bluetoothIsOpenAction));
		conditionManager.addChecker(new ConditionCheckerImpl(conditionManager, bluetoothAction));
		conditionManager.addChecker(new ConditionCheckerImpl(conditionManager, gpsAction));

		checkPermission();
		
	}


	private void checkPermission(){
		conditionManager.checkConditions(this, new ConditionCallback() {
			@Override
			public void onAllConditionsMet() {

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
			String ssid = WifiUtils.getCurrentSsid(WifiConfigurationActivity.this).replace("\"", "").replace("\"", "");
			RBQLog.i("--------->得到蓝牙权限，---------> 得到ssid:"+ssid);
			binding.etWifi.setText(ssid);
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
							conditionManager.onConditionResult(activity,getKey(),true);
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

	ConnectManager.OnDeviceConnectListener onDeviceConnectListener = new ConnectManager.OnDeviceConnectListener() {
		@Override
		public void onDeviceConnectStart(Device device) {
			progressDialog.show(WifiConfigurationActivity.this,"",getString(R.string.connecting));
		}
		
		@Override
		public void onDeviceConnectSucceed(Device device) {

			progressDialog.dismiss();

			showToast(getString(R.string.connect_success), AppCompatResources.getDrawable(WifiConfigurationActivity.this,R.mipmap.ic_check_blue));

			//连接成功后，记录下mac，下次可以自动连接
			ParameterManager.saveDevice(WifiConfigurationActivity.this,device);

			mainHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					RBQAppManager.share().killCurrentWithNavigateToActivity(backClassName);
				}
			},300);
		}
		
		@Override
		public void onDeviceDisconnect(Device device) {
			progressDialog.dismiss();
		}
		
		@Override
		public void onDeviceConnectFail(Device device, String error) {
			progressDialog.dismiss();
			showToast(getString(R.string.connect_fail),AppCompatResources.getDrawable(WifiConfigurationActivity.this,R.mipmap.ic_close_red));
		}
	};

	private void updateLlTvsState(){
		int visible = binding.tvTitle.isChecked() ? View.GONE : View.VISIBLE;
		binding.llTvs.setVisibility(visible);
	}
	
	private void initView() {
		updateLlTvsState();
		binding.tvTitle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				binding.tvTitle.setChecked(!binding.tvTitle.isChecked());
				updateLlTvsState();
			}
		});
		binding.tvInstruction.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showInstructionDialog();
			}
		});
		try {
			String str = getString(R.string.config_wifi2);
			binding.tvAttention.setText(str);
			int aStart = str.indexOf("2.4GHz");
			int aEnd = aStart + 6;//2.4GHz长度是6
			int bStart = str.indexOf("2.4GHz", aStart + 6);
			int bEnd = bStart + 6;
			int cEnd = str.indexOf("：");
			SpannableStringBuilder style = new SpannableStringBuilder(str);
			//这个一定要记得设置，不然点击不生效
			binding.tvAttention.setMovementMethod(LinkMovementMethod.getInstance());
			style.setSpan(new ForegroundColorSpan(Color.parseColor("#E86762")), 0, cEnd + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			style.setSpan(new ForegroundColorSpan(Color.parseColor("#E86762")), aStart, aEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			style.setSpan(new ForegroundColorSpan(Color.parseColor("#E86762")), bStart, bEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			binding.tvAttention.setText(style);
		} catch (Exception e) {
			RBQLog.e(e.getMessage());
		}
		
		binding.distributionNetworkBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String ssid = (binding.etWifi.getText() != null) ? binding.etWifi.getText().toString() : "";
				String password = (binding.etPwd.getText() != null) ? binding.etPwd.getText().toString() : "";
				if (TextUtils.isEmpty(ssid)||TextUtils.isEmpty(password)) {
					showToast(R.string.step2_tip);
					return;
				}
				ConnectManager.share().disconnect();
				ConnectManager.share().distributionNetwork(distNetDevice,ssid,password,60);
			}
		});
	}

	private void showToast(String msg, Drawable drawable) {
		ToastViewBinding toastViewBinding = ToastViewBinding.inflate(LayoutInflater.from(this));
		toastViewBinding.toastImage.setImageDrawable(drawable);
		toastViewBinding.toastText.setText(msg);
		ToastView toastView = new ToastView(this, toastViewBinding.getRoot());
		toastView.show();
	}
	
	ConnectManager.OnDistributionNetworkListener onDistributionNetworkListener = new ConnectManager.OnDistributionNetworkListener() {
		@Override
		public void onDistributionNetworkStart() {
			RBQLog.i("reTryTime:" + reTryTime);
			if (reTryTime == 0) {
				RBQLog.i("配网开始");
				showStartConfigDialog();
			}
		}
		
		@Override
		public void onDistributionNetworkSucceed(Device device) {
			if (dialog != null) {
				dialog.dismiss();
			}
			reTryTime = 0;

			showDistributionNetworkSucceed();

			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					//自动连接
					ConnectManager.share().connect(device);
				}
			}, 2000);
			
		}
		
		@Override
		public void onDistributionNetworkFail() {
			RBQLog.i("配网--onDistributionNetworkFail,reTryTime:" + reTryTime);
			//notifyDistributionNetworkFail 看sdk可能有三种情况：1.ble连接失败 2.通知打开失败 3.写入失败
			if (reTryTime < 2) {
				reTryTime++;
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						String SSID = String.valueOf(binding.etWifi.getText());
						String PASSWORD = String.valueOf(binding.etPwd.getText());
						ConnectManager.share().distributionNetwork(distNetDevice, SSID, PASSWORD, 60);
					}
				}, 2000);
			} else {
				
				if (dialog != null) {
					dialog.dismiss();
				}
				reTryTime = 0;
				showDistributionNetworkFail();
				showToast(getString(R.string.config_timeout),AppCompatResources.getDrawable(WifiConfigurationActivity.this,R.mipmap.ic_close_red));
			}
		}
		
		@Override
		public void onDistributionNetworkTimeOut() {
			
			RBQLog.i("配网--onDistributionNetworkTimeOut,reTryTime:" + reTryTime);
			if (reTryTime < 2) {
				reTryTime++;
				new Handler().postDelayed(() -> {
					
					String SSID = String.valueOf(binding.etWifi.getText());
					String PASSWORD = String.valueOf(binding.etPwd.getText());
					ConnectManager.share().distributionNetwork(distNetDevice, SSID, PASSWORD, 40);
					
				}, 2000);
				
			} else {
				reTryTime = 0;
				if (dialog != null) {
					dialog.dismiss();
				}
				showToast(getString(R.string.config_timeout), AppCompatResources.getDrawable(WifiConfigurationActivity.this,R.mipmap.ic_close_red));
			}
		}
	};

	private void showDistributionNetworkSucceed(){
		binding.llStepSuccess.setVisibility(View.VISIBLE);
		binding.llStepFail.setVisibility(View.GONE);
	}

	private void showDistributionNetworkFail(){
		binding.llStepSuccess.setVisibility(View.GONE);
		binding.llStepFail.setVisibility(View.VISIBLE);
	}

	//频段说明按钮
	private void showInstructionDialog() {
		DialogInstructionLayout0Binding dialogInstructionLayoutBinding = DialogInstructionLayout0Binding.inflate(LayoutInflater.from(this));

		Dialog dialog = DialogFactory.share().showCustomDialog(this,dialogInstructionLayoutBinding.getRoot());

		dialogInstructionLayoutBinding.tvKnow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		dialog.show();
	}
	
	
	//配网中。。。
	@SuppressLint("SetTextI18n")
	private void showStartConfigDialog() {

		DialogStartConfigLayoutBinding dialogStartConfigLayoutBinding = DialogStartConfigLayoutBinding.inflate(LayoutInflater.from(this));

		dialog = DialogFactory.share().showCustomDialog(this,dialogStartConfigLayoutBinding.getRoot(),0.65f);

		dialogStartConfigLayoutBinding.tvIng.post(new Runnable() {
            @Override
			public void run() {
				String text = dialogStartConfigLayoutBinding.tvIng.getText().toString();
				if (text.endsWith("......")) {
					dialogStartConfigLayoutBinding.tvIng.setText(R.string.start_config_toast2);
				} else {
					dialogStartConfigLayoutBinding.tvIng.setText(text + ".");
				}
				dialogStartConfigLayoutBinding.tvIng.postDelayed(this, 500);
			}
		});
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		ConnectManager.share().unregisterDistributionNetworkListener(onDistributionNetworkListener);
		ConnectManager.share().unregisterDeviceConnectListener(onDeviceConnectListener);
	}

}