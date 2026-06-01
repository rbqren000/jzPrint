package com.mx.mxSdk.WifiCenter;

import android.os.Handler;
import android.os.Looper;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class UdpServiceThread implements Runnable {

	private final Handler mHandler = new Handler(Looper.getMainLooper());
	private DatagramSocket ds;
	private volatile boolean isStart;
	private Thread udpThread;

	private OnUpdMonitorListener onUpdMonitorListener;

	public boolean isStart() {
		return isStart;
	}

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

	public synchronized void startMonitorUdp() {

		if (isStart) return;

		stopMonitorUdp();

		isStart = true;
		mHandler.postDelayed(delayUdpMonitorRunnable, 500);
	}

	private final Runnable delayUdpMonitorRunnable = new Runnable() {
		@Override
		public void run() {
			udpThread = new Thread(UdpServiceThread.this);
			udpThread.start();
			if (onUpdMonitorListener != null) {
				runOnUiThread(() -> onUpdMonitorListener.onUpdMonitorStart());
			}
		}
	};

	public synchronized void stopMonitorUdp() {

		if (!isStart) return;

		isStart = false;
		mHandler.removeCallbacks(delayUdpMonitorRunnable);
		mHandler.removeCallbacksAndMessages(null);

		if (ds != null && !ds.isClosed()) {
			ds.close();
		}

		if (udpThread != null) {
			udpThread.interrupt();
			udpThread = null;
		}

		if (onUpdMonitorListener != null) {
			runOnUiThread(() -> onUpdMonitorListener.onUpdMonitorStop());
		}
	}

	public void registerUpdMonitorListener(OnUpdMonitorListener onUpdMonitorListener) {
		this.onUpdMonitorListener = onUpdMonitorListener;
	}

	public void unregisterUpdMonitorListener() {
		this.onUpdMonitorListener = null;
	}

	@Override
	public void run() {
		try {
			ds = new DatagramSocket(null);
			ds.setReuseAddress(true);
			ds.bind(new InetSocketAddress(6099));

			while (isStart && !Thread.currentThread().isInterrupted() && !ds.isClosed()) {
				try {
					byte[] bys = new byte[1024];
					DatagramPacket dp = new DatagramPacket(bys, bys.length);
					ds.receive(dp);
					runOnUiThread(() -> {
						if (onUpdMonitorListener != null) {
							// 使用实际接收到的数据长度
							byte[] receivedData = new byte[dp.getLength()];
							System.arraycopy(dp.getData(), 0, receivedData, 0, dp.getLength());
							onUpdMonitorListener.onUdpReceive(receivedData);
						}
					});
				} catch (SocketException e) {
					if (isStart) {
						runOnUiThread(() -> {
							if (onUpdMonitorListener != null) {
								onUpdMonitorListener.onUdpError(e);
							}
						});
					}
					break; // 跳出循环
				} catch (Exception e) {
					runOnUiThread(() -> {
						if (onUpdMonitorListener != null) {
							onUpdMonitorListener.onUdpError(e);
						}
					});
					break; // 跳出循环
				}
			}
		} catch (Exception e) {
			runOnUiThread(() -> {
				if (onUpdMonitorListener != null) {
					onUpdMonitorListener.onUdpError(e);
				}
			});
		} finally {
			if (ds != null && !ds.isClosed()) {
				ds.close();
			}
		}
	}

	public interface OnUpdMonitorListener {
		void onUpdMonitorStart();
		void onUdpReceive(byte[] data);
		void onUpdMonitorStop();
		void onUdpError(Exception e);
	}
}


