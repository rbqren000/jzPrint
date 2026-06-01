package com.mx.mxSdk;

import android.os.Handler;
import android.os.Looper;
import java.nio.charset.StandardCharsets;
import android.util.Log;

import org.json.JSONObject;

public class JsonStreamAssembler {
    private static final String TAG = "JsonStreamAssembler";
    private static final long DEFAULT_TIMEOUT_MS = 5000;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable = this::reset;

    private final OnJsonCompleteListener listener;
    private final long timeoutMs;
    private final StringBuilder buffer = new StringBuilder();

    private boolean receiving = false;
    private int bracketCount = 0;

    public interface OnJsonCompleteListener {
        void onJsonComplete(String json);
    }

    public JsonStreamAssembler(OnJsonCompleteListener listener, long timeoutMs) {
        this.listener = listener;
        this.timeoutMs = timeoutMs;
    }

    public JsonStreamAssembler(OnJsonCompleteListener listener) {
        this(listener, DEFAULT_TIMEOUT_MS);
    }

    public void feed(byte[] data) {
        // 过滤空数据或 null，避免无意义的日志打印和计时器重启
        if (data == null || data.length == 0) {
            return;
        }
        
        String text;
        try {
            text = new String(data, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            Log.w(TAG, "非UTF-8数据被跳过");
            return;
        }
        
        // 过滤空白字符串
        if (text.isEmpty()) {
            return;
        }
        
        Log.d(TAG, "Received: " + preview(text));
        restartTimeoutTimer();

        // 完整 JSON
        if (text.startsWith("{") && text.endsWith("}") && countBrackets(text) == 0) {
            Log.d(TAG, "检测到完整JSON");
            tryDispatch(text);
            return;
        }

        // 噪声中嵌套 JSON
        int startIdx = text.indexOf("{");
        int endIdx = text.lastIndexOf("}");
        if (startIdx >= 0 && endIdx > startIdx) {
            String possibleJson = text.substring(startIdx, endIdx + 1);
            if (tryDispatch(possibleJson)) return;
        }

        // 新 JSON 开始
        if (text.contains("{") && !text.contains("}")) {
            if (receiving) reset();
            String start = text.substring(text.indexOf("{"));
            receiving = true;
            bracketCount = countBrackets(start);
            buffer.setLength(0);
            buffer.append(start);
            Log.d(TAG, "开始接收新JSON，括号计数: " + bracketCount);
            return;
        }

        // 拼接后可能完整
        if (receiving) {
            buffer.append(text);
            bracketCount += countBrackets(text);
            Log.d(TAG, "继续接收JSON片段，当前括号计数: " + bracketCount);

            if (text.contains("}") && bracketCount <= 0) {
                String joined = buffer.toString();
                int level = 0;
                int end = -1;

                for (int i = 0; i < joined.length(); i++) {
                    char ch = joined.charAt(i);
                    if (ch == '{') level++;
                    else if (ch == '}') level--;
                    if (level == 0 && ch == '}') {
                        end = i;
                        break;
                    }
                }

                if (end != -1) {
                    String fragment = joined.substring(0, end + 1);
                    boolean success = tryDispatch(fragment);

                    String remaining = joined.substring(end + 1);
                    if (remaining.contains("{")) {
                        String nextStart = remaining.substring(remaining.indexOf("{"));
                        receiving = true;
                        bracketCount = countBrackets(nextStart);
                        buffer.setLength(0);
                        buffer.append(nextStart);
                    } else {
                        if (!success) reset();
                    }
                }
            }
        }
    }

    private boolean tryDispatch(String json) {
        try {
            new JSONObject(json); // 验证 JSON
            Log.d(TAG, "成功解析JSON");
            if (listener != null) listener.onJsonComplete(json);
            reset();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "JSON解析失败: " + e.getMessage());
            return false;
        }
    }

    private int countBrackets(String s) {
        int count = 0;
        for (char ch : s.toCharArray()) {
            if (ch == '{') count++;
            else if (ch == '}') count--;
        }
        return count;
    }

    private void restartTimeoutTimer() {
        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, timeoutMs);
        Log.d(TAG, "启动超时计时器: " + timeoutMs + "ms");
    }

    /**
     * 重置 JSON 接收状态
     * 在开始发送数据包等场景下调用，清理未完成的 JSON 接收
     */
    public void reset() {
        buffer.setLength(0);
        bracketCount = 0;
        receiving = false;
        timeoutHandler.removeCallbacks(timeoutRunnable);
        Log.d(TAG, "JSON状态已重置");
    }

    /**
     * 是否正在接收 JSON 数据
     * @return true 表示正在接收分片的 JSON
     */
    public boolean isReceiving() {
        return receiving;
    }

    private String preview(String str) {
        return str.length() > 50 ? str.substring(0, 50) + "..." : str;
    }
}


