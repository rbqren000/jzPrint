package com.mx.mxSdk;

import static android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE;
import static com.mx.mxSdk.Opcode.WriteAutoPowerOffState;
import static com.mx.mxSdk.SppCenter.BluetoothUUID.notifyUuid;
import static com.mx.mxSdk.SppCenter.BluetoothUUID.serviceUuid;
import static com.mx.mxSdk.SppCenter.BluetoothUUID.writeUuid;
import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.mx.mxSdk.BleCenter.BleDevice;
import com.mx.mxSdk.BleCenter.BleManager;
import com.mx.mxSdk.BleCenter.beacon.Beacon;
import com.mx.mxSdk.BleCenter.beacon.BeaconItem;
import com.mx.mxSdk.BleCenter.gatt.data.CharacteristicInfo;
import com.mx.mxSdk.BleCenter.gatt.data.ServiceInfo;
import com.mx.mxSdk.OpencvUtils.OpenCVUtils;
import com.mx.mxSdk.Packet.BasePacket;
import com.mx.mxSdk.Packet.CMYKMultiRowDataPacket;
import com.mx.mxSdk.Packet.LogoPacket;
import com.mx.mxSdk.Packet.MultiRowDataPacket;
import com.mx.mxSdk.Packet.OtaPacket;
import com.mx.mxSdk.Serial.SerialThread;
import com.mx.mxSdk.SppCenter.BluetoothAdapterUtils;
import com.mx.mxSdk.SppCenter.BluetoothDiscoverUtils;
import com.mx.mxSdk.SppCenter.BluetoothSocketThread;
import com.mx.mxSdk.SppCenter.BluetoothUtils;
import com.mx.mxSdk.SppCenter.ConnectA2dpThread;
import com.mx.mxSdk.SppCenter.PairThread;
import com.mx.mxSdk.SppCenter.ReadThread;
import com.mx.mxSdk.SppCenter.WriteThread;
import com.mx.mxSdk.Utils.Arrays;
import com.mx.mxSdk.Utils.RBQLog;
import com.mx.mxSdk.Utils.MxSdkStore;
import com.mx.mxSdk.WifiCenter.TcpClientThread;
import com.mx.mxSdk.WifiCenter.UdpServiceThread;
import com.mx.mxSdk.BleCenter.gatt.callback.BleConnectCallback;
import com.mx.mxSdk.BleCenter.gatt.callback.BleMtuCallback;
import com.mx.mxSdk.BleCenter.gatt.callback.BleNotifyCallback;
import com.mx.mxSdk.BleCenter.gatt.callback.BleWriteCallback;
import com.mx.mxSdk.BleCenter.scan.BleScanCallback;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;
import android_serialport_api.SerialPortFinder;
import tp.xmaihh.serialport.stick.AbsStickPackageHelper;
import tp.xmaihh.serialport.stick.BaseStickPackageHelper;

/**
 * Created by rbq on 2017/5/17.
 */
