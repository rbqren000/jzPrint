package com.org.jzprinter.manager;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import com.mx.mxSdk.ConnModel;
import com.mx.mxSdk.ConnectManager;
import com.mx.mxSdk.Device;
import com.mx.mxSdk.MxUtils;
import com.mx.mxSdk.RepeatingTask;
import com.mx.mxSdk.RepeatingTaskWithTimeout;
import com.mx.mxSdk.Utils.BroadcastHelper;
import com.mx.mxSdk.Utils.RBQLog;

import java.lang.ref.WeakReference;

public class AutoConnectManager {

    // 网络状态广播接收器
	private final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
				ConnectivityManager connectivityManager =
						(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
				Network network = connectivityManager.getActiveNetwork();
				NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);

				if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
					repeatingTaskWithTimeout.stop();
					mainHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							repeatingTaskWithTimeout.start();
						}
					}, 100);
				}
			}
		}
	};

	private static final String TAG = "AutoConnectManager";

	// 使用静态内部类实现单例模式，避免类加载时初始化
	private static class Holder {
		private static final AutoConnectManager INSTANCE = new AutoConnectManager();
	}

	private WeakReference<Context> contextRef;

	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private boolean isAllowAutoConn = true;
	private boolean isStart = false;

	// ssid
	private String ssid;
	// connModel
	private ConnModel model;

	private Device localDevice;

	// 私有构造函数
	private AutoConnectManager() {}

	// 单例访问方法
	public static AutoConnectManager share() {
		return Holder.INSTANCE;
	}

	// 初始化方法，使用 Application Context
	public void init(Application application) {
		if (application != null) {
			contextRef = new WeakReference<>(application.getApplicationContext());
		}
	}

	// 获取当前 Context 的方法
	private Context getContext() {
		return contextRef != null ? contextRef.get() : null;
	}

	public Boolean isAllowAutoConn() {
		return isAllowAutoConn;
	}

	public void setAllowAutoConn(boolean allowAutoConn) {
		isAllowAutoConn = allowAutoConn;
	}

	public void startAutoConn() {
		startAutoConn(0.3f);
	}

	public void startAutoConn(float startTimeInSeconds) {
		synchronized (this) {
			if (!isAllowAutoConn || isStart) {
				// 判断是否允许自动连接和是否已经启动
				String logMessage = !isAllowAutoConn ? "【startAutoConn】不允许自动连接" :
                        "【startAutoConn】已经启动了自动连接，无需再次启动";
				Log.d(TAG, logMessage);
				return;
			}
			isStart = true;
			Log.d(TAG, "【startAutoConn】启动自动扫描");

			mainHandler.postDelayed(this::localDiscoverWithConnect, (long) (startTimeInSeconds * 1000));
		}
	}

	public void clearWithCancelAutoConn() {
		synchronized (this) {
			if (!isStart) {
				Log.d(TAG, "【clearWithCancelAutoConn】还没有启动自动连接，无需停止");
				return;
			}

			isStart = false;
			Log.d(TAG, "【clearWithCancelAutoConn】停止自动连接");

			mainHandler.removeCallbacksAndMessages(null);
			localClearWithCancelDiscover();
		}
	}

	public void enableWithAutoConnect(){
		RBQLog.i("【enableWithAutoConnect】");
		setAllowAutoConn(true);
		Device device = ConnectManager.share().getConnectedDevice();
		if (device != null){
			return;
		}
		startAutoConn();
	}

	private void localDiscoverWithConnect() {
		Log.d(TAG, "Executing local discover and connect...");
		clearWithResetCacheData();
		Context context = getContext();
		if (context == null) {
			Log.e(TAG, "Context is null. Cannot proceed.");
			return;
		}

		Device _device = ParameterManager.loadDevice(context);
		if (_device != null) {
			Log.i(TAG, "【localDiscoverWithConnect】读取到本地保存的-->device: " + _device);
		}else {
			Log.i(TAG, "【localDiscoverWithConnect】读取到本地保存device为null");
		}
		localDevice = _device;

		//开始的时候先获取一次ssid
		repeatingTaskWithTimeout.start();

		ConnectManager.share().registerDeviceDiscoverListener(onDeviceDiscoverListener);
		ConnectManager.share().registerConnModelDiscoverListeners(onConnModelDiscoverListener);

		// 注册网络状态广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		//注册广播
		BroadcastHelper.registerReceiver(context, networkReceiver, filter, false);
		DeviceScanner.share().startScanning();
	}

	private void localClearWithCancelDiscover() {
		Log.d(TAG, "Clearing local discovery resources...");

		ConnectManager.share().unregisterDeviceDiscoverListener(onDeviceDiscoverListener);
		ConnectManager.share().unregisterConnModelDiscoverListeners(onConnModelDiscoverListener);

		repeatingTaskWithTimeout.stop();
		Context context = getContext();
		// 注销广播接收器
        BroadcastHelper.unregisterReceiver(context, networkReceiver, false);
		DeviceScanner.share().stopScanning();
		clearWithResetCacheData();
	}

	ConnectManager.OnDeviceDiscoverListener onDeviceDiscoverListener = new ConnectManager.OnDeviceDiscoverListener() {
		@Override
		public void onDeviceStartDiscover() {}

		@Override
		public void onDeviceStopDiscover() {}

		@Override
		public void onDeviceDiscovered(Device device) {
			RBQLog.i(TAG,"自动连接中搜索到设备-->device:"+device);
			Device connectedDevice = ConnectManager.share().getConnectedDevice();
			//这里只自动连接WiFi设备
			if (!isAllowAutoConn() || connectedDevice != null || !TextUtils.isEmpty(ssid)) {
				return;
			}
			// wifi 连接
			if (model!=null
					&&!TextUtils.isEmpty(model.getMac())
					&& device.isWifiConnType()
					&& !model.isWifiReady()
					&& model.getMac().equals(device.mac)) {

				model.setWifiName(device.wifiName);
				model.setIp(device.ip);
				model.setPort(device.port);
				//则连接WiFi设备
				Device _device = Device.createWifiDevice(model.getWifiName(), model.getIp(), model.getPort(), model.getBleDevice(), model.getMac(), model.getConnTypes(), model.getDeviceModel());
				ConnectManager.share().connect(_device);
				return;
			}
			// spp连接 这里得分2中情况，既是否存在bleDevice，如果不存在bleDevice，则这里读到直接连接，如果存在bleDevice则 需要connModel配合 mx-02机型
			if (localDevice!=null
					&& TextUtils.isEmpty(localDevice.bleAddress)
					&& device.bluetoothDevice != null
					&& device.isSPPConnType()
					&& !TextUtils.isEmpty(device.bluetoothAddress)
					&& device.bluetoothAddress.equals(localDevice.bluetoothAddress)) {
				Device _device = Device.createSppDevice(device.bluetoothDevice, null, null, ConnectManager.share().mx02ConnTypes(), null);
				ConnectManager.share().connect(_device);
				return;
			}
			//存在ble部分，则要求model不为null inksi-01机型
			if (model != null
                    && !TextUtils.isEmpty(model.getBleAddress())
                    && device.isSPPConnType()
                    && !TextUtils.isEmpty(device.bluetoothAddress)
                    && device.equals(localDevice)) {

				model.setBluetoothDevice(device.bluetoothDevice);

				Device _device = Device.createSppDevice(device.bluetoothDevice, model.getBleDevice(), model.getMac(), model.getConnTypes(), model.getDeviceModel());
				ConnectManager.share().connect(_device);
			}
		}
	};

	ConnectManager.OnConnModelDiscoverListener onConnModelDiscoverListener = new ConnectManager.OnConnModelDiscoverListener() {
		@Override
		public void onConnModelStartDiscover() {}

		@Override
		public void onConnModelDiscovered(ConnModel connModel) {
//			RBQLog.i(TAG,"connModel:"+connModel);
			Device connectedDevice = ConnectManager.share().getConnectedDevice();
			if (!isAllowAutoConn() || connectedDevice != null) {
				return;
			}
			//优先ap方式连接
			if (!TextUtils.isEmpty(ssid) && MxUtils.isSSIDConnModel(connModel, ssid)) {
				//则连接ssid设备
				Device device = Device.createApDevice(ssid, connModel.getBleDevice(), connModel.getMac(), connModel.getConnTypes(), connModel.getDeviceModel());
				ConnectManager.share().connect(device);
				return;
			}

			if (localDevice == null) {
				return;
			}
//			RBQLog.i(TAG,"localDevice:"+localDevice);
			// 查找并为model赋值
			if (MxUtils.isEqualModel(connModel, localDevice) && !connModel.equals(model)) {
				model = connModel;
				RBQLog.i(TAG,"【onConnModelDiscovered】搜索到-->model:"+ model);
			}
		}

		@Override
		public void onConnModelStopDiscover() {}
	};

	// 移除重复扫描任务 (vivo应用市场要求不能频繁获取ssid所以改用了repeatingTaskWithTimeout)
	RepeatingTask ssidScanTask = new RepeatingTask(new Runnable() {
		@Override
		public void run() {
			fetchSSID();
		}
		}, 0, 0.2, new RepeatingTask.Callback() {
		@Override
		public void onTaskStarted() {
			Log.d(TAG, "【onTaskStarted】");
		}

		@Override
		public void onTaskStopped() {
			Log.d(TAG, "【onTaskStopped】");
		}

		@Override
		public void onTaskError(Exception e) {
			Log.d(TAG, "【onTaskError】");
		}
	});

	RepeatingTaskWithTimeout repeatingTaskWithTimeout = new RepeatingTaskWithTimeout(new Runnable() {
		@Override
		public void run() {
			fetchSSID();
		}
	}, 0, 0.5, 5, new RepeatingTaskWithTimeout.Callback() {
		@Override
		public void onTaskStarted() {
			Log.d(TAG, "【onTaskStarted】");
		}

		@Override
		public void onTaskStopped() {
			Log.d(TAG, "【onTaskStopped】");
		}

		@Override
		public void onTaskError(Exception e) {
			Log.d(TAG, "【onTaskError】");
		}

		@Override
		public void onTaskTimeout() {
			Log.d(TAG, "【onTaskTimeout】");
		}
	});

	public void clearWithResetCacheData() {
		ssid = null;
		model = null;
		localDevice = null;
	}

	public void fetchSSID() {
		Context context = getContext();
		if (context == null) {
			Log.e(TAG, "Context is null. Cannot fetch SSID.");
			return;
		}
		String currentSsid = getCurrentWifiSsid(context);
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

	@android.annotation.SuppressLint("MissingPermission")
	private String getCurrentWifiSsid(Context context) {
		WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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
}


