package com.mx.mxSdk.SppCenter;

import com.mx.mxSdk.CommandContext;
import com.mx.mxSdk.DataObjContext;
import com.mx.mxSdk.Utils.RBQLog;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import tp.xmaihh.serialport.utils.ByteUtil;
import java.util.concurrent.atomic.AtomicBoolean;

public class WriteThread implements Runnable {

    public static final String TAG = WriteThread.class.getSimpleName();

    protected final Queue<Object> dataQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isStart = new AtomicBoolean(false);
    private OutputStream outputStream;

    private Thread writeThread;

    private Thread heartbeatThread;
    private final AtomicBoolean isHeartbeatRunning = new AtomicBoolean(false);
    private float heartbeatInterval = 1.0f; // 心跳时间间隔，单位秒
    private long lastHeartBeatTime = 0; // 上次心跳包发送时间戳
    private long lastDataSentTime = 0; // 上次发送数据时间戳

    private final byte[] heartbeatData = new byte[]{0x00};

    public synchronized void start(OutputStream outputStream) {
        if (isStart.get()||outputStream == null) return;

        cancel();

        isStart.set(true);
        this.outputStream = outputStream;
        writeThread = new Thread(this);
        writeThread.start();
        RBQLog.i(TAG, "WriteThread开始执行");
    }

    public synchronized void cancel() {
        if (!isStart.get()) return;

        isStart.set(false);

        if (writeThread != null) {
            writeThread.interrupt();
            writeThread = null;
        }

        dataQueue.clear();
        stopHeartbeat();
        outputStream = null;
        RBQLog.i(TAG, "WriteThread停止执行");
    }

    public void write(byte[] data) {
        if (data == null) return;
        dataQueue.add(data);
    }

    public void write(CommandContext context) {
        if (context == null || context.command == null || context.command.data == null) return;
        dataQueue.add(context);
    }

    public void write(DataObjContext context) {
        if (context == null || context.dataObj == null || context.dataObj.data == null) return;
        dataQueue.add(context);
    }

    public void writeHex(String sHex) {
        byte[] bOutArray = ByteUtil.HexToByteArr(sHex);
        write(bOutArray);
    }

    public void writeText(String sTxt) {
        byte[] bOutArray = sTxt.getBytes();
        write(bOutArray);
    }

    @Override
    public void run() {

        while (isStart.get() && outputStream != null) {
            long currentTime = System.currentTimeMillis();
            boolean isHeartbeat = isHeartbeatRunning.get();
            boolean timeSinceSendData = currentTime - lastDataSentTime >= 1;
            boolean timeSinceSendHeartBeat = isHeartbeat && currentTime - lastHeartBeatTime >= 20;

            if ((!isHeartbeat && timeSinceSendData) || (timeSinceSendHeartBeat && timeSinceSendData)) {
                Object obj = dataQueue.poll();
                if (obj != null) {
                    localWrite(obj);
                    lastDataSentTime = currentTime;
                }
            }
        }
    }

    private synchronized void localWrite(Object obj) {

        try {
            if (outputStream == null) {
                return;
            }
            if (obj instanceof DataObjContext context) {
                byte[] data = context.dataObj.data;
                outputStream.write(data);
                outputStream.flush();

                if (context.callback != null) {
                    context.callback.success(context.dataObj, "数据发送成功");
                }
            } else if (obj instanceof CommandContext context) {
                byte[] data = context.command.data;
                outputStream.write(data);
                outputStream.flush();

                if (context.callback != null) {
                    context.callback.success(context.command, "指令发送成功");
                }
            } else if (obj instanceof byte[] data) {
                outputStream.write(data);
                outputStream.flush();
            }
        } catch (Exception e) {
            handleError(obj);
        }
    }

    private void handleError(Object obj) {
        if (obj instanceof DataObjContext context) {
            if (context.callback != null) {
                context.callback.error(context.dataObj, "数据发送失败");
            }
        } else if (obj instanceof CommandContext context) {
            if (context.callback != null) {
                context.callback.error(context.command, "指令发送失败");
            }
        }
    }

    public void startHeartbeat(float time) {
        stopHeartbeat();

        RBQLog.i(">>>【心跳】启动心跳");
        isHeartbeatRunning.set(true);
        heartbeatInterval = time;

        heartbeatThread = new Thread(() -> {
            while (isHeartbeatRunning.get()) {

                long currentTime = System.currentTimeMillis();
                long timeSinceLastData = currentTime - lastDataSentTime;
                long timeSinceLastHeart = currentTime - lastHeartBeatTime;

                if (outputStream != null &&
                        dataQueue.isEmpty() &&
                        timeSinceLastHeart >= heartbeatInterval * 1000 &&
                        timeSinceLastData >= heartbeatInterval * 1000) {
                    try {
                        outputStream.write(heartbeatData);
                        outputStream.flush();
                        lastHeartBeatTime = currentTime;
//                        RBQLog.i("心跳包已发送");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                ThreadUtils.sleep(heartbeatInterval);
            }
        });
        heartbeatThread.start();
        RBQLog.i(TAG, "Heartbeat started");
    }

    public void stopHeartbeat() {
        if (!isHeartbeatRunning.get()) return;

        isHeartbeatRunning.set(false);

        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
            heartbeatThread = null;
        }
        RBQLog.i(TAG, "Heartbeat stopped");
    }
}