public class ConnectManager implements BluetoothDiscoverUtils.OnBluetoothDeviceListener,
		ConnectA2dpThread.OnA2dpConnListener,
		BluetoothSocketThread.OnSocketConnListener,
		ReadThread.OnReadDataListener,
		TcpClientThread.OnTCPConnectListener,
		SerialThread.OnSerialConnectListener {

	public static final int SyncingDataError = 100;
	public static final int CommandQueueIsNoEmptyError = 200;
	private static final String TAG = ConnectManager.class.getSimpleName();
	private static final int MAX_LOSE_HEART_TIMES = 4;
	//心跳
	private final Handler heartHandler = new Handler(Looper.getMainLooper());

	//等待机器相应handler
	private final Handler waitResponseHandler = new Handler(Looper.getMainLooper());
	private static final int retrySendDataTime = 3*1000;

	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private void runOnUiThread(Runnable runnable) {
		if (isAndroidMainThread()) {
			runnable.run();
		} else {
			mainHandler.post(runnable);
		}
	}
	private static boolean isAndroidMainThread() {
		return Looper.myLooper() == Looper.getMainLooper();
	}

	private static final ConnectManager SPP_MANAGER = new ConnectManager();
	//普通指令和XModem指令都用这个
	private static final Object SPP_TAG_NORMAL_COMMAND = 1000;
	//发送同步数据的指令。。。
	private static final Object SPP_TAG_NORMAL_SYNCING_COMMAND = 2000;
	//使用到的线程工具类
	private final PairThread pairThread = new PairThread();
	private final ConnectA2dpThread connectA2DpThread = new ConnectA2dpThread();
	private final BluetoothSocketThread bluetoothSocketThread = new BluetoothSocketThread();//经典蓝牙的socket
	private final ReadThread readThread = new ReadThread();
	private final WriteThread writeThread = new WriteThread();
	private final TcpClientThread tcpClientThread = new TcpClientThread();//wifi的socket

	protected final ArrayList<CommandContext> commandQueue = new ArrayList<CommandContext>();
	//指令发送handler
	private final Handler commandHandler = new Handler(Looper.getMainLooper());

	private static final long delayCheckHeartTime = 5*1000;//0.6秒

	private static final long commandInterval = 600;//0.6秒
	private volatile long lastSendCommandTime = 0;

	//接收json数据 - 使用 JsonStreamAssembler 替代原有逻辑
	private JsonStreamAssembler jsonStreamAssembler;

	/**
	 * 是否正在接收json
	 * @return the boolean
	 */
	public boolean isJsonDataReceiving() {
		return jsonStreamAssembler != null && jsonStreamAssembler.isReceiving();
	}

	private static Application application;
	private int send_packet_Index = 0;
	private final MultiRowDataPacket multiRowDataPacket = new MultiRowDataPacket();
	private final CMYKMultiRowDataPacket cmykMultiRowDataPacket = new CMYKMultiRowDataPacket();
	private final OtaPacket otaPacket = new OtaPacket();
	private final LogoPacket logoPacket = new LogoPacket();

	//下面使用到的工具类
	private BluetoothDiscoverUtils bluetoothDiscoverUtils;
	private volatile boolean isConning = false;//用来标志整个流程是否正在进行
	private Device device;
	private static final int tryTime = 5;//默认的尝试连接次数
	private BluetoothSocket bluetoothSocket;
	private Socket wifiSocket;
	private InputStream inputStream;
	private OutputStream outputStream;
	private volatile int deviceHeartLooseTimes;
	private BleManager bleManager;
	private int sequenceNumber = 1;
	private final Random random = new SecureRandom();
	private final UdpServiceThread discoverUdpServiceThread = new UdpServiceThread();
	/**
	 * 配网相关的参数
	 */
	public static final float defaultTimeoutValue = 60.0f;
	private final UdpServiceThread distributionNetworkUdpServiceThread = new UdpServiceThread();
	private String ssid;
	private String password;
	private DistNetDevice distNetDevice;
	private final ArrayList<BasePacket> packets = new ArrayList<>();

	public void startConning() {
		isConning = true;
	}

	private void cancelConning(){
		isConning = false;
	}

	/**
	 * 开始调用连接true，断开连接后 false
	 * @return the boolean
	 */
	public boolean isConning() {
		return isConning;
	}

	//是否正在发送打印数据
	private volatile boolean isDataSending = false;

	//是否正在和打印机同步数据
	private volatile boolean isDataSynchronize = false;

	// 追踪待同步的 opcode 集合
	private final Set<Integer> pendingSyncOpcodes = Collections.synchronizedSet(new HashSet<>());
	
	// 同步超时相关
	private volatile long lastSyncActivityTime = 0;
	private static final long SYNC_IDLE_TIMEOUT = 5000; // 5秒无响应则超时
	private static final long SYNC_START_CHECK_DELAY = 2000; // 延迟2秒后开始超时检测（等待最后一条指令发送）
	private static final long SYNC_COMPLETE_DELAY = 1500; // 必须指令完成后延迟1.5秒再通知完成（等待可选指令响应）
	private final Handler syncTimeoutHandler = new Handler(Looper.getMainLooper());

	public boolean isDataSending() {
		return isDataSending;
	}

	public boolean isDataSynchronize() {
		return isDataSynchronize;
	}

	private boolean openDataSynchronize = true;

	public boolean isOpenDataSynchronize() {
		return openDataSynchronize;
	}

	public void setOpenDataSynchronize(boolean openDataSynchronize) {
		this.openDataSynchronize = openDataSynchronize;
	}

	private boolean isOpenHeartbeat;
	private static final long heartbeatInterval = 3;//3秒

	private final Handler serialConnHandler = new Handler(Looper.getMainLooper());

	private final ArrayList<OnDeviceBluetoothStateListener> onDeviceBluetoothStateListeners = new ArrayList<OnDeviceBluetoothStateListener>();
	/**
	 * 注册设备蓝牙状态事件
	 * @param onDeviceBluetoothStateListener  设备蓝牙状态事件
	 */
	public void registerDeviceBluetoothStateListener(OnDeviceBluetoothStateListener onDeviceBluetoothStateListener) {
		if (!onDeviceBluetoothStateListeners.contains(onDeviceBluetoothStateListener)) {
			onDeviceBluetoothStateListeners.add(onDeviceBluetoothStateListener);
		}
	}

	/**
	 * 注销设备蓝牙状态事件
	 * @param onDeviceBluetoothStateListener 设备蓝牙状态事件
	 */
	public void unregisterDeviceBluetoothStateListener(OnDeviceBluetoothStateListener onDeviceBluetoothStateListener) {
		onDeviceBluetoothStateListeners.remove(onDeviceBluetoothStateListener);
	}

	/**
	 * 通知设备蓝牙正在打开
	 */
	private void notifyDeviceBlueToothOpening() {
		for (OnDeviceBluetoothStateListener onDeviceBluetoothStateListener : onDeviceBluetoothStateListeners) {
			runOnUiThread(onDeviceBluetoothStateListener::onDeviceBlueToothOpening);
		}
	}

	/**
	 * 设备蓝牙已打开
	 */
	private void notifyDeviceBlueToothOpened() {
		for (OnDeviceBluetoothStateListener onDeviceBluetoothStateListener : onDeviceBluetoothStateListeners) {
			runOnUiThread(onDeviceBluetoothStateListener::onDeviceBlueToothOpened);
		}
	}

	/**
	 * 设备蓝牙正在关闭
	 */
	private void notifyDeviceBlueToothClosing() {
		for (OnDeviceBluetoothStateListener onDeviceBluetoothStateListener : onDeviceBluetoothStateListeners) {
			runOnUiThread(onDeviceBluetoothStateListener::onDeviceBlueToothClosing);
		}
	}

	/**
	 * 设备蓝牙已关闭
	 */
	private void notifyDeviceBlueToothClosed() {
		for (OnDeviceBluetoothStateListener onDeviceBluetoothStateListener : onDeviceBluetoothStateListeners) {
			runOnUiThread(onDeviceBluetoothStateListener::onDeviceBlueToothClosed);
		}
	}

	/**
	 * 设备发现事件
	 */
	private final ArrayList<OnDeviceDiscoverListener> onDeviceDiscoverListeners = new ArrayList<OnDeviceDiscoverListener>();
	/**
	 * Register device discover listener.
	 *
	 * @param onDeviceDiscoverListener the on device discover listener
	 */
	public void registerDeviceDiscoverListener(OnDeviceDiscoverListener onDeviceDiscoverListener) {
		if (!onDeviceDiscoverListeners.contains(onDeviceDiscoverListener)) {
			onDeviceDiscoverListeners.add(onDeviceDiscoverListener);
		}
	}
	/**
	 * Unregister device discover listener.
	 *
	 * @param onDeviceDiscoverListener the on device discover listener
	 */
	public void unregisterDeviceDiscoverListener(OnDeviceDiscoverListener onDeviceDiscoverListener) {
		onDeviceDiscoverListeners.remove(onDeviceDiscoverListener);
	}
	private void notifyStartDiscover() {
		for (OnDeviceDiscoverListener onDeviceDiscoverListener : onDeviceDiscoverListeners) {
			runOnUiThread(onDeviceDiscoverListener::onDeviceStartDiscover);
		}
	}
	private void notifyStopDiscover() {
		for (OnDeviceDiscoverListener onDeviceDiscoverListener : onDeviceDiscoverListeners) {
			runOnUiThread(onDeviceDiscoverListener::onDeviceStopDiscover);
		}
	}
	private void notifyDiscoveredDevice(Device device) {
		for (OnDeviceDiscoverListener onDeviceDiscoverListener : onDeviceDiscoverListeners) {
			runOnUiThread(() -> onDeviceDiscoverListener.onDeviceDiscovered(device));
		}
	}

	private final ArrayList<OnConnModelDiscoverListener> onConnModelDiscoverListeners = new ArrayList<OnConnModelDiscoverListener>();
	public void registerConnModelDiscoverListeners(OnConnModelDiscoverListener onConnModelDiscoverListener) {
		if (!onConnModelDiscoverListeners.contains(onConnModelDiscoverListener)) {
			onConnModelDiscoverListeners.add(onConnModelDiscoverListener);
		}
	}
	public void unregisterConnModelDiscoverListeners(OnConnModelDiscoverListener onConnModelDiscoverListener) {
		onConnModelDiscoverListeners.remove(onConnModelDiscoverListener);
	}

	private void notifyConnModelStartDiscover() {
		for (OnConnModelDiscoverListener onConnModelDiscoverListener : onConnModelDiscoverListeners) {
			runOnUiThread(onConnModelDiscoverListener::onConnModelStartDiscover);
		}
	}
	private void notifyConnModelStopDiscover() {
		for (OnConnModelDiscoverListener onConnModelDiscoverListener : onConnModelDiscoverListeners) {
			runOnUiThread(onConnModelDiscoverListener::onConnModelStopDiscover);
		}
	}
	private void notifyConnModelDiscoveredDevice(ConnModel connModel) {
		for (OnConnModelDiscoverListener onConnModelDiscoverListener : onConnModelDiscoverListeners) {
			runOnUiThread(() -> onConnModelDiscoverListener.onConnModelDiscovered(connModel));
		}
	}


	private final ArrayList<OnDeviceBondListener> onDeviceBondListeners = new ArrayList<OnDeviceBondListener>();

	/**
	 * Register device bond listener.
	 *
	 * @param onDeviceBondListener the on device bond listener
	 */
	public void registerDeviceBondListener(OnDeviceBondListener onDeviceBondListener) {
		if (!onDeviceBondListeners.contains(onDeviceBondListener)) {
			onDeviceBondListeners.add(onDeviceBondListener);
		}
	}

	/**
	 * Unregister device bond listener.
	 *
	 * @param onDeviceBondListener the on device bond listener
	 */
	public void unregisterDeviceBondListener(OnDeviceBondListener onDeviceBondListener) {
		onDeviceBondListeners.remove(onDeviceBondListener);
	}
	private void notifyDeviceBonding(Device device) {
		for (OnDeviceBondListener onDeviceBondListener : onDeviceBondListeners) {
			runOnUiThread(() -> onDeviceBondListener.onDeviceBonding(device));
		}
	}
	private void notifyDeviceBonded(Device device) {

		for (OnDeviceBondListener onDeviceBondListener : onDeviceBondListeners) {
			runOnUiThread(() -> onDeviceBondListener.onDeviceBonded(device));
		}
	}
	private void notifyDeviceDisBond(Device device) {

		for (OnDeviceBondListener onDeviceBondListener : onDeviceBondListeners) {
			runOnUiThread(() -> onDeviceBondListener.onDeviceDisBond(device));
		}
	}

	private final ArrayList<OnDeviceConnectListener> onDeviceConnectListeners = new ArrayList<OnDeviceConnectListener>();
	/**
	 * Register device connect listener.
	 *
	 * @param onDeviceConnectListener the on device connect listener
	 */
	public void registerDeviceConnectListener(OnDeviceConnectListener onDeviceConnectListener) {
		if (!onDeviceConnectListeners.contains(onDeviceConnectListener)) {
			onDeviceConnectListeners.add(onDeviceConnectListener);
		}
	}
	/**
	 * Unregister device connect listener.
	 *
	 * @param onDeviceConnectListener the on device connect listener
	 */
	public void unregisterDeviceConnectListener(OnDeviceConnectListener onDeviceConnectListener) {
		onDeviceConnectListeners.remove(onDeviceConnectListener);
	}
	private void notifyDeviceConnectStart(Device device) {

		for (OnDeviceConnectListener onDeviceConnectListener : onDeviceConnectListeners) {
			runOnUiThread(() -> onDeviceConnectListener.onDeviceConnectStart(device));
		}
	}
	private void notifyDeviceConnectSucceed(Device device) {

		for (OnDeviceConnectListener onDeviceConnectListener : onDeviceConnectListeners) {
			runOnUiThread(() -> onDeviceConnectListener.onDeviceConnectSucceed(device));
		}
	}
	private void notifyDeviceDisconnect(Device device) {

		for (OnDeviceConnectListener onDeviceConnectListener : onDeviceConnectListeners) {
			runOnUiThread(() -> onDeviceConnectListener.onDeviceDisconnect(device));
		}
	}

	private void notifyDeviceConnectFail(Device device, String error) {

		for (OnDeviceConnectListener onDeviceConnectListener : onDeviceConnectListeners) {
			runOnUiThread(() -> onDeviceConnectListener.onDeviceConnectFail(device, error));
		}
	}

	//******************************接收到数据分发出去******************************
	private final ArrayList<OnReceiveMsgListener> onReceiveMsgListeners = new ArrayList<OnReceiveMsgListener>();
	/**
	 * Register receive message listener.
	 *
	 * @param onReceiveMsgListener the on receive msg listener
	 */
	public void registerReceiveMessageListener(OnReceiveMsgListener onReceiveMsgListener) {
		if (!onReceiveMsgListeners.contains(onReceiveMsgListener)) {
			onReceiveMsgListeners.add(onReceiveMsgListener);
		}
	}
	/**
	 * Unregister receive message listener.
	 *
	 * @param onReceiveMsgListener the on receive msg listener
	 */
	public void unregisterReceiveMessageListener(OnReceiveMsgListener onReceiveMsgListener) {
		onReceiveMsgListeners.remove(onReceiveMsgListener);
	}
	private void notifyReadPrinterHeadParameter(Device device, int headValue, int l_pix, int p_pix, int distance) {
		for (OnReceiveMsgListener onReceiveMsgListener : onReceiveMsgListeners) {
			runOnUiThread(() -> onReceiveMsgListener.onReadPrinterHeadParameter(device, headValue, l_pix, p_pix, distance));
		}
	}
	private void notifyReadCirculationAndRepeatTime(Device device, int circulation_time, int repeat_time) {
		for (OnReceiveMsgListener onReceiveMsgListener : onReceiveMsgListeners) {
			runOnUiThread(() -> onReceiveMsgListener.onReadCirculationAndRepeatTime(device, circulation_time, repeat_time));
		}
	}
	private void notifyReadDirection(Device device,int oldHorizontalDirection,int horizontalDirection,int oldVerticalDirection,int verticalDirection) {
		for (OnReceiveMsgListener onReceiveMsgListener : onReceiveMsgListeners) {
			runOnUiThread(() -> onReceiveMsgListener.onReadDirection(device, oldVerticalDirection,horizontalDirection,oldVerticalDirection, verticalDirection));
		}
	}
	private void notifyReadSoftwareInfo(Device device, String id, String name, String mcu_version, String mcu_date) {
		for (OnReceiveMsgListener onReceiveMsgListener : onReceiveMsgListeners) {
			runOnUiThread(() -> onReceiveMsgListener.onReadSoftwareInfo(device, id, name, mcu_version, mcu_date));
		}
	}
	private void notifyTemperature(Device device, int temp) {
		for (OnReceiveMsgListener onReceiveMsgListener : onReceiveMsgListeners) {
			runOnUiThread(() -> onReceiveMsgListener.onReadTemperature(device, temp));
		}
	}
	private void notifyReadBattery(Device device, int bat) {
		for (OnReceiveMsgListener onReceiveMsgListener : onReceiveMsgListeners) {
			runOnUiThread(() -> onReceiveMsgListener.onReadBattery(device, bat));
		}
	}
	private void notifyReadCartridgeId(Device device,String cartridgeId) {
		for (OnReceiveMsgListener onReceiveMsgListener : onReceiveMsgListeners) {
			runOnUiThread(() -> onReceiveMsgListener.onReadCartridgeId(device,cartridgeId));
		}
	}
	private void notifyReadSilentState(Device device,boolean silentState) {
		for (OnReceiveMsgListener onReceiveMsgListener : onReceiveMsgListeners) {
			runOnUiThread(() -> onReceiveMsgListener.onReadSilentState(device,silentState));
		}
	}
	private void notifyReadAutoPowerOffState(Device device,boolean autoPowerOff) {
		for (OnReceiveMsgListener onReceiveMsgListener : onReceiveMsgListeners) {
			runOnUiThread(() -> onReceiveMsgListener.onReadAutoPowerOffState(device,autoPowerOff));
		}
	}

	private void notifyReadContinuousPrintState(Device device,boolean continuousPrintState)  {
		for (OnReceiveMsgListener onReceiveMsgListener : onReceiveMsgListeners) {
			runOnUiThread(() -> onReceiveMsgListener.onReadContinuousPrintState(device,continuousPrintState));
		}
	}

	private void notifyError(Device device, String error) {
		for (OnReceiveMsgListener onReceiveMsgListener : onReceiveMsgListeners) {
			runOnUiThread(() -> onReceiveMsgListener.onError(device, error));
		}
	}


	/* 读取命令事件 */
	private final ArrayList<OnReadGeneralCommandListener> onReadGeneralCommandListeners = new ArrayList<>();

	/**
	 * 注册读取命令监听器.
	 *
	 * @param onReadGeneralCommandListener 读取命令监听器
	 */
	public void registerReadGeneralCommandListener(OnReadGeneralCommandListener onReadGeneralCommandListener) {
		if (!onReadGeneralCommandListeners.contains(onReadGeneralCommandListener)) {
			onReadGeneralCommandListeners.add(onReadGeneralCommandListener);
		}
	}

	/**
	 * 注销读取命令监听器.
	 *
	 * @param onReadGeneralCommandListener 读取命令监听器
	 */
	public void unregisterReadGeneralCommandListener(OnReadGeneralCommandListener onReadGeneralCommandListener) {
		onReadGeneralCommandListeners.remove(onReadGeneralCommandListener);
	}

	private void notifyReadGeneralCommand(int opcode, String json) {
		for (OnReadGeneralCommandListener onReadGeneralCommandListener : onReadGeneralCommandListeners) {
			runOnUiThread(() -> onReadGeneralCommandListener.onReadGeneralCommand(opcode, json));
		}
	}


	/* 读取打印开始命令事件 */
	private final ArrayList<OnReadPrintStartCommandListener> onReadPrintStartCommandListeners = new ArrayList<>();

	/**
	 * 注册读取打印开始命令监听器.
	 *
	 * @param onReadPrintStartCommandListener 读取打印开始命令监听器
	 */
	public void registerReadPrintStartCommandListener(OnReadPrintStartCommandListener onReadPrintStartCommandListener) {
		if (!onReadPrintStartCommandListeners.contains(onReadPrintStartCommandListener)) {
			onReadPrintStartCommandListeners.add(onReadPrintStartCommandListener);
		}
	}

	/**
	 * 注销读取打印开始命令监听器.
	 *
	 * @param onReadPrintStartCommandListener 读取打印开始命令监听器
	 */
	public void unregisterReadPrintStartCommandListener(OnReadPrintStartCommandListener onReadPrintStartCommandListener) {
		onReadPrintStartCommandListeners.remove(onReadPrintStartCommandListener);
	}

	private void notifyReadPrintStartCommand() {
		for (OnReadPrintStartCommandListener onReadPrintStartCommandListener : onReadPrintStartCommandListeners) {
			runOnUiThread(onReadPrintStartCommandListener::onReadStartPrintCommand);
		}
	}


	private final ArrayList<OnDataProgressListener> onDataProgressListeners = new ArrayList<OnDataProgressListener>();
	/**
	 * Register multi row data transfer listener.
	 *
	 * @param onDataProgressListener the on multi row data transfer listener
	 */
	public void registerDataProgressListener(OnDataProgressListener onDataProgressListener) {

		if (!onDataProgressListeners.contains(onDataProgressListener)) {
			onDataProgressListeners.add(onDataProgressListener);
		}
	}
	/**
	 * Unregister multi row data transfer listener.
	 *
	 * @param onDataProgressListener the on multi row data transfer listener
	 */
	public void unregisterDataProgressListener(OnDataProgressListener onDataProgressListener) {

		this.onDataProgressListeners.remove(onDataProgressListener);
	}
	private void notifyDataProgressStart(float size, long startTime) {
		for (OnDataProgressListener onDataProgressListener : onDataProgressListeners) {
			runOnUiThread(() -> onDataProgressListener.onDataProgressStart(size, 0, startTime));
		}
	}
	/**
	 * Notify multi row data transfer progress.
	 *
	 * @param size        the size
	 * @param progress    the progress
	 * @param startTime   the start time
	 * @param currentTime the current time
	 */
	public void notifyDataProgressProgress(float size, int progress, long startTime, long currentTime) {
		for (OnDataProgressListener onDataProgressListener : onDataProgressListeners) {
			runOnUiThread(() -> onDataProgressListener.onDataProgress(size, progress, startTime, currentTime));
		}
	}
	/**
	 * Notify multi row data transfer finish.
	 *
	 * @param size        the size
	 * @param startTime   the start time
	 * @param currentTime the current time
	 */
	public void notifyDataProgressFinish(float size, long startTime, long currentTime) {
		for (OnDataProgressListener onDataProgressListener : onDataProgressListeners) {
			runOnUiThread(() -> onDataProgressListener.onDataProgressFinish(size, startTime, currentTime));
		}
	}
	private void notifyDataProgressError(String error,int code) {
		for (OnDataProgressListener onDataProgressListener : onDataProgressListeners) {
			runOnUiThread(() -> onDataProgressListener.onDataProgressError(error,code));
		}
	}

	/*发现网络设备事件*/
	private final ArrayList<OnDistNetDeviceDiscoverListener> onDistNetDeviceDiscoverListeners = new ArrayList<OnDistNetDeviceDiscoverListener>();
	/**
	 * Register dist net device discover listener.
	 *
	 * @param onDistNetDeviceDiscoverListener the on dist net device discover listener
	 */
	public void registerDistNetDeviceDiscoverListener(OnDistNetDeviceDiscoverListener onDistNetDeviceDiscoverListener) {

		if (!onDistNetDeviceDiscoverListeners.contains(onDistNetDeviceDiscoverListener)) {
			onDistNetDeviceDiscoverListeners.add(onDistNetDeviceDiscoverListener);
		}
	}
	/**
	 * Unregister dist net device discover listener.
	 *
	 * @param onDistNetDeviceDiscoverListener the on dist net device discover listener
	 */
	public void unregisterDistNetDeviceDiscoverListener(OnDistNetDeviceDiscoverListener onDistNetDeviceDiscoverListener) {
		this.onDistNetDeviceDiscoverListeners.remove(onDistNetDeviceDiscoverListener);
	}
	private void notifyDistNetDeviceDiscoverStart() {
		for (OnDistNetDeviceDiscoverListener onDistNetDeviceDiscoverListener : onDistNetDeviceDiscoverListeners) {
			runOnUiThread(onDistNetDeviceDiscoverListener::onDistNetDeviceDiscoverStart);
		}
	}
	private void notifyDistNetDeviceDiscover(DistNetDevice device) {
		for (OnDistNetDeviceDiscoverListener onDistNetDeviceDiscoverListener : onDistNetDeviceDiscoverListeners) {
			runOnUiThread(() -> onDistNetDeviceDiscoverListener.onDistNetDeviceDiscover(device));
		}
	}
	private void notifyDistNetDeviceDiscoverCancel() {
		for (OnDistNetDeviceDiscoverListener onDistNetDeviceDiscoverListener : onDistNetDeviceDiscoverListeners) {
			runOnUiThread(onDistNetDeviceDiscoverListener::onDistNetDeviceDiscoverCancel);
		}
	}

	/*配网事件*/
	private final ArrayList<OnDistributionNetworkListener> onDistributionNetworkListeners = new ArrayList<OnDistributionNetworkListener>();
	/**
	 * Register distribution network listener.
	 *
	 * @param onDistributionNetworkListener the on distribution network listener
	 */
	public void registerDistributionNetworkListener(OnDistributionNetworkListener onDistributionNetworkListener) {

		if (!onDistributionNetworkListeners.contains(onDistributionNetworkListener)) {
			onDistributionNetworkListeners.add(onDistributionNetworkListener);
		}
	}
	/**
	 * Unregister distribution network listener.
	 *
	 * @param onDistributionNetworkListener the on distribution network listener
	 */
	public void unregisterDistributionNetworkListener(OnDistributionNetworkListener onDistributionNetworkListener) {
		this.onDistributionNetworkListeners.remove(onDistributionNetworkListener);
	}
	private void notifyDistributionNetworkStart() {
		for (OnDistributionNetworkListener onDistributionNetworkListener : onDistributionNetworkListeners) {
			runOnUiThread(onDistributionNetworkListener::onDistributionNetworkStart);
		}
	}
	private void notifyDistributionNetworkFail() {
		for (OnDistributionNetworkListener onDistributionNetworkListener : onDistributionNetworkListeners) {
			runOnUiThread(onDistributionNetworkListener::onDistributionNetworkFail);
		}
	}
	private void notifyDistributionNetworkTimeOut() {
		for (OnDistributionNetworkListener onDistributionNetworkListener : onDistributionNetworkListeners) {
			runOnUiThread(onDistributionNetworkListener::onDistributionNetworkTimeOut);
		}
	}
	private void notifyDistributionNetworkSucceed(Device device) {
		for (OnDistributionNetworkListener onDistributionNetworkListener : onDistributionNetworkListeners) {
			runOnUiThread(() -> onDistributionNetworkListener.onDistributionNetworkSucceed(device));
		}
	}

	private final ArrayList<OnPrintListener> onPrintListeners = new ArrayList<OnPrintListener>();
	/**
	 * Register print listener.
	 *
	 * @param onPrintListener the on print listener
	 */
	public void registerPrintListener(OnPrintListener onPrintListener) {

		if (!onPrintListeners.contains(onPrintListener)) {
			onPrintListeners.add(onPrintListener);
		}
	}
	/**
	 * Unregister print listener.
	 *
	 * @param onPrintListener the on print listener
	 */
	public void unregisterPrintListener(OnPrintListener onPrintListener) {
		this.onPrintListeners.remove(onPrintListener);
	}
	private void notifyPrintStart(int beginIndex,int endIndex,int currentIndex) {
		for (OnPrintListener onPrintListener : onPrintListeners) {
			runOnUiThread(() -> onPrintListener.onPrintStart(beginIndex,endIndex,currentIndex));
		}
	}
	private void notifyPrintComplete(int beginIndex,int endIndex,int currentIndex,String cartridgeId) {
		for (OnPrintListener onPrintListener : onPrintListeners) {
			runOnUiThread(() -> onPrintListener.onPrintComplete(beginIndex,endIndex,currentIndex, cartridgeId));
		}
	}


	private final ArrayList<OnCommandWriteListener> onCommandWriteListeners = new ArrayList<OnCommandWriteListener>();
	public void registerCommandWriteListener(OnCommandWriteListener onCommandWriteListener) {

		if (!onCommandWriteListeners.contains(onCommandWriteListener)) {
			onCommandWriteListeners.add(onCommandWriteListener);
		}
	}
	public void unregisterCommandWriteListener(OnCommandWriteListener onCommandWriteListener) {
		this.onCommandWriteListeners.remove(onCommandWriteListener);
	}
	private void notifyCommandWriteSuccess(Device device,Command command,Object object) {
		for (OnCommandWriteListener onCommandWriteListener : onCommandWriteListeners) {
			runOnUiThread(() -> onCommandWriteListener.onCommandWriteSuccess(device,command,object));
		}
	}
	private void notifyCommandWriteError(Device device,Command command,String errorMsg) {
		for (OnCommandWriteListener onCommandWriteListener : onCommandWriteListeners) {
			runOnUiThread(() -> onCommandWriteListener.onCommandWriteError(device,command,errorMsg));
		}
	}


	private final ArrayList<OnDataWriteListener> onDataWriteListeners = new ArrayList<OnDataWriteListener>();
	public void registerDataWriteListener(OnDataWriteListener onDataWriteListener) {

		if (!onDataWriteListeners.contains(onDataWriteListener)) {
			onDataWriteListeners.add(onDataWriteListener);
		}
	}
	public void unregisterDataWriteListener(OnDataWriteListener onDataWriteListener) {
		this.onDataWriteListeners.remove(onDataWriteListener);
	}
	private void notifyDataWriteSuccess(Device device,DataObj dataObj,Object object) {
		for (OnDataWriteListener onDataWriteListener : onDataWriteListeners) {
			runOnUiThread(() -> onDataWriteListener.onDataWriteSuccess(device,dataObj,object));
		}
	}
	private void notifyDataWriteError(Device device,DataObj obj,String errorMsg) {
		for (OnDataWriteListener onDataWriteListener : onDataWriteListeners) {
			runOnUiThread(() -> onDataWriteListener.onDataWriteError(device,obj,errorMsg));
		}
	}


	private final ArrayList<OnDataSynchronizeListener> onDataSynchronizeListeners = new ArrayList<>();
	public void registerDataSynchronizeListener(OnDataSynchronizeListener onDataSynchronizeListener) {

		if (!onDataSynchronizeListeners.contains(onDataSynchronizeListener)) {
			onDataSynchronizeListeners.add(onDataSynchronizeListener);
		}
	}
	public void unregisterDataSynchronizeListener(OnDataSynchronizeListener onDataSynchronizeListener) {
		this.onDataSynchronizeListeners.remove(onDataSynchronizeListener);
	}
	private void notifyDataSynchronizeStart(Device device) {
		for (OnDataSynchronizeListener onDataSynchronizeListener : onDataSynchronizeListeners) {
			runOnUiThread(() -> onDataSynchronizeListener.onDataSynchronizeStart(device));
		}
	}
	private void notifyDataSynchronizeComplete(Device device) {
		for (OnDataSynchronizeListener onDataSynchronizeListener : onDataSynchronizeListeners) {
			runOnUiThread(() -> onDataSynchronizeListener.onDataSynchronizeComplete(device));
		}
	}
	private void notifyDataSynchronizeTimeout(Device device, int pendingCount) {
		for (OnDataSynchronizeListener onDataSynchronizeListener : onDataSynchronizeListeners) {
			runOnUiThread(() -> onDataSynchronizeListener.onDataSynchronizeTimeout(device, pendingCount));
		}
	}
	private void notifyDataSynchronizeInterrupted(Device device) {
		for (OnDataSynchronizeListener onDataSynchronizeListener : onDataSynchronizeListeners) {
			runOnUiThread(() -> onDataSynchronizeListener.onDataSynchronizeInterrupted(device));
		}
	}


	private final ArrayList<OnReadJsonListener> onReadJsonListeners = new ArrayList<OnReadJsonListener>();
	public void registerReadJsonListener(OnReadJsonListener onReadJsonListener) {

		if (!onReadJsonListeners.contains(onReadJsonListener)) {
			onReadJsonListeners.add(onReadJsonListener);
		}
	}
	public void unregisterReadJsonListener(OnReadJsonListener onReadJsonListener) {
		this.onReadJsonListeners.remove(onReadJsonListener);
	}
	private void notifyReadJson(Device device,String json) {
		for (OnReadJsonListener onReadJsonListener : onReadJsonListeners) {
			runOnUiThread(() -> onReadJsonListener.onReadJson(device,json));
		}
	}

	/**
	 * Share connect manager.
	 *
	 * @return the connect manager
	 */
	public synchronized static ConnectManager share() {
		return SPP_MANAGER;
	}
	/**
	 * 初始化方法，要求在使用前先进行初始化
	 *
	 * @param _application 初始化
	 */
	public synchronized void init(@NonNull Application _application) {
		//这里无论如何调用init方法，都只能初始化一次内部的值
		if (application == null) {

			RBQLog.i("初始化SppManager");

			application = _application;
			/*加载opencv*/
			OpenCVUtils.staticLoadCVLibraries();
			/*清除缓存*/
			MxSdkStore.clearImageCache(application,null);
			MxSdkStore.clearDataCache(application,null);

			packets.add(multiRowDataPacket);
			packets.add(cmykMultiRowDataPacket);
			packets.add(logoPacket);
			packets.add(otaPacket);

			// 初始化 JSON 流组装器
			jsonStreamAssembler = new JsonStreamAssembler(this::dispatchJsonEven);

			this.bluetoothDiscoverUtils = new BluetoothDiscoverUtils(application);
			this.bluetoothDiscoverUtils.registerBluetoothBroadcastReceiver();
			this.bluetoothDiscoverUtils.registerBluetoothDeviceDiscoverListener(this);

			this.connectA2DpThread.registerA2dpConnListener(this);
			this.bluetoothSocketThread.registerSocketConnListener(this);
			this.tcpClientThread.registerSocketConnListener(this);
			this.readThread.registerReadDataListener(this);
			this.serialThread.registerSerialConnectListener(this);

			this.initBleManager();

			this.distributionNetworkUdpServiceThread.registerUpdMonitorListener(onUpdMonitorListener);
			this.discoverUdpServiceThread.registerUpdMonitorListener(discoverDeviceUpdMonitorListener);

		}
	}

	/**
	 * 释放所有资源
	 */
	public void destroy() {

		//断开连接
		writeConnectStateDisconnected();

		cancelConning();
		isOpenHeartbeat = false;
		clearDataSynchronizeState();
		if (bluetoothDiscoverUtils != null) {
			this.bluetoothDiscoverUtils.unregisterBluetoothBroadcastReceiver();
			this.bluetoothDiscoverUtils.unregisterBluetoothDeviceDiscoverListener();
		}
		this.connectA2DpThread.unregisterA2dpConnListener();
		this.bluetoothSocketThread.unregisterSocketConnListener();
		this.tcpClientThread.unregisterSocketConnListener();
		this.readThread.unregisterReadDataListener();
		this.distributionNetworkUdpServiceThread.unregisterUpdMonitorListener();
		this.discoverUdpServiceThread.unregisterUpdMonitorListener();
		this.serialThread.unregisterSerialConnectListener();

		this.distributionNetworkUdpServiceThread.stopMonitorUdp();
		this.discoverUdpServiceThread.stopMonitorUdp();
		this.bluetoothSocketThread.cancel();
		this.tcpClientThread.cancel();
		this.connectA2DpThread.cancel();
		this.pairThread.cancel();
		this.readThread.cancel();
		this.writeThread.stopHeartbeat();
		this.writeThread.cancel();
		this.serialThread.cancel();

		if (this.bluetoothSocket != null) {
			try {
				this.bluetoothSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.bluetoothSocket = null;
		}
		if (this.wifiSocket != null){
			try {
				this.wifiSocket.close();
			}catch (IOException e){
				e.printStackTrace();
			}
			this.wifiSocket = null;
		}
		if (this.inputStream != null) {
			try {
				this.inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.inputStream = null;
		}
		if (this.outputStream != null) {
			try {
				this.outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.outputStream = null;
		}
		if (this.device != null){
			this.device = null;
		}
	}

	/**
	 * 判断蓝牙是否打开
	 * @return the boolean
	 */
	public boolean isEnable() {

		return BluetoothAdapterUtils.isEnabled();
	}

	/**
	 * 打开蓝牙
	 */
	public void enable() {
		if (isEnable()) {
			return;
		}
		BluetoothAdapterUtils.enable();
	}

	/**
	 * Disable.
	 */
	public void disable() {
		if (isEnable()) {
			return;
		}
		BluetoothAdapterUtils.disable();
	}

	public BluetoothDevice createRemoteDevice(String mac){
		return BluetoothAdapterUtils.getRemoteDevice(mac);
	}

	/**
	 * 是否在扫描
	 * @return the boolean
	 */
	public Boolean isDiscoveringBluetoothDevice() {
		if (bluetoothDiscoverUtils == null) {
			RBQLog.i("bluetoothDiscoverUtils 未初始化");
			return false;
		}
		return bluetoothDiscoverUtils.isDiscovering();
	}

	/**
	 * 开始扫描蓝牙设备
	 * @param scanTime the scan time
	 */
	public void discoverBluetoothDevice(float scanTime) {
		if (bluetoothDiscoverUtils == null) {
			RBQLog.i("bluetoothDiscoverUtils 未初始化");
			return;
		}
		if (!BluetoothAdapterUtils.isEnabled()) {
			return;
		}
		if (bluetoothDiscoverUtils.isDiscovering()){
			cancelDiscoveryBluetoothDevice();
			mainHandler.postDelayed(() -> {
				bluetoothDiscoverUtils.startDiscovery();
				if (scanTime>0){
					int time = (int) (scanTime*1000.0f);
					mainHandler.postDelayed(BluetoothRunnable,time);
				}
			},200);
		}else {
			bluetoothDiscoverUtils.startDiscovery();
			if (scanTime>0){
				int time = (int) (scanTime*1000.0f);
				mainHandler.postDelayed(BluetoothRunnable,time);
			}
		}
	}

	private final Runnable BluetoothRunnable = new Runnable() {
		@Override
		public void run() {
			cancelDiscoveryBluetoothDevice();
		}
	};

	/**
	 * 停止扫描
	 */
	public void cancelDiscoveryBluetoothDevice() {
		if (bluetoothDiscoverUtils == null) return;
		if (!bluetoothDiscoverUtils.isDiscovering() || !BluetoothAdapterUtils.isEnabled()) {
			return;
		}
		mainHandler.removeCallbacks(BluetoothRunnable);
		bluetoothDiscoverUtils.cancelDiscovery();
	}

	/**
	 *
	 * @param device 连接的设备
	 */
	public void connect(Device device) {
		if (device == null) {
			return;
		}
		if (device.isSPPConnType()) {
			connectSpp(device,false);
			return;
		}
		if (device.isApOrWifiConnType()){
			connectWifi(device,false);
			return;
		}
		if (device.isSerialConnType()){
			connectSerial(device,500000,1,8,false);
		}
	}

	/**
	 *
	 * @param device  连接的设备
	 * @param isOpenHeartbeat 是否开启app端向打印机的心跳
	 */
	public void connect(Device device,boolean isOpenHeartbeat) {
		if (device == null) {
			return;
		}
		if (device.isSPPConnType()) {
			connectSpp(device,isOpenHeartbeat);
			return;
		}
		if (device.isApOrWifiConnType()){
			connectWifi(device,isOpenHeartbeat);
			return;
		}
		if (device.isSerialConnType()){
			connectSerial(device,500000,1,8,isOpenHeartbeat);
		}
	}

	/**
	 * 连接Spp设备
	 * @param device
	 */
	public synchronized void connectSpp(Device device,boolean isOpenHeartbeat) {
		if (isConning) {
			return;
		}
		//这里标志接下来要进行配对、连接等操作
		startConning();
		this.device = device;
		this.isOpenHeartbeat = isOpenHeartbeat;
		cancelDiscoveryBluetoothDevice();
		// 开始配对或者开始连接蓝牙的socket
		notifyDeviceConnectStart(device);

		//说明设备正在配对、或者已经配对，则接下来的流程交给下面的响应事件进行流程传递，暂时不做处理
		if (BluetoothUtils.isBonding(device.bluetoothDevice)) {
			return;
		}
		if (BluetoothUtils.isBonded(device.bluetoothDevice)) {
			//进行socket连接
			bluetoothSocketThread.connect(device.bluetoothDevice, tryTime);
		} else {//进行配对操作
			pairThread.start(device.bluetoothDevice, tryTime);
		}
	}

	/**
	 * 连接wifi设备
	 * @param device
	 */
	public synchronized void connectWifi(Device device,boolean isOpenHeartbeat) {
		if (isConning) {
			return;
		}
		//这里标志接下来要进行配对、连接等操作
		startConning();
		this.device = device;
		this.isOpenHeartbeat = isOpenHeartbeat;
		tcpClientThread.connect(device.ip,device.port);
	}

	/**
	 * 连接串口设备
	 * @param device
	 * @param baudRate 波特率
	 * @param stopBits 数据停止位
	 * @param dataBits 数据位
	 */
	public synchronized void connectSerial(Device device,int baudRate,int stopBits,int dataBits,boolean isOpenHeartbeat){
		if (isConning){
			return;
		}
		startConning();
		this.device = device;
		this.isOpenHeartbeat = isOpenHeartbeat;
		serialThread.start(device.sPort,baudRate,stopBits,dataBits);
	}

	/**
	 * 断开连接
	 */
	public void disconnect() {
		if (device == null) return;
		if (device.isSPPConnType()){
			disconnectSpp(false);
			return;
		}
		if (device.isApOrWifiConnType()){
			disconnectWifi();
			return;
		}
		if (device.isSerialConnType()){
			disconnectSerial();
		}
	}

	public void disconnectIfBluetoothRemoveBond() {
		if (device == null) return;
		if (device.isSPPConnType()){
			disconnectSpp(true);
			return;
		}
		if (device.isApOrWifiConnType()){
			disconnectWifi();
			return;
		}
		if (device.isSerialConnType()){
			disconnectSerial();
		}
	}

	private void disconnectSpp(Boolean isRemoveBond) {//这里后面完善

		cancelConning();
		isOpenHeartbeat = false;
		clearDataSynchronizeState();
		readThread.cancel();
		writeThread.cancel();
		bluetoothSocketThread.cancel();
		pairThread.cancel();

		//移除配对
		if (this.device!=null
				&&this.device.isSPPConnType()
				&&isRemoveBond) {
			BluetoothUtils.removeBond(this.device.bluetoothDevice);
		}

		if (this.bluetoothSocket != null) {
			try {
				this.bluetoothSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.bluetoothSocket = null;
		}
		if (this.inputStream != null) {
			try {
				this.inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.inputStream = null;
		}
		if (this.outputStream != null) {
			try {
				this.outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.outputStream = null;
		}
	}

	private void disconnectWifi() {

		cancelConning();
		isOpenHeartbeat = false;
		clearDataSynchronizeState();
		stopMonitorHeartData();
		readThread.cancel();
		writeThread.cancel();

		if (this.wifiSocket != null) {
			try {
				this.wifiSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.wifiSocket = null;
		}
		if (this.inputStream != null) {
			try {
				this.inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.inputStream = null;
		}
		if (this.outputStream != null) {
			try {
				this.outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.outputStream = null;
		}
	}

	private void disconnectSerial() {

		// 发送一条连接断开指令
		writeConnectStateDisconnected();

		cancelConning();
		isOpenHeartbeat = false;
		clearDataSynchronizeState();
		serialThread.cancel();
		readThread.cancel();
		writeThread.stopHeartbeat();
		writeThread.cancel();
		if (this.inputStream != null) {
			try {
				this.inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.inputStream = null;
		}
		if (this.outputStream != null) {
			try {
				this.outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.outputStream = null;
		}
	}

	/**
	 * 判断是否已连接.
	 * @return the boolean
	 */
	public Boolean isConnected() {
		if (device == null) {
			return false;
		}
		if (device.isSPPConnType()) {//蓝牙
			return bluetoothSocket != null && bluetoothSocket.isConnected();
		}
		if (device.isApOrWifiConnType()){
			return wifiSocket != null && wifiSocket.isConnected();
		}
		if (device.isSerialConnType()){
			return serialThread.isOpen();
		}
		return false;
	}

	/**
	 * Is connected device boolean.
	 *
	 * @param device the device
	 * @return the boolean
	 */
	public Boolean isConnectedDevice(Device device) {
		if (!isConnected()) {
			return false;
		}
		return device.equals(this.device);
	}

	/**
	 * Is connecting device boolean.
	 *
	 * @param device the device
	 * @return the boolean
	 */
	public Boolean isConnectingDevice(Device device) {
		if (device==null){
			return false;
		}
		return isConning() && !isConnected() && device.equals(this.device);
	}

	/**
	 * Gets connected device.
	 *
	 * @return the connected device
	 */
	public Device getConnectedDevice() {
		if (!isConnected()) {
			return null;
		}
		return device;
	}

	public boolean isSupperDeviceByName(String deviceName) {
		if (TextUtils.isEmpty(deviceName)){
			return false;
		}
		return (deviceName.toLowerCase().contains("inksi"))||
				(deviceName.toLowerCase().contains("emeterai"))
				||(deviceName.toLowerCase().contains("crypto stamp"));
	}

	//*****************************蓝牙开关事件***************************
	@Override
	public void onOpeningBlueTooth() {
		notifyDeviceBlueToothOpening();
	}

	@Override
	public void onOpenedBlueTooth() {
		notifyDeviceBlueToothOpened();
	}

	@Override
	public void onClosingBlueTooth() {
		notifyDeviceBlueToothClosing();
	}

	@Override
	public void onClosedBlueTooth() {
		notifyDeviceBlueToothClosed();
	}

	//*****************************蓝牙扫描***************************
	@Override
	public void onStartDiscovering() {
		notifyStartDiscover();
	}

	@Override
	public void onStopDiscovering() {
		notifyStopDiscover();
	}

	@SuppressLint("MissingPermission")
	@Override
	public void onDiscovered(BluetoothDevice bluetoothDevice) {
		//这里可增加过滤，只往缓存设备里面装符合要求的设备，后续完善
		synchronized (SPP_MANAGER){
			String deviceName = bluetoothDevice.getName();
//			if (TextUtils.isEmpty(deviceName)){
//				return;
//			}
			if (TextUtils.isEmpty(deviceName)||bluetoothDevice.getType() == DEVICE_TYPE_LE){
				return;
			}
			if (isSupperDeviceByName(deviceName)) {
				Device device = Device.createSppDevice(bluetoothDevice,null,null,0,null);
				notifyDiscoveredDevice(device);
			}
		}
	}

	//*****************************蓝牙配对***************************
	@Override
	public void onBonding(BluetoothDevice bluetoothDevice) {
		if(this.device == null) return;
		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
			notifyDeviceBonding(this.device);
		}
	}

	@Override
	public void onBonded(BluetoothDevice bluetoothDevice) {
		if(this.device == null) return;
		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
			notifyDeviceBonded(this.device);
		}
		//监听到配对完成的事件后，进行判断是否为当前设备，如果为当前设备则接下来进行socket连接，这里暂时不进行a2dp连接，a2dp暂时不使用
		mainHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (bluetoothDevice.equals(device.bluetoothDevice)) {
					bluetoothSocketThread.connect(bluetoothDevice, tryTime);
				}
			}
		}, 500);
	}

	@Override
	public void onDisBond(BluetoothDevice bluetoothDevice) {
		if(this.device == null) return;
		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
			cancelConning();//恢复isStart的值
			isOpenHeartbeat = false;
			notifyDeviceDisBond(this.device);
		}
	}

	//*****************************a2dp***************************
	@Override
	public void onA2dpConnStart(BluetoothDevice bluetoothDevice) {
		if (this.device==null) return;
		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
//			notifySppA2dpConnStart(this.device);
		}
	}

	@Override
	public void onA2dpConnSucceed(BluetoothDevice bluetoothDevice) {
		if (this.device==null) return;
		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
//			notifySppA2dpConnSucceed(this.device);
		}
	}

	@Override
	public void onA2dpConnTimeout(BluetoothDevice bluetoothDevice) {
		if (this.device==null) return;
		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
//			notifySppA2dpConnTimeout(this.device);
		}
	}

	//*****************************socket连接***************************
	@Override
	public void onBluetoothSocketConnStart(BluetoothDevice bluetoothDevice) {
		if (this.device==null) return;
		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
			// 这里的连接事件提前到对设备开始配对
//			notifyDeviceConnectStart(this.device);
			RBQLog.i(TAG,"---开始连接蓝牙设备的Socket---");
		}
	}

	@Override
	public void onBluetoothSocketConnSucceed(BluetoothDevice bluetoothDevice, BluetoothSocket socket) {
		if (this.device==null) return;
		if (!bluetoothDevice.equals(this.device.bluetoothDevice)) {
			return;
		}
		RBQLog.i(TAG, "socket创建成功");
		this.bluetoothSocket = socket;
		try {
			this.inputStream = this.bluetoothSocket.getInputStream();
			this.outputStream = this.bluetoothSocket.getOutputStream();
			//启动数据读取线程
			readThread.start(inputStream);
			//启动数据写入线程
			writeThread.start(outputStream);
			//发送设备连接成功事件
			notifyDeviceConnectSucceed(this.device);
			//同步打印机数据
			dataSynchronize();

			if (isOpenHeartbeat){
				writeThread.startHeartbeat(heartbeatInterval);
			}else {
				RBQLog.i(">>>【心跳】✂️无需开启心跳");
			}

		} catch (IOException e) {

			e.printStackTrace();
			notifyDeviceConnectFail(this.device, "数据流获取失败");
		}
	}

	@Override
	public void onBluetoothSocketConnTimeout(BluetoothDevice bluetoothDevice) {
		//这里socket连接失败也恢复isStart的值
		cancelConning();
		isOpenHeartbeat = false;
		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
			notifyDeviceConnectFail(this.device,"连接超时");
		}
	}

	synchronized public void startMonitorHeartData(long interval) {
		RBQLog.i("启动心跳检测，当前计数清零");
		synchronized (this) {
			deviceHeartLooseTimes = 0;
			scheduleNextHeartCheck(interval);
		}
	}

	synchronized public void stopMonitorHeartData() {
		synchronized (this) {
			heartHandler.removeCallbacksAndMessages(null);
		}
	}

	synchronized private void clearMonitorHeartData() {
		synchronized (this) {
//			RBQLog.i("收到心跳包，清除丢包记录");
			deviceHeartLooseTimes = 0;
		}
	}

	private void scheduleNextHeartCheck(long delay) {
		heartHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				checkHeart();
			}
		}, delay);
	}

	private void checkHeart() {
		synchronized (this) {
			if (deviceHeartLooseTimes < MAX_LOSE_HEART_TIMES) {
				deviceHeartLooseTimes++;
//				RBQLog.i("+1之后的heartLooseTimes:" + heartLooseTimes);
				scheduleNextHeartCheck(3000);
			} else {
				RBQLog.i("当前计数大于" + MAX_LOSE_HEART_TIMES + "次，主动断开连接");
				disconnect();
			}
		}
	}

	public void dataSynchronize(){

		if (!isConnected()||isDataSending()||!isOpenDataSynchronize()||isDataSynchronize){
			return;
		}

		isDataSynchronize = true;
		
		// 初始化待同步的 opcode 集合（只添加所有机型都支持的必须指令）
		pendingSyncOpcodes.clear();
		pendingSyncOpcodes.add(Opcode.ReadSoftwareInfo);
		pendingSyncOpcodes.add(Opcode.readPrinterHeadParameters);
		pendingSyncOpcodes.add(Opcode.ReadCirculationAndRepeatTimes);
		pendingSyncOpcodes.add(Opcode.ReadBattery);
		// 以下为可选指令，部分机型可能不支持，不加入 pending 列表
		// ReadSilentState, ReadAutoPowerOffState, ReadContinuousPrint
		
		// 记录开始时间，延迟启动超时检测（等待最后一条指令发送后再开始检测）
		lastSyncActivityTime = System.currentTimeMillis();
		scheduleSyncTimeoutCheck();

		notifyDataSynchronizeStart(this.device);

		//读取软件信息
		readSoftwareInfo(500,SPP_TAG_NORMAL_SYNCING_COMMAND);
		//读取打印头信息
		readPrinterHeadParameters(600,SPP_TAG_NORMAL_SYNCING_COMMAND);
		//读取循环次数和重复次数
		readCirculationAndRepeatTime(700,SPP_TAG_NORMAL_SYNCING_COMMAND);
		//读取电池电量
		readBattery(800,SPP_TAG_NORMAL_SYNCING_COMMAND);

		readSilentState(900,SPP_TAG_NORMAL_SYNCING_COMMAND);

		readAutoPowerOffState(1000,SPP_TAG_NORMAL_SYNCING_COMMAND);

		readContinuousPrintState(1100,SPP_TAG_NORMAL_SYNCING_COMMAND);

	}
	
	/**
	 * 延迟启动同步超时检测
	 * 最后一条指令延时1100ms发送，延迟2秒后开始检测，确保所有指令都已发出
	 */
	private void scheduleSyncTimeoutCheck() {
		syncTimeoutHandler.removeCallbacks(syncTimeoutCheckRunnable);
		syncTimeoutHandler.removeCallbacks(startSyncTimeoutCheckRunnable);
		syncTimeoutHandler.postDelayed(startSyncTimeoutCheckRunnable, SYNC_START_CHECK_DELAY);
	}
	
	/**
	 * 启动超时检测的 Runnable
	 */
	private final Runnable startSyncTimeoutCheckRunnable = new Runnable() {
		@Override
		public void run() {
			if (isDataSynchronize && !pendingSyncOpcodes.isEmpty()) {
				startSyncTimeoutCheck();
			}
		}
	};
	
	/**
	 * 启动同步超时检测
	 */
	private void startSyncTimeoutCheck() {
		syncTimeoutHandler.removeCallbacks(syncTimeoutCheckRunnable);
		syncTimeoutHandler.postDelayed(syncTimeoutCheckRunnable, SYNC_IDLE_TIMEOUT);
	}
	
	/**
	 * 停止同步超时检测
	 */
	private void stopSyncTimeoutCheck() {
		syncTimeoutHandler.removeCallbacks(startSyncTimeoutCheckRunnable);
		syncTimeoutHandler.removeCallbacks(syncTimeoutCheckRunnable);
		syncTimeoutHandler.removeCallbacks(syncCompleteRunnable);
	}
	
	/**
	 * 同步超时检测 Runnable
	 */
	private final Runnable syncTimeoutCheckRunnable = new Runnable() {
		@Override
		public void run() {
			if (!isDataSynchronize || pendingSyncOpcodes.isEmpty()) {
				return;
			}
			
			long idleTime = System.currentTimeMillis() - lastSyncActivityTime;
			if (idleTime >= SYNC_IDLE_TIMEOUT) {
				// 超时了，强制完成同步（忽略未响应的指令）
				int pendingCount = pendingSyncOpcodes.size();
				RBQLog.i(TAG, "数据同步超时，未响应的指令数: " + pendingCount);
				stopSyncTimeoutCheck();
				isDataSynchronize = false;
				pendingSyncOpcodes.clear();
				notifyDataSynchronizeTimeout(device, pendingCount);
			} else {
				// 还没超时，继续检查
				long remainingTime = SYNC_IDLE_TIMEOUT - idleTime;
				syncTimeoutHandler.postDelayed(this, remainingTime);
			}
		}
	};
	
	/**
	 * 处理同步响应，从待同步集合中移除对应的 opcode
	 * @param opcode 收到响应的 opcode
	 */
	private void onSyncResponseReceived(int opcode) {
		if (!isDataSynchronize) {
			return;
		}
		// 更新最后活动时间
		lastSyncActivityTime = System.currentTimeMillis();
		pendingSyncOpcodes.remove(opcode);
		if (pendingSyncOpcodes.isEmpty()) {
			stopSyncTimeoutCheck();
			// 延迟通知完成，等待可选指令响应
			syncTimeoutHandler.postDelayed(syncCompleteRunnable, SYNC_COMPLETE_DELAY);
		}
	}
	
	/**
	 * 同步完成延迟通知 Runnable
	 */
	private final Runnable syncCompleteRunnable = new Runnable() {
		@Override
		public void run() {
			if (!isDataSynchronize) {
				return;
			}
			isDataSynchronize = false;
			RBQLog.i(TAG, "数据同步完成");
			notifyDataSynchronizeComplete(device);
		}
	};
	
	/**
	 * 清理同步状态（断连时调用）
	 */
	private void clearDataSynchronizeState() {
		stopSyncTimeoutCheck();
		boolean wasSynchronizing = isDataSynchronize;
		isDataSynchronize = false;
		pendingSyncOpcodes.clear();
		// 如果之前正在同步，通知监听者同步被中断
		if (wasSynchronizing) {
            RBQLog.i(TAG, "数据同步被中断");
			notifyDataSynchronizeInterrupted(device);
		}
	}

	@Override
	public void onWifiSocketConnectStart() {
		notifyDeviceConnectStart(this.device);
	}

	@Override
	public void onWifiSocketConnect(Socket socket) {

		RBQLog.i(TAG, "socket创建成功");

		this.wifiSocket = socket;

		try {

			this.inputStream = this.wifiSocket.getInputStream();
			this.outputStream = this.wifiSocket.getOutputStream();

			//启动数据读取线程
			readThread.start(inputStream);
			//启动数据写入线程
			writeThread.start(outputStream);
			//发送设备连接成功事件
			notifyDeviceConnectSucceed(this.device);

			//同步打印机数据
			dataSynchronize();

			if (isOpenHeartbeat){
				writeThread.startHeartbeat(heartbeatInterval);
			}else {
				RBQLog.i(">>>【心跳】✂️无需开启心跳");
			}

			startMonitorHeartData(delayCheckHeartTime);

		} catch (IOException e) {
			e.printStackTrace();
			cancelConning();
			isOpenHeartbeat = false;
			notifyDeviceConnectFail(this.device, "wifi连接异常");
		}

	}

	@Override
	public void onWifiConnectFailed(String msg) {
		cancelConning();
		isOpenHeartbeat = false;
		notifyDeviceConnectFail(this.device,msg);
	}

	@Override
	public void onReadData(byte[] data) {

		synchronized (SPP_MANAGER) {

			if (device.isApOrWifiConnType()){
				clearMonitorHeartData();
			}
//			RBQLog.i("onReadData收到数据->16进制格式:"+ Arrays.bytesToHexString1(data,",")+"; 字符串格式:" + (new String(data, StandardCharsets.UTF_8).trim()));
			if(device!=null&&device.isApOrWifiConnType()){//wifi
				deviceHeartLooseTimes = 0;
			}
			try {
				//在ota或者发送图片数据过程中屏蔽json数据接收
				if (!hasPacketStartSending()) {
					// 使用 JsonStreamAssembler 处理 JSON 数据
					jsonStreamAssembler.feed(data);
					return;
				}

				dispatchDataEven(data);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void dispatchJsonEven(String json) {
		RBQLog.i("dispatchJsonEven:" + json);
		try {

			notifyReadJson(this.device,json);

			JSONObject jsonObject = new JSONObject(json);
			int code = jsonObject.getInt("code");
//			RBQLog.i("code:" + code);
			if (code == 0) {
				int cmd = jsonObject.getInt("cmd");
//				RBQLog.i("cmd:" + cmd);
				if (cmd == Opcode.readPrinterHeadParameters) {

					RBQLog.i("同步打印机参数接受到数据");

					String msg = jsonObject.getString("msg");
					String[] parameters = msg.split(",");

					int headValue = Integer.parseInt(parameters[0]);
					int l_pix = Integer.parseInt(parameters[1]);
					int p_pix = Integer.parseInt(parameters[2]);
					int distance = Integer.parseInt(parameters[3]);

					device.printer_head = headValue;
					device.l_pix = l_pix;
					device.p_pix = p_pix;
					device.distance = distance;

					notifyReadPrinterHeadParameter(device, headValue, l_pix, p_pix, distance);
					onSyncResponseReceived(Opcode.readPrinterHeadParameters);

				}else if (cmd == Opcode.ReadCirculationAndRepeatTimes) {

					String msg = jsonObject.getString("msg");
					String[] parameters = msg.split(",");
					int circulation_time = Integer.parseInt(parameters[0]);
					int repeat_time = Integer.parseInt(parameters[1]);

					device.circulation = circulation_time;
					device.repeat_time = repeat_time;

					notifyReadCirculationAndRepeatTime(device, circulation_time, repeat_time);
					onSyncResponseReceived(Opcode.ReadCirculationAndRepeatTimes);

				}else if (cmd == Opcode.ReadPrintDirection) {

					String msg = jsonObject.getString("msg");
					String[] parameters = msg.split(",");
					int horizontalDirection = Integer.parseInt(parameters[0]);
					int verticalDirection = Integer.parseInt(parameters[1]);
					RBQLog.i(" 设置页面 同步得打印方向 horizontalDirection:" + horizontalDirection+"; verticalDirection:"+verticalDirection);

					//这里是从打印机获得的方向，因此也要同步给oldDirection
//					device.oldDirection = horizontalDirection;
					int oldHorizontalDirection = device.horizontalDirection;
					int oldVerticalDirection = device.verticalDirection;

					device.horizontalDirection = horizontalDirection;
					device.verticalDirection = verticalDirection;

					notifyReadDirection(device,oldHorizontalDirection,horizontalDirection,oldVerticalDirection,verticalDirection);

				}else if (cmd == Opcode.ReadSoftwareInfo) {

					RBQLog.i("读取到打印机信息");

					String id = jsonObject.getString("id");
					String name = jsonObject.getString("name");
					String mcu_version = jsonObject.getString("mcu_ver");
					String mcu_date = jsonObject.getString("date");

					RBQLog.i("获取到版本号:" + mcu_version);

					device.printer_head_id = id;
					device.mcuName = name;
					device.mcuVersion = mcu_version;
					device.mcu_date = mcu_date;

					notifyReadSoftwareInfo(device, id, name, mcu_version, mcu_date);
					onSyncResponseReceived(Opcode.ReadSoftwareInfo);

				}else if (cmd == Opcode.ReadCartridgeId) {
					String id = jsonObject.getString("id");
					RBQLog.i("读取到打印头ID:"+id);

					notifyReadCartridgeId(device,id);

				}else if (cmd == Opcode.ReadPrintTemperature) {

					int temperature1 = jsonObject.getInt("temp_set");
					int temperature2 = jsonObject.getInt("temp_get");
					int temperature = temperature1 > 0 ? temperature1 : temperature2;

					RBQLog.i("读取到打印头温度为:" + temperature1+","+temperature2);
					device.temperature = temperature;
					notifyTemperature(device, temperature);

				}else if (cmd == Opcode.ReadBattery) {

					int bat = jsonObject.getInt("bat");
					RBQLog.i("读取到电量值为:" + bat);
					device.batteryLevel = bat;
					notifyReadBattery(device, bat);
					onSyncResponseReceived(Opcode.ReadBattery);
				}else if (cmd == 4130) {
					//每5秒发一次低点提醒
					RBQLog.i("每5秒发一次低电提醒:");
//					Toast.makeText(RBQAppManager.share().getApplicationContext(),"请充电",Toast.LENGTH_LONG).show();
//					notifyReadBattery(device, bat);
				}else if(cmd == 4098){
					//4098开始清洗
					RBQLog.i("打印头清洗开始");
				}else if(cmd == 4099){
					//4099结束清洗
					RBQLog.i("打印头清洗结束");
				}else if (cmd==Opcode.printStart) {
					String msg = jsonObject.getString("msg");
					String[] parameters = msg.split(",");
					int beginIndex = Integer.parseInt(parameters[0]);
					int endIndex = Integer.parseInt(parameters[1]);
					int currentIndex = Integer.parseInt(parameters[2]);
					RBQLog.i("开始打印 beginIndex:"+beginIndex+"; endIndex:"+endIndex+"; currentIndex:"+currentIndex);
					notifyPrintStart(beginIndex,endIndex,currentIndex);
				}else if(cmd==Opcode.printCompleted){
					String msg = jsonObject.getString("msg");
					RBQLog.i("打印完成:"+msg);
					String[] parameters = msg.split(",");
					int beginIndex = Integer.parseInt(parameters[0]);
					int endIndex = Integer.parseInt(parameters[1]);
					int currentIndex = Integer.parseInt(parameters[2]);
					String cartridgeId = "";
					if (parameters.length>3){
						cartridgeId = parameters[3];
					}
					RBQLog.i("结束打印 beginIndex:"+beginIndex+"; endIndex:"+endIndex+"; currentIndex:"+currentIndex);
					notifyPrintComplete(beginIndex,endIndex,currentIndex, cartridgeId);
				}else if (cmd == 514 && device!=null && device.isSerialConnType()){

					int pack = jsonObject.getInt("pack");
					//已连接
                    serialConnHandler.removeCallbacks(serialConnectTimeRunnable);
                    serialConnHandler.removeCallbacksAndMessages(null);

                    if (pack == 1){
						//清除连接超时倒计时
                        //如果连接的是串口，连接成功状态app发过去的，如果收到回复，则认为连接成功，否则主动关闭串口认为连接失败
						notifyDeviceConnectSucceed(this.device);
						//同步打印机数据
						dataSynchronize();
						if (isOpenHeartbeat){
							writeThread.startHeartbeat(heartbeatInterval);
						}else {
							RBQLog.i(">>>【心跳】✂️无需开启心跳");
						}
					}else { // 0 则表示已断开
                        //连接已断开
						disconnectSerial();
						notifyDeviceDisconnect(device);
					}

				} else if (cmd == Opcode.ReadSilentState) {
					boolean silentState = "1".equals(jsonObject.getString("msg"));//勿扰模式状态
					RBQLog.i("读取到静音状态:" + silentState+","+silentState);
					device.silentState = silentState;
					notifyReadSilentState(device,silentState);
					onSyncResponseReceived(Opcode.ReadSilentState);
				} else if (cmd == Opcode.ReadAutoPowerOffState) {
					boolean autoPowerOffState = "1".equals(jsonObject.getString("msg"));
					RBQLog.i("读取到自动关机状态:" + autoPowerOffState+","+autoPowerOffState);
					device.autoPowerOffState = autoPowerOffState;
					notifyReadAutoPowerOffState(device,autoPowerOffState);
					onSyncResponseReceived(Opcode.ReadAutoPowerOffState);
				} else if (cmd == Opcode.WritePrintStartCommand){
					RBQLog.i("读取到 打印指令 cmd:"+cmd+"; json:"+json);
					notifyReadPrintStartCommand();
				} else if (cmd == Opcode.WriteContinuousPrint) {
					RBQLog.i("写入成功 连续打印指令 cmd:"+cmd+"; json:"+json);
				} else if (cmd == Opcode.ReadContinuousPrint) {
					RBQLog.i("读取成功 连续打印指令 cmd:"+cmd+"; json:"+json);
					boolean continuousPrintState = "1".equals(jsonObject.getString("msg"));
					RBQLog.i("读取到连续打印状态:" + continuousPrintState+","+continuousPrintState);
					device.continuousPrintState = continuousPrintState;
					notifyReadContinuousPrintState(device,continuousPrintState);
					onSyncResponseReceived(Opcode.ReadContinuousPrint);
				} else {
					notifyReadGeneralCommand(cmd,json);
				}
			} else {
				String error = Errors.Instance().getDescribeByCode(code);
				RBQLog.i("打印机错误码error:" + error);
				notifyError(device, error);
			}

		} catch (Exception e) {

			e.printStackTrace();
			RBQLog.i("解析打印机信息数据异常" + json);
		}

	}

	private void dispatchDataEven(byte[] data) {

		if (multiRowDataPacket.isStartSendingData()) {
			dispatchMultiRowReadData(data);
		} else if (cmykMultiRowDataPacket.isStartSendingData()) {
			dispatchCMYKMultiRowReadData(data);
		} else if (otaPacket.isStartSendingData()) {
			dispatchOtaReadData(data);
		} else if (logoPacket.isStartSendingData()) {
			dispatchLogoReadData(data);
		}
	}

	private final Runnable retryLogoDataRunnable = new Runnable() {
		@Override
		public void run() {
			localSetWidthSendLogoPacket();
		}
	};

	private void dispatchLogoReadData(byte[] data) {

		//请求数据
		if (logoPacket.isRequest(data)) {//c
			waitResponseHandler.removeCallbacks(retryLogoDataRunnable);
			waitResponseHandler.removeCallbacksAndMessages(null);
//			RBQLog.i("收到logo数据请求");
			//还有下一帧数据，继续发送下一帧数据
			if (this.logoPacket.hasNextPacket()) {

				this.sendNextLogoPacket();
			}

		} else if (this.logoPacket.isNAK(data)) {

			this.sendNAKLogoPacket();

		} else if (this.logoPacket.isEOT(data)) {

			RBQLog.i("logo数据传输完毕");
			cancelAllPacketStart();
			isDataSending = false;

			this.logoPacket.currentTime = System.currentTimeMillis();
			float size = (float) this.logoPacket.dataLength / 1000.0f;

			notifyDataProgressFinish(size,this.logoPacket.startTime, this.logoPacket.currentTime);
		}
	}

	private final Runnable retryOtaRunnable = new Runnable() {
		@Override
		public void run() {
			localSetWidthSendOtaPacket();
		}
	};

	private void dispatchOtaReadData(byte[] data) {

		//请求数据
		if (this.otaPacket.isRequest(data)) {//c
			waitResponseHandler.removeCallbacks(retryOtaRunnable);
			waitResponseHandler.removeCallbacksAndMessages(null);
//			RBQLog.i("请求ota数据 >>> 第" + send_packet_Index + "次请求");
			send_packet_Index++;
			//            RBQLog.i("ota");
			//还有下一帧数据，继续发送下一帧数据
			if (this.otaPacket.hasNextPacket()) {
				this.sendNextOtaPacket();
			}
		} else if (this.otaPacket.isNAK(data)) {
			this.sendNAKOtaPacket();
		} else if (this.otaPacket.isEOT(data)) {

			RBQLog.i("ota数据传输完毕");
			cancelAllPacketStart();
			isDataSending = false;

			this.otaPacket.currentTime = System.currentTimeMillis();
			float size = (float) this.otaPacket.dataLength / 1000.0f;
			notifyDataProgressFinish(size, this.otaPacket.startTime, this.otaPacket.currentTime);
		}
	}

	private final Runnable retryMultiRowDataRunnable = new Runnable() {
		@Override
		public void run() {
			localSetWithSendMultiRowDataPacket();
		}
	};

	private void dispatchMultiRowReadData(byte[] data) {
		//请求数据
		if (this.multiRowDataPacket.isRequest(data)) {//c
			waitResponseHandler.removeCallbacks(retryMultiRowDataRunnable);
			waitResponseHandler.removeCallbacksAndMessages(null);
//			RBQLog.i("请求图片数据 >>> 第" + send_packet_Index + "次请求");
			send_packet_Index++;
			//还有下一帧数据，继续发送下一帧数据
			if (this.multiRowDataPacket.hasNextPacketWithCurrentRow()) {
//				RBQLog.i("继续发送下一包数据 >>> ");
				this.sendNextMultiRowImagePacket();
			}
		} else if (this.multiRowDataPacket.isNAK(data)) {
//			RBQLog.i("重传当前数据包请求命令" + new String(data));
			this.sendNakMultiRowImagePacket();
		} else if (this.multiRowDataPacket.isEOT(data)) {
			send_packet_Index = 0;
			//在测试过程中发现，在高性能手机中，发现在每行数据一样多的情况下极少数的概率只打最后一行，
			// 但是在每行数据不一样多且数据量比较大，比如在当前的选区打印模式下，会经常性的碰到只能打印出来最后一行的情况，
			// 并且经过数据分析，app生成的图片、生成的数据、读取出来的数据都没有任何问题
			//最终，在打印下一行的时候延时500ms没有任何问题
			// (目前估计可能是打印机固件在多行打印时，每行数据量不同时，可能存在某种缺陷;或者硬件性能引起处理跟不上)
//			mainHandler.postDelayed(new Runnable() {
//				@Override
//				public void run() {
//
			delayCheckNextRowData();
//				}
//			}, 500);
		}
	}

	/**
	 * Delay check next row data.
	 */
	protected void delayCheckNextRowData() {

		if (this.multiRowDataPacket.hasNextRow()) {

			mainHandler.postDelayed(() -> {

				//判断如果当前组中没有下一包数据，则判断是否有下一组数据
				multiRowDataPacket.cursorMoveToNext();
				byte currentRow = (byte)(multiRowDataPacket.getCurrentRow() & 0xFF);
				RBQLog.i("发送第:"+currentRow+"行数据 ");
				int arrIndexDataSize = multiRowDataPacket.currentRowDataLength;//4byte
				byte dataSize0 = (byte)(arrIndexDataSize & 0xFF);
				byte dataSize1 = (byte)((arrIndexDataSize >> 8) & 0xFF);
				byte dataSize2 = (byte)((arrIndexDataSize >> 16) & 0xFF);
				byte dataSize3 = (byte)((arrIndexDataSize >> 24) & 0xFF);
				byte compress = (byte)(multiRowDataPacket.compress & 0xFF);
				byte[] transmitParams = {currentRow,dataSize0,dataSize1,dataSize2,dataSize3,compress};
				innerSendCommand(Opcode.TransmitPictureData,transmitParams);
			},300);

		}else {

			mainHandler.postDelayed(() -> {
				//从0索引开始打
				int index = multiRowDataPacket.totalRowCount;
				byte[] printPictureParams = {0, (byte) index};
				innerSendCommand(Opcode.PrintPicture,printPictureParams);//延时100ms发送打印指令
                RBQLog.i("----单色图片数据传输完毕---并发送了图片打印指令，打印从0索引开始到"+index+"索引的图片");
				multiRowDataPacket.currentTime = System.currentTimeMillis();
				float size = (float) multiRowDataPacket.totalDataLen /1000.0f;
				notifyDataProgressFinish(size, multiRowDataPacket.startTime, multiRowDataPacket.currentTime);
				cancelAllPacketStart();

				isDataSending = false;

			},100);
		}
	}

    private void dispatchCMYKMultiRowReadData(byte[] data) {
		// 请求数据
		if (this.cmykMultiRowDataPacket.isRequest(data)) {
			waitResponseHandler.removeCallbacks(retryCMYKMultiRowDataRunnable);
			waitResponseHandler.removeCallbacksAndMessages(null);
			send_packet_Index++;
			// 当前通道还有下一包数据，继续发送
			if (this.cmykMultiRowDataPacket.hasNextPacketInCurrentChannel()) {
				this.sendNextCMYKMultiRowImagePacket();
			}
		} else if (this.cmykMultiRowDataPacket.isNAK(data)) {
			// 重传当前包
			this.sendNakCMYKMultiRowImagePacket();
		} else if (this.cmykMultiRowDataPacket.isEOT(data)) {
			// 当前通道数据传输完毕
			send_packet_Index = 0;
			// 检查是否有下一个通道或下一行
			delayCMYKCheckNextChannelOrRowData();
		}
	}

	@Override
	public void onReadError() {//读或者写错误，表示socket连接断开，这里使用读来判断，因为读是不停的进行的，写不一定一直进行

		RBQLog.i(TAG, "onSocketDisconnect");

		cancelConning();
		stopMonitorHeartData();
		clearDataSynchronizeState();

		isOpenHeartbeat = false;
		readThread.cancel();
		writeThread.stopHeartbeat();
		writeThread.cancel();
		bluetoothSocketThread.cancel();
		tcpClientThread.cancel();
		pairThread.cancel();
		serialThread.cancel();

		if (this.wifiSocket != null) {
			try {
				this.wifiSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.wifiSocket = null;
		}
		if (this.bluetoothSocket != null) {
			try {
				this.bluetoothSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.bluetoothSocket = null;
		}
		if (this.inputStream != null) {
			try {
				this.inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.inputStream = null;
		}
		if (this.outputStream != null) {
			try {
				this.outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.outputStream = null;
		}
		//连接断开
		notifyDeviceDisconnect(this.device);
	}

	/********************************************************************************
	 * Command API
	 *******************************************************************************/

	/**
	 * 读取打印头参数
	 */
	public void readPrinterHeadParameters(){
		sendCommand(Opcode.readPrinterHeadParameters, null);
	}

	/**
	 * 读取打印头参数
	 * @param delayTime 指令延时发送时间  单位  毫秒
	 */
	public void readPrinterHeadParameters(int delayTime){
		sendCommand(Opcode.readPrinterHeadParameters, null,delayTime);
	}

	public void readPrinterHeadParameters(int delayTime,Object tag){
		sendCommand(Opcode.readPrinterHeadParameters, null,delayTime,tag);
	}

	/**
	 * 设置打印头参数
	 * @param printer_head 设置打印头通道
	 * @param pix  分辨率
	 */
	public void writePrinterHeadParameters(int printer_head, int pix){

		int l_pix_0 = pix & 0xFF;
		int l_pix_1 = (pix >> 8) & 0xFF;
		int p_pix_0 = pix & 0xFF;
		int p_pix_1 = (pix >> 8) & 0xFF;

		//打印机分行行数
		byte delayDistance0 = 0x00;
		byte delayDistance1 = 0x00;
		byte delayDistance2 = 0x00;
		byte delayDistance3 = 0x00;

		byte[] configurationParams = new byte[]{(byte) printer_head, (byte) p_pix_0, (byte) p_pix_1, (byte) l_pix_0, (byte) l_pix_1, delayDistance0, delayDistance1, delayDistance2, delayDistance3, 0, 0};
		sendCommand(Opcode.WritePrinterHeadParameters, configurationParams);
	}

	/**
	 * 设置打印头参数
	 * @param printer_head 设置打印头通道
	 * @param pix  分辨率
	 * @param delayTime  指令延时发送时间  单位  毫秒
	 */
	public void writePrinterHeadParameters(int printer_head, int pix,int delayTime){

		int l_pix_0 = pix & 0xFF;
		int l_pix_1 = (pix >> 8) & 0xFF;
		int p_pix_0 = pix & 0xFF;
		int p_pix_1 = (pix >> 8) & 0xFF;

		//打印机分行行数
		byte delayDistance0 = 0x00;
		byte delayDistance1 = 0x00;
		byte delayDistance2 = 0x00;
		byte delayDistance3 = 0x00;

		byte[] configurationParams = new byte[]{(byte) printer_head, (byte) p_pix_0, (byte) p_pix_1, (byte) l_pix_0, (byte) l_pix_1, delayDistance0, delayDistance1, delayDistance2, delayDistance3, 0, 0};
		sendCommand(Opcode.WritePrinterHeadParameters, configurationParams,delayTime);
	}

	/**
	 * 设置打印头参数
	 * @param printer_head 设置打印头通道
	 * @param l_pix  水平方向分辨率
	 * @param p_pix  竖向分辨率
	 * @param distance 延时打印距离
	 */
	public void writePrinterHeadParameters(int printer_head, int l_pix, int p_pix, int distance){

		int l_pix_0 = l_pix & 0xFF;
		int l_pix_1 = (l_pix >> 8) & 0xFF;
		int p_pix_0 = p_pix & 0xFF;
		int p_pix_1 = (p_pix >> 8) & 0xFF;

		//打印机分行行数
		byte delayDistance0 = (byte) (distance & 0xFF);
		byte delayDistance1 = (byte) ((distance >> 8) & 0xFF);
		byte delayDistance2 = (byte) ((distance >> 16) & 0xFF);
		byte delayDistance3 = (byte) ((distance >> 24) & 0xFF);

		byte[] configurationParams = new byte[]{(byte) printer_head, (byte) p_pix_0, (byte) p_pix_1, (byte) l_pix_0, (byte) l_pix_1, delayDistance0, delayDistance1, delayDistance2, delayDistance3, 0, 0};
		sendCommand(Opcode.WritePrinterHeadParameters, configurationParams);
	}

	/**
	 * 设置打印头参数
	 * @param printer_head 设置打印头通道
	 * @param l_pix  水平方向分辨率
	 * @param p_pix  竖向分辨率
	 * @param distance 延时打印距离
	 * @param delayTime  指令延时发送时间  单位  毫秒
	 */
	public void writePrinterHeadParameters(int printer_head, int l_pix, int p_pix, int distance,int delayTime){

		int l_pix_0 = l_pix & 0xFF;
		int l_pix_1 = (l_pix >> 8) & 0xFF;
		int p_pix_0 = p_pix & 0xFF;
		int p_pix_1 = (p_pix >> 8) & 0xFF;

		//打印机分行行数
		byte delayDistance0 = (byte) (distance & 0xFF);
		byte delayDistance1 = (byte) ((distance >> 8) & 0xFF);
		byte delayDistance2 = (byte) ((distance >> 16) & 0xFF);
		byte delayDistance3 = (byte) ((distance >> 24) & 0xFF);

		byte[] configurationParams = new byte[]{(byte) printer_head, (byte) p_pix_0, (byte) p_pix_1, (byte) l_pix_0, (byte) l_pix_1, delayDistance0, delayDistance1, delayDistance2, delayDistance3, 0, 0};
		sendCommand(Opcode.WritePrinterHeadParameters, configurationParams,delayTime);
	}

	/**
	 * 读取循环次数和重复次数
	 */
	public void readCirculationAndRepeatTime(){
		sendCommand(Opcode.ReadCirculationAndRepeatTimes, null);
	}

	/**
	 * 读取循环次数和重复次数
	 * @param delayTime  指令延时发送时间  单位  毫秒
	 */
	public void readCirculationAndRepeatTime(int delayTime){
		sendCommand(Opcode.ReadCirculationAndRepeatTimes, null,delayTime);
	}

	public void readCirculationAndRepeatTime(int delayTime,Object tag){
		sendCommand(Opcode.ReadCirculationAndRepeatTimes, null,delayTime,tag);
	}

	/**
	 * 设置循环次数和重复次数
	 *
	 * @param circulation_time 打印循环次数
	 * @param repeat_time  打印重复次数
	 */
	public void writeCirculationAndRepeatTime(int circulation_time, int repeat_time){
		byte circulation0 = (byte) (circulation_time & 0xFF);
		byte circulation1 = (byte) ((circulation_time >> 8) & 0xFF);
		byte repeat_time0 = (byte) (repeat_time & 0xFF);
		byte repeat_time1 = (byte) ((repeat_time >> 8) & 0xFF);
		byte[] circulation_repeat_timeParams = new byte[]{circulation0, circulation1, repeat_time0, repeat_time1};
		sendCommand(Opcode.WriteCirculationAndRepeatTimes, circulation_repeat_timeParams);
	}

	/**
	 *设置打印机打印循环次数和重复打印次数
	 * @param circulation_time  打印循环次数
	 * @param repeat_time  打印重复次数
	 * @param delayTime 指令延时发送时间  单位  毫秒
	 */
	public void writeCirculationAndRepeatTime(int circulation_time, int repeat_time,int delayTime){
		byte circulation0 = (byte) (circulation_time & 0xFF);
		byte circulation1 = (byte) ((circulation_time >> 8) & 0xFF);
		byte repeat_time0 = (byte) (repeat_time & 0xFF);
		byte repeat_time1 = (byte) ((repeat_time >> 8) & 0xFF);
		byte[] circulation_repeat_timeParams = new byte[]{circulation0, circulation1, repeat_time0, repeat_time1};
		sendCommand(Opcode.WriteCirculationAndRepeatTimes, circulation_repeat_timeParams,delayTime);
	}

	/**
	 * 读取打印方向
	 */
	public void readPrintDirection(){
		sendCommand(Opcode.ReadPrintDirection, null);
	}

	/**
	 * 读取打印方向
	 * @param delayTime  指令延时发送时间  单位  毫秒
	 */
	public void readPrintDirection(int delayTime){
		sendCommand(Opcode.ReadPrintDirection, null,delayTime);
	}

	/**
	 *
	 * @param horizontalDirection 水平防方向 1从左往右 0 从右向左
	 * @param verticalDirection   垂直方向  1 从上往下  0 从下往上
	 */
	public void writePrintDirection(int horizontalDirection, int verticalDirection){
		if (horizontalDirection>1||horizontalDirection<0||verticalDirection>1||verticalDirection<0){
			return;
		}
		byte[] directionParams = new byte[]{(byte) horizontalDirection, (byte) verticalDirection};//打印方向和打印头方向
		sendCommand(Opcode.WritePrintDirection, directionParams);
	}

	/**
	 * 设置打印方向
	 * @param horizontalDirection 水平方向 1从左往右 0 从右向左
	 * @param verticalDirection 垂直方向 1 从上往下  0 从下往上
	 * @param delayTime  指令延时时间 单位 毫秒
	 */
	public void writePrintDirection(int horizontalDirection, int verticalDirection,int delayTime){
		if (horizontalDirection>1||horizontalDirection<0||verticalDirection>1||verticalDirection<0){
			return;
		}
		byte[] directionParams = new byte[]{(byte) horizontalDirection, (byte) verticalDirection};//打印方向和打印头方向
		sendCommand(Opcode.WritePrintDirection, directionParams,delayTime);
	}

	/**
	 * 读取软件信息
	 */
	public void readSoftwareInfo(){
		sendCommand(Opcode.ReadSoftwareInfo, null);
	}

	/**
	 * 读取软件信息
	 * @param delayTime 指令延时发送时间 单位毫秒
	 */
	public void readSoftwareInfo(int delayTime){
		sendCommand(Opcode.ReadSoftwareInfo, null,delayTime);
	}

	public void readSoftwareInfo(int delayTime,Object tag){
		sendCommand(Opcode.ReadSoftwareInfo, null,delayTime,tag);
	}

	/**
	 * 读取电量
	 */
	public void readBattery(){
		sendCommand(Opcode.ReadBattery, null);
	}

	/**
	 * 读取电量
	 * @param delayTime 指令延时发送时间 单位毫秒
	 */
	public void readBattery(int delayTime){
		sendCommand(Opcode.ReadBattery, null,delayTime);
	}

	public void readBattery(int delayTime,Object tag){
		sendCommand(Opcode.ReadBattery, null,delayTime,tag);
	}

	public void readSilentState(){
		sendCommand(Opcode.ReadSilentState, null);
	}

	public void readSilentState(int delayTime){
		sendCommand(Opcode.ReadSilentState, null,delayTime);
	}

	public void readSilentState(int delayTime,Object tag){
		sendCommand(Opcode.ReadSilentState, null,delayTime,tag);
	}

	public void writeSilentState(boolean state) {
		byte value = (byte) (state ? 1 : 0);
		byte[] silentStateParams = new byte[]{value};
		sendCommand(Opcode.WriteSilentState, silentStateParams);
	}

	public void writeSilentState(boolean state,int delayTime){
		byte value = (byte) (state ? 1 : 0);
		byte[] silentStateParams = new byte[]{value};
		sendCommand(Opcode.WriteSilentState, silentStateParams,delayTime);
	}

	public void writeSilentState(boolean state,int delayTime,Object tag) {
		byte value = (byte) (state ? 1 : 0);
		byte[] silentStateParams = new byte[]{value};
		sendCommand(Opcode.WriteSilentState, silentStateParams,delayTime,tag);
	}

	public void readAutoPowerOffState(){
		sendCommand(Opcode.ReadAutoPowerOffState, null);
	}

	public void readAutoPowerOffState(int delayTime){
		sendCommand(Opcode.ReadAutoPowerOffState, null,delayTime);
	}

	public void readAutoPowerOffState(int delayTime,Object tag){
		sendCommand(Opcode.ReadAutoPowerOffState, null,delayTime,tag);
	}

	public void writeAutoPowerOffState(boolean state) {
		byte value = (byte) (state ? 1 : 0);
		byte[] params = new byte[]{value};
		sendCommand(WriteAutoPowerOffState,params);
	}

	public void writeAutoPowerOffState(boolean state,int delayTime) {
		byte value = (byte) (state ? 1 : 0);
		byte[] params = new byte[]{value};
		sendCommand(WriteAutoPowerOffState,params,delayTime);
	}

	public void writeAutoPowerOffState(boolean state,int delayTime,Object tag) {
		byte value = (byte) (state ? 1 : 0);
		byte[] params = new byte[]{value};
		sendCommand(WriteAutoPowerOffState,params,delayTime,tag);
	}

	public void writeStartPrintCommand(){
		sendCommand(Opcode.WritePrintStartCommand, null);
	}

	public void writeStartPrintCommand(int delayTime){
		sendCommand(Opcode.WritePrintStartCommand, null,delayTime);
	}

	public void writeStartPrintCommand(int delayTime,Object tag){
		sendCommand(Opcode.WritePrintStartCommand, null,delayTime,tag);
	}

	public void readContinuousPrintState(){
		sendCommand(Opcode.ReadContinuousPrint, null);
	}

	public void readContinuousPrintState(int delayTime){
		sendCommand(Opcode.ReadContinuousPrint, null,delayTime);
	}

	public void readContinuousPrintState(int delayTime,Object tag){
		sendCommand(Opcode.ReadContinuousPrint, null,delayTime,tag);
	}

	public void  writeContinuousPrintCommand(Boolean state){
		byte value = (byte) (state ? 1 : 0);
		byte[] params = new byte[]{value};
		sendCommand(Opcode.WriteContinuousPrint, params);
	}

	public void  writeContinuousPrintCommand(Boolean state,int delayTime){
		byte value = (byte) (state ? 1 : 0);
		byte[] params = new byte[]{value};
		sendCommand(Opcode.WriteContinuousPrint, params,delayTime);
	}

	public void writeContinuousPrintCommand(Boolean state,int delayTime,Object tag){
		byte value = (byte) (state ? 1 : 0);
		byte[] params = new byte[]{value};
		sendCommand(Opcode.WriteContinuousPrint, params,delayTime,tag);
	}

	/**
	 * 读取墨盒id
	 */
	public void readCartridgeId(){
		sendCommand(Opcode.ReadCartridgeId, null);
	}

	/**
	 * 读取墨盒id
	 * @param delayTime 指令延时发送的时间
	 */
	public void readCartridgeId(int delayTime){
		sendCommand(Opcode.ReadCartridgeId, null,delayTime);
	}

	public void clearPrintHead(){
		sendCommand(Opcode.ClearPrintHead, null);
	}

	public void clearPrintHead(int delayTime){
		sendCommand(Opcode.ClearPrintHead, null,delayTime);
	}

	public void writeConnectStateConnected(){
		byte[] state = {0x17,0x07,0x00, (byte) 0xF8, (byte) 0xFF,0x01,0x00,0x00,0x00,0x02,0x02,0x01, (byte) 0xE2,0x55};
		DataObj dataObj = new DataObj(state);
		DataObjContext context = new DataObjContext(dataObj, dataObjCallback);
		writeData(context);
	}

	public void writeConnectStateDisconnected(){
		byte[] state = {0x17,0x07,0x00, (byte) 0xF8, (byte) 0xFF,0x01,0x00,0x00,0x00,0x02,0x02,0x00,0x62,0x50};
		DataObj dataObj = new DataObj(state);
		DataObjContext context = new DataObjContext(dataObj, dataObjCallback);
		writeData(context);
	}

	/**
	 *  用于创建指令对象 Command
	 * @param opcode  指令的opcode
	 * @param params  指令参数
	 * @return
	 */
	public Command createCommand(int opcode, byte[] params) {
		return this.createCommand(opcode,params,-1);
	}

	/**
	 *  用于创建指令对象 Command
	 * @param opcode  指令的opcode
	 * @param params  指令参数
	 * @param delayTime  延时发送时间，单位为毫秒
	 * @return
	 */
	/*
	public Command createCommand(int opcode, byte[] params,int delayTime) {

		final int prefixLen = 1;
		final int packetLenLen = 2;
		final int packetXorLenLen = 2;
		final int packet_ctLen = 4;
		final int opcodeLen = 2;
		int paramsLen = 0;
		if (params != null) {
			paramsLen = params.length;
		}
		final int crcLen = 2;

		int byteLen = prefixLen + packetLenLen + packetXorLenLen + packet_ctLen + opcodeLen + paramsLen + crcLen;

		int pack_Len = packet_ctLen + opcodeLen + paramsLen;

		int crcByteLen = prefixLen + packetLenLen + packetXorLenLen + packet_ctLen + opcodeLen + paramsLen;

		byte[] data = new byte[byteLen];

		int offset = 0;
		// prefix 前缀
		data[offset++] = 0x17;
		// packageLen
		data[offset++] = (byte) (pack_Len & 0xFF);
		data[offset++] = (byte) (pack_Len >> 8 & 0xFF);

		//packageLen取反
		data[offset++] = (byte) ((~pack_Len) & 0xFF);
		data[offset++] = (byte) ((~pack_Len) >> 8 & 0xFF);

		//pack_ct 帧序列 总共4byte
		int pack_ct = this.generateSequenceNumber();

		RBQLog.i("pack_ct:" + pack_ct);

		data[offset++] = (byte) (pack_ct & 0xFF);
		data[offset++] = (byte) (pack_ct >> 8 & 0xFF);
		data[offset++] = (byte) (pack_ct >> 16 & 0xFF);
		data[offset++] = (byte) (pack_ct >> 24 & 0xFF);

		// opcode
		data[offset++] = (byte) (opcode & 0xFF);
		data[offset++] = (byte) (opcode >> 8 & 0xFF);

		// params
		if (params != null) {
			System.arraycopy(params, 0, data, offset, params.length);
		}

		byte[] crcByte = new byte[crcByteLen];
		System.arraycopy(data, 0, crcByte, 0, crcByteLen);

		char crc = CRC16.crc16_calc(crcByte);

		offset = offset + paramsLen;

		data[offset++] = (byte) (crc >> 8 & 0xFF);
		data[offset] = (byte) (crc & 0xFF);

		//        RBQLog.i("[***参与crc计算的byte ***: "+ Arrays.bytesToHexString1(crcByte,",")+" ;crc的值:"+(int)crc+" ["+ (crc >> 8 & 0xFF) + "] + ["+(crc & 0xFF)+"]");
        return new Command(data,SPP_TAG_NORMAL_COMMAND,delayTime);
	}
	*/

	public Command createCommand(int opcode, byte[] params, int delayTime) {
		return createCommand(opcode,params,delayTime,SPP_TAG_NORMAL_COMMAND);
	}

	public Command createCommand(int opcode, byte[] params, int delayTime,Object tag) {
		final int prefixLen = 1;
		final int packetLenLen = 2;
		final int packetXorLenLen = 2;
		final int packetCtLen = 4;
		final int opcodeLen = 2;
		final int crcLen = 2;

		int paramsLen = (params != null) ? params.length : 0;
		int byteLen = prefixLen + packetLenLen + packetXorLenLen + packetCtLen + opcodeLen + paramsLen + crcLen;
		int packLen = packetCtLen + opcodeLen + paramsLen;
		int crcByteLen = prefixLen + packetLenLen + packetXorLenLen + packetCtLen + opcodeLen + paramsLen;

		byte[] data = new byte[byteLen];
		int offset = 0;

		data[offset++] = 0x17;  // prefix
		data[offset++] = (byte) (packLen & 0xFF);  // packageLen
		data[offset++] = (byte) ((packLen >> 8) & 0xFF);

		data[offset++] = (byte) ((~packLen) & 0xFF);  // packageLen取反
		data[offset++] = (byte) ((~packLen >> 8) & 0xFF);

		int packCt = this.generateSequenceNumber();
		data[offset++] = (byte) (packCt & 0xFF);  // pack_ct 帧序列
		data[offset++] = (byte) ((packCt >> 8) & 0xFF);
		data[offset++] = (byte) ((packCt >> 16) & 0xFF);
		data[offset++] = (byte) ((packCt >> 24) & 0xFF);

		data[offset++] = (byte) (opcode & 0xFF);  // opcode
		data[offset++] = (byte) ((opcode >> 8) & 0xFF);

		if (params != null) {
			System.arraycopy(params, 0, data, offset, paramsLen);  // params
		}

//		byte[] crcByte = new byte[crcByteLen];
//		System.arraycopy(data, 0, crcByte, 0, crcByteLen);
//
//		char crc = CRC16.crc16_calc(crcByte);
		char crc = CRC16.crc16_calc(data,0,crcByteLen);
		offset += paramsLen;

		data[offset++] = (byte) ((crc >> 8) & 0xFF);  // CRC
		data[offset] = (byte) (crc & 0xFF);

		return new Command(data, tag, delayTime);
	}

	/**
	 *
	 * @param opcode 指令的opcode
	 * @param params 指令的参数，可以为null
	 */
	public void sendCommand(int opcode, byte[] params) {
		if (this.isDataSending){
			return;
		}
		Command command = createCommand(opcode, params);
		CommandContext context = new CommandContext(command, commandCallback);
		localSendCommand(context);
	}

	public void sendCommand(int opcode, byte[] params,Command.Callback callback) {
		if (this.isDataSending){
			return;
		}
		Command command = createCommand(opcode, params);
		CommandContext context = new CommandContext(command, callback);
		localSendCommand(context);
	}

	/**
	 *
	 * @param opcode  指令的opcode
	 * @param params  指令的参数，可以为null
	 * @param delayTime 指令的延时发送时间，单位为毫秒
	 */
	public void sendCommand(int opcode, byte[] params,int delayTime) {
		if (this.isDataSending){
			return;
		}
		Command command = createCommand(opcode, params,delayTime);
		CommandContext context = new CommandContext(command, commandCallback);
		localSendCommand(context);
	}

	public void sendCommand(int opcode, byte[] params,int delayTime,Object tag) {
		if (this.isDataSending){
			return;
		}
		Command command = createCommand(opcode, params,delayTime,tag);
		CommandContext context = new CommandContext(command, commandCallback);
		localSendCommand(context);
	}

	public void sendCommand(int opcode, byte[] params,int delayTime,Command.Callback callback) {
		if (this.isDataSending){
			return;
		}
		Command command = createCommand(opcode, params,delayTime);
		CommandContext context = new CommandContext(command, callback);
		localSendCommand(context);
	}

	private void innerSendCommand(int opcode, byte[] params) {
		Command command = createCommand(opcode, params);
		CommandContext context = new CommandContext(command, commandCallback);
		localSendCommand(context);
	}

	private void innerSendCommand(int opcode, byte[] params,Command.Callback callback) {
		Command command = createCommand(opcode, params);
		CommandContext context = new CommandContext(command, callback);
		localSendCommand(context);
	}

	private void innerSendCommand(int opcode, byte[] params,int delayTime) {
		Command command = createCommand(opcode, params,delayTime);
		CommandContext context = new CommandContext(command, commandCallback);
		localSendCommand(context);
	}

	private void innerSendCommand(int opcode, byte[] params,int delayTime,Object tag) {
		Command command = createCommand(opcode, params,delayTime,tag);
		CommandContext context = new CommandContext(command, commandCallback);
		localSendCommand(context);
	}

	private void innerSendCommand(int opcode, byte[] params,int delayTime,Command.Callback callback) {
		Command command = createCommand(opcode, params,delayTime);
		CommandContext context = new CommandContext(command, callback);
		localSendCommand(context);
	}

	private boolean isCurrentSendCommandContext(CommandContext context){
		if(context == null || context.command == null|| context.command.data == null){
			return false;
		}
		if(context.command.delayTime == -1){
			return true;
		}
		long currentTime = System.currentTimeMillis();
		long offset = currentTime - context.command.createTime;
		//        if(offset>=command.delayTime&&command.isLossOnTimeout){
		// 目前还没设计合理的丢弃方案，暂时不考虑 isLossOnTimeout 值
		return offset >= context.command.delayTime;
	}

	private CommandContext findWithRemoveCommandContext(){
		CommandContext context;
		for (int i=0; i<commandQueue.size(); i++) {
			context = commandQueue.get(i);
			if(context.command.delayTime==-1){
				//移除并返回
				commandQueue.remove(context);
				return context;
			}
			long currentTime = System.currentTimeMillis();
			long offset = currentTime - context.command.createTime;
//        if(offset>=command.delayTime&&command.isLossOnTimeout){
			// 目前还没设计合理的丢弃方案，暂时不考虑 isLossOnTimeout 值
			if(offset >= context.command.delayTime){
				//移除并返回
				commandQueue.remove(context);
				return context;
			}
		}
		return null;
	}

	/**
	 *
	 * @param context
	 */
	public void localSendCommand(CommandContext context) {

		long currentTime = System.currentTimeMillis();
		long offsetTime = currentTime - lastSendCommandTime;

		if (offsetTime >= commandInterval
				&& commandQueue.isEmpty()
				&& isCurrentSendCommandContext(context)) {

			RBQLog.i("发送的指令数据为:>>>" + Arrays.bytesToPrefixedHexString(context.command.data, ","));

			lastSendCommandTime = currentTime;
			writeCommandContext(context);

		} else {

			RBQLog.i("添加指令数据为:>>>" + Arrays.bytesToPrefixedHexString(context.command.data, ","));

			commandQueue.add(context);

			commandHandler.removeCallbacks(commandRunnable);
			commandHandler.removeCallbacksAndMessages(null);

			commandHandler.postDelayed(commandRunnable, commandInterval);
		}
	}

	/**
	 * The Command runnable.
	 */
	Runnable commandRunnable = new Runnable() {
		@Override
		public void run() {

			if (!commandQueue.isEmpty()) {

				RBQLog.i("发送commandQueue中的指令");

				commandHandler.removeCallbacks(commandRunnable);
				commandHandler.removeCallbacksAndMessages(null);

				CommandContext context = findWithRemoveCommandContext();
				writeCommandContext(context);

				lastSendCommandTime = System.currentTimeMillis();

				if (!commandQueue.isEmpty()) {

					commandHandler.removeCallbacks(commandRunnable);
					commandHandler.removeCallbacksAndMessages(null);
					//
					RBQLog.i("启动下次commandQueue中指令发送");
					commandHandler.postDelayed(commandRunnable, commandInterval);
				} else {
					RBQLog.i("commandQueue中指令发送完毕");
				}
			}

		}
	};

	private void writeCommandContext(CommandContext context){
		if (context == null || context.command == null || context.command.data == null){
			return;
		}
		writeThread.write(context);
	}

	private void writeData(DataObjContext context){
		if (context == null || context.dataObj == null || context.dataObj.data == null){
			return;
		}
		writeThread.write(context);
	}
	/**
	 * Clear command queue.
	 */
	public synchronized void clearCommandQueue() {
		commandHandler.removeCallbacks(commandRunnable);
		commandHandler.removeCallbacksAndMessages(null);
		commandQueue.clear();
	}

	/**
	 * The Normal callback.
	 */
	Command.Callback commandCallback = new Command.Callback() {
		@Override
		public void success(Command command, Object obj) {
//			RBQLog.i("发送指令成功，指令 -> " + Arrays.bytesToPrefixedHexString(command.data, ","));
			RBQLog.i("发送指令成功，指令 -> " + Arrays.bytesToHexString(command.data, " "));
			notifyCommandWriteSuccess(device,command,obj);
		}

		@Override
		public void error(Command command, String errorMsg) {
			RBQLog.i("指令发送错误:" + errorMsg);
			notifyCommandWriteError(device,command,errorMsg);
		}

		@Override
		public boolean timeout(Command command, boolean delayEfficacy) {
			return false;
		}
	};

	DataObj.Callback dataObjCallback = new DataObj.Callback() {
		@Override
		public void success(DataObj dataObj, Object obj) {
			notifyDataWriteSuccess(device,dataObj,obj);
		}

		@Override
		public void error(DataObj dataObj, String errorMsg) {
			notifyDataWriteError(device,dataObj,errorMsg);
		}

		@Override
		public boolean timeout(DataObj dataObj, boolean delayEfficacy) {
			return false;
		}
	};

	private int generateSequenceNumber() {

		int maxNum = 255;

		if (this.sequenceNumber > maxNum) {

			this.sequenceNumber = random.nextInt(maxNum - 1) + 1;
		}
		this.sequenceNumber++;

		return this.sequenceNumber;
	}

	private byte[] generateRandom(byte[] random) {
		this.random.nextBytes(random);
		return random;
	}

	/********************************************************************************
	 * 发送连续的图片大数据包
	 * @param packet the packet
	 */
	public void startPacket(BasePacket packet){
		for (BasePacket _packet : packets){
			if (!packet.equals(_packet)){
				packet.setStartSendingData(false);
			}
		}
		packet.setStartSendingData(true);
	}

	/**
	 * Cancel all packet start.
	 */
	public void cancelAllPacketStart(){
		for (BasePacket packet : packets){
			packet.setStartSendingData(false);
		}
	}

	/**
	 * Has packet start boolean.
	 *
	 * @return the boolean
	 */
	public boolean hasPacketStartSending(){
		boolean isStartSenddingData = false;
		for (BasePacket packet : packets){
			if (packet.isStartSendingData()){
				isStartSenddingData = true;
				break;
			}
		}
		return isStartSenddingData;
	}

	/**
	 * 中止传输
	 */
	public void cancelSendMultiRowDataPacket(){
		if(!multiRowDataPacket.isStartSendingData()){
			return;
		}
		waitResponseHandler.removeCallbacks(retryMultiRowDataRunnable);
		waitResponseHandler.removeCallbacksAndMessages(null);
		multiRowDataPacket.clear();
		isDataSending = false;
	}

	/**
	 * Sets with send multi row data packet.
	 *
	 * @param multiRowData the multi row data
	 */
	public void setWithSendMultiRowDataPacket(MultiRowData multiRowData) {
		setWithSendMultiRowDataPacket(multiRowData, TransportProtocol.STX_E);
	}

	/**
	 * Sets with send multi row data packet.
	 *
	 * @param multiRowData the multi row data
	 * @param fh           the fh
	 */
	public void setWithSendMultiRowDataPacket(MultiRowData multiRowData, int fh) {

		if (!isConnected()) {
			return;
		}
		if (isDataSynchronize()) {
			notifyDataProgressError("mxSdk错误，正在和设备进行数据同步中...",SyncingDataError);
			return;
		}
		if (!commandQueue.isEmpty()) {
			notifyDataProgressError("mxSdk错误，指令集中还存在指令正在处理，请稍等...",CommandQueueIsNoEmptyError);
			return;
		}
		send_packet_Index = 0;
		multiRowDataPacket.set(multiRowData, fh);
		localSetWithSendMultiRowDataPacket();
		waitResponseHandler.postDelayed(retryMultiRowDataRunnable,retrySendDataTime);
	}

	private void localSetWithSendMultiRowDataPacket() {

		if (!this.multiRowDataPacket.hasData()){
			return;
		}
		this.isDataSending = true;

		// 重置 JSON 接收状态
		if (jsonStreamAssembler != null) {
			jsonStreamAssembler.reset();
		}
		startPacket(this.multiRowDataPacket);

		clearCommandQueue();//确保在图片打印过程中，指令列表里面一定是没其他指令

		byte currentRow = (byte) (multiRowDataPacket.getCurrentRow() & 0xFF);

		RBQLog.i("打印第:" + currentRow + "行");

		int arrIndexDataSize = multiRowDataPacket.currentRowDataLength;//4byte

		byte dataSize0 = (byte) (arrIndexDataSize & 0xFF);
		byte dataSize1 = (byte) ((arrIndexDataSize >> 8) & 0xFF);
		byte dataSize2 = (byte) ((arrIndexDataSize >> 16) & 0xFF);
		byte dataSize3 = (byte) ((arrIndexDataSize >> 24) & 0xFF);
		byte compress = (byte) (multiRowDataPacket.compress & 0xFF);

		byte[] transmitParams = {currentRow, dataSize0, dataSize1, dataSize2, dataSize3, compress};
		innerSendCommand(Opcode.TransmitPictureData, transmitParams);

		multiRowDataPacket.startTime = System.currentTimeMillis();
		float size = (float) multiRowDataPacket.totalDataLen / 1000.0f;
		//发送进度更新事件
		notifyDataProgressStart(size, multiRowDataPacket.startTime);
	}

	private void sendNextMultiRowImagePacket() {

		if (!isConnected()) {
			return;
		}

		byte[] data = this.multiRowDataPacket.getNextPacket();

		multiRowDataPacket.currentTime = System.currentTimeMillis();

		boolean updateProgress = multiRowDataPacket.invalidateProgress();
		if (updateProgress) {
			int progress = multiRowDataPacket.getProgress();
			float size = (float) multiRowDataPacket.totalDataLen / 1000.0f;
			//发送进度更新事件
			notifyDataProgressProgress(size, progress, multiRowDataPacket.startTime, multiRowDataPacket.currentTime);
		}
		byte[] formatData = this.multiRowDataPacket.packetFormat(data);
		DataObj dataObj = new DataObj(formatData);
		DataObjContext context = new DataObjContext(dataObj, dataObjCallback);
		writeData(context);

	}

	/**
	 * 重传当前数据包指令
	 */
	private void sendNakMultiRowImagePacket() {

		if (!isConnected()) {
			return;
		}

		byte[] data = this.multiRowDataPacket.getCurrentPacket();
		RBQLog.i("NAK 重传当前包");
		multiRowDataPacket.currentTime = System.currentTimeMillis();
		byte[] formatData = this.multiRowDataPacket.packetFormat(data);
		DataObj dataObj = new DataObj(formatData);
		DataObjContext context = new DataObjContext(dataObj, dataObjCallback);
		writeData(context);
	}

	/**
	 * 中止传输
	 */
	public void cancelSendOtaPacket(){
		if(!otaPacket.isStartSendingData()){
			return;
		}
		waitResponseHandler.removeCallbacks(retryOtaRunnable);
		waitResponseHandler.removeCallbacksAndMessages(null);
		otaPacket.clear();
	}

	// ==================== CMYK MultiRowData 相关方法 ====================

	private final Runnable retryCMYKMultiRowDataRunnable = new Runnable() {
		@Override
		public void run() {
			localSetWithSendCMYKMultiRowDataPacket();
		}
	};

	/**
	 * 中止CMYK数据传输
	 */
	public void cancelSendCMYKMultiRowDataPacket() {
		if (!cmykMultiRowDataPacket.isStartSendingData()) {
			return;
		}
		waitResponseHandler.removeCallbacks(retryCMYKMultiRowDataRunnable);
		waitResponseHandler.removeCallbacksAndMessages(null);
		cmykMultiRowDataPacket.clear();
		isDataSending = false;
	}

	/**
	 * 设置并发送CMYK多行数据包（使用默认帧头）
	 *
	 * @param cmykMultiRowData CMYK多行数据
	 */
	public void setWithSendCMYKMultiRowDataPacket(CMYKMultiRowData cmykMultiRowData) {
		setWithSendCMYKMultiRowDataPacket(cmykMultiRowData, TransportProtocol.STX_E);
	}

	/**
	 * 设置并发送CMYK多行数据包
	 *
	 * @param cmykMultiRowData CMYK多行数据
	 * @param fh               帧头
	 */
	public void setWithSendCMYKMultiRowDataPacket(CMYKMultiRowData cmykMultiRowData, int fh) {

		if (!isConnected()) {
			return;
		}
		if (isDataSynchronize()) {
			notifyDataProgressError("mxSdk错误，正在和设备进行数据同步中...", SyncingDataError);
			return;
		}
		if (!commandQueue.isEmpty()) {
			notifyDataProgressError("mxSdk错误，指令集中还存在指令正在处理，请稍等...", CommandQueueIsNoEmptyError);
			return;
		}
		send_packet_Index = 0;
		cmykMultiRowDataPacket.set(cmykMultiRowData, fh);
		localSetWithSendCMYKMultiRowDataPacket();
		waitResponseHandler.postDelayed(retryCMYKMultiRowDataRunnable, retrySendDataTime);
	}

	/**
	 * 构建行+通道的组合字节
	 * 高4位是行号，低4位是通道号(0=C, 1=M, 2=Y, 3=K)
	 */
	private byte buildRowChannelByte(int row, int channelIndex) {
		return (byte) (((row & 0x0F) << 4) | (channelIndex & 0x0F));
	}

	private void localSetWithSendCMYKMultiRowDataPacket() {

		if (!this.cmykMultiRowDataPacket.hasData()) {
			return;
		}
		this.isDataSending = true;

		// 重置 JSON 接收状态
		if (jsonStreamAssembler != null) {
			jsonStreamAssembler.reset();
		}
		startPacket(this.cmykMultiRowDataPacket);

		clearCommandQueue(); // 确保在图片打印过程中，指令列表里面一定是没其他指令

		// 高4位是行号，低4位是通道号
		byte rowChannel = buildRowChannelByte(
				cmykMultiRowDataPacket.getCurrentRow(),
				cmykMultiRowDataPacket.getCurrentChannelIndex()
		);

		RBQLog.i("CMYK开始打印 CMYK发送 行:" + cmykMultiRowDataPacket.getCurrentRow() +
				" 通道:" + cmykMultiRowDataPacket.getCurrentChannel().getName());

		int channelDataSize = cmykMultiRowDataPacket.getCurrentChannelDataLength(); // 4byte

		byte dataSize0 = (byte) (channelDataSize & 0xFF);
		byte dataSize1 = (byte) ((channelDataSize >> 8) & 0xFF);
		byte dataSize2 = (byte) ((channelDataSize >> 16) & 0xFF);
		byte dataSize3 = (byte) ((channelDataSize >> 24) & 0xFF);
		byte compress = (byte) (cmykMultiRowDataPacket.compress & 0xFF);

		byte[] transmitParams = {rowChannel, dataSize0, dataSize1, dataSize2, dataSize3, compress};
		innerSendCommand(Opcode.TransmitPictureData, transmitParams);

		cmykMultiRowDataPacket.startTime = System.currentTimeMillis();
		float size = (float) cmykMultiRowDataPacket.totalDataLen / 1000.0f;
		// 发送进度更新事件
		notifyDataProgressStart(size, cmykMultiRowDataPacket.startTime);
	}

	private void sendNextCMYKMultiRowImagePacket() {

		if (!isConnected()) {
			return;
		}

		byte[] data = this.cmykMultiRowDataPacket.getNextPacket();

		cmykMultiRowDataPacket.currentTime = System.currentTimeMillis();

		boolean updateProgress = cmykMultiRowDataPacket.invalidateProgress();
		if (updateProgress) {
			int progress = cmykMultiRowDataPacket.getProgress();
			float size = (float) cmykMultiRowDataPacket.totalDataLen / 1000.0f;
			// 发送进度更新事件
			notifyDataProgressProgress(size, progress, cmykMultiRowDataPacket.startTime, cmykMultiRowDataPacket.currentTime);
		}
		byte[] formatData = this.cmykMultiRowDataPacket.packetFormat(data);
		DataObj dataObj = new DataObj(formatData);
		DataObjContext context = new DataObjContext(dataObj, dataObjCallback);
		writeData(context);
	}

	/**
	 * 重传当前CMYK数据包
	 */
	private void sendNakCMYKMultiRowImagePacket() {

		if (!isConnected()) {
			return;
		}

		byte[] data = this.cmykMultiRowDataPacket.getCurrentPacket();
		RBQLog.i("CMYK NAK 重传当前包");
		cmykMultiRowDataPacket.currentTime = System.currentTimeMillis();
		byte[] formatData = this.cmykMultiRowDataPacket.packetFormat(data);
		DataObj dataObj = new DataObj(formatData);
		DataObjContext context = new DataObjContext(dataObj, dataObjCallback);
		writeData(context);
	}

	/**
	 * 处理CMYK当前通道发送完毕后的逻辑
	 */
	protected void delayCMYKCheckNextChannelOrRowData() {

		// 判断当前行是否还有下一个通道
		if (this.cmykMultiRowDataPacket.hasNextChannelInCurrentRow()) {

			mainHandler.postDelayed(() -> {
				// 切换到下一个通道
				cmykMultiRowDataPacket.moveToNextChannel();

				byte rowChannel = buildRowChannelByte(
						cmykMultiRowDataPacket.getCurrentRow(),
						cmykMultiRowDataPacket.getCurrentChannelIndex()
				);

				RBQLog.i("CMYK发送 行:" + cmykMultiRowDataPacket.getCurrentRow() +
						" 通道:" + cmykMultiRowDataPacket.getCurrentChannel().getName());

				int channelDataSize = cmykMultiRowDataPacket.getCurrentChannelDataLength();
				byte dataSize0 = (byte) (channelDataSize & 0xFF);
				byte dataSize1 = (byte) ((channelDataSize >> 8) & 0xFF);
				byte dataSize2 = (byte) ((channelDataSize >> 16) & 0xFF);
				byte dataSize3 = (byte) ((channelDataSize >> 24) & 0xFF);
				byte compress = (byte) (cmykMultiRowDataPacket.compress & 0xFF);

				byte[] transmitParams = {rowChannel, dataSize0, dataSize1, dataSize2, dataSize3, compress};
				innerSendCommand(Opcode.TransmitPictureData, transmitParams);
			}, 300);

		} else if (this.cmykMultiRowDataPacket.hasNextRow()) {
			// 当前行所有通道都发完了，切换到下一行
			mainHandler.postDelayed(() -> {
				cmykMultiRowDataPacket.moveToNextRow();

				byte rowChannel = buildRowChannelByte(
						cmykMultiRowDataPacket.getCurrentRow(),
						cmykMultiRowDataPacket.getCurrentChannelIndex()
				);

				RBQLog.i("CMYK发送下一行 行:" + cmykMultiRowDataPacket.getCurrentRow() +
						" 通道:" + cmykMultiRowDataPacket.getCurrentChannel().getName());

				int channelDataSize = cmykMultiRowDataPacket.getCurrentChannelDataLength();
				byte dataSize0 = (byte) (channelDataSize & 0xFF);
				byte dataSize1 = (byte) ((channelDataSize >> 8) & 0xFF);
				byte dataSize2 = (byte) ((channelDataSize >> 16) & 0xFF);
				byte dataSize3 = (byte) ((channelDataSize >> 24) & 0xFF);
				byte compress = (byte) (cmykMultiRowDataPacket.compress & 0xFF);

				byte[] transmitParams = {rowChannel, dataSize0, dataSize1, dataSize2, dataSize3, compress};
				innerSendCommand(Opcode.TransmitPictureData, transmitParams);
			}, 300);

		} else {
			// 所有行所有通道都发完了，发送打印指令
			mainHandler.postDelayed(() -> {
				int index = cmykMultiRowDataPacket.totalRowCount;
				byte[] printPictureParams = {0, (byte) index};
				innerSendCommand(Opcode.PrintPicture, printPictureParams);
                RBQLog.i("----CMYK图片数据传输完毕---并发送了图片打印指令，打印从0索引开始到"+index+"索引的图片");
				cmykMultiRowDataPacket.currentTime = System.currentTimeMillis();
				float size = (float) cmykMultiRowDataPacket.totalDataLen / 1000.0f;
				notifyDataProgressFinish(size, cmykMultiRowDataPacket.startTime, cmykMultiRowDataPacket.currentTime);
				cancelAllPacketStart();

				isDataSending = false;
			}, 100);
		}
	}

	/**
	 * Sets with send ota packet.
	 *
	 * @param data the data
	 */
	public void setWithSendOtaPacket(byte[] data) {
		setWithSendOtaPacket(data, TransportProtocol.STX_E);
	}

	/**
	 * Sets with send ota packet.
	 *
	 * @param data the data
	 * @param fn   the fn
	 */
	public void setWithSendOtaPacket(byte[] data, int fn) {

		if (!isConnected()) {
			return;
		}

		if (isDataSynchronize()) {
			notifyDataProgressError("mxSdk错误，正在和设备进行数据同步中...",SyncingDataError);
			return;
		}
		if (!commandQueue.isEmpty()) {
			notifyDataProgressError("mxSdk错误，指令集中还存在指令正在处理，请稍等...",CommandQueueIsNoEmptyError);
			return;
		}
		this.otaPacket.set(data, fn);
		localSetWidthSendOtaPacket();
		waitResponseHandler.postDelayed(retryOtaRunnable,retrySendDataTime);
	}

	private void localSetWidthSendOtaPacket() {

		if (!this.otaPacket.hasData()) {
			return;
		}
		this.isDataSending = true;

		// 重置 JSON 接收状态
		if (jsonStreamAssembler != null) {
			jsonStreamAssembler.reset();
		}
		startPacket(this.otaPacket);

		int dataLength = this.otaPacket.dataLength;//4byte

		byte dataSize0 = (byte) (dataLength & 0xFF);
		byte dataSize1 = (byte) ((dataLength >> 8) & 0xFF);
		byte dataSize2 = (byte) ((dataLength >> 16) & 0xFF);
		byte dataSize3 = (byte) ((dataLength >> 24) & 0xFF);

		byte[] otaParams = {dataSize0, dataSize1, dataSize2, dataSize3};

		innerSendCommand(Opcode.UpdateMcu, otaParams);

		float size = (float) otaPacket.dataLength / 1000.0f;
		otaPacket.startTime = System.currentTimeMillis();
		notifyDataProgressStart(size, otaPacket.startTime);
	}

	private void sendNextOtaPacket() {

		if (!isConnected() || !otaPacket.hasNextPacket()) {
			return;
		}
		byte[] data = otaPacket.getNextPacket();

		if (data == null) {
			RBQLog.i("获取下一包为null");
			return;
		}
		//        RBQLog.i("发送第 ["+ sppOTAPacket.index+"] 包数据");
		otaPacket.currentTime = System.currentTimeMillis();

		boolean updateProgress = otaPacket.invalidateProgress();
		if (updateProgress) {
			int progress = otaPacket.getProgress();
			float size = (float) otaPacket.dataLength / 1000.0f;
			//发送进度更新事件
			notifyDataProgressProgress(size, progress, otaPacket.startTime, otaPacket.currentTime);
		}
		byte[] formatData = this.otaPacket.packetFormat(data);
		DataObj dataObj = new DataObj(formatData);
		DataObjContext context = new DataObjContext(dataObj, dataObjCallback);
		writeData(context);
	}

	/**
	 * 重传当前数据包指令
	 */
	private void sendNAKOtaPacket() {

		if (!isConnected()) {
			return;
		}
		byte[] data = otaPacket.getPacket();
		RBQLog.i("NAK 重传当前包");
		otaPacket.currentTime = System.currentTimeMillis();
		byte[] formatData = this.otaPacket.packetFormat(data);
		DataObj dataObj = new  DataObj(formatData);
		DataObjContext context = new DataObjContext(dataObj, dataObjCallback);
		writeData(context);
	}

	/**
	 * Cancel send logo packet.
	 */
	//中止传输
	public void cancelSendLogoPacket(){
		if(!logoPacket.isStartSendingData()){
			return;
		}
		waitResponseHandler.removeCallbacks(retryLogoDataRunnable);
		waitResponseHandler.removeCallbacksAndMessages(null);
		logoPacket.clear();
	}

	/**
	 * Sets with send logo packet.
	 *
	 * @param logoData the data
	 */
	public void setWithSendLogoPacket(LogoData logoData) {
		setWithSendLogoPacket(logoData, TransportProtocol.STX_E);
	}

	/**
	 * Sets with send logo packet.
	 *
	 * @param logoData the data
	 * @param fn   the fn
	 */
	public void setWithSendLogoPacket(LogoData logoData, int fn) {

		if (!isConnected()) {
			return;
		}

		if (isDataSynchronize()) {
			notifyDataProgressError("mxSdk错误，正在和设备进行数据同步中...",SyncingDataError);
			return;
		}
		if (!commandQueue.isEmpty()) {
			notifyDataProgressError("mxSdk错误，指令集中还存在指令正在处理，请稍等...",CommandQueueIsNoEmptyError);
			return;
		}
		this.logoPacket.set(logoData, fn);
		localSetWidthSendLogoPacket();
		waitResponseHandler.postDelayed(retryLogoDataRunnable,retrySendDataTime);
	}

	private void localSetWidthSendLogoPacket() {

		if (!this.logoPacket.hasData()) return;

		this.isDataSending = true;

		// 重置 JSON 接收状态
		if (jsonStreamAssembler != null) {
			jsonStreamAssembler.reset();
		}
		startPacket(this.logoPacket);

		int dataLength = this.logoPacket.dataLength;//4byte

		byte dataSize0 = (byte) (dataLength & 0xFF);
		byte dataSize1 = (byte) ((dataLength >> 8) & 0xFF);
		byte dataSize2 = (byte) ((dataLength >> 16) & 0xFF);
		byte dataSize3 = (byte) ((dataLength >> 24) & 0xFF);

		byte[] logoParams = {dataSize0, dataSize1, dataSize2, dataSize3};

		innerSendCommand(Opcode.WriteLogoData, logoParams);

		float size = (float) logoPacket.dataLength / 1000.0f;
		logoPacket.startTime = System.currentTimeMillis();
		notifyDataProgressStart(size, logoPacket.startTime);
	}

	private void sendNextLogoPacket() {

		if (!isConnected() || !logoPacket.hasNextPacket()) return;
		byte[] data = logoPacket.getNextPacket();

		if (data == null) {
			RBQLog.i("获取下一包为null");
			return;
		}

//		RBQLog.i("发送第 [" + logoPacket.index + "] 包数据");

		logoPacket.currentTime = System.currentTimeMillis();
		boolean updateProgress = logoPacket.invalidateProgress();
		if (updateProgress) {
			int progress = logoPacket.getProgress();
			float size = (float) logoPacket.dataLength / 1000.0f;
			//发送进度更新事件
			notifyDataProgressProgress(size, progress, logoPacket.startTime, logoPacket.currentTime);
		}
		byte[] formatData = this.logoPacket.packetFormat(data);
		DataObj dataObj = new DataObj(formatData);
		DataObjContext context = new DataObjContext(dataObj, dataObjCallback);
		writeData(context);
	}

	/**
	 * 重传当前数据包指令
	 */
	private void sendNAKLogoPacket() {

		if (!isConnected()) {
			return;
		}
		byte[] data = logoPacket.getPacket();
		RBQLog.i("NAK 重传当前包");
		logoPacket.currentTime = System.currentTimeMillis();
		byte[] formatData = this.logoPacket.packetFormat(data);
		DataObj dataObj = new DataObj(formatData);
		DataObjContext context = new DataObjContext(dataObj, dataObjCallback);
		writeData(context);
	}

	/**
	 * The interface On multi row data transfer listener.
	 */
	public interface OnDataProgressListener {
		/**
		 * On multi row data transfer start.
		 *
		 * @param size      the size
		 * @param progress  the progress
		 * @param startTime the start time
		 */
		void onDataProgressStart(float size, int progress, long startTime);//打印开始

		/**
		 * On multi row data transfer progress.
		 *
		 * @param size        the size
		 * @param progress    the progress
		 * @param startTime   the start time
		 * @param currentTime the current time
		 */
		void onDataProgress(float size, int progress, long startTime, long currentTime);//打印过程进度更新

		/**
		 * On multi row data transfer finish.
		 *
		 * @param size        the size
		 * @param startTime   the start time
		 * @param currentTime the current time
		 */
		void onDataProgressFinish(float size, long startTime, long currentTime);//打印完成

		/**
		 * On multi row data transfer error.
		 *
		 * @param error the error
		 */
		void onDataProgressError(String error,int code);
	}

	public interface OnCommandWriteListener {

		/**
		 *
		 * @param command
		 * @param object
		 */
		void onCommandWriteSuccess(Device device,Command command, Object object);

		/**
		 *
		 * @param command
		 * @param errorMsg
		 */
		void onCommandWriteError(Device device,Command command,String errorMsg);
	}

	public interface OnDataWriteListener {

		/**
		 *
		 * @param dataObj
		 * @param object
		 */
		void onDataWriteSuccess(Device device,DataObj dataObj, Object object);

		/**
		 *
		 * @param dataObj
		 * @param errorMsg
		 */
		void onDataWriteError(Device device,DataObj dataObj,String errorMsg);
	}

	public interface OnDataSynchronizeListener {

		/**
		 * 数据同步开始
		 * @param device
		 */
		void onDataSynchronizeStart(Device device);

		/**
		 * 数据同步完成
		 * @param device
		 */
		void onDataSynchronizeComplete(Device device);

		/**
		 * 数据同步超时
		 * @param device
		 * @param pendingCount 未响应的指令数量
		 */
		void onDataSynchronizeTimeout(Device device, int pendingCount);

		/**
		 * 数据同步被中断（如断连）
		 * @param device
		 */
		void onDataSynchronizeInterrupted(Device device);
	}

	/**
	 * 以下几个响应事件是由该类内部提供，调用该类，向外的事件定义的时候，都是以Spp开头，
	 * 之前其他地方的事件在该类进行了实现，再由该类以Spp格式的事件向外传递。
	 */
	public interface OnDeviceBluetoothStateListener {

		/**
		 * On device blue tooth opening.
		 */
		void onDeviceBlueToothOpening();//蓝牙正在打开

		/**
		 * On device blue tooth opened.
		 */
		void onDeviceBlueToothOpened();//蓝牙打开

		/**
		 * On device blue tooth closing.
		 */
		void onDeviceBlueToothClosing();//蓝牙正在关闭

		/**
		 * On device blue tooth closed.
		 */
		void onDeviceBlueToothClosed();//蓝牙关闭
	}

	/**
	 * The interface On device discover listener.
	 */
//用于蓝牙扫描和发现相关的事件
	public interface OnDeviceDiscoverListener {
		/**
		 * On start discover.
		 */
		void onDeviceStartDiscover();

		/**
		 * On stop discover.
		 */
		void onDeviceStopDiscover();

		/**
		 * On discovered.
		 *
		 * @param device the device
		 */
		void onDeviceDiscovered(Device device);
	}

	public interface OnConnModelDiscoverListener {
		/**
		 * On start discover.
		 */
		void onConnModelStartDiscover();

		/**
		 * On discovered.
		 *
		 * @param connModel the device
		 */
		void onConnModelDiscovered(ConnModel connModel);

		/**
		 * On stop discover.
		 */
		void onConnModelStopDiscover();
	}

	/**
	 * The interface On device bond listener.
	 */
//配对、a2dp、socket等事件统一由该接口一次性提供
	public interface OnDeviceBondListener {
		/**
		 * On device bonding.
		 *
		 * @param device the device
		 */
		void onDeviceBonding(Device device);

		/**
		 * On device bonded.
		 *
		 * @param device the device
		 */
		void onDeviceBonded(Device device);

		/**
		 * On device dis bond.
		 *
		 * @param device the device
		 */
		void onDeviceDisBond(Device device);
	}

	/**
	 * The interface On device connect listener.
	 */
	public interface OnDeviceConnectListener {
		/**
		 * On device connect start.
		 *
		 * @param device the device
		 */
		void onDeviceConnectStart(Device device);

		/**
		 * On device connect succeed.
		 *
		 * @param device the device
		 */
		void onDeviceConnectSucceed(Device device);

		/**
		 * On device disconnect.
		 *
		 * @param device the device
		 */
		void onDeviceDisconnect(Device device);

		/**
		 * On device connect fail.
		 *
		 * @param device the device
		 * @param error  the error
		 */
		void onDeviceConnectFail(Device device, String error);
	}


	/**
	 * The interface On receive msg listener.
	 */
	public interface OnReceiveMsgListener {
		/**
		 * On read parameter.
		 *
		 * @param device    the device
		 * @param headValue the head value
		 * @param l_pix     the l pix
		 * @param p_pix     the p pix
		 * @param distance  the distance
		 */
		void onReadPrinterHeadParameter(Device device, int headValue, int l_pix, int p_pix, int distance);

		/**
		 * On read circulation and repeat time.
		 *
		 * @param device           the device
		 * @param circulation_time the circulation time
		 * @param repeat_time      the repeat time
		 */
		void onReadCirculationAndRepeatTime(Device device, int circulation_time, int repeat_time);

		/**
		 * On read direction.
		 *
		 * @param device                 the device
		 * @param oldHorizontalDirection the old horizontal direction
		 * @param horizontalDirection    the horizontal direction
		 * @param oldVerticalDirection   the old vertical direction
		 * @param verticalDirection      the vertical direction
		 */
		void onReadDirection(Device device,int oldHorizontalDirection, int horizontalDirection,int oldVerticalDirection,int verticalDirection);

		/**
		 * On read software info.
		 *
		 * @param device      the device
		 * @param id          the id
		 * @param name        the name
		 * @param mcu_version the mcu version
		 * @param mcu_date    the mcu date
		 */
		void onReadSoftwareInfo(Device device, String id, String name, String mcu_version, String mcu_date);

		/**
		 * On read temperature.
		 *
		 * @param device the device
		 * @param temp   the temp
		 */
		void onReadTemperature(Device device, int temp);

		/**
		 * On read battery.
		 *
		 * @param device the device
		 * @param bat    the bat
		 */
		void onReadBattery(Device device, int bat);

		void onReadCartridgeId(Device device,String cartridgeId);
		void onReadSilentState(Device device,boolean silentState);
		void onReadAutoPowerOffState(Device device,boolean autoPowerOff);
		void onReadContinuousPrintState(Device device,boolean continuousPrintState);
		/**
		 * On error.
		 *
		 * @param device the device
		 * @param error  the error
		 */
		void onError(Device device, String error);
	}

	public interface OnReadGeneralCommandListener {
		void onReadGeneralCommand(int opcode, String json);
	}

	public interface OnReadPrintStartCommandListener{
		void onReadStartPrintCommand();
	}

	public interface OnReadJsonListener{
		void onReadJson(Device device, String json);
	}

	/**
	 * mark 配网相关
	 */

	BleManager.ScanOptions scanOptions = BleManager.ScanOptions
			.newInstance()
			.scanPeriod(10000)
			.scanDeviceName(null);

	 BleManager.ConnectionOptions connectionOptions = BleManager.ConnectionOptions
			.newInstance()
			.connectionPeriod(12000);

	private void initBleManager(){
		if (this.bleManager!=null){
			this.bleManager.destroy();
			this.bleManager = null;
		}
		this.bleManager = BleManager.getInstance()
				.setScanOptions(scanOptions)//非必须设置项
				.setConnectionOptions(connectionOptions)
				.setLog(true, "TAG")
				.init(application);
	}

	private BeaconItem getManufacturer(Beacon beacon){
		for (int i=0;i<beacon.mItems.size();i++){
			BeaconItem beaconItem = beacon.mItems.get(i);
			if (beaconItem.type==0xff){
				return beaconItem;
			}
		}
		return null;
	}

	private BeaconItem getLocalName(Beacon beacon){
		for (int i=0;i<beacon.mItems.size();i++){
			BeaconItem beaconItem = beacon.mItems.get(i);
			if (beaconItem.type==0x09){
				return beaconItem;
			}
		}
		return null;
	}

		public String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte aByte : bytes) {
			String hex = Integer.toHexString(aByte & 0xFF);
			if (hex.length() < 2) {
				sb.append(0);
			}
			sb.append(hex);
		}
		return sb.toString();
	}

	public String bytesToHexMac(byte[] bytes) {
		StringBuilder hexString = new StringBuilder();
		for (byte b : bytes) {
			hexString.append(String.format("%02X", b)).append(":");
		}
		return hexString.substring(0,hexString.length()-1);
	}

	/**
	 * 将BluetoothDevice地址转为小写并去掉冒号
	 *
	 * @param address 蓝牙设备地址
	 * @return 转换后的地址字符串
	 */
	public String formatBluetoothAddress(String address) {
		if (address == null) {
			return null;
		}
		return address.toLowerCase().replace(":", "");
	}

	/**
	 * 将去掉冒号的小写地址还原回带冒号的大写形式
	 *
	 * @param mac 去掉冒号的小写地址
	 * @return 还原后的地址字符串
	 */
	public String restoreBluetoothAddress(String mac) {
		if (mac == null || mac.length() != 12) {
			return null;
		}
		return String.format(
				"%02X:%02X:%02X:%02X:%02X:%02X",
				Integer.parseInt(mac.substring(0, 2), 16),
				Integer.parseInt(mac.substring(2, 4), 16),
				Integer.parseInt(mac.substring(4, 6), 16),
				Integer.parseInt(mac.substring(6, 8), 16),
				Integer.parseInt(mac.substring(8, 10), 16),
				Integer.parseInt(mac.substring(10, 12), 16)
		);
	}

	public int mx02ConnTypes(){
		return ConnType.SPP.getValue();
	}

	public int mx06ConnTypes(){
		return ConnType.WiFi.getValue()|ConnType.AP.getValue();
	}

	public int inksi01ConnTypes(int apWifi){
		return ConnType.SPP.getValue()|apWifi;
	}

	public synchronized void discoverConnModel(float scanTime){
		if(this.bleManager.isScanning()){
			cancelDiscoverConnModel();
		}

		this.bleManager.startScan(connModelScanCallback);
		if (scanTime>0){
			int time = (int) (scanTime*1000.0f);
			mainHandler.postDelayed(connModelRunnable,time);
		}
	}

	public void cancelDiscoverConnModel(){
		if(this.bleManager.isScanning())
			this.bleManager.stopScan();
		mainHandler.removeCallbacks(connModelRunnable);
	}

	private final Runnable connModelRunnable = new Runnable() {
		@Override
		public void run() {
			//时间到停止扫描
			if(ConnectManager.this.bleManager.isScanning())
				ConnectManager.this.bleManager.stopScan();
			mainHandler.removeCallbacks(connModelRunnable);
		}
	};

	private final BleScanCallback connModelScanCallback = new BleScanCallback() {
		@Override
		public void onLeScan(BleDevice device, int rssi, byte[] scanRecord) {

			if (scanRecord == null || scanRecord.length == 0) {
				return;
			}
			Beacon beacon = new Beacon(scanRecord);
			String name = device.getName();
			String localName = null;
			BeaconItem localNameBeaconItem = getLocalName(beacon);
			if (localNameBeaconItem != null && localNameBeaconItem.bytes != null && localNameBeaconItem.bytes.length > 0){
				localName = new String(localNameBeaconItem.bytes);
			}
			if(isSupperDeviceByName(name)){

				BeaconItem beaconItem = getManufacturer(beacon);
//				RBQLog.i("onLeScan扫描到设备",name+","+beaconItem);
				if (beaconItem==null || beaconItem.bytes == null || beaconItem.bytes.length == 0){
					//此为MX-02设备，由于mx-02机型没广播信号去定位，所以这个没办法和经典蓝牙对应起来，故在Android端Spp连接模式下这里不再传递事件出去
//					ConnModel connModel = new ConnModel(device.device,localName,mx02ConnTypes(),null,null,DeviceModel.createMX02(null,null));
//					notifyConnModelDiscoveredDevice(connModel);
					return;
				}
				byte[] data = beaconItem.bytes;
//				RBQLog.i("onLeScan扫描到设备","dataLength:"+data.length);
				if (data.length == 7){// 这里长度为7是已经确定的，且往后不会再生产，也不会有变数
					//此为MX-06设备
					byte[] macByte = new byte[6];
					System.arraycopy(data,0,macByte,0,6);
					String mac = bytesToHexMac(macByte);
					byte[] stateByte = new byte[1];
					System.arraycopy(data,6,stateByte,0,1);
					int state = stateByte[0]&0xff;

					// 获取后两个字节
					byte[] lastTwoBytes = new byte[2];
					System.arraycopy(macByte, 4, lastTwoBytes, 0, 2);
					String last4MacStr = Arrays.bytesToHexString(lastTwoBytes,"");

					ConnModel connModel = new ConnModel(device.getBluetoothDevice(),localName,mx06ConnTypes(),null,mac,null,state, DeviceDefinitionRegistry.getInstance().createMX06DeviceModel(last4MacStr));
					notifyConnModelDiscoveredDevice(connModel);
					return;
				}

				if(data.length >= 12){
					byte[] macByte = new byte[6];
					System.arraycopy(data,0,macByte,0,6);
					String mac = bytesToHexMac(macByte);
					byte[] stateByte = new byte[1];
					System.arraycopy(data,6,stateByte,0,1);
					int state = stateByte[0]&0xff;//0代表没配网，1代表已配网

					byte[] device_model_data = new byte[4];//设备类型inksi01
					System.arraycopy(data,7,device_model_data,0,4);
					String device_model_data_str = new String(device_model_data,StandardCharsets.UTF_8);

					RBQLog.i("device_model_data_str:"+device_model_data_str);

					byte[] apWifiData = new byte[1];//值1代表当前设备是ap模式，2代表是WiFi模式
					System.arraycopy(data,11,apWifiData,0,1);

					// 获取后两个字节
					byte[] lastTwoBytes = new byte[2];
					System.arraycopy(macByte, 4, lastTwoBytes, 0, 2);
					String last4MacStr = Arrays.bytesToHexString(lastTwoBytes,"");

					int apWifi = apWifiData[0] & 0xFF; // 将字节转换为无符号整数
					switch (apWifi) {
						case 1:
							apWifi = ConnType.AP.getValue();
							break;
						case 2:
							apWifi = ConnType.WiFi.getValue();
							break;
						default:
							apWifi = ConnType.AP.getValue() | ConnType.WiFi.getValue();
							break;
					}
					ConnModel connModel = new ConnModel(device.getBluetoothDevice(),mac,inksi01ConnTypes(apWifi),null,mac,null,state, DeviceDefinitionRegistry.getInstance().createNewDeviceModel(last4MacStr,device_model_data_str));
					notifyConnModelDiscoveredDevice(connModel);
				}
			}
		}

		@Override
		public void onStart(boolean startScanSuccess, String info) {
			RBQLog.i(TAG,"************onStart********");
			notifyConnModelStartDiscover();
		}

		@Override
		public void onFinish() {
			RBQLog.i(TAG,"************onFinish********");
			notifyConnModelStopDiscover();
		}
	};

	/**
	 * Discover dist net device.
	 */
	public synchronized void discoverDistNetDevice(){
		this.bleManager.startScan(distNetDeviceScanCallback);
	}

	/**
	 * Cancel discover dist net device.
	 */
	public synchronized void cancelDiscoverDistNetDevice(){
		this.bleManager.stopScan();
	}

	private final BleScanCallback distNetDeviceScanCallback = new BleScanCallback() {
		@Override
		public void onLeScan(BleDevice device, int rssi, byte[] scanRecord) {

			if (scanRecord == null || scanRecord.length == 0) {
				return;
			}
			Beacon beacon = new Beacon(scanRecord);
			String name = device.getName();
			String localName = null;
			BeaconItem localNameBeaconItem = getLocalName(beacon);
			if (localNameBeaconItem != null && localNameBeaconItem.bytes != null && localNameBeaconItem.bytes.length > 0){
				localName = new String(localNameBeaconItem.bytes);
			}
			if(isSupperDeviceByName(name)){

				BeaconItem beaconItem = getManufacturer(beacon);
				if (beaconItem==null || beaconItem.bytes == null || beaconItem.bytes.length == 0){
					return;
				}
				byte[] data = beaconItem.bytes;
				if (data.length<7){
					return;
				}
				if (data.length == 7){

					byte[] macByte = new byte[6];
					System.arraycopy(data,0,macByte,0,6);
					String mac = bytesToHexMac(macByte);
					byte[] stateByte = new byte[1];
					System.arraycopy(data,6,stateByte,0,1);
					int state = stateByte[0]&0xff;

					// 获取后两个字节
					byte[] lastTwoBytes = new byte[2];
					System.arraycopy(macByte, 4, lastTwoBytes, 0, 2);
					String last4MacStr = Arrays.bytesToHexString(lastTwoBytes,"");

					DistNetDevice distNetDevice = new DistNetDevice(device.getBluetoothDevice(),localName,mac,state,mx06ConnTypes(),null, DeviceDefinitionRegistry.getInstance().createMX06DeviceModel(last4MacStr));
					notifyDistNetDeviceDiscover(distNetDevice);
					return;
				}

				if (data.length >= 12){
					byte[] macByte = new byte[6];
					System.arraycopy(data,0,macByte,0,6);
					String mac = bytesToHexMac(macByte);
					byte[] stateByte = new byte[1];
					System.arraycopy(data,6,stateByte,0,1);
					int state = stateByte[0]&0xff;//0代表没配网，1代表已配网

					byte[] device_model_data = new byte[4];//设备类型inksi01
					System.arraycopy(data,7,device_model_data,0,4);
					String device_model_data_str = new String(device_model_data,StandardCharsets.UTF_8);

					byte[] apWifiData = new byte[1];//值1代表当前设备是ap模式，2代表是WiFi模式
					System.arraycopy(data,11,apWifiData,0,1);

					// 获取后两个字节
					byte[] lastTwoBytes = new byte[2];
					System.arraycopy(macByte, 4, lastTwoBytes, 0, 2);
					String last4MacStr = Arrays.bytesToHexString(lastTwoBytes,"");

					int apWifi = apWifiData[0] & 0xFF; // 将字节转换为无符号整数
					switch (apWifi) {
						case 1:
							apWifi = ConnType.AP.getValue();
							break;
						case 2:
							apWifi = ConnType.WiFi.getValue();
							break;
						default:
							apWifi = ConnType.AP.getValue() | ConnType.WiFi.getValue();
							break;
					}
					DistNetDevice distNetDevice = new DistNetDevice(device.getBluetoothDevice(),localName,mac,state,inksi01ConnTypes(apWifi),null, DeviceDefinitionRegistry.getInstance().createNewDeviceModel(last4MacStr,device_model_data_str));
					notifyDistNetDeviceDiscover(distNetDevice);
				}
			}
		}

		@Override
		public void onStart(boolean startScanSuccess, String info) {
			notifyDistNetDeviceDiscoverStart();
		}

		@Override
		public void onFinish() {
			notifyDistNetDeviceDiscoverCancel();
		}
	};

	/**
	 * Distribution network.
	 *
	 * @param distNetDevice the dist net device
	 * @param ssid          the ssid
	 * @param password      the password
	 * @param timeoutValue  the timeout value
	 */
	public synchronized void distributionNetwork(DistNetDevice distNetDevice, String ssid, String password, float timeoutValue){
		this.distNetDevice = distNetDevice;
		this.ssid = ssid;
		this.password = password;

		if (isConnected()){
			disconnect();
		}

		//通知配网开始
		notifyDistributionNetworkStart();
		this.bleManager.connect(distNetDevice,bleConnectCallback);
		//开始计算配网超时
		long timeout = (long) (defaultTimeoutValue*1000);
		if (timeoutValue>0){
			timeout = (long) (timeoutValue*1000);
		}
		mainHandler.postDelayed(timeoutRun, timeout);
	}

	private final Runnable timeoutRun = new Runnable() {
		@Override
		public void run() {
			if (distNetDevice !=null){
				bleManager.disconnect(distNetDevice);
			}
			distributionNetworkUdpServiceThread.stopMonitorUdp();
			//重新初始化
			initBleManager();
			//配网超时
			notifyDistributionNetworkTimeOut();
		}
	};

	private final BleConnectCallback bleConnectCallback =  new BleConnectCallback() {
		@Override
		public void onStart(boolean startConnectSuccess, String info, BleDevice device) {

		}
		@Override
		public void onFailure(int failCode, String info, BleDevice device) {
			RBQLog.i("ble连接失败");
			mainHandler.removeCallbacks(timeoutRun);
			//配网失败
			initBleManager();
			//配网失败
			notifyDistributionNetworkFail();
		}
		@Override
		public void onConnected(BleDevice device) {
			RBQLog.i("ble连接成功");

			List<ServiceInfo> serviceInfos = bleManager.getDeviceServices(device);
			for (ServiceInfo serviceInfo : serviceInfos){
				RBQLog.i("serviceInfo:"+serviceInfo.getUuid().toString());
				for (CharacteristicInfo characteristicInfo : serviceInfo.getCharacteristics()){
					RBQLog.i("characteristicInfo:"+characteristicInfo.getUuid().toString());
				}
			}

			mainHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					//打开设备的通知
					bleManager.notify(device, serviceUuid, notifyUuid, bleNotifyCallback);
				}
			},500);
		}
		@Override
		public void onDisconnected(String info, int status, BleDevice device) {
			RBQLog.i("ble连接断开");
		}
	};

	private final BleNotifyCallback bleNotifyCallback = new BleNotifyCallback() {
		@Override
		public void onCharacteristicChanged(byte[] data, BleDevice device) {
			String str = new String(data);
			RBQLog.i("onCharacteristicChanged:"+str);
		}
		@Override
		public void onNotifySuccess(String notifySuccessUuid, BleDevice device) {

			RBQLog.i("通知已打开，申请mtu");
			mainHandler.postDelayed(new Runnable() {
				@Override
				public void run() {

					//申请mtu
					bleManager.setMtu(device,256,bleMtuCallback);

					mainHandler.postDelayed(() -> {

						JSONObject jsonObject = new JSONObject();
						try {
							jsonObject.put("SSID",ssid);
							jsonObject.put("PASSWORD",password);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						String jsonSrt = jsonObject.toString();
						RBQLog.i("写入jsonSrt:"+jsonSrt);
						byte[] data = jsonSrt.getBytes();
						String _data = Arrays.bytesToHexString(data," ");
						RBQLog.i("发送的配网16进制数据:"+_data);
						bleManager.write(device, serviceUuid, writeUuid, data,bleWriteCallback);

						//写入成功后，1秒后启动Udp监听，  这里需要注意的是，写入成功，在当前环境，安卓6无法执行
						discoverUdpServiceThread.stopMonitorUdp();
						mainHandler.postDelayed(new Runnable() {
							@Override
							public void run() {
								//监听前主动调用断开
								bleManager.disconnect(device);
								RBQLog.i("开始监听配网udp广播");
								distributionNetworkUdpServiceThread.startMonitorUdp();
							}
						},500);

					},500);

				}
			},500);

		}
		@Override
		public void onFailure(int failCode, String info, BleDevice device) {
			mainHandler.removeCallbacks(timeoutRun);
			//将连接断开
			initBleManager();
			//配网失败
			notifyDistributionNetworkFail();
		}
	};
	/*android 6上没调用，不知道为何*/
	private final BleMtuCallback bleMtuCallback = new BleMtuCallback() {
		@Override
		public void onMtuChanged(int mtu, BleDevice device) {
			RBQLog.i("mtu申请成功 mtu:"+mtu);
		}

		@Override
		public void onFailure(int failCode, String info, BleDevice device) {
			RBQLog.i("mtu申请失败");
		}
	};

	private final BleWriteCallback bleWriteCallback = new BleWriteCallback() {
		@Override
		public void onWriteSuccess(byte[] data, BleDevice device) {
			String str = new String(data);
			RBQLog.i("bleWriteCallback:"+str);
		}

		@Override
		public void onFailure(int failCode, String info, BleDevice device) {
			mainHandler.removeCallbacks(timeoutRun);
			//将连接断开
			initBleManager();
			//配网失败
			notifyDistributionNetworkFail();
		}
	};

	private final UdpServiceThread.OnUpdMonitorListener onUpdMonitorListener = new UdpServiceThread.OnUpdMonitorListener() {
		@Override
		public void onUpdMonitorStart() {

		}
		@Override
		public void onUdpReceive(byte[] data) {
			synchronized (SPP_MANAGER){
				try {

					String str = new String(data);

					RBQLog.i("接收到数据："+ str);

					JSONObject jsonObject = new JSONObject(str);

					String name = jsonObject.getString("NAME");
					String ip = jsonObject.getString("IP");
					int port = jsonObject.getInt("POAT");
					String mac = restoreBluetoothAddress(jsonObject.getString("MAC"));

					if(!TextUtils.isEmpty(mac) && distNetDevice!=null && !TextUtils.isEmpty(distNetDevice.getMac()) && mac.equals(distNetDevice.getMac())){
						runOnUiThread(() -> {
							RBQLog.i("配网成功");
							mainHandler.removeCallbacks(timeoutRun);
							distributionNetworkUdpServiceThread.stopMonitorUdp();
							initBleManager();

							Device device = Device.createWifiDevice(name,ip,port,distNetDevice.getBluetoothDevice(),distNetDevice.getMac(),distNetDevice.getConnTypes(),distNetDevice.getDeviceModel());
							//配网完成
							notifyDistributionNetworkSucceed(device);
						});
					}

				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		@Override
		public void onUpdMonitorStop() {

		}

		@Override
		public void onUdpError(Exception e) {

		}
	};

	/**
	 * Discover wifi device.
	 *
	 * @param scanTime the scan time
	 */
	public void discoverWifiDevice(float scanTime){
		if (discoverUdpServiceThread.isStart()){
			discoverUdpServiceThread.stopMonitorUdp();
		}
		distributionNetworkUdpServiceThread.stopMonitorUdp();
		mainHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				discoverUdpServiceThread.startMonitorUdp();
				if (scanTime>0){
					int time = (int) (scanTime*1000.0f);
					mainHandler.postDelayed(wifiRunnable,time);
				}
			}
		},50);
	}

	private final Runnable wifiRunnable = new Runnable() {
		@Override
		public void run() {
			cancelDiscoverWifiDevice();
		}
	};

	/**
	 * Cancel discover wifi device.
	 */
	public void cancelDiscoverWifiDevice(){
		mainHandler.removeCallbacks(wifiRunnable);
		discoverUdpServiceThread.stopMonitorUdp();
	}

	private final UdpServiceThread.OnUpdMonitorListener discoverDeviceUpdMonitorListener = new UdpServiceThread.OnUpdMonitorListener() {
		@Override
		public void onUpdMonitorStart() {
			RBQLog.i(" discoverDeviceUpdMonitorListener 开始监听udp");
			notifyStartDiscover();
		}

		@Override
		public void onUdpReceive(byte[] data) {
			synchronized (SPP_MANAGER){

				try {
					String str = new String(data);
					JSONObject jsonObject = new JSONObject(str);
					String name = jsonObject.getString("NAME");
					String ip = jsonObject.getString("IP");
					int port = jsonObject.getInt("POAT");
					String mac = restoreBluetoothAddress(jsonObject.getString("MAC"));
					Device device = Device.createWifiDevice(name,ip,port,null,mac,0,null);
					RBQLog.i("发现设备 udp name:"+name+"; ip:"+ip+"; port:"+port+"; mac:"+mac);
					notifyDiscoveredDevice(device);

				}catch (Exception e){
					e.printStackTrace();
				}

			}
		}
		@Override
		public void onUpdMonitorStop() {
			RBQLog.i(" discoverDeviceUpdMonitorListener 停止监听udp");
			notifyStopDiscover();
		}

		@Override
		public void onUdpError(Exception e) {

		}
	};

	/**
	 * The interface On dist net device discover listener.
	 */
	public interface OnDistNetDeviceDiscoverListener {
		/**
		 * On dist net device discover start.
		 */
		void onDistNetDeviceDiscoverStart();

		/**
		 * On dist net device discover.
		 *
		 * @param device the device
		 */
		void onDistNetDeviceDiscover(DistNetDevice device);

		/**
		 * On dist net device discover cancel.
		 */
		void onDistNetDeviceDiscoverCancel();
	}

	/**
	 * The interface On distribution network listener.
	 */
	public interface OnDistributionNetworkListener {
		/**
		 * On distribution network start.
		 */
		void onDistributionNetworkStart();

		/**
		 * On distribution network succeed.
		 *
		 * @param device the device
		 */
		void onDistributionNetworkSucceed(Device device);

		/**
		 * On distribution network fail.
		 */
		void onDistributionNetworkFail();

		/**
		 * On distribution network time out.
		 */
		void onDistributionNetworkTimeOut();//配网超时
	}

	/**
	 * 这个接口用来监听打印的开始和完成，例如：发送了3拼数据，当开始打印1拼的时候，会回调onPrintStart，
	 * 此时参数beginIndex为0，endIndex为2，currentIndex为0，当打印完成的时候，
	 * 会回调onPrintComplete，此时参数beginIndex为0，endIndex为2，currentIndex为0，以此类推。
	 */
	public interface OnPrintListener {
		/**
		 * On print start.
		 *
		 * @param beginIndex   开始索引
		 * @param endIndex     结束索引
		 * @param currentIndex 当前打印索引
		 */
		void onPrintStart(int beginIndex,int endIndex,int currentIndex);

		/**
		 * On print complete.
		 *
		 * @param beginIndex   开始索引
		 * @param endIndex     结束索引
		 * @param currentIndex 当前打印完成索引
		 * @param cartridgeId 墨盒id
		 */
		void onPrintComplete(int beginIndex,int endIndex,int currentIndex,String cartridgeId);
	}

	///////////////////////////////////////////////////////////////////
	//                   串口相关 begin
	///////////////////////////////////////////////////////////////////
	private final SerialPortFinder serialPortFinder = new SerialPortFinder();
	private final AbsStickPackageHelper mStickPackageHelper = new BaseStickPackageHelper();
	private final SerialThread serialThread = new SerialThread();

	/**
	 * 获取串口设备
	 * @return
	 */
	public List<Device> getSerialDevices(){
		String[] sports = serialPortFinder.getAllDevicesPath();
		List<Device> devices = new ArrayList<Device>();
		for (String sport:sports){
			Device device = Device.createSerialDevice(sport);
			devices.add(device);
		}
		return devices;
	}

	@Override
	public void onSerialOpenStart() {
		notifyDeviceConnectStart(this.device);
	}

	@Override
	public void onSerialOpenSuccess(InputStream inputStream, OutputStream outputStream) {
		//启动数据读取线程
		readThread.start(inputStream,mStickPackageHelper);
		//启动数据写入线程
		writeThread.start(outputStream);

		//先启用计时
		serialConnHandler.postDelayed(serialConnectTimeRunnable,3000);
		//发送连接状态指令，如果得到响应，则认为连接成功，否则认为连接失败，关闭串口
		writeConnectStateConnected();

	}

	@Override
	public void onSerialClose() {
		notifyDeviceDisconnect(this.device);
	}

	@Override
	public void onSerialOpenFail(String msg) {

		serialConnHandler.removeCallbacks(serialConnectTimeRunnable);
		serialConnHandler.removeCallbacksAndMessages(null);

		cancelConning();
		isOpenHeartbeat = false;
		notifyDeviceConnectFail(this.device,"串口打开失败");
	}

	private final Runnable serialConnectTimeRunnable = new Runnable() {
		@Override
		public void run() {
			disconnectSerial();
			notifyDeviceConnectFail(device,"连接失败");
		}
	};

	///////////////////////////////////////////////////////////////////
	//                   串口相关 end
	///////////////////////////////////////////////////////////////////

}
