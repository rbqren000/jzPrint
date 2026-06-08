package com.org.jzprinter.ui.activity;

import static com.org.jzprinter.constant.Constant.KEY_INTENT_EXTRA_BACK_CLASS_NAME;
import static com.mx.mxSdk.DeviceDefinitionRegistry.DeviceType_CRYPTO_STAMP;
import static com.mx.mxSdk.DeviceDefinitionRegistry.DeviceType_INKSI_01;
import static com.mx.mxSdk.DeviceDefinitionRegistry.DeviceType_INKSI_01_Lite;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;
import com.org.jzprinter.R;
import com.org.jzprinter.XXPermissions.PermissionInterceptor;
import com.org.jzprinter.constant.Constant;
import com.org.jzprinter.databinding.ActivityDeviceSelectBinding;
import com.org.jzprinter.databinding.ItemDeviceListBinding;
import com.org.jzprinter.manager.AutoConnectManager;
import com.org.jzprinter.manager.BatterySyncManager;
import com.org.jzprinter.manager.DeviceIconRegistry;
import com.org.jzprinter.manager.DeviceScanner;
import com.org.jzprinter.manager.ParameterManager;
import com.org.jzprinter.utils.location.GPSUtils;
import com.org.jzprinter.widget.CustomDialog.DialogFactory;
import com.org.jzprinter.widget.CustomDialog.RBQProgressDialog;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.mx.mxSdk.ConnModel;
import com.mx.mxSdk.ConnType;
import com.mx.mxSdk.ConnectManager;
import com.mx.mxSdk.Device;
import com.mx.mxSdk.DistNetDevice;
import com.mx.mxSdk.Conditions.ConditionAction;
import com.mx.mxSdk.Conditions.ConditionCallback;
import com.mx.mxSdk.Conditions.ConditionCheckerImpl;
import com.mx.mxSdk.Conditions.ConditionManager;
import com.mx.mxSdk.RepeatingTaskWithTimeout;
import com.mx.mxSdk.Utils.RBQLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import uk.co.imallan.jellyrefresh.JellyRefreshLayout;

public class DeviceSelectActivity extends BaseActivity implements ConnectManager.OnReceiveMsgListener {

	private static final String TAG = DeviceSelectActivity.class.getSimpleName();

	String backClassName;
	
	private ActivityDeviceSelectBinding binding;

	private final List<ConnModel> models = new ArrayList<>();

	RBQProgressDialog progressDialog = new RBQProgressDialog();
	//将要断开的设备的ssid
    private String willDisconnectSsid;
	//将要连接的ConnModel
	private volatile ConnModel willConnectModel;

	private final Handler timeOutHandler = new Handler(Looper.getMainLooper());
	private final Handler mainHandler = new Handler(Looper.getMainLooper());

