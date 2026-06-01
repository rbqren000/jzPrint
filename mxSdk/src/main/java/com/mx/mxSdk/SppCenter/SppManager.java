//package com.mx.mxSdk.SppCenter;
//
//import android.app.Application;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothSocket;
//import android.os.Handler;
//import android.os.Looper;
//import com.belon.printer.core.Device;
//import com.belon.printer.core.packet.MultiRowImageDataPacket;
//import com.belon.printer.core.packet.OtaPacket;
//import com.belon.printer.core.SppLogoPacket;
//import com.belon.printer.core.TransportProtocol;
//import com.belon.printer.ui.preview.MultiRowImageData;
//import com.belon.printer.utils.Arrays;
//import com.belon.printer.core.CRC16;
//import com.belon.printer.utils.RBQLog;
//import com.belon.printer.core.wifiCenter.TcpClientThread;
//import org.json.JSONObject;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.net.Socket;
//import java.nio.charset.StandardCharsets;
//import java.security.SecureRandom;
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.Queue;
//import java.util.Random;
//
//import androidx.annotation.NonNull;
//
///**
// * Created by rbq on 2017/5/17.
// */
//public class SppManager implements BluetoothDiscoverUtils.OnBluetoothDeviceDiscoverListener, ConnectA2dpThread.OnA2dpConnListener, BluetoothSocketThread.OnSocketConnListener, ReadThread.OnReadDataListener, TcpClientThread.OnSocketConnListener {
//	
//	private static final String TAG2 = SppManager.class.getSimpleName();
//	private static final int MAX_LOSE_HEART_TIMES = 3;
//	private final Handler mainHandler = new Handler(Looper.getMainLooper());
//	
//	private void runOnUiThread(Runnable runnable) {
//		if (isAndroidMainThread()) {
//			runnable.run();
//		} else {
//			mainHandler.post(runnable);
//		}
//	}
//	
//	private static boolean isAndroidMainThread() {
//		return Looper.myLooper() == Looper.getMainLooper();
//	}
//	
//	private static final SppManager SPP_MANAGER = new SppManager();
//	
//	//普通指令和XModem指令都用这个
//	private static final int SPP_TAG_NORMAL_COMMAND = 1000;
//	
//	//使用到的线程工具类
//	private final PairThread pairThread = new PairThread();
//	private final ConnectA2dpThread connectA2DpThread = new ConnectA2dpThread();
//	private final BluetoothSocketThread bluetoothSocketThread = new BluetoothSocketThread();//经典蓝牙的socket
//	private final ReadThread readThread = new ReadThread();
//	private final WriteThread writeThread = new WriteThread();
//	private final TcpClientThread tcpClientThread = new TcpClientThread();//wifi的socket
//	
//	private final Queue<Command> commandQueue = new LinkedList<Command>();
//	private static final long commandInterval = 600;//0.6秒
//	private long lastSendCommandTime = 0;
//	private final Handler commandHandler = new Handler(Looper.getMainLooper());
//	
//	//接收json数据
//	private volatile boolean receivingJsonData = false;
//	private final StringBuilder jsonStringBuilder = new StringBuilder();
//	
//	private void deleteJsonStringBuilder(StringBuilder stringBuilder) {
//		stringBuilder.delete(0, stringBuilder.length());
//		stringBuilder.setLength(0);
//	}
//	
//	private static Application application;
//	
//	private int N_Index = 0;
//	
//	private final MultiRowImageDataPacket multiRowImageDataPacket = new MultiRowImageDataPacket();
//	private final OtaPacket otaPacket = new OtaPacket();
//	private final SppLogoPacket sppLogoPacket = new SppLogoPacket();
//	
//	//下面使用到的工具类
//	private BluetoothDiscoverUtils bluetoothDiscoverUtils;
//	
//	private boolean isStart = false;//用来标志整个流程是否正在进行
//	
//	private Device device;
//	
//	private static final int tryTime = 5;//默认的尝试连接次数
//	
//	private BluetoothSocket bluetoothSocket;
//	private InputStream inputStream;
//	private OutputStream outputStream;
//
//	private Socket wifiSocket;
//	private int heartLooseTimes;
//	
//	private int sequenceNumber = 1;
//	private final Random random = new SecureRandom();
//
//	private final ArrayList<OnSppBluetoothStateListener> onSppBluetoothStateListeners = new ArrayList<OnSppBluetoothStateListener>();
//	
//	public void registerSppBluetoothStateListener(OnSppBluetoothStateListener onSppBluetoothStateListener) {
//		if (!onSppBluetoothStateListeners.contains(onSppBluetoothStateListener)) {
//			onSppBluetoothStateListeners.add(onSppBluetoothStateListener);
//		}
//	}
//	
//	public void unregisterSppBluetoothStateListener(OnSppBluetoothStateListener onSppBluetoothStateListener) {
//		onSppBluetoothStateListeners.remove(onSppBluetoothStateListener);
//	}
//	
//	public void notifySppOpeningBlueToothListener() {
//		for (OnSppBluetoothStateListener onSppBluetoothStateListener : onSppBluetoothStateListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppBluetoothStateListener.onSppOpeningBlueToothListener();
//				}
//			});
//		}
//	}
//	
//	public void notifySppOpenedBlueToothListener() {
//		for (OnSppBluetoothStateListener onSppBluetoothStateListener : onSppBluetoothStateListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppBluetoothStateListener.onSppOpenedBlueToothListener();
//				}
//			});
//		}
//	}
//	
//	public void notifySppClosingBlueToothListener() {
//		for (OnSppBluetoothStateListener onSppBluetoothStateListener : onSppBluetoothStateListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppBluetoothStateListener.onSppClosingBlueToothListener();
//				}
//			});
//		}
//	}
//	
//	public void notifySppClosedBlueToothListener() {
//		for (OnSppBluetoothStateListener onSppBluetoothStateListener : onSppBluetoothStateListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppBluetoothStateListener.onSppClosedBlueToothListener();
//				}
//			});
//		}
//	}
//	
//	private final ArrayList<OnSppBluetoothDeviceDiscoverListener> onSppBluetoothDeviceDiscoverListeners = new ArrayList<OnSppBluetoothDeviceDiscoverListener>();
//	
//	public void registerSppDiscoverBluetoothDeviceListener(OnSppBluetoothDeviceDiscoverListener onSppBluetoothDeviceDiscoverListener) {
//		if (!onSppBluetoothDeviceDiscoverListeners.contains(onSppBluetoothDeviceDiscoverListener)) {
//			onSppBluetoothDeviceDiscoverListeners.add(onSppBluetoothDeviceDiscoverListener);
//		}
//	}
//	
//	public void unregisterSppDiscoverBluetoothDeviceListener(OnSppBluetoothDeviceDiscoverListener onSppBluetoothDeviceDiscoverListener) {
//		onSppBluetoothDeviceDiscoverListeners.remove(onSppBluetoothDeviceDiscoverListener);
//	}
//	
//	public void notifySppStartDiscovering() {
//		for (OnSppBluetoothDeviceDiscoverListener onSppBluetoothDeviceDiscoverListener : onSppBluetoothDeviceDiscoverListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppBluetoothDeviceDiscoverListener.onSppStartDiscovering();
//				}
//			});
//		}
//	}
//	
//	public void notifySppStopDiscovering() {
//		for (OnSppBluetoothDeviceDiscoverListener onSppBluetoothDeviceDiscoverListener : onSppBluetoothDeviceDiscoverListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppBluetoothDeviceDiscoverListener.onSppStopDiscovering();
//				}
//			});
//		}
//	}
//	
//	public void notifySppDiscovered(BluetoothDevice device) {
//		for (OnSppBluetoothDeviceDiscoverListener onSppBluetoothDeviceDiscoverListener : onSppBluetoothDeviceDiscoverListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppBluetoothDeviceDiscoverListener.onSppDiscovered(device);
//				}
//			});
//		}
//	}
//	
//	private final ArrayList<OnWifiConnectListener> onWifiConnectListeners = new ArrayList<OnWifiConnectListener>();
//	
//	public void registerWifiConnectListener(OnWifiConnectListener onWifiConnectListener) {
//		if (!onWifiConnectListeners.contains(onWifiConnectListener)) {
//			onWifiConnectListeners.add(onWifiConnectListener);
//		}
//	}
//	
//	public void unregisterWifiConnectListener(OnWifiConnectListener onWifiConnectListener) {
//		onWifiConnectListeners.remove(onWifiConnectListener);
//	}
//	
//	public void notifyWifiOnConnecting(Device device) {
//		
//		for (OnWifiConnectListener onWifiConnectListener : onWifiConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onWifiConnectListener.onConnecting(device);
//				}
//			});
//		}
//	}
//	
//	public void notifyWifiOnConnect(Device device) {
//		
//		for (OnWifiConnectListener onWifiConnectListener : onWifiConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onWifiConnectListener.onConnect(device);
//				}
//			});
//		}
//	}
//	
//	public void notifyWifiOnConnectFailed(Device device, String msg) {
//		
//		for (OnWifiConnectListener onWifiConnectListener : onWifiConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onWifiConnectListener.onConnectFailed(device, msg);
//				}
//			});
//		}
//	}
//	
//	private final ArrayList<OnSppDeviceConnectListener> onSppDeviceConnectListeners = new ArrayList<OnSppDeviceConnectListener>();
//	
//	public void registerSppConnectListener(OnSppDeviceConnectListener onSppDeviceConnectListener) {
//		if (!onSppDeviceConnectListeners.contains(onSppDeviceConnectListener)) {
//			onSppDeviceConnectListeners.add(onSppDeviceConnectListener);
//		}
//	}
//	
//	public void unregisterSppConnectListener(OnSppDeviceConnectListener onSppDeviceConnectListener) {
//		onSppDeviceConnectListeners.remove(onSppDeviceConnectListener);
//	}
//	
//	public void notifySppBonding(Device device) {
//		
//		for (OnSppDeviceConnectListener onSppDeviceConnectListener : onSppDeviceConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppDeviceConnectListener.onSppBonding(device);
//				}
//			});
//		}
//	}
//	
//	public void notifySppBonded(Device device) {
//		
//		for (OnSppDeviceConnectListener onSppDeviceConnectListener : onSppDeviceConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppDeviceConnectListener.onSppBonded(device);
//				}
//			});
//		}
//	}
//	
//	public void notifySppDisBond(Device device) {
//		
//		for (OnSppDeviceConnectListener onSppDeviceConnectListener : onSppDeviceConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppDeviceConnectListener.onSppDisBond(device);
//				}
//			});
//		}
//	}
//	
//	public void notifySppA2dpConnStart(Device device) {
//		
//		for (OnSppDeviceConnectListener onSppDeviceConnectListener : onSppDeviceConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppDeviceConnectListener.onSppA2dpConnStart(device);
//				}
//			});
//		}
//	}
//	
//	public void notifySppA2dpConnSucceed(Device device) {
//		
//		for (OnSppDeviceConnectListener onSppDeviceConnectListener : onSppDeviceConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppDeviceConnectListener.onSppA2dpConnSucceed(device);
//				}
//			});
//		}
//	}
//	
//	public void notifySppA2dpConnTimeout(Device device) {
//		
//		for (OnSppDeviceConnectListener onSppDeviceConnectListener : onSppDeviceConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppDeviceConnectListener.onSppA2dpConnTimeout(device);
//				}
//			});
//		}
//	}
//	
//	public void notifySppSocketConnStart(Device device) {
//		
//		for (OnSppDeviceConnectListener onSppDeviceConnectListener : onSppDeviceConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppDeviceConnectListener.onSppSocketConnStart(device);
//				}
//			});
//		}
//	}
//	
//	public void notifySppSocketConnSucceed(Device device) {
//		
//		for (OnSppDeviceConnectListener onSppDeviceConnectListener : onSppDeviceConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppDeviceConnectListener.onSppSocketConnSucceed(device);
//				}
//			});
//		}
//	}
//	
//	public void notifySppSocketConnTimeout(Device device) {
//		
//		for (OnSppDeviceConnectListener onSppDeviceConnectListener : onSppDeviceConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppDeviceConnectListener.onSppSocketConnTimeout(device);
//				}
//			});
//		}
//	}
//	
//	public void notifySppSocketDisconnect(Device device) {
//		
//		for (OnSppDeviceConnectListener onSppDeviceConnectListener : onSppDeviceConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppDeviceConnectListener.onSppSocketDisconnect(device);
//				}
//			});
//		}
//	}
//	
//	public void notifySppConnError(Device device, String error) {
//		
//		for (OnSppDeviceConnectListener onSppDeviceConnectListener : onSppDeviceConnectListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppDeviceConnectListener.onSppConnError(device, error);
//				}
//			});
//		}
//	}
//	
//	
//	//******************************接收到数据分发出去******************************
//	
//	private final ArrayList<OnReceiveMessageListener> onReceiveMessageListeners = new ArrayList<OnReceiveMessageListener>();
//	
//	public void registerReceiveMessageListener(OnReceiveMessageListener onReceiveMessageListener) {
//		if (!onReceiveMessageListeners.contains(onReceiveMessageListener)) {
//			onReceiveMessageListeners.add(onReceiveMessageListener);
//		}
//	}
//	
//	public void unregisterReceiveMessageListener(OnReceiveMessageListener onReceiveMessageListener) {
//		onReceiveMessageListeners.remove(onReceiveMessageListener);
//	}
//	
//	public void notifyParameterChange(Device device, int headValue, int l_pix, int p_pix, int distance) {
//		for (OnReceiveMessageListener onReceiveMessageListener : onReceiveMessageListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onReceiveMessageListener.onParameterChange(device, headValue, l_pix, p_pix, distance);
//				}
//			});
//		}
//	}
//	
//	public void notifyCirculationAndRepeatTimeChange(Device device, int circulation_time, int repeat_time) {
//		for (OnReceiveMessageListener onReceiveMessageListener : onReceiveMessageListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onReceiveMessageListener.onCirculationAndRepeatTimeChange(device, circulation_time, repeat_time);
//				}
//			});
//		}
//	}
//	
//	public void notifyDirectionChange(Device device, int direction, int printHeadDirection) {
//		for (OnReceiveMessageListener onReceiveMessageListener : onReceiveMessageListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onReceiveMessageListener.onDirectionChange(device, direction, printHeadDirection);
//				}
//			});
//		}
//	}
//	
//	public void notifyReadDeviceInfo(Device device, String id, String name, String mcu_version, String mcu_date) {
//		for (OnReceiveMessageListener onReceiveMessageListener : onReceiveMessageListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onReceiveMessageListener.onReadDeviceInfo(device, id, name, mcu_version, mcu_date);
//				}
//			});
//		}
//	}
//	
//	public void notifyTemperature(Device device, int temp) {
//		for (OnReceiveMessageListener onReceiveMessageListener : onReceiveMessageListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onReceiveMessageListener.onReadTemperature(device, temp);
//				}
//			});
//		}
//	}
//	
//	public void notifyReadBattery(Device device, int bat) {
//		for (OnReceiveMessageListener onReceiveMessageListener : onReceiveMessageListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onReceiveMessageListener.onReadBattery(device, bat);
//				}
//			});
//		}
//	}
//	
//	public void notifyError(Device device, String error) {
//		for (OnReceiveMessageListener onReceiveMessageListener : onReceiveMessageListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onReceiveMessageListener.onError(device, error);
//				}
//			});
//		}
//	}
//	
//	private final ArrayList<OnSppImageProgressListener> onSppImageProgressListeners = new ArrayList<OnSppImageProgressListener>();
//	
//	public void registerSppImageProgressListener(OnSppImageProgressListener onSppImageProgressListener) {
//		
//		if (!onSppImageProgressListeners.contains(onSppImageProgressListener)) {
//			onSppImageProgressListeners.add(onSppImageProgressListener);
//		}
//	}
//	
//	public void unregisterSppImageProgressListener(OnSppImageProgressListener onSppImageProgressListener) {
//		
//		this.onSppImageProgressListeners.remove(onSppImageProgressListener);
//	}
//	
//	public void notifySppImageStartListener(float size, int progress, long startTime) {
//		for (OnSppImageProgressListener onSppImageProgressListener : onSppImageProgressListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppImageProgressListener.onSppImageStartListener(size, progress, startTime);
//				}
//			});
//		}
//	}
//	
//	public void notifySppImageProgress(float size, int progress, long startTime, long currentTime) {
//		for (OnSppImageProgressListener onSppImageProgressListener : onSppImageProgressListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppImageProgressListener.onSppImageProgress(size, progress, startTime, currentTime);
//				}
//			});
//		}
//	}
//	
//	public void notifySppImageFinishListener(float size, int progress, long startTime, long currentTime) {
//		for (OnSppImageProgressListener onSppImageProgressListener : onSppImageProgressListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppImageProgressListener.onSppImageFinishListener(size, progress, startTime, currentTime);
//				}
//			});
//		}
//	}
//	
//	public void notifySppImageErrorListener(String error) {
//		for (OnSppImageProgressListener onSppImageProgressListener : onSppImageProgressListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppImageProgressListener.onSppImageError(error);
//				}
//			});
//		}
//	}
//	
//	private final ArrayList<OnSppOTAProgressListener> onSppOTAProgressListeners = new ArrayList<OnSppOTAProgressListener>();
//	
//	public void registerSppOTAProgressListener(OnSppOTAProgressListener onSppOTAProgressListener) {
//		
//		if (!onSppOTAProgressListeners.contains(onSppOTAProgressListener)) {
//			onSppOTAProgressListeners.add(onSppOTAProgressListener);
//		}
//	}
//	
//	public void unregisterSppOTAProgressListener(OnSppOTAProgressListener onSppOTAProgressListener) {
//		
//		this.onSppOTAProgressListeners.remove(onSppOTAProgressListener);
//	}
//	
//	public void notifySppOTAStartListener(float size, int progress, long startTime) {
//		for (OnSppOTAProgressListener onSppOTAProgressListener : onSppOTAProgressListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppOTAProgressListener.onSppOTAStartListener(size, progress, startTime);
//				}
//			});
//		}
//	}
//	
//	public void notifySppOTAProgress(float size, int progress, long startTime, long currentTime) {
//		for (OnSppOTAProgressListener onSppOTAProgressListener : onSppOTAProgressListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppOTAProgressListener.onSppOTAProgress(size, progress, startTime, currentTime);
//				}
//			});
//		}
//	}
//	
//	public void notifySppOTAFinishListener(float size, int progress, long startTime, long currentTime) {
//		for (OnSppOTAProgressListener onSppOTAProgressListener : onSppOTAProgressListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppOTAProgressListener.onSppOTAFinishListener(size, progress, startTime, currentTime);
//				}
//			});
//		}
//	}
//	
//	public void notifySppOTAErrorListener(String error) {
//		for (OnSppOTAProgressListener onSppOTAProgressListener : onSppOTAProgressListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppOTAProgressListener.onSppOTAError(error);
//				}
//			});
//		}
//	}
//	
//	private final ArrayList<OnSppLogoProgressListener> onSppLogoProgressListeners = new ArrayList<OnSppLogoProgressListener>();
//	
//	public void registerSppLogoProgressListener(OnSppLogoProgressListener onSppLogoProgressListener) {
//		
//		if (!onSppLogoProgressListeners.contains(onSppLogoProgressListener)) {
//			onSppLogoProgressListeners.add(onSppLogoProgressListener);
//		}
//	}
//	
//	public void unregisterSppLogoProgressListener(OnSppLogoProgressListener onSppLogoProgressListener) {
//		
//		this.onSppLogoProgressListeners.remove(onSppLogoProgressListener);
//	}
//	
//	public void notifySppLogoStartListener(float size, int progress, long startTime) {
//		for (OnSppLogoProgressListener onSppLogoProgressListener : onSppLogoProgressListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppLogoProgressListener.onSppLogoStartListener(size, progress, startTime);
//				}
//			});
//		}
//	}
//	
//	public void notifySppLogoProgress(float size, int progress, long startTime, long currentTime) {
//		for (OnSppLogoProgressListener onSppLogoProgressListener : onSppLogoProgressListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppLogoProgressListener.onSppLogoProgress(size, progress, startTime, currentTime);
//				}
//			});
//		}
//	}
//	
//	public void notifySppLogoFinishListener(float size, int progress, long startTime, long currentTime) {
//		for (OnSppLogoProgressListener onSppLogoProgressListener : onSppLogoProgressListeners) {
//			runOnUiThread(new Runnable() {
//				@Override
//				public void run() {
//					onSppLogoProgressListener.onSppLogoFinishListener(size, progress, startTime, currentTime);
//				}
//			});
//		}
//	}
//	
//	
//	public synchronized static SppManager share() {
//		
//		return SPP_MANAGER;
//	}
//	
//	/**
//	 * 初始化方法，要求在使用前先进行初始化
//	 *
//	 * @param _application 初始化
//	 */
//	public synchronized void init(@NonNull Application _application) {
//		//这里无论如何调用init方法，都只能初始化一次内部的值
//		if (application == null) {
//			
//			RBQLog.i("初始化SppManager");
//			
//			application = _application;
//			
//			this.bluetoothDiscoverUtils = new BluetoothDiscoverUtils(application);
//			
//			this.bluetoothDiscoverUtils.registerBluetoothBroadcastReceiver();
//			this.bluetoothDiscoverUtils.registerBluetoothDeviceDiscoverListener(this);
//			
//			this.connectA2DpThread.registerA2dpConnListener(this);
//			this.bluetoothSocketThread.registerSocketConnListener(this);
//			this.tcpClientThread.registerSocketConnListener(this);
//			this.readThread.registerReadDataListener(this);
//		}
//	}
//	
//	/**
//	 * 释放所有资源
//	 */
//	public void release() {
//		
//		if (bluetoothDiscoverUtils != null) {
//			this.bluetoothDiscoverUtils.unregisterBluetoothBroadcastReceiver();
//			this.bluetoothDiscoverUtils.unregisterBluetoothDeviceDiscoverListener();
//		}
//		this.connectA2DpThread.unregisterA2dpConnListener();
//		this.bluetoothSocketThread.unregisterSocketConnListener();
//		this.tcpClientThread.unregisterSocketConnListener();
//		this.readThread.unregisterReadDataListener();
//		
//		this.bluetoothSocketThread.cancel();
//		this.tcpClientThread.exeCancel();
//		this.connectA2DpThread.exeCancel();
//		this.pairThread.cancel();
//		
//		if (this.bluetoothSocket != null) {
//			try {
//				this.bluetoothSocket.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			this.bluetoothSocket = null;
//		}
//		if (this.inputStream != null) {
//			try {
//				this.inputStream.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			this.inputStream = null;
//		}
//		if (this.outputStream != null) {
//			try {
//				this.outputStream.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			this.outputStream = null;
//		}
//		
//	}
//	
//	//判断蓝牙是否打开
//	public boolean isEnable() {
//		
//		return BluetoothAdapterUtils.isEnabled();
//	}
//	
//	//打开蓝牙
//	public void enable() {
//		
//		if (isEnable()) {
//			return;
//		}
//		BluetoothAdapterUtils.enable();
//	}
//	
//	//是否在扫描
//	public Boolean isDiscovering() {
//		if (bluetoothDiscoverUtils == null) {
//			RBQLog.i("bluetoothDiscoverUtils 未初始化");
//			return false;
//		}
//		return bluetoothDiscoverUtils.isDiscovering();
//	}
//	
//	//开始扫描
//	public void startDiscovering() {
//		if (bluetoothDiscoverUtils == null) {
//			RBQLog.i("bluetoothDiscoverUtils 未初始化");
//			return;
//		}
//		if (bluetoothDiscoverUtils.isDiscovering() || !BluetoothAdapterUtils.isEnabled()) {
//			return;
//		}
//		bluetoothDiscoverUtils.startDiscovery();
//	}
//	
//	//停止扫描
//	public void cancelDiscovery() {
//		if (bluetoothDiscoverUtils == null) return;
//		if (!bluetoothDiscoverUtils.isDiscovering() || !BluetoothAdapterUtils.isEnabled()) {
//			return;
//		}
//		bluetoothDiscoverUtils.cancelDiscovery();
//	}
//	
//	//开始调用连接true，断开连接后 false
//	public boolean isStart() {
//		return isStart;
//	}
//	
//	public void connect(Device device) {
//		if (device == null) {
//			return;
//		}
//		if (device.isBle()) {
//			connectSpp(device);
//			return;
//		}
//		if (device.isWifi()){
//			connectWifi(device);
//		}
//	}
//	
//	//开始连接设备
//	private synchronized void connectSpp(Device device) {
//		if (isStart) {
//			return;
//		}
//		//这里标志接下来要进行配对、连接等操作
//		isStart = true;
//		this.device = device;
//		//说明设备正在配对、或者已经配对，则接下来的流程交给下面的响应事件进行流程传递，暂时不做处理
//		if (BluetoothUtils.isBonding(device.bluetoothDevice)) {
//			return;
//		}
//		if (BluetoothUtils.isBonded(device.bluetoothDevice)) {
//			//进行socket连接
//			bluetoothSocketThread.connect(device.bluetoothDevice, tryTime);
//		} else {//进行配对操作
//			pairThread.startPair(device.bluetoothDevice, tryTime);
//		}
//	}
//	
//	//连接wifi
//	private synchronized void connectWifi(Device device) {
//		if (isStart) {
//			return;
//		}
//		//这里标志接下来要进行配对、连接等操作
//		isStart = true;
//		this.device = device;
//		tcpClientThread.connect(device.ip,device.port);
//	}
//	
//	//断开连接
//	public void disconnect() {
//		if (device == null) return;
//		if (device.isBle()){
//			disconnectSpp();
//			return;
//		}
//		if (device.isWifi()){
//			disconnectWifi();
//		}
//	}
//	
//	private void disconnectWifi() {
//		isStart = false;
//		readThread.release();
//		writeThread.release();
//		
//		if (this.wifiSocket != null) {
//			try {
//				this.wifiSocket.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			this.wifiSocket = null;
//		}
//		if (this.inputStream != null) {
//			try {
//				this.inputStream.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			this.inputStream = null;
//		}
//		if (this.outputStream != null) {
//			try {
//				this.outputStream.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			this.outputStream = null;
//		}
//		
//		notifyWifiOnConnectFailed(this.device, null);
//	}
//	
//	private void disconnectSpp() {//这里后面完善
//		
//		isStart = false;
//		
//		readThread.release();
//		writeThread.release();
//		bluetoothSocketThread.release();
//		
//		//移除配对
//        /*
//        if (this.sppDevice!=null) {
//            BluetoothUtils.removeBond(this.sppDevice.device);
//        }
//        */
//		pairThread.release();
//		
//		if (this.bluetoothSocket != null) {
//			try {
//				this.bluetoothSocket.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			this.bluetoothSocket = null;
//		}
//		if (this.inputStream != null) {
//			try {
//				this.inputStream.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			this.inputStream = null;
//		}
//		if (this.outputStream != null) {
//			try {
//				this.outputStream.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			this.outputStream = null;
//		}
//		
//		notifySppSocketDisconnect(this.device);
//	}
//	
//	public Boolean isConnected() {
//		if (device == null) {
//			return false;
//		}
//		if (device.isBle()) {//蓝牙
//			if (bluetoothSocket == null) {
//				return false;
//			} else
//				return bluetoothSocket.isConnected();
//		}
//		if (device.isWifi()){
//			if (wifiSocket == null)
//				return false;
//			else return wifiSocket.isConnected();
//		}
//		return false;
//	}
//	
//	public Device getDevice() {
//		return this.device;
//	}
//	
//	public Boolean isConnectedDevice(Device device) {
//		if (!isConnected() || device == null) {
//			return false;
//		}
//		return device.equals(this.device);
//	}
//
//	//正在连接
//	public Boolean isConnectingDevice(BluetoothDevice bluetoothDevice) {
//		return isStart() && bluetoothDevice.equals(device.bluetoothDevice) && !isConnected();
//	}
//	
//	public Boolean isConnectingDevice(Device device) {
//		return isStart() && device.bluetoothDevice.equals(this.device.bluetoothDevice) && !isConnected();
//	}
//	
//	public Device getConnectedDevice() {
//		if (!isConnected()) {
//			return null;
//		}
//		return device;
//	}
//	
//	//*****************************蓝牙开关事件***************************
//	@Override
//	public void onOpeningBlueTooth() {
//		notifySppOpeningBlueToothListener();
//	}
//	
//	@Override
//	public void onOpenedBlueTooth() {
//		notifySppOpenedBlueToothListener();
//	}
//	
//	@Override
//	public void onClosingBlueTooth() {
//		notifySppClosingBlueToothListener();
//	}
//	
//	@Override
//	public void onClosedBlueTooth() {
//		notifySppClosedBlueToothListener();
//	}
//	
//	//*****************************蓝牙扫描***************************
//	@Override
//	public void onStartDiscovering() {
//		notifySppStartDiscovering();
//	}
//	
//	@Override
//	public void onStopDiscovering() {
//		notifySppStopDiscovering();
//	}
//	
//	@Override
//	public void onDiscovered(BluetoothDevice device) {
//		
//		//这里可增加过滤，只往缓存设备里面装符合要求的设备，后续完善
//		notifySppDiscovered(device);
//	}
//	
//	//*****************************蓝牙配对***************************
//	@Override
//	public void onBonding(BluetoothDevice bluetoothDevice) {
//		if(this.device == null)
//			return;
//		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
//			notifySppBonding(this.device);
//		}
//	}
//	
//	@Override
//	public void onBonded(BluetoothDevice bluetoothDevice) {
//		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
//			notifySppBonded(this.device);
//		}
//		//监听到配对完成的事件后，进行判断是否为当前设备，如果为当前设备则接下来进行socket连接，这里暂时不进行a2dp连接，a2dp暂时不使用
//		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
//			
//			bluetoothSocketThread.connect(bluetoothDevice, tryTime);
//		}
//	}
//	
//	@Override
//	public void onDisBond(BluetoothDevice bluetoothDevice) {
//		
//		isStart = false;//恢复isStart的值
//		if(this.device == null)
//			return;
//		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
//			notifySppDisBond(this.device);
//		}
//	}
//	
//	//*****************************a2dp***************************
//	@Override
//	public void onA2dpConnStart(BluetoothDevice bluetoothDevice) {
//		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
//			notifySppA2dpConnStart(this.device);
//		}
//	}
//	
//	@Override
//	public void onA2dpConnSucceed(BluetoothDevice bluetoothDevice) {
//		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
//			notifySppA2dpConnSucceed(this.device);
//		}
//	}
//	
//	@Override
//	public void onA2dpConnTimeout(BluetoothDevice bluetoothDevice) {
//		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
//			notifySppA2dpConnTimeout(this.device);
//		}
//	}
//	
//	//*****************************socket连接***************************
//	@Override
//	public void onSocketConnStart(BluetoothDevice bluetoothDevice) {
//		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
//			notifySppSocketConnStart(this.device);
//		}
//	}
//	
//	@Override
//	public void onSocketConnSucceed(BluetoothDevice bluetoothDevice, BluetoothSocket socket) {
//		
//		if (!bluetoothDevice.equals(this.device.bluetoothDevice)) {
//			return;
//		}
//		
//		RBQLog.i(TAG2, "socket创建成功");
//		
//		this.bluetoothSocket = socket;
//		
//		try {
//			
//			this.inputStream = this.bluetoothSocket.getInputStream();
//			this.outputStream = this.bluetoothSocket.getOutputStream();
//			
//			notifySppSocketConnSucceed(this.device);
//			
//			//启动数据读取线程
//			readThread.startRead(inputStream);
//			//启动数据写入线程
//			writeThread.startWrite(outputStream);
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//			
//			notifySppConnError(this.device, "数据流获取失败");
//		}
//		
//	}
//	
//	@Override
//	public void onSocketConnTimeout(BluetoothDevice bluetoothDevice) {
//		//这里socket连接失败也恢复isStart的值
//		isStart = false;
//		
//		if (bluetoothDevice.equals(this.device.bluetoothDevice)) {
//			notifySppSocketConnTimeout(this.device);
//		}
//	}
//	
//	//*******wifi连接************************************************
//	public void onWifiSocketConnSucceed(Socket socket) {
//		RBQLog.i(TAG2, "socket创建成功");
//		
//		this.wifiSocket = socket;
//
//		try {
//			
//			this.inputStream = this.wifiSocket.getInputStream();
//			this.outputStream = this.wifiSocket.getOutputStream();
//			
//			notifyWifiOnConnect(this.device);
//			
//			//启动数据读取线程
//			readThread.startRead(inputStream);
//			//启动数据写入线程
//			writeThread.startWrite(outputStream);
//			
//			//连接成功后，5秒开始启动监视器
//			mainHandler.postDelayed(new Runnable() {
//				@Override
//				public void run() {
//					startMonitorHeartData();
//				}
//			},5000);
//
//		} catch (IOException e) {
//			e.printStackTrace();
//			
//			notifyWifiOnConnectFailed(this.device, e.getMessage());
//		}
//		
//	}
//	
//	private void startMonitorHeartData() {
//		RBQLog.i("当前计数清零");
//		heartLooseTimes = 0;
//		mainHandler.postDelayed(new Runnable() {
//			@Override
//			public void run() {
//				clientConnectTimeout();
//			}
//		},2000);
//	}
//	
//	private void clientConnectTimeout() {
//		heartLooseTimes +=1;
//		RBQLog.i("当前计数加一"+heartLooseTimes);
//		if(heartLooseTimes < MAX_LOSE_HEART_TIMES){
//			mainHandler.postDelayed(new Runnable() {
//				@Override
//				public void run() {
//					clientConnectTimeout();
//				}
//			},3000);
//		}else{//次数大于3的时候断开连接
//			RBQLog.i("当前计数大于等于三，主动断开连接");
//			disconnect();
//		}
//	}
//	
//	
//	@Override
//	public void onConnecting() {
//		notifyWifiOnConnecting(this.device);
//	}
//	
//	@Override
//	public void onConnect(Socket socket) {
//		onWifiSocketConnSucceed(socket);
//		
//	}
//	
//	@Override
//	public void onConnectFailed(String msg) {
//		isStart = false;
//		notifyWifiOnConnectFailed(this.device, msg);
//	}
//	
//	@Override
//	public void onReadData(byte[] data) {
//		
//		synchronized (SPP_MANAGER) {
//			RBQLog.i("onReadData16进制格式:"+ Arrays.bytesToHexString1(data,","));
//			if(device.deviceType == 1){//wifi
//				heartLooseTimes = 0;
//			}
//			try {
//				//在ota或者发送图片数据过程中屏蔽json数据接收
//				if (!otaPacket.isStart() || !multiRowImageDataPacket.isStart() || !sppLogoPacket.isStart()) {
//					
//					String read_data = new String(data, StandardCharsets.UTF_8).trim();
//					
//					if (read_data.startsWith("{") && read_data.endsWith("}")) {
//						
//						dispatchJsonEven(read_data);
//						return;
//					}
//					//此处加容错处理，当接收是整句完整json { }但是有跟些没必要当内容时候
//					if (read_data.contains("{")
//							&&read_data.contains("}")
//							&&(!read_data.startsWith("{")||!read_data.endsWith("}"))) {
//						
//						read_data=read_data.substring(read_data.indexOf("{"),read_data.indexOf("}")+1);
//						
//						RBQLog.i("收到json --> 数据:"+ read_data);
//						
//						dispatchJsonEven(read_data);
//						return;
//					}
//					//此处加容错处理，当接收到包含 { 但是 {前面带有没必要当内容时
//					if (read_data.contains("{")
//							&&!read_data.contains("}")
//							&&!read_data.startsWith("{")) {
//						
//						read_data = read_data.substring(read_data.indexOf("{"));
//					}
//					
//					if (read_data.startsWith("{")) {
//						
//						receivingJsonData = true;
//						//删除stringBuilder中的数据
//						deleteJsonStringBuilder(jsonStringBuilder);
//						jsonStringBuilder.append(read_data);
//						return;
//					}
//					if (receivingJsonData) {
//						
//						if(read_data.contains("}") && !read_data.endsWith("}")){
////							RBQLog.i("onReadData:"+read_data);
//							read_data = read_data.substring(0,read_data.indexOf("}")+1);
//						}
//						
//						if (read_data.endsWith("}")) {
//							
//							jsonStringBuilder.append(read_data);
//							
//							String jsonString = jsonStringBuilder.toString();
//							
//							RBQLog.i("收到json数据:" + jsonString);
//							
//							dispatchJsonEven(jsonString);
//							
//							//删除stringBuilder中的数据
//							deleteJsonStringBuilder(jsonStringBuilder);
//							
//							//设置json数据接收结束标志receivingJsonData
//							receivingJsonData = false;
//							
//						} else {
//							
//							jsonStringBuilder.append(read_data);
//						}
//						return;
//					}
//				}
//				
//				dispatchDataEven(data);
//				
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			
//		}
//		
//	}
//	
//	private void dispatchJsonEven(String json) {
//		RBQLog.i("dispatchJsonEven:" + json);
//		try {
//			
//			JSONObject jsonObject = new JSONObject(json);
//			
//			int code = jsonObject.getInt("code");
//			
//			RBQLog.i("code:" + code);
//			
//			if (code == 0) {
//				
//				int cmd = jsonObject.getInt("cmd");
//				
//				RBQLog.i("cmd:" + cmd);
//				
//				if (cmd == Opcode.readPrinterParameters) {
//					
//					RBQLog.i("同步打印机参数接受到数据");
//					
//					String msg = jsonObject.getString("msg");
//					String[] parameters = msg.split(",");
//					
//					int headValue = Integer.parseInt(parameters[0]);
//					int l_pix = Integer.parseInt(parameters[1]);
//					int p_pix = Integer.parseInt(parameters[2]);
//					int distance = Integer.parseInt(parameters[3]);
//					
//					device.printer_head = headValue;
//					device.l_pix = l_pix;
//					device.p_pix = p_pix;
//					device.distance = distance;
//					
//					notifyParameterChange(device, headValue, l_pix, p_pix, distance);
//					
//				}
//				else if (cmd == Opcode.readPrinterCirculationAndRepeatTime) {
//					
//					String msg = jsonObject.getString("msg");
//					String[] parameters = msg.split(",");
//					int circulation_time = Integer.parseInt(parameters[0]);
//					int repeat_time = Integer.parseInt(parameters[1]);
//					
//					device.circulation = circulation_time;
//					device.repeat_time = repeat_time;
//					
//					notifyCirculationAndRepeatTimeChange(device, circulation_time, repeat_time);
//					
//				}
//				else if (cmd == Opcode.readPrinterDirectionAndPrintHeadDirection) {
//					
//					String msg = jsonObject.getString("msg");
//					String[] parameters = msg.split(",");
//					int direction = Integer.parseInt(parameters[0]);
//					RBQLog.i(" 设置页面 同步得打印方向 direction:" + direction);
//					int printHeadDirection = Integer.parseInt(parameters[1]);
//					
//					device.direction = direction;
//					//这里是从打印机获得的方向，因此也要同步给oldDirection
//					device.oldDirection = direction;
//					
//					notifyDirectionChange(device, direction, printHeadDirection);
//				}
//				else if (cmd == Opcode.readPrinterInfo) {
//					
//					RBQLog.i("读取到打印机信息");
//					
//					String id = jsonObject.getString("id");
//					String name = jsonObject.getString("name");
//					String mcu_version = jsonObject.getString("mcu_ver");
//					String mcu_date = jsonObject.getString("date");
//					
//					RBQLog.i("获取到版本号:" + mcu_version);
//					
//					device.printer_head_id = id;
//					device.deviceName = name;
//					device.mcu_version = mcu_version;
//					device.mcu_date = mcu_date;
//					
//					notifyReadDeviceInfo(device, id, name, mcu_version, mcu_date);
//					
//				}
//				else if (cmd == Opcode.readPrinterHeadId) {
//					String id = jsonObject.getString("id");
//					//                        RBQLog.i("读取到打印头温度为:"+temperature);
//					//                        device.printer_head_id = id;
//					
//				}
//				else if (cmd == Opcode.readPrinterHeadTemperature) {
//					
//					int temperature1 = jsonObject.getInt("temp_set");
//					int temperature2 = jsonObject.getInt("temp_get");
//					int temperature = temperature1 == 0 ? temperature2 : temperature1;
//					
//					RBQLog.i("读取到打印头温度为:" + temperature1+","+temperature2);
//					device.current_temperature = temperature;
//					
//					notifyTemperature(device, temperature);
//				}
//				else if (cmd == Opcode.readBattery) {
//					
//					int bat = jsonObject.getInt("bat");
//					RBQLog.i("读取到电量值为:" + bat);
//					
//					notifyReadBattery(device, bat);
//				}
//				else if (cmd == 4130) {
//					//每5秒发一次低点提醒
//					
//					RBQLog.i("每5秒发一次低点提醒:");
////					Toast.makeText(RBQAppManager.share().getApplicationContext(),"请充电",Toast.LENGTH_LONG).show();
////					notifyReadBattery(device, bat);
//				}
//				
//			} else {
//				
//				String error = Errors.Instance().getDescribeByCode(code);
//				RBQLog.i("同步打印机参数error:" + error);
//				notifyError(device, error);
//			}
//			
//		} catch (Exception e) {
//			
//			e.printStackTrace();
//			RBQLog.i("解析打印机信息数据异常" + json);
//		}
//		
//	}
//	
//	private void dispatchDataEven(byte[] data) {
//		
//		if (multiRowImageDataPacket.isStart()) {
//			
//			dispatchMultiRowImageReadData(data);
//			
//		} else if (otaPacket.isStart()) {
//			
//			dispatchOtaReadData(data);
//			
//		} else if (sppLogoPacket.isStart()) {
//			
//			dispatchLogoReadData(data);
//		}
//	}
//	
//	private void dispatchLogoReadData(byte[] data) {
//		
//		//请求数据
//		if (sppLogoPacket.isLogoRequest(data)) {//c
//			
//			//            RBQLog.i("ota");
//			
//			//还有下一帧数据，继续发送下一帧数据
//			if (this.sppLogoPacket.hasNextPacket()) {
//				
//				this.sendNextLogoPacket();
//			}
//			
//		} else if (data.length == 1 && ((data[0] & 0xFF) == TransportProtocol.NAK)) {
//			
//			this.sendNAKLogoPacket();
//			
//		} else if (data.length == 1 && ((data[0] & 0xFF) == TransportProtocol.EOT)) {
//			
//			RBQLog.i("logo数据传输完毕");
//			this.sppLogoPacket.setStart(false);
//			
//			sppLogoPacket.currentTime = System.currentTimeMillis();
//			float size = (float) sppLogoPacket.dataLength / 1000.0f;
//			
//			notifySppLogoFinishListener(size, 100, sppLogoPacket.startTime, sppLogoPacket.currentTime);
//			
//		}
//		
//	}
//	
//	private void dispatchOtaReadData(byte[] data) {
//		
//		//请求数据
//		if (otaPacket.isOtaRequest(data)) {//c
//			
//			//            RBQLog.i("ota");
//			//还有下一帧数据，继续发送下一帧数据
//			if (this.otaPacket.hasNextPacket()) {
//				
//				this.sendNextOtaPacket();
//			}
//			
//		} else if (data.length == 1 && ((data[0] & 0xFF) == TransportProtocol.NAK)) {
//			
//			this.sendNAKOtaPacket();
//			
//		} else if (data.length == 1 && ((data[0] & 0xFF) == TransportProtocol.EOT)) {
//			
//			RBQLog.i("ota数据传输完毕");
//			this.otaPacket.setStart(false);
//			
//			otaPacket.currentTime = System.currentTimeMillis();
//			float size = (float) otaPacket.dataLength / 1000.0f;
//			notifySppOTAFinishListener(size, 100, otaPacket.startTime, otaPacket.currentTime);
//			
//			otaPacket.setStart(false);
//		}
//	}
//	
//	private void dispatchMultiRowImageReadData(byte[] data) {
//		
//		//请求数据
//		if (multiRowImageDataPacket.isMultiRowImageRequest(data)) {//c
////			RBQLog.i("请求图片数据 >>> 第" + N_Index + "次请求");
//			N_Index = N_Index + 1;
//			//还有下一帧数据，继续发送下一帧数据
//			if (this.multiRowImageDataPacket.hasNextPacketWithCurrentRow()) {
//				//                RBQLog.i("继续发送下一包数据 >>> ");
//				this.sendNextMultiRowImagePacket();
//			}
//			
//		} else if (data.length == 1 && ((data[0] & 0xFF) == TransportProtocol.NAK)) {
//			RBQLog.i("重传当前数据包请求命令" + new String(data));
//			this.sendNakMultiRowImagePacket();
//			
//		} else if (data.length == 1 && ((data[0] & 0xFF) == TransportProtocol.EOT)) {
//			N_Index = 0;
//			//在测试过程中发现，在高性能手机中，发现在每行数据一样多的情况下极少数的概率只打最后一行，
//			// 但是在每行数据不一样多且数据量比较大，比如在当前的选区打印模式下，会经常性的碰到只能打印出来最后一行的情况，
//			// 并且经过数据分析，app生成的图片、生成的数据、读取出来的数据都没有任何问题
//			//最终，在打印下一行的时候延时500ms没有任何问题
//			// (目前估计可能是打印机固件在多行打印时，每行数据量不同时，可能存在某种缺陷;或者硬件性能引起处理跟不上)
////			mainHandler.postDelayed(new Runnable() {
////				@Override
////				public void run() {
////
//					delayCheckNextRowData();
////				}
////			}, 500);
//		}
//	}
//	
//	protected void delayCheckNextRowData() {
//
//		if (this.multiRowImageDataPacket.hasNextRow()) {
//			
//			mainHandler.postDelayed(new Runnable() {
//				@Override
//				public void run() {
//					
//					//判断如果当前组中没有下一包数据，则判断是否有下一组数据
//					multiRowImageDataPacket.cursorMoveToNext();
//					
//					byte currentRow = (byte)(multiRowImageDataPacket.getCurrentRow() & 0xFF);
//					
//					RBQLog.i("打印第:"+currentRow+"行");
//					
//					int arrIndexDataSize = multiRowImageDataPacket.currentRowDataLength;//4byte
//					
//					byte dataSize0 = (byte)(arrIndexDataSize & 0xFF);
//					byte dataSize1 = (byte)((arrIndexDataSize >> 8) & 0xFF);
//					byte dataSize2 = (byte)((arrIndexDataSize >> 16) & 0xFF);
//					byte dataSize3 = (byte)((arrIndexDataSize >> 24) & 0xFF);
//					byte compress = (byte)(multiRowImageDataPacket.compress & 0xFF);
//					
//					byte[] transmitParams = {currentRow,dataSize0,dataSize1,dataSize2,dataSize3,compress};
//					
//					sendCommand(Opcode.transmitPicture,transmitParams);
//				}
//			},300);
//			
//		}else {
//
//			mainHandler.postDelayed(new Runnable() {
//				@Override
//				public void run() {
//					//从0索引开始打
//					int index = multiRowImageDataPacket.totalRowCount;
//					byte[] printPictureParams = {0, (byte) index};
//					
//					sendCommand(Opcode.printerPicture,printPictureParams);//延时100ms发送打印指令
//					
//					//                RBQLog.i("数据传输完毕,从0开始打印，共打印"+count+"个");
////					multiRowImageDataPacket.setStart(false);
//					
//					multiRowImageDataPacket.currentTime = System.currentTimeMillis();
//					float size = (float) multiRowImageDataPacket.totalDataLen /1000.0f;
//					
//					notifySppImageFinishListener(size,100, multiRowImageDataPacket.startTime, multiRowImageDataPacket.currentTime);
//					
//					multiRowImageDataPacket.setStart(false);
//				}
//			},100);
//			
//			
//		}
//		
//		
////		if (this.multiRowImageDataPacket.hasNextRow()) {
////
////			//判断如果当前组中没有下一包数据，则判断是否有下一组数据
////			this.multiRowImageDataPacket.cursorMoveToNext();
////
////			byte currentRow = (byte) (multiRowImageDataPacket.getCurrentRow() & 0xFF);
////
////			RBQLog.i("打印第:" + currentRow + "行");
////
////			int arrIndexDataSize = multiRowImageDataPacket.currentRowDataLength;//4byte
////
////			byte dataSize0 = (byte) (arrIndexDataSize & 0xFF);
////			byte dataSize1 = (byte) ((arrIndexDataSize >> 8) & 0xFF);
////			byte dataSize2 = (byte) ((arrIndexDataSize >> 16) & 0xFF);
////			byte dataSize3 = (byte) ((arrIndexDataSize >> 24) & 0xFF);
////			byte compress = (byte) (multiRowImageDataPacket.compress & 0xFF);
////
////			byte[] transmitParams = {currentRow, dataSize0, dataSize1, dataSize2, dataSize3, compress};
////
////			this.sendCommand(Opcode.transmitPicture, transmitParams);
////
////		} else {
////
////			//从0索引开始打
////			int index = this.multiRowImageDataPacket.totalRowCount;
////			byte[] printPictureParams = {0, (byte) index};
////
////			this.sendCommand(Opcode.printerPicture, printPictureParams);
////
////			//                RBQLog.i("数据传输完毕,从0开始打印，共打印"+count+"个");
////			this.multiRowImageDataPacket.setStart(false);
////
////			multiRowImageDataPacket.currentTime = System.currentTimeMillis();
////			float size = (float) multiRowImageDataPacket.totalDataLen / 1000.0f;
////
////			notifySppImageFinishListener(size, 100, multiRowImageDataPacket.startTime, multiRowImageDataPacket.currentTime);
////
////			multiRowImageDataPacket.setStart(false);
////
////		}
//		
//	}
//	
//	@Override
//	public void onReadError() {//读或者写错误，表示socket连接断开，这里使用读来判断，因为读是不停的进行的，写不一定一直进行
//		
//		RBQLog.i(TAG2, "onSocketDisconnect");
//		
//		isStart = false;
//		
//		readThread.release();
//		writeThread.release();
//		
//		bluetoothSocketThread.release();
//		tcpClientThread.exeCancel();
//		pairThread.release();
//		
//		if (this.wifiSocket != null) {
//			try {
//				this.wifiSocket.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			this.wifiSocket = null;
//		}
//		if (this.bluetoothSocket != null) {
//			try {
//				this.bluetoothSocket.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			this.bluetoothSocket = null;
//		}
//		if (this.inputStream != null) {
//			try {
//				this.inputStream.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			this.inputStream = null;
//		}
//		if (this.outputStream != null) {
//			try {
//				this.outputStream.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			this.outputStream = null;
//		}
//		//连接断开
//		if (this.device.deviceType == 0)
//			notifySppSocketDisconnect(this.device);
//		else if (this.device.deviceType == 1)
//			notifyWifiOnConnectFailed(this.device, "onReadError，disconnect wifi");
//	}
//	
//	/********************************************************************************
//	 * Command API
//	 *******************************************************************************/
//	
//	public Command createCommand(int opcode, byte[] params) {
//		
//		final int prefixLen = 1;
//		final int packetLenLen = 2;
//		final int packetXorLenLen = 2;
//		final int packet_ctLen = 4;
//		final int opcodeLen = 2;
//		int paramsLen = 0;
//		if (params != null) {
//			paramsLen = params.length;
//		}
//		final int crcLen = 2;
//		
//		int byteLen = prefixLen + packetLenLen + packetXorLenLen + packet_ctLen + opcodeLen + paramsLen + crcLen;
//		
//		int pack_Len = packet_ctLen + opcodeLen + paramsLen;
//		
//		int crcByteLen = prefixLen + packetLenLen + packetXorLenLen + packet_ctLen + opcodeLen + paramsLen;
//		
//		byte[] data = new byte[byteLen];
//		
//		int offset = 0;
//		// prefix 前缀
//		data[offset++] = 0x17;
//		// packageLen
//		data[offset++] = (byte) (pack_Len & 0xFF);
//		data[offset++] = (byte) (pack_Len >> 8 & 0xFF);
//		
//		//packageLen取反
//		data[offset++] = (byte) ((~pack_Len) & 0xFF);
//		data[offset++] = (byte) ((~pack_Len) >> 8 & 0xFF);
//		
//		//pack_ct 帧序列 总共4byte
//		int pack_ct = this.generateSequenceNumber();
//		
//		RBQLog.i("pack_ct:" + pack_ct);
//		
//		data[offset++] = (byte) (pack_ct & 0xFF);
//		data[offset++] = (byte) (pack_ct >> 8 & 0xFF);
//		data[offset++] = (byte) (pack_ct >> 16 & 0xFF);
//		data[offset++] = (byte) (pack_ct >> 24 & 0xFF);
//		
//		// opcode
//		data[offset++] = (byte) (opcode & 0xFF);
//		data[offset++] = (byte) (opcode >> 8 & 0xFF);
//		
//		// params
//		if (params != null) {
//			System.arraycopy(params, 0, data, offset, params.length);
//		}
//		
//		byte[] crcByte = new byte[crcByteLen];
//		System.arraycopy(data, 0, crcByte, 0, crcByteLen);
//		
//		char crc = CRC16.crc16_calc(crcByte);
//		
//		offset = offset + paramsLen;
//		
//		data[offset++] = (byte) (crc >> 8 & 0xFF);
//		data[offset] = (byte) (crc & 0xFF);
//		
//		//        RBQLog.i("[***参与crc计算的byte ***: "+ Arrays.bytesToHexString1(crcByte,",")+" ;crc的值:"+(int)crc+" ["+ (crc >> 8 & 0xFF) + "] + ["+(crc & 0xFF)+"]");
//		
//		Command command = Command.newInstance();
//		command.data = data;
//		command.tag = SPP_TAG_NORMAL_COMMAND;
//		return command;
//	}
//	
//	public void sendCommand(int opcode, byte[] params) {
//		
//		long currentTime = System.currentTimeMillis();
//		long offsetTime = currentTime - lastSendCommandTime;
//		
//		Command command = createCommand(opcode, params);
//		
//		if (offsetTime >= commandInterval
//				&& commandQueue.isEmpty()) {
//			
//			RBQLog.i("发送的指令数据为:>>>" + Arrays.bytesToHexString1(command.data, ","));
//			
//			lastSendCommandTime = currentTime;
//			writeThread.write(command, normalCallback);
//			
//		} else {
//			
//			RBQLog.i("添加指令数据为:>>>" + Arrays.bytesToHexString1(command.data, ","));
//			
//			commandQueue.add(command);
//			
//			commandHandler.removeCallbacks(commandRunnable);
//			commandHandler.removeCallbacksAndMessages(null);
//			
//			long temp_offset = commandInterval - offsetTime;
//			commandHandler.postDelayed(commandRunnable, temp_offset);
//		}
//	}
//	
//	Runnable commandRunnable = new Runnable() {
//		@Override
//		public void run() {
//			
//			if (commandQueue.size() > 0) {
//				
//				RBQLog.i("发送commandQueue中的指令");
//				
//				commandHandler.removeCallbacks(commandRunnable);
//				commandHandler.removeCallbacksAndMessages(null);
//				
//				Command command = commandQueue.poll();
//				writeThread.write(command, normalCallback);
//				
//				lastSendCommandTime = System.currentTimeMillis();
//				;
//				
//				if (commandQueue.size() > 0) {
//					
//					commandHandler.removeCallbacks(commandRunnable);
//					commandHandler.removeCallbacksAndMessages(null);
//					//
//					RBQLog.i("启动下次commandQueue中指令发送");
//					commandHandler.postDelayed(commandRunnable, commandInterval);
//				} else {
//					RBQLog.i("commandQueue中指令发送完毕");
//				}
//			}
//			
//		}
//	};
//	
//	public void clearCommandQueue() {
//		commandHandler.removeCallbacks(commandRunnable);
//		commandHandler.removeCallbacksAndMessages(null);
//		commandQueue.clear();
//	}
//	
//	Command.Callback normalCallback = new Command.Callback() {
//		@Override
//		public void success(Command command, Object obj) {
//			RBQLog.i("发送数据成功，数据 -> " + Arrays.bytesToHexString1(command.data, ","));
//		}
//		
//		@Override
//		public void error(Command command, String errorMsg) {
//			RBQLog.i("指令发送错误:" + errorMsg);
//		}
//		
//		@Override
//		public boolean timeout(Command command, boolean delayEfficacy) {
//			return false;
//		}
//	};
//	
//	private int generateSequenceNumber() {
//		
//		int maxNum = 255;
//		
//		if (this.sequenceNumber > maxNum) {
//			
//			this.sequenceNumber = random.nextInt(maxNum - 1) + 1;
//		}
//		this.sequenceNumber++;
//		
//		return this.sequenceNumber;
//	}
//	
//	private byte[] generateRandom(byte[] random) {
//		this.random.nextBytes(random);
//		return random;
//	}
//	
//	/********************************************************************************
//	 * 发送连续的图片大数据包
//	 *******************************************************************************/
//	public void setWidthSendMultiRowImageDataPacket(MultiRowImageData multiRowImageData) {
//		
//		if (!isConnected()) {
//			return;
//		}
//		N_Index = 0;
//		multiRowImageDataPacket.set(application, multiRowImageData);
//		localMultiRowImageDataPacketReadyStart();
//	}
//	
//	public void setWidthSendMultiRowImageDataPacket(MultiRowImageData multiRowImageData, int fh) {
//		
//		if (!isConnected()) {
//			return;
//		}
//		N_Index = 0;
//		multiRowImageDataPacket.set(application, multiRowImageData, fh);
//		localMultiRowImageDataPacketReadyStart();
//	}
//	
//	void localMultiRowImageDataPacketReadyStart() {
//		
//		if (receivingJsonData) {
//			receivingJsonData = false;
//		}
//		this.otaPacket.setStart(false);
//		this.sppLogoPacket.setStart(false);
//		this.multiRowImageDataPacket.setStart(true);
//		
//		clearCommandQueue();//确保在图片打印过程中，指令列表里面一定是没其他指令
//		
//		byte currentRow = (byte) (multiRowImageDataPacket.getCurrentRow() & 0xFF);
//		
//		RBQLog.i("打印第:" + currentRow + "行");
//		
//		int arrIndexDataSize = multiRowImageDataPacket.currentRowDataLength;//4byte
//		
//		byte dataSize0 = (byte) (arrIndexDataSize & 0xFF);
//		byte dataSize1 = (byte) ((arrIndexDataSize >> 8) & 0xFF);
//		byte dataSize2 = (byte) ((arrIndexDataSize >> 16) & 0xFF);
//		byte dataSize3 = (byte) ((arrIndexDataSize >> 24) & 0xFF);
//		byte compress = (byte) (multiRowImageDataPacket.compress & 0xFF);
//		
//		byte[] transmitParams = {currentRow, dataSize0, dataSize1, dataSize2, dataSize3, compress};
//		sendCommand(Opcode.transmitPicture, transmitParams);
//		
//		
//		multiRowImageDataPacket.startTime = System.currentTimeMillis();
//		float size = (float) multiRowImageDataPacket.totalDataLen / 1000.0f;
//		//发送进度更新事件
//		notifySppImageStartListener(size, 0, multiRowImageDataPacket.startTime);
//	}
//	
//	private void sendNextMultiRowImagePacket() {
//		
//		if (!isConnected()) {
//			return;
//		}
//		
//		byte[] data = this.multiRowImageDataPacket.getNextPacket();
//		
//		multiRowImageDataPacket.currentTime = System.currentTimeMillis();
//		
//		boolean updateProgress = multiRowImageDataPacket.invalidateProgress();
//		if (updateProgress) {
//			int progress = multiRowImageDataPacket.getProgress();
//			float size = (float) multiRowImageDataPacket.totalDataLen / 1000.0f;
//			//发送进度更新事件
//			notifySppImageProgress(size, progress, multiRowImageDataPacket.startTime, multiRowImageDataPacket.currentTime);
//		}
//		Command command = Command.newInstance();
//		command.data = this.multiRowImageDataPacket.packetFormat(data);
//		command.arrIndex = multiRowImageDataPacket.currentRow;
//		command.indexInArr = multiRowImageDataPacket.indexInCurrentRowPacket;
//		writeThread.write(command, multiRowImagePacketCallback);
//		
//	}
//	
//	/**
//	 * 重传当前数据包指令
//	 */
//	private void sendNakMultiRowImagePacket() {
//		
//		if (!isConnected()) {
//			return;
//		}
//		
//		byte[] data = this.multiRowImageDataPacket.getCurrentPacket();
//		
//		RBQLog.i("NAK 重传当前包");
//		
//		multiRowImageDataPacket.currentTime = System.currentTimeMillis();
//		
//		Command command = Command.newInstance();
//		command.data = this.multiRowImageDataPacket.packetFormat(data);
//		command.arrIndex = this.multiRowImageDataPacket.currentRow;
//		command.indexInArr = this.multiRowImageDataPacket.indexInCurrentRowPacket;
//		writeThread.write(command, multiRowImagePacketCallback);
//	}
//	
//	
//	Command.Callback multiRowImagePacketCallback = new Command.Callback() {
//		@Override
//		public void success(Command command, Object obj) {
//			//            RBQLog.i("发送第 ["+sppCommand.indexInArr+"] 包图片数据成功，数据 -> "+ Arrays.bytesToHexString1(sppCommand.data,","));
//		}
//		
//		@Override
//		public void error(Command command, String errorMsg) {
//			RBQLog.i("数据写入错误:" + errorMsg);
//		}
//		
//		@Override
//		public boolean timeout(Command command, boolean delayEfficacy) {
//			return false;
//		}
//	};
//	
//	/**
//	 *
//	 */
//	
//	public void setWithOtaPacketBySpp(byte[] data) {
//		
//		if (!isConnected()) {
//			return;
//		}
//		
//		this.otaPacket.set(data);
//		localSetWidthSendOtaPacket();
//	}
//	
//	public void setWithOtaPacketBySpp(byte[] data, int fn) {
//		
//		if (!isConnected()) {
//			return;
//		}
//		
//		this.otaPacket.set(data, fn);
//		localSetWidthSendOtaPacket();
//	}
//	
//	private void localSetWidthSendOtaPacket() {
//		
//		if (!this.otaPacket.hasOtaData()) {
//			return;
//		}
//		
//		if (this.receivingJsonData) {
//			this.receivingJsonData = false;
//		}
//		
//		this.multiRowImageDataPacket.setStart(false);
//		this.sppLogoPacket.setStart(false);
//		this.otaPacket.setStart(true);
//		
//		int dataLength = this.otaPacket.dataLength;//4byte
//		
//		byte dataSize0 = (byte) (dataLength & 0xFF);
//		byte dataSize1 = (byte) ((dataLength >> 8) & 0xFF);
//		byte dataSize2 = (byte) ((dataLength >> 16) & 0xFF);
//		byte dataSize3 = (byte) ((dataLength >> 24) & 0xFF);
//		
//		byte[] otaParams = {dataSize0, dataSize1, dataSize2, dataSize3};
//		
//		this.sendCommand(Opcode.updateMcu, otaParams);
//		
//		float size = (float) otaPacket.dataLength / 1000.0f;
//		otaPacket.startTime = System.currentTimeMillis();
//		notifySppOTAStartListener(size, 0, otaPacket.startTime);
//	}
//	
//	private void sendNextOtaPacket() {
//		
//		if (!isConnected() || !otaPacket.hasNextPacket()) {
//			return;
//		}
//		byte[] data = otaPacket.getNextPacket();
//		
//		if (data == null) {
//			RBQLog.i("获取下一包为null");
//			return;
//		}
//		
//		//        RBQLog.i("发送第 ["+ sppOTAPacket.index+"] 包数据");
//		
//		otaPacket.currentTime = System.currentTimeMillis();
//		
//		boolean updateProgress = otaPacket.invalidateProgress();
//		if (updateProgress) {
//			int progress = otaPacket.getProgress();
//			float size = (float) otaPacket.dataLength / 1000.0f;
//			//发送进度更新事件
//			notifySppOTAProgress(size, progress, otaPacket.startTime, otaPacket.currentTime);
//		}
//		Command command = Command.newInstance();
//		command.data = this.otaPacket.packetFormat(data);
//		command.arrIndex = 0;
//		command.indexInArr = otaPacket.index;
//		writeThread.write(command, sppOTAPacketCallback);
//	}
//	
//	/**
//	 * 重传当前数据包指令
//	 */
//	private void sendNAKOtaPacket() {
//		
//		if (!isConnected()) {
//			return;
//		}
//		byte[] data = otaPacket.getPacket();
//		
//		RBQLog.i("NAK 重传当前包");
//		
//		otaPacket.currentTime = System.currentTimeMillis();
//		
//		Command command = Command.newInstance();
//		command.data = this.otaPacket.packetFormat(data);
//		command.arrIndex = 0;
//		command.indexInArr = otaPacket.index;
//		writeThread.write(command, sppOTAPacketCallback);
//	}
//	
//	Command.Callback sppOTAPacketCallback = new Command.Callback() {
//		@Override
//		public void success(Command command, Object obj) {
//			
//			//            RBQLog.i("发送第 ["+sppCommand.indexInArr+"] 包OTA数据成功，数据 -> "+ Arrays.bytesToHexString1(sppCommand.data,","));
//			
//		}
//		
//		@Override
//		public void error(Command command, String errorMsg) {
//			
//			RBQLog.i("数据写入错误:" + errorMsg);
//		}
//		
//		@Override
//		public boolean timeout(Command command, boolean delayEfficacy) {
//			return false;
//		}
//	};
//	
//	/**
//	 *
//	 */
//	public void setWithLogoPacketBySpp(byte[] data, int fn) {
//		
//		if (!isConnected()) {
//			return;
//		}
//		
//		this.sppLogoPacket.set(data, fn);
//		localSetWidthSendLogoPacket();
//	}
//	
//	private void localSetWidthSendLogoPacket() {
//		
//		if (!this.sppLogoPacket.hasLogoData()) {
//			return;
//		}
//		
//		if (this.receivingJsonData) {
//			this.receivingJsonData = false;
//		}
//		
//		this.multiRowImageDataPacket.setStart(false);
//		this.otaPacket.setStart(false);
//		this.sppLogoPacket.setStart(true);
//		
//		int dataLength = this.sppLogoPacket.dataLength;//4byte
//		
//		byte dataSize0 = (byte) (dataLength & 0xFF);
//		byte dataSize1 = (byte) ((dataLength >> 8) & 0xFF);
//		byte dataSize2 = (byte) ((dataLength >> 16) & 0xFF);
//		byte dataSize3 = (byte) ((dataLength >> 24) & 0xFF);
//		
//		byte[] logoParams = {dataSize0, dataSize1, dataSize2, dataSize3};
//		
//		this.sendCommand(Opcode.writeLogo, logoParams);
//		
//		float size = (float) sppLogoPacket.dataLength / 1000.0f;
//		sppLogoPacket.startTime = System.currentTimeMillis();
//		notifySppLogoStartListener(size, 0, sppLogoPacket.startTime);
//	}
//	
//	private void sendNextLogoPacket() {
//		
//		if (!isConnected() || !sppLogoPacket.hasNextPacket()) {
//			return;
//		}
//		byte[] data = sppLogoPacket.getNextPacket();
//		
//		if (data == null) {
//			RBQLog.i("获取下一包为null");
//			return;
//		}
//		
//		RBQLog.i("发送第 [" + sppLogoPacket.index + "] 包数据");
//		
//		sppLogoPacket.currentTime = System.currentTimeMillis();
//		
//		boolean updateProgress = sppLogoPacket.invalidateProgress();
//		if (updateProgress) {
//			int progress = sppLogoPacket.getProgress();
//			float size = (float) sppLogoPacket.dataLength / 1000.0f;
//			//发送进度更新事件
//			notifySppLogoProgress(size, progress, sppLogoPacket.startTime, sppLogoPacket.currentTime);
//		}
//		Command command = Command.newInstance();
//		command.data = this.sppLogoPacket.packetFormat(data);
//		command.arrIndex = 0;
//		command.indexInArr = sppLogoPacket.index;
//		writeThread.write(command, sppLogoPacketCallback);
//	}
//	
//	/**
//	 * 重传当前数据包指令
//	 */
//	private void sendNAKLogoPacket() {
//		
//		if (!isConnected()) {
//			return;
//		}
//		byte[] data = sppLogoPacket.getPacket();
//		
//		RBQLog.i("NAK 重传当前包");
//		
//		sppLogoPacket.currentTime = System.currentTimeMillis();
//		
//		Command command = Command.newInstance();
//		command.data = this.sppLogoPacket.packetFormat(data);
//		command.arrIndex = 0;
//		command.indexInArr = sppLogoPacket.index;
//		writeThread.write(command, sppLogoPacketCallback);
//	}
//	
//	Command.Callback sppLogoPacketCallback = new Command.Callback() {
//		@Override
//		public void success(Command command, Object obj) {
//			
//			//			            RBQLog.i("发送第 ["+sppCommand.indexInArr+"] 包logo数据成功，数据 -> "+ Arrays.bytesToHexString1(sppCommand.data,","));
//			RBQLog.i("发送第 [" + command.indexInArr + "] 包logo数据成功，数据 -> ");
//		}
//		
//		@Override
//		public void error(Command command, String errorMsg) {
//			
//			RBQLog.i("数据写入错误:" + errorMsg);
//		}
//		
//		@Override
//		public boolean timeout(Command command, boolean delayEfficacy) {
//			return false;
//		}
//	};
//	
//	
//	public interface OnSppImageProgressListener {
//		void onSppImageStartListener(float size, int progress, long startTime);//打印开始
//		
//		void onSppImageProgress(float size, int progress, long startTime, long currentTime);//打印过程进度更新
//		
//		void onSppImageFinishListener(float size, int progress, long startTime, long currentTime);//打印完成
//		
//		void onSppImageError(String error);
//	}
//	
//	public interface OnSppOTAProgressListener {
//		void onSppOTAStartListener(float size, int progress, long startTime);//打印开始
//		
//		void onSppOTAProgress(float size, int progress, long startTime, long currentTime);//打印过程进度更新
//		
//		void onSppOTAFinishListener(float size, int progress, long startTime, long currentTime);//打印完成
//		
//		void onSppOTAError(String error);
//	}
//	
//	public interface OnSppLogoProgressListener {
//
//		void onSppLogoStartListener(float size, int progress, long startTime);//打印开始
//		
//		void onSppLogoProgress(float size, int progress, long startTime, long currentTime);//打印过程进度更新
//		
//		void onSppLogoFinishListener(float size, int progress, long startTime, long currentTime);//打印完成
//		
//		void onSppLogoError(String error);
//	}
//	
//	/**
//	 * 以下几个响应事件是由该类内部提供，调用该类，向外的事件定义的时候，都是以Spp开头，
//	 * 之前其他地方的事件在该类进行了实现，再由该类以Spp格式的事件向外传递。
//	 */
//	public interface OnSppBluetoothStateListener {
//		
//		void onSppOpeningBlueToothListener();//蓝牙正在打开
//		
//		void onSppOpenedBlueToothListener();//蓝牙打开
//		
//		void onSppClosingBlueToothListener();//蓝牙正在关闭
//		
//		void onSppClosedBlueToothListener();//蓝牙关闭
//	}
//	
//	//用于蓝牙扫描和发现相关的事件
//	public interface OnSppBluetoothDeviceDiscoverListener {
//		
//		void onSppStartDiscovering();
//		
//		void onSppStopDiscovering();
//		
//		void onSppDiscovered(BluetoothDevice device);
//	}
//	
//	//配对、a2dp、socket等事件统一由该接口一次性提供
//	public interface OnSppDeviceConnectListener {
//		
//		void onSppBonding(Device device);
//		
//		void onSppBonded(Device device);
//		
//		void onSppDisBond(Device device);
//		
//		void onSppA2dpConnStart(Device device);
//		
//		void onSppA2dpConnSucceed(Device device);
//		
//		void onSppA2dpConnTimeout(Device device);
//		
//		void onSppSocketConnStart(Device device);
//		
//		void onSppSocketConnSucceed(Device device);
//		
//		void onSppSocketConnTimeout(Device device);
//		
//		void onSppSocketDisconnect(Device device);
//		
//		void onSppConnError(Device device, String error);
//		
//	}
//	
//	public interface OnReceiveMessageListener {
//		void onParameterChange(Device device, int headValue, int l_pix, int p_pix, int distance);
//		
//		void onCirculationAndRepeatTimeChange(Device device, int circulation_time, int repeat_time);
//		
//		void onDirectionChange(Device device, int direction, int printHeadDirection);
//		
//		void onReadDeviceInfo(Device device, String id, String name, String mcu_version, String mcu_date);
//		
//		void onReadTemperature(Device device, int temp);
//		
//		void onReadBattery(Device device, int bat);
//		
//		void onError(Device device, String error);
//	}
//	
//	
//	public interface OnWifiConnectListener {
//		void onConnect(Device device);
//		
//		void onConnectFailed(Device device, String msg);
//		
//		void onConnecting(Device device);
//	}
//}
