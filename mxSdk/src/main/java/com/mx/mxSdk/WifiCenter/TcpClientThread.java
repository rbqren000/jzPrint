package com.mx.mxSdk.WifiCenter;

import android.os.Handler;
import android.os.Looper;
import com.mx.mxSdk.Utils.RBQLog;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpClientThread implements Runnable {

	private static final String TAG = "TcpClientThread";
	private static final Handler mHandler = new Handler(Looper.getMainLooper());

	private int port;
	private String hostIP;
	private Socket mSocket;
	private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

	private OnTCPConnectListener onTCPConnectListener;

	private void runOnUiThread(Runnable runnable) {
		if (isAndroidMainThread()) {
			runnable.run();
		} else {
			mHandler.post(runnable);
		}
	}

	private static boolean isAndroidMainThread() {
		return Looper.myLooper() == Looper.getMainLooper();
	}

	public synchronized void connect(String hostIP, int port) {
		disconnect(); // 确保前一个连接已断开

		this.hostIP = hostIP;
		this.port = port;
		threadPool.submit(this); // 提交到线程池

		if (onTCPConnectListener != null) {
			runOnUiThread(() -> onTCPConnectListener.onWifiSocketConnectStart());
		}
	}

	public synchronized void cancel() {
		disconnect();
	}

	public synchronized void disconnect() {
		closeSocket();
	}

	public boolean isConnected() {
		return mSocket != null && mSocket.isConnected();
	}

	private void closeSocket() {
		if (mSocket != null) {
			try {
				if (!mSocket.isClosed()) {
					mSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				mSocket = null;
			}
		}
	}

	public void registerSocketConnListener(OnTCPConnectListener onTCPConnectListener) {
		this.onTCPConnectListener = onTCPConnectListener;
	}

	public void unregisterSocketConnListener() {
		this.onTCPConnectListener = null;
	}

	@Override
	public void run() {
		RBQLog.i(TAG, "【TcpClientThread】开始连接设备--->hostIP:" + hostIP + "; port:" + port);
		try {
			mSocket = new Socket();
			// 设置连接超时时间为5秒
			mSocket.connect(new InetSocketAddress(hostIP, port), 5000);

			if (onTCPConnectListener != null) {
				RBQLog.i(TAG, "onWifiSocketConnect");
				// 在 post 回调前捕获当前 socket 值，避免竞态条件导致 socket 为 null
				// 局部变量被 lambda 捕获的是值，不会受后续 mSocket = null 影响
				final Socket socket = mSocket;
				if (socket != null && socket.isConnected()) {
					runOnUiThread(() -> onTCPConnectListener.onWifiSocketConnect(socket));
				}
			}
		} catch (SocketTimeoutException e) {
			RBQLog.e( "SocketTimeoutException: " + e.getMessage());
			notifyConnectFailed("连接超时");
		} catch (IOException e) {
			RBQLog.e("IOException: " + e.getMessage());
			notifyConnectFailed("IO异常");
		} catch (Exception e) {
			RBQLog.e("Exception: " + e.getMessage());
			notifyConnectFailed("连接失败");
		}
	}

	private void notifyConnectFailed(String message) {
		if (onTCPConnectListener != null) {
			runOnUiThread(() -> onTCPConnectListener.onWifiConnectFailed(message));
		}
	}

	public interface OnTCPConnectListener {
		void onWifiSocketConnectStart();
		void onWifiSocketConnect(Socket socket);
		void onWifiConnectFailed(String msg);
	}
}