	private final ConditionManager conditionManager = new ConditionManager();
	private final ActivityResultLauncher<Intent> gpsActivityResultLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			new ActivityResultCallback<ActivityResult>() {
				@Override
				public void onActivityResult(ActivityResult result) {
					// 在返回后检查GPS状态
					if (GPSUtils.isLocationEnabled(DeviceSelectActivity.this)){
						conditionManager.onConditionResult(DeviceSelectActivity.this,gpsAction.getKey(),true);
					}
				}
			}
	);

	private final ActivityResultLauncher<Intent> wifiSettingsLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
		@Override
		public void onActivityResult(ActivityResult result) {
//			String ssid = getCurrentWifiSsid().replace("\"", "").replace("\"", "");
//			if (!ssid.equals(willDisconnectSsid)) {//如果wifi不一样了说明断开打印wifi了
//				//手动断开,存储mac为""，就不会自动连接了
//				ConnectManager.share().disconnect();
//			}
			// 优化改为下边的重复执行的任务
			fetchSsidWithDisconnectTask.start();
		}
	});

	private final RepeatingTaskWithTimeout fetchSsidWithDisconnectTask = new RepeatingTaskWithTimeout(new Runnable() {
		@Override
		public void run() {

			String ssid = getCurrentWifiSsid().replace("\"", "").replace("\"", "");

			if (!ssid.equals(willDisconnectSsid)) {//如果wifi不一样了说明断开打印wifi了

				RBQLog.i(TAG,"检测到"+willDisconnectSsid+"~~已经断开~~，当前已连ssid:"+ssid);
				//手动断开，存储mac为""，就不会自动连接了
				ConnectManager.share().disconnect();

				fetchSsidWithDisconnectTask.stop();
			}
		}
	}, 0, 0.2,2, new RepeatingTaskWithTimeout.Callback() {
		@Override
		public void onTaskStarted() {
			RBQLog.i(TAG,"-----开始获取ssid-----");
		}

		@Override
		public void onTaskStopped() {
			RBQLog.i(TAG,"-----停止获取ssid-----");
		}

		@Override
		public void onTaskError(Exception e) {
			RBQLog.i(TAG,"-----onTaskError-----");
		}

		@Override
		public void onTaskTimeout() {
			RBQLog.i(TAG,"-----onTaskTimeout-----");
		}
	});

	// 打开 WiFi 设置
	private void openWifiSettings() {
		Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
		wifiSettingsLauncher.launch(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityDeviceSelectBinding.inflate(LayoutInflater.from(this));
		setContentView(binding.getRoot());

		setupStatusBarWithCustomColorResId(R.color.primary_blue);

		backClassName = getIntent().getStringExtra(KEY_INTENT_EXTRA_BACK_CLASS_NAME);
		RBQLog.i(TAG,"backClassName:"+backClassName);

		binding.appBar.titleTextView.setText(R.string.add_device);
		binding.appBar.leftMenuLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
		binding.recyclerView.setAdapter(new DeviceListAdapter());
		binding.jellyRefreshLayout.setRefreshListener(new JellyRefreshLayout.JellyRefreshListener() {
			@Override
			public void onRefresh(final JellyRefreshLayout jellyRefreshLayout) {

				models.clear();
				localStopScanningWithClear();

				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						binding.jellyRefreshLayout.finishRefreshing();

						localStartScanning();

					}
				}, 3000);
			}
		});

		conditionManager.addChecker(new ConditionCheckerImpl(conditionManager, bluetoothIsOpenAction));
		conditionManager.addChecker(new ConditionCheckerImpl(conditionManager, bluetoothAction));
		conditionManager.addChecker(new ConditionCheckerImpl(conditionManager, gpsAction));

		ConnectManager.share().registerReceiveMessageListener(this);

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

	@Override
	protected void onResume() {
		super.onResume();

		AutoConnectManager.share().setAllowAutoConn(false);
		AutoConnectManager.share().clearWithCancelAutoConn();

		ConnectManager.share().registerConnModelDiscoverListeners(onConnModelDiscoverListener);
		ConnectManager.share().registerDeviceDiscoverListener(onDeviceDiscoverListener);//wifi配网device及spp蓝牙device
		ConnectManager.share().registerDeviceConnectListener(onDeviceConnectListener);
		ConnectManager.share().registerDataSynchronizeListener(onDataSynchronizeListener);

		localStartScanning();
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		RBQLog.i(TAG,"----onPause----");

		ConnectManager.share().unregisterConnModelDiscoverListeners(onConnModelDiscoverListener);
		ConnectManager.share().unregisterDeviceDiscoverListener(onDeviceDiscoverListener);
		ConnectManager.share().unregisterDeviceConnectListener(onDeviceConnectListener);
		ConnectManager.share().unregisterDataSynchronizeListener(onDataSynchronizeListener);

		localStopScanningWithClear();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// 清理 Handler 延迟任务，防止 Activity 销毁后弹 Dialog 导致 BadTokenException
		mainHandler.removeCallbacksAndMessages(null);
		timeOutHandler.removeCallbacksAndMessages(null);
		progressDialog.dismiss();

		ConnectManager.share().unregisterReceiveMessageListener(this);
	}

	private synchronized void localStartScanning(){

		if (!conditionManager.allConditionsProcessed(this)){
			return;
		}

		Device device = ConnectManager.share().getConnectedDevice();
		if (device != null) {
			RBQLog.i("reDiscoverDevices", "isWifi:" + device.isWifiConnType() + ",isSPP" + device.isSPPConnType());
			ConnModel connModel = new ConnModel(device.bleDevice,device.localName,device.connTypes,null,device.mac,device.bluetoothDevice,device.state,device.deviceModel);
			if (!models.contains(connModel)){
				models.add(connModel);
				refreshDeviceListView();
			}
		}
		DeviceScanner.share().startScanning();
	}

	private synchronized void localStopScanningWithClear(){
		//停止扫描后，重新扫描
		DeviceScanner.share().stopScanning();
		refreshDeviceListView();
	}
	
	@SuppressLint("NotifyDataSetChanged")
    private void refreshDeviceListView() {
		runOnUiThread(() -> {
			DeviceListAdapter deviceAdapter = (DeviceListAdapter) binding.recyclerView.getAdapter();
			if (deviceAdapter == null){
				return;
			}
			deviceAdapter.notifyDataSetChanged();
			if (deviceAdapter.getItemCount() == 0) {
				binding.tvEmpty.setVisibility(View.VISIBLE);
			} else {
				binding.tvEmpty.setVisibility(View.GONE);
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
		public void requestCondition(Activity activity, ConditionManager manager) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.mAlertDialog);
			builder.setMessage(getResources().getString(R.string.turnOnBluetoothHint))
					.setCancelable(false)
					.setNegativeButton(R.string.custom_dialog_cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							showToast(getString(R.string.bluetooth_not_enabled));
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

	public void showDisConnectApDialog() {
		willDisconnectSsid = getCurrentWifiSsid().replace("\"", "").replace("\"", "");
		DialogFactory.share().showMessageDialog(this,R.string.disconnect_ap, R.string.custom_dialog_ok, R.string.custom_dialog_cancel, new DialogFactory.OnMessageDialogListener() {
			@Override
			public void onOkClicked(Dialog dialog, boolean isChecked) {
				dialog.dismiss();
				openWifiSettings();
			}

			@Override
			public void onCancelClicked(Dialog dialog) {
				dialog.dismiss();
			}

			@Override
			public void onCheckChange(Dialog dialog, CompoundButton buttonView, boolean isChecked) {

			}
		});
	}

	private void showConnectingProgress() {
		if (progressDialog.isShowing()){
			return;
		}
		progressDialog.show(this,"",getString(R.string.connecting));
	}
	
	private void dismissConnectingProgress() {
		progressDialog.dismiss();
	}

	@SuppressLint("MissingPermission")
	private String getCurrentWifiSsid() {
		WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (wifiManager == null) {
			return "";
		}
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		if (wifiInfo == null) {
			return "";
		}
		String ssid = wifiInfo.getSSID();
		if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
			ssid = ssid.substring(1, ssid.length() - 1);
		}
		return ssid;
	}

	private void showToast(String msg, Drawable drawable) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}

	private synchronized void waitWithConnectModelBySpp(ConnModel model){
		if (model == null){
			return;
		}
		showConnectingProgress();
		this.willConnectModel = model;
		//最长连接事件30秒
		timeOutHandler.postDelayed(timeoutRun,30*1000);
	}

	private final Runnable timeoutRun = new Runnable() {
		@Override
		public void run() {
			//恢复将要连接ConnModel的值
			willConnectModel = null;
			dismissConnectingProgress();
			showToast(R.string.connect_timeout);
		}
	};

	private void clearWillConnectModel(){
		if (this.willConnectModel != null) {
			this.willConnectModel = null;
		}
		timeOutHandler.removeCallbacks(timeoutRun);
		timeOutHandler.removeCallbacksAndMessages(null);
	}
	
	ConnectManager.OnDeviceDiscoverListener onDeviceDiscoverListener = new ConnectManager.OnDeviceDiscoverListener() {
		@Override
		public void onDeviceStartDiscover() {

		}

		@Override
		public void onDeviceStopDiscover() {

		}

		@Override
		public void onDeviceDiscovered(Device device) {
			updateConnModel(device);
		}
	};

	void updateConnModel(Device device) {
		if (device.isWifiConnType()) {
			updateWifiDevice(device);
		} else if (device.isSPPConnType()) {
			updateSppDevice(device);
		}
	}

	private synchronized void updateWifiDevice(Device device) {

		Map<String,Object> dic = getConnModelByWifiDevice(device);
		if (dic == null) {
			return;
		}

		ConnModel model = (ConnModel) dic.get("model");
		if (model == null) {
			return;
		}

		RBQLog.i("搜索到WiFi设备 device:"+ device);

		boolean needsRefresh = false;
		if (device.wifiName != null && !device.wifiName.equals(model.getWifiName())) {
			model.setWifiName(device.wifiName);
			needsRefresh = true;
		}

		if (!device.ip.equals(model.getIp())) {
			model.setIp(device.ip);
			needsRefresh = true;
		}

		if (device.port != model.getPort()) {
			model.setPort(device.port);
			needsRefresh = true;
		}

		if (needsRefresh) {
			refreshDeviceListView();
		}
	}

	private synchronized void updateSppDevice(Device device) {

		Map<String,Object> dic = getConnModelBySppDevice(device);

		if (dic == null) {
			ConnModel connModel = new ConnModel(null,null,ConnType.SPP.getValue(),null,null,device.bluetoothDevice,-1, null);
			if (models.contains(connModel)){
				return;
			}

			RBQLog.i("搜索到SPP设备 device:"+ device);
			models.add(connModel);
			refreshDeviceListView();
			return;
		}

		ConnModel model = (ConnModel) dic.get("model");
		if (model == null) {
			return;
		}

		boolean needsRefresh = false;
		if (device.bluetoothDevice != null && !device.bluetoothDevice.equals(model.getBluetoothDevice())) {

			RBQLog.i("搜索到SPP设备 device:"+ device);

			model.setBluetoothDevice(device.bluetoothDevice);
			needsRefresh = true;
		}

		if (needsRefresh) {
			refreshDeviceListView();
		}

		if (willConnectModel != null && willConnectModel.equals(model)){
			Device _device = Device.createSppDevice(model.getBluetoothDevice(), model.getBleDevice(), model.getMac(), model.getConnTypes(), model.getDeviceModel());
			ConnectManager.share().connect(_device);
		}

	}


	ConnectManager.OnConnModelDiscoverListener onConnModelDiscoverListener = new ConnectManager.OnConnModelDiscoverListener() {
		@Override
		public void onConnModelStartDiscover() {
			RBQLog.i("****onDiscoveredStart2222222*****");
		}
		
		@Override
		public void onConnModelStopDiscover() {
			RBQLog.i("****onDiscoveredStop2222222*****");
		}

		@Override
		public void onConnModelDiscovered(ConnModel connModel) {

			Map<String,Object> dic = getConnModelByConnModel(connModel);
			if (dic == null){
				RBQLog.i("搜索到设备connModel:"+connModel.toString());
				models.add(connModel);
				refreshDeviceListView();
			}else {
				ConnModel _model = (ConnModel) dic.get("model");
				if (_model == null){
					return;
				}
				boolean needsRefresh = false;
				if (connModel.getBluetoothDevice() != null && !connModel.getBluetoothDevice().equals(_model.getBluetoothDevice())){
					_model.setBluetoothDevice(connModel.getBluetoothDevice());
					needsRefresh = true;
				}
				if (connModel.getBleDevice()!=null && !connModel.getBleDevice().equals(_model.getBleDevice())){
					_model.setBleDevice(connModel.getBleDevice());
					needsRefresh = true;
				}
				if (connModel.getDeviceModel() !=null && !connModel.getDeviceModel().equals(_model.getDeviceModel())){
					_model.setDeviceModel(connModel.getDeviceModel());
				}
				if (connModel.getMac() != null && !connModel.getMac().equals(_model.getMac())) {
					_model.setMac(connModel.getMac());
					needsRefresh = true;
				}
				if (connModel.getConnTypes()!=0 && connModel.getConnTypes() != _model.getConnTypes()) {
					_model.setConnTypes(connModel.getConnTypes());
					needsRefresh = true;
				}
				if (needsRefresh) {
					refreshDeviceListView();
				}
			}
		}
	};
	
	ConnectManager.OnDeviceConnectListener onDeviceConnectListener = new ConnectManager.OnDeviceConnectListener() {
		@Override
		public void onDeviceConnectStart(Device device) {
			showConnectingProgress();
			refreshDeviceListView();
		}
		
		@SuppressLint("NotifyDataSetChanged")
		@Override
		public void onDeviceConnectSucceed(Device device) {

			clearWillConnectModel();

			// 显示连接成功提示
			showToast(getString(R.string.connect_success), AppCompatResources.getDrawable(DeviceSelectActivity.this,R.mipmap.ic_check_blue));

			RBQLog.i("保存的自动连接的device:"+device);
			//连接成功后，记录下mac，下次可以自动连接
			ParameterManager.saveDevice(DeviceSelectActivity.this,device);
			refreshDeviceListView();

			// 关闭连接进度，延迟重新弹出同步进度
			dismissConnectingProgress();
			mainHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					progressDialog.show(DeviceSelectActivity.this, "", getString(R.string.syncing_data));
				}
			}, 2000);
		}
		
		@Override
		public void onDeviceDisconnect(Device device) {

			clearWillConnectModel();

			dismissConnectingProgress();
			refreshDeviceListView();
			showToast(getString(R.string.disconnect),AppCompatResources.getDrawable(DeviceSelectActivity.this,R.mipmap.ic_close_red));
		}
		
		@Override
		public void onDeviceConnectFail(Device device, String error) {

			clearWillConnectModel();

			dismissConnectingProgress();
			showToast(getString(R.string.connect_fail),AppCompatResources.getDrawable(DeviceSelectActivity.this,R.mipmap.ic_close_red));
		}
	};

	public synchronized boolean isConnectedConnModel(ConnModel model) {
		// 获取当前连接的设备
		Device device = ConnectManager.share().getConnectedDevice();
		if (device == null) {
			return false;
		}
		// 如果设备是AP模式
		if (device.isApConnType()) {
			// 验证模型是否包含AP类型以及设备的MAC地址是否存在
			if (!model.containsConnType(ConnType.AP.getValue()) || model.getMac() == null || model.getMac().length() < 4
					|| device.mac == null || device.mac.length() < 4||device.ssid == null) {
				return false;
			}

			// 获取SSID名称并验证其有效性
			String ssid = device.ssid;

			String ssidLastFourMac = ssid.substring(ssid.length() - 4);

			String mac = model.getMac();
			// 去掉 MAC 地址中的冒号
			String modelMacWithoutColons = mac.replace(":", "").toLowerCase();
			// 如果处理后的 MAC 地址长度小于 4，跳过此检查
			if (modelMacWithoutColons.length() < 4) {
				return false;
			}
			// 提取处理后 MAC 地址的后四位并转换为小写
			String modelLastFourMac = modelMacWithoutColons.substring(modelMacWithoutColons.length() - 4);

			return ssidLastFourMac.equalsIgnoreCase(modelLastFourMac);
		}

		// 验证设备和模型的peripheral是否有效并且相等
		if (model.getBleDevice() != null && device.bleDevice != null) {
			return model.getBleDevice().equals(device.bleDevice);
		}

		if (model.getBluetoothDevice() != null && device.bluetoothDevice != null){
			return model.getBluetoothDevice().equals(device.bluetoothDevice);
		}

		if (model.getMac() != null && device.mac != null) {
			return model.getMac().equals(device.mac);
		}
		return false;
	}

	public Integer batteryLevel(ConnModel model){
		Device device = ConnectManager.share().getConnectedDevice();
		if (device == null) {
			return -1;
		}
		// 如果设备是AP模式
		if (device.isApConnType()) {
			// 验证模型是否包含AP类型以及设备的MAC地址是否存在
			if (!model.containsConnType(ConnType.AP.getValue()) || model.getMac() == null || model.getMac().length() < 4
					|| device.mac == null || device.mac.length() < 4||device.ssid == null) {
				return device.batteryLevel;
			}

			// 获取SSID名称并验证其有效性
			String ssid = device.ssid;

			String ssidLastFourMac = ssid.substring(ssid.length() - 4);

			String mac = model.getMac();
			// 去掉 MAC 地址中的冒号
			String modelMacWithoutColons = mac.replace(":", "").toLowerCase();
			// 如果处理后的 MAC 地址长度小于 4，跳过此检查
			if (modelMacWithoutColons.length() < 4) {
				return -1;
			}
			// 提取处理后 MAC 地址的后四位并转换为小写
			String modelLastFourMac = modelMacWithoutColons.substring(modelMacWithoutColons.length() - 4);

			if (ssidLastFourMac.equalsIgnoreCase(modelLastFourMac)){
				return device.batteryLevel;
			}
		}

		// 验证设备和模型的peripheral是否有效并且相等
		if (model.getBleDevice() != null && device.bleDevice != null) {
			if (model.getBleDevice().equals(device.bleDevice)){
				return device.batteryLevel;
			}
		}

		if (model.getBluetoothDevice() != null && device.bluetoothDevice != null){
			if (model.getBluetoothDevice().equals(device.bluetoothDevice)){
				return device.batteryLevel;
			}
		}

		if (model.getMac() != null && device.mac != null) {
			if (model.getMac().equals(device.mac)){
				return device.batteryLevel;
			}
		}
		return -1;
	}

	public synchronized Map<String,Object> getConnModelByConnModel(ConnModel connModel){
		for (int i = 0; i< models.size(); i++){
			ConnModel _connModel = models.get(i);
			if (_connModel.equals(connModel)){
				Map<String,Object> dic = new HashMap<>();
				dic.put("index",i);
				dic.put("model",_connModel);
				return dic;
			}
		}
		return null;
	}

	public synchronized Map<String,Object> getConnModelByWifiDevice(Device device){
		if (device.connType != ConnType.WiFi){
			return null;
		}
		String mac = device.mac;
		for (int i = 0; i< models.size(); i++){
			ConnModel connModel = models.get(i);
			String _mac = connModel.getMac();
			if (mac!=null && mac.equals(_mac)){
				Map<String,Object> dic = new HashMap<>();
				dic.put("index",i);
				dic.put("model",connModel);
				return dic;
			}
		}
		return null;
	}

	public synchronized Map<String,Object> getConnModelBySppDevice(Device device){
		if (device.connType != ConnType.SPP){
			return null;
		}
		BluetoothDevice bluetoothDevice = device.bluetoothDevice;
		for (int i = 0; i< models.size(); i++){
			ConnModel connModel = models.get(i);
			String mac = bluetoothDevice.getAddress();
			String _mac = connModel.getMac();
			if (_mac!=null && _mac.equals(mac)){
				Map<String,Object> dic = new HashMap<>();
				dic.put("index",i);
				dic.put("model",connModel);
				return dic;
			}
		}
		return null;
	}

	private final ConnectManager.OnDataSynchronizeListener onDataSynchronizeListener = new ConnectManager.OnDataSynchronizeListener() {
		@Override
		public void onDataSynchronizeStart(Device device) {
			RBQLog.i("设备选择页-数据同步开始");
		}

		@Override
		public void onDataSynchronizeComplete(Device device) {
			RBQLog.i("设备选择页-数据同步完成");
			dismissConnectingProgress();
			showToast(getString(R.string.syncing_complete), AppCompatResources.getDrawable(DeviceSelectActivity.this,R.mipmap.ic_check_blue));
		}

		@Override
		public void onDataSynchronizeTimeout(Device device, int pendingCount) {
			RBQLog.i("设备选择页-数据同步超时, pendingCount:" + pendingCount);
			dismissConnectingProgress();
		}

		@Override
		public void onDataSynchronizeInterrupted(Device device) {
			RBQLog.i("设备选择页-数据同步被中断");
			dismissConnectingProgress();
		}
	};

	@Override
	public void onReadPrinterHeadParameter(Device device, int headValue, int l_pix, int p_pix, int distance) {

	}

	@Override
	public void onReadCirculationAndRepeatTime(Device device, int circulation_time, int repeat_time) {

	}

	@Override
	public void onReadDirection(Device device, int oldHorizontalDirection, int horizontalDirection, int oldVerticalDirection, int verticalDirection) {

	}

	@Override
	public void onReadSoftwareInfo(Device device, String id, String name, String mcu_version, String mcu_date) {

	}

	@Override
	public void onReadTemperature(Device device, int temp) {

	}

	@Override
	public void onReadBattery(Device device, int bat) {
		Integer resourceId = BatterySyncManager.share().getIconByValue(bat);
		if(resourceId == -1){
			return;
		}
		refreshDeviceListView();
	}

	@Override
	public void onReadCartridgeId(Device device, String cartridgeId) {

	}

	@Override
	public void onReadSilentState(Device device, boolean silentState) {

	}

	@Override
	public void onReadAutoPowerOffState(Device device, boolean autoPowerOff) {

	}

	@Override
	public void onReadContinuousPrintState(Device device, boolean continuousPrintState) {

	}

	@Override
	public void onError(Device device, String error) {

	}

	class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceListViewHolder> {

		@NonNull
		@Override
		public DeviceListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_list, parent, false);
			return new DeviceListViewHolder(view);
		}

		private Drawable getDrawableByConnModel(ConnModel connModel) {
			final int defaultResId = R.mipmap.ic_inksi_printer;
			int deviceType = connModel != null && connModel.getDeviceModel() != null
					? connModel.getDeviceModel().getDeviceType()
					: -1;

			int resId = DeviceIconRegistry.getInstance().getDeviceIcon(deviceType);
			if (resId == 0) resId = defaultResId;

			return ResourcesCompat.getDrawable(getResources(), resId, getTheme());
		}

		@Override
		public void onBindViewHolder(@NonNull DeviceListViewHolder holder, int position) {

			ConnModel model = models.get(position);

			int deviceType = model.getDeviceType();
			boolean isINKSI = deviceType == DeviceType_INKSI_01 || deviceType == DeviceType_INKSI_01_Lite || deviceType == DeviceType_CRYPTO_STAMP;

			holder.binding.iv.setImageDrawable(getDrawableByConnModel(model));
			holder.binding.tvName.setText(model.getAliases());

			String mac = model.getMac();
			if (!TextUtils.isEmpty(mac)){
				holder.binding.tvMac.setText(mac);
				holder.binding.tvMac.setVisibility(View.VISIBLE);
			}else {
				holder.binding.tvMac.setVisibility(View.GONE);
			}

			boolean isConnected = isConnectedConnModel(model);

			int batteryLevel = batteryLevel(model);
			int visibility = !isConnected || batteryLevel == -1 ? View.GONE : View.VISIBLE;
			holder.binding.batteryImageView.setVisibility(visibility);
			int resourceId = BatterySyncManager.share().getIconByValue(batteryLevel);
			if (resourceId != -1){
				holder.binding.batteryImageView.setImageResource(resourceId);
			}

			if (isConnected) {

				holder.binding.addBtn.setVisibility(View.GONE);
				holder.binding.blueToothConnBtn.setVisibility(View.GONE);
				holder.binding.wifiConfigBtn.setVisibility(View.GONE);
				holder.binding.wifiConnBtn.setVisibility(View.GONE);
				holder.binding.apConnBtn.setVisibility(View.GONE);
				holder.binding.disConnectBtn.setVisibility(View.VISIBLE);

			} else {

				int add_visibility = model.containsConnType(ConnType.SPP.getValue()) && model.getBluetoothDevice() != null && !isINKSI ? View.VISIBLE : View.GONE;
				holder.binding.addBtn.setVisibility(add_visibility);

				int spp_visibility = model.containsConnType(ConnType.SPP.getValue()) && isINKSI ? View.VISIBLE : View.GONE;
				holder.binding.blueToothConnBtn.setVisibility(spp_visibility);


				int wifi_config_visibility = model.containsConnType(ConnType.WiFi.getValue()) && !model.isWifiReady() ? View.VISIBLE : View.GONE;
				holder.binding.wifiConfigBtn.setVisibility(wifi_config_visibility);

				int wifi_conn_visibility = model.containsConnType(ConnType.WiFi.getValue()) && model.isWifiReady() ? View.VISIBLE : View.GONE;
				holder.binding.wifiConnBtn.setVisibility(wifi_conn_visibility);

				int ap_visibility = model.containsConnType(ConnType.AP.getValue()) ? View.VISIBLE : View.GONE;
				holder.binding.apConnBtn.setVisibility(ap_visibility);
				holder.binding.disConnectBtn.setVisibility(View.GONE);
			}

			holder.binding.addBtn.setOnClickListener(v -> {
				if (ConnectManager.share().isConnected()) {
					showToast(R.string.disconnect_first);
					return;
				}
				clearWillConnectModel();
				Device device = Device.createSppDevice(model.getBluetoothDevice(), model.getBleDevice(), model.getMac(), model.getConnTypes(), model.getDeviceModel());
				ConnectManager.share().connect(device);
			});

			holder.binding.blueToothConnBtn.setOnClickListener(v -> {
				if (ConnectManager.share().isConnected()) {
					showToast(R.string.disconnect_first);
					return;
				}
				if (model.getBluetoothDevice() != null){
					Device device = Device.createSppDevice(model.getBluetoothDevice(), model.getBleDevice(), model.getMac(), model.getConnTypes(), model.getDeviceModel());
					ConnectManager.share().connect(device);
				}else {
					waitWithConnectModelBySpp(model);
				}
			});

			holder.binding.wifiConfigBtn.setOnClickListener(v -> {
				clearWillConnectModel();
				RBQLog.i("将要配网的model:"+model);
				DistNetDevice distNetDevice = new DistNetDevice(model.getBleDevice(), model.getLocalName(), model.getMac(), model.getState(), model.getConnTypes(), model.getFirmwareConfigs(), model.getDeviceModel());
				RBQLog.i("将要配网的distNetDevice:"+distNetDevice);
				
				// 使用Intent跳转
				Intent intent = new Intent(DeviceSelectActivity.this, WifiConfigurationActivity.class);
				intent.putExtra("distNetDevice", distNetDevice);
				intent.putExtra(KEY_INTENT_EXTRA_BACK_CLASS_NAME, backClassName);
				startActivity(intent);
			});

			holder.binding.wifiConnBtn.setOnClickListener(v -> {
				if (ConnectManager.share().isConnected()) {
					showToast(R.string.disconnect_first);
					return;
				}
				clearWillConnectModel();
				Device device = Device.createWifiDevice(model.getWifiName(), model.getIp(), model.getPort(), model.getBleDevice(), model.getMac(), model.getConnTypes(), model.getDeviceModel());
				ConnectManager.share().connect(device);
			});

			holder.binding.apConnBtn.setOnClickListener(v -> {
				if (ConnectManager.share().isConnected()) {
					showToast(R.string.disconnect_first);
					return;
				}
				clearWillConnectModel();
				
				// 使用Intent跳转
				Intent intent = new Intent(DeviceSelectActivity.this, ApActivity.class);
				intent.putExtra(KEY_INTENT_EXTRA_BACK_CLASS_NAME, backClassName);
				startActivity(intent);
			});

			holder.binding.disConnectBtn.setOnClickListener(v -> {
				Device device = ConnectManager.share().getConnectedDevice();
				if (device != null) {
					if (device.isApConnType()) {
						showDisConnectApDialog();
						return;
					}
				}
				ParameterManager.saveDevice(DeviceSelectActivity.this, null);
				ConnectManager.share().disconnect();
			});
		}

		@Override
		public int getItemCount() {
			return models.size();
		}

		static class DeviceListViewHolder extends RecyclerView.ViewHolder {
			private final ItemDeviceListBinding binding;

			public DeviceListViewHolder(View view) {
				super(view);
				binding = ItemDeviceListBinding.bind(view);
			}
		}

	}

}