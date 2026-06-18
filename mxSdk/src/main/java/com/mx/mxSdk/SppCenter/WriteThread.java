package com.mx.mxSdk.SppCenter;

import com.mx.mxSdk.CommandContext;
import com.mx.mxSdk.DataObjContext;
import com.mx.mxSdk.Utils.RBQLog;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import tp.xmaihh.serialport.utils.ByteUtil;
import java.util.concurrent.atomic.AtomicBoolean;

public class WriteThread implements Runnable {

    public static final String TAG = WriteThread.class.getSimpleName();

    // 使用 BlockingQueue 替代 ConcurrentLinkedQueue，消除忙等待
    private final BlockingQueue<Object> dataQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isStart = new AtomicBoolean(false);
    private OutputStream outputStream;

    private Thread writeThread;

    private Thread heartbeatThread;
    private final AtomicBoolean isHeartbeatRunning = new AtomicBoolean(false);
    private float heartbeatInterval = 1.0f; // 心跳时间间隔，单位秒
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
            try {
                // 阻塞等待队列中的数据，最多等 500ms（兼顾及时响应 stop 信号）
                Object obj = dataQueue.poll(500, TimeUnit.MILLISECONDS);
                if (obj == null) {
                    continue; // 超时，重新检查 isStart 状态
                }

                // 速率控制：确保连续发包间隔至少 1ms（保留原有保护逻辑）
                long elapsed = System.currentTimeMillis() - lastDataSentTime;
                if (elapsed < 1) {
                    Thread.sleep(1 - elapsed);
                }

                localWrite(obj);
                lastDataSentTime = System.currentTimeMillis();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
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

    /**
     * 启动心跳。
     * 改由通过队列发送心跳包，避免心跳线程与写线程直接共享 OutputStream 的竞态问题。
     * 心跳线程仅在队列为空时入队心跳包，不会打断正常数据流。
     */
    public void startHeartbeat(float time) {
        stopHeartbeat();

        RBQLog.i(">>>【心跳】启动心跳");
        isHeartbeatRunning.set(true);
        heartbeatInterval = time;

        heartbeatThread = new Thread(() -> {
            while (isHeartbeatRunning.get()) {
                try {
                    ThreadUtils.sleepInterruptible(heartbeatInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                // 仅在队列空闲时入队心跳包，避免占用数据传输带宽
                if (dataQueue.isEmpty()) {
                    dataQueue.add(heartbeatData);
                }
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







