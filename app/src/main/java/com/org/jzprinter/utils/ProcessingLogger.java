package com.org.jzprinter.utils;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 图像处理日志工具类（线程安全版本）
 * 
 * 提供结构化的日志记录,用于追踪票据渲染和打印流程中的:
 * - 配置信息 (CONFIG)
 * - 方法调用流程 (FLOW)
 * - 处理阶段数据 (STAGE)
 * - 性能指标 (PERF)
 * - 数据统计 (DATA)
 * 
 * 线程安全:所有公共方法都是线程安全的,可在多线程环境中使用。
 * 
 * 使用示例:
 * <pre>
 * ProcessingLogger.startSession("renderTicket");
 * ProcessingLogger.config("templateName", "tax_ticket_001");
 * ProcessingLogger.enterMethod("TicketRenderer.render");
 * // ... 处理逻辑
 * ProcessingLogger.exitMethod();
 * ProcessingLogger.stage("渲染完成", "width=4826px, height=3518px");
 * ProcessingLogger.endSession();
 * </pre>
 * 
 * Created by RBQ on 2025
 */
public class ProcessingLogger {
    
    private static final String TAG = "ProcessingLogger";
    
    // ======================== 线程安全锁 ========================
    private static final ReentrantLock lock = new ReentrantLock();
    
    // ======================== 配置开关 ========================
    
    /** 主开关:是否启用日志 */
    private static volatile boolean enabled = true;

    /** 是否记录配置信息 */
    private static volatile boolean logConfig = true;
    
    /** 是否记录方法流程 */
    private static volatile boolean logFlow = true;
    
    /** 是否记录阶段数据 */
    private static volatile boolean logStage = true;
    
    /** 是否记录性能指标 */
    private static volatile boolean logPerf = true;
    
    /** 是否记录详细数据统计 */
    private static volatile boolean logData = true;
    
    /** 是否在会话结束时输出汇总报告 */
    private static volatile boolean outputSummary = true;
    
    /** 是否实时输出日志(每条日志立即打印到 Logcat) */
    private static volatile boolean realtimeOutput = true;
    
    /** 日志缓存最大长度(字符数),超过后自动截断旧日志 */
    private static final int MAX_SESSION_LOG_SIZE = 100_000;
    
    // ======================== 会话状态 ========================
    
    /** 当前会话ID */
    private static String currentSessionId = null;
    
    /** 会话开始时间 */
    private static long sessionStartTime = 0;
    
    /** 方法调用栈(用于追踪嵌套调用) */
    private static final Deque<MethodEntry> methodStack = new ArrayDeque<>();
    
    /** 阶段计时器 */
    private static final Map<String, Long> stageTimers = new LinkedHashMap<>();
    
    /** 阶段耗时记录 */
    private static final Map<String, Long> stageDurations = new LinkedHashMap<>();
    
    /** 配置快照 */
    private static final Map<String, String> configSnapshot = new LinkedHashMap<>();
    
    /** 阶段日志缓存(用于汇总报告) */
    private static final StringBuilder sessionLog = new StringBuilder();
    
    // ======================== 日志类型标签 ========================
    
    private static final String PREFIX_CONFIG = "[CONFIG]";
    private static final String PREFIX_FLOW = "[FLOW]";
    private static final String PREFIX_STAGE = "[STAGE]";
    private static final String PREFIX_PERF = "[PERF]";
    private static final String PREFIX_DATA = "[DATA]";
    private static final String PREFIX_ERROR = "[ERROR]";
    
    // ======================== 方法入口记录 ========================
    
    /**
     * 方法调用栈条目
     */
    private static class MethodEntry {
        final String methodName;
        final long startTime;
        final long startMemory;
        
        MethodEntry(String methodName) {
            this.methodName = methodName;
            this.startTime = System.currentTimeMillis();
            this.startMemory = getUsedMemory();
        }
    }

    
    // ======================== 公共API ========================
    
    /**
     * 开始一个处理会话
     * @param sessionName 会话名称(如 "renderTicket")
     */
    public static void startSession(String sessionName) {
        if (!enabled) return;
        
        lock.lock();
        try {
            // 清理上一个会话的状态
            methodStack.clear();
            stageTimers.clear();
            stageDurations.clear();
            configSnapshot.clear();
            sessionLog.setLength(0);
            
            currentSessionId = sessionName + "_" + System.currentTimeMillis();
            sessionStartTime = System.currentTimeMillis();
            
            String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
            String header = String.format("========== 处理会话开始 ==========\n" +
                                          "会话: %s\n" +
                                          "时间: %s\n" +
                                          "==================================",
                                          sessionName, timestamp);
            if (realtimeOutput) {
                Log.i(TAG, header);
            }
            appendToSessionLogInternal(header);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 结束当前处理会话,输出汇总报告
     */
    public static void endSession() {
        if (!enabled) return;
        
        lock.lock();
        try {
            if (currentSessionId == null) {
                Log.w(TAG, "endSession() 调用时没有活跃会话");
                return;
            }
            
            long totalDuration = System.currentTimeMillis() - sessionStartTime;
            
            // 构建汇总报告
            StringBuilder summary = new StringBuilder();
            summary.append("\n---------- 会话汇总 ----------\n");
            
            // 配置摘要
            if (!configSnapshot.isEmpty()) {
                summary.append("【配置】\n");
                for (Map.Entry<String, String> entry : configSnapshot.entrySet()) {
                    summary.append(String.format("  %s: %s\n", entry.getKey(), entry.getValue()));
                }
            }
            
            // 性能摘要
            if (!stageDurations.isEmpty()) {
                summary.append("【耗时】\n");
                for (Map.Entry<String, Long> entry : stageDurations.entrySet()) {
                    summary.append(String.format("  %s: %s\n", entry.getKey(), formatDuration(entry.getValue())));
                }
            }
            
            summary.append(String.format("【总计】 %s, 内存: %s\n", formatDuration(totalDuration), formatMemory(getUsedMemory())));
            summary.append("------------------------------");
            
            // 追加到会话日志
            appendToSessionLogInternal(summary.toString());
            
            String footer = String.format("\n========== 处理会话结束 (总耗时: %s) ==========", formatDuration(totalDuration));
            appendToSessionLogInternal(footer);
            
            // 输出完整会话日志(一条语句,方便复制)
            Log.i(TAG, "\n\n" + SESSION_LOG_MARKER_START + "\n" + sessionLog.toString() + "\n" + SESSION_LOG_MARKER_END + "\n");
            
            currentSessionId = null;
        } finally {
            lock.unlock();
        }
    }
    
    /** 会话日志标记(方便在 Logcat 中定位和复制) */
    private static final String SESSION_LOG_MARKER_START = "=== PROCESSING_LOG_START ===";
    private static final String SESSION_LOG_MARKER_END = "=== PROCESSING_LOG_END ===";
    
    /**
     * 检查是否有活跃会话
     */
    public static boolean hasActiveSession() {
        lock.lock();
        try {
            return currentSessionId != null;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 记录配置参数
     * @param key 配置项名称
     * @param value 配置值
     */
    public static void config(String key, Object value) {
        if (!enabled || !logConfig) return;
        
        lock.lock();
        try {
            String valueStr = String.valueOf(value);
            configSnapshot.put(key, valueStr);
            
            String msg = String.format("%s %s = %s", PREFIX_CONFIG, key, valueStr);
            if (realtimeOutput) Log.d(TAG, msg);
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 批量记录配置参数
     * @param configs 配置键值对
     */
    public static void configBatch(Map<String, Object> configs) {
        if (!enabled || !logConfig || configs == null || configs.isEmpty()) return;
        
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(PREFIX_CONFIG).append(" 配置快照:\n");
            
            for (Map.Entry<String, Object> entry : configs.entrySet()) {
                String valueStr = String.valueOf(entry.getValue());
                configSnapshot.put(entry.getKey(), valueStr);
                sb.append("  ").append(entry.getKey()).append(" = ").append(valueStr).append("\n");
            }
            
            String msg = sb.toString().trim();
            if (realtimeOutput) Log.d(TAG, msg);
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }

    
    /**
     * 进入方法(记录方法调用)
     * @param methodName 方法全名(如 "TicketRenderer.render")
     */
    public static void enterMethod(String methodName) {
        if (!enabled || !logFlow) return;
        
        lock.lock();
        try {
            int depth = methodStack.size();
            String indent = getIndent(depth);
            
            methodStack.push(new MethodEntry(methodName));
            
            String msg = String.format("%s %s→ %s", PREFIX_FLOW, indent, methodName);
            if (realtimeOutput) Log.d(TAG, msg);
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 进入方法(带参数信息)
     * @param methodName 方法名
     * @param params 关键参数描述
     */
    public static void enterMethod(String methodName, String params) {
        if (!enabled || !logFlow) return;
        
        lock.lock();
        try {
            int depth = methodStack.size();
            String indent = getIndent(depth);
            
            methodStack.push(new MethodEntry(methodName));
            
            String msg = String.format("%s %s→ %s(%s)", PREFIX_FLOW, indent, methodName, params);
            if (realtimeOutput) Log.d(TAG, msg);
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 退出当前方法
     */
    public static void exitMethod() {
        if (!enabled || !logFlow) return;
        
        lock.lock();
        try {
            if (methodStack.isEmpty()) {
                Log.w(TAG, "exitMethod() 调用时方法栈为空");
                return;
            }
            
            MethodEntry entry = methodStack.pop();
            long duration = System.currentTimeMillis() - entry.startTime;
            long memoryDelta = getUsedMemory() - entry.startMemory;
            
            int depth = methodStack.size();
            String indent = getIndent(depth);
            
            String msg = String.format("%s %s← %s [%s, 内存%s]", 
                    PREFIX_FLOW, indent, entry.methodName, 
                    formatDuration(duration), formatMemory(memoryDelta));
            if (realtimeOutput) Log.d(TAG, msg);
            appendToSessionLogInternal(msg);
            
            // 记录到性能统计
            stageDurations.put(entry.methodName, duration);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 退出方法并附带结果信息
     * @param result 方法执行结果描述
     */
    public static void exitMethod(String result) {
        if (!enabled || !logFlow) return;
        
        lock.lock();
        try {
            if (methodStack.isEmpty()) {
                Log.w(TAG, "exitMethod() 调用时方法栈为空");
                return;
            }
            
            MethodEntry entry = methodStack.pop();
            long duration = System.currentTimeMillis() - entry.startTime;
            
            int depth = methodStack.size();
            String indent = getIndent(depth);
            
            String msg = String.format("%s %s← %s [%s] => %s", 
                    PREFIX_FLOW, indent, entry.methodName, formatDuration(duration), result);
            if (realtimeOutput) Log.d(TAG, msg);
            appendToSessionLogInternal(msg);
            
            stageDurations.put(entry.methodName, duration);
        } finally {
            lock.unlock();
        }
    }

    
    /**
     * 记录处理阶段信息
     * @param stageName 阶段名称
     * @param description 阶段描述/结果
     */
    public static void stage(String stageName, String description) {
        if (!enabled || !logStage) return;
        
        lock.lock();
        try {
            String msg = String.format("%s [%s] %s", PREFIX_STAGE, stageName, description);
            if (realtimeOutput) Log.i(TAG, msg);
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 开始计时一个阶段
     * @param stageName 阶段名称
     */
    public static void startStage(String stageName) {
        if (!enabled || !logPerf) return;
        
        lock.lock();
        try {
            stageTimers.put(stageName, System.currentTimeMillis());
            
            String msg = String.format("%s ▶ %s 开始", PREFIX_PERF, stageName);
            if (realtimeOutput) Log.d(TAG, msg);
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 结束计时一个阶段
     * @param stageName 阶段名称
     */
    public static void endStage(String stageName) {
        if (!enabled || !logPerf) return;
        
        lock.lock();
        try {
            Long startTime = stageTimers.remove(stageName);
            if (startTime == null) {
                Log.w(TAG, "阶段 '" + stageName + "' 未找到开始时间,可能未调用 startStage()");
                return;
            }
            
            long duration = System.currentTimeMillis() - startTime;
            stageDurations.put(stageName, duration);
            
            String msg = String.format("%s ■ %s 完成 [%s]", PREFIX_PERF, stageName, formatDuration(duration));
            if (realtimeOutput) Log.i(TAG, msg);
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 记录性能指标
     * @param metric 指标名称
     * @param value 指标值
     * @param unit 单位
     */
    public static void perf(String metric, long value, String unit) {
        if (!enabled || !logPerf) return;
        
        lock.lock();
        try {
            String msg = String.format("%s %s: %d %s", PREFIX_PERF, metric, value, unit);
            if (realtimeOutput) Log.i(TAG, msg);
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 记录数据统计(通道数据)
     */
    public static void channelStats(String tag, String channelName, int min, int max, int avg, int nonZeroCount) {
        if (!enabled || !logData) return;
        
        lock.lock();
        try {
            String msg = String.format("%s [%s] %s: min=%d, max=%d, avg=%d, nonzero=%d", 
                    PREFIX_DATA, tag, channelName, min, max, avg, nonZeroCount);
            if (realtimeOutput) Log.d(TAG, msg);
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 记录CMYK四通道统计
     * @param tag 标签(如 "ICC转换后")
     * @param cData C通道数据
     * @param mData M通道数据
     * @param yData Y通道数据
     * @param kData K通道数据
     */
    public static void cmykStats(String tag, byte[] cData, byte[] mData, byte[] yData, byte[] kData) {
        if (!enabled || !logData) return;
        
        lock.lock();
        try {
            int[] cStats = calcStats(cData);
            int[] mStats = calcStats(mData);
            int[] yStats = calcStats(yData);
            int[] kStats = calcStats(kData);
            
            StringBuilder sb = new StringBuilder();
            sb.append(PREFIX_DATA).append(" [").append(tag).append("] CMYK通道统计:\n");
            sb.append(String.format("  C: min=%3d, max=%3d, avg=%3d, nonzero=%d\n", cStats[0], cStats[1], cStats[2], cStats[3]));
            sb.append(String.format("  M: min=%3d, max=%3d, avg=%3d, nonzero=%d\n", mStats[0], mStats[1], mStats[2], mStats[3]));
            sb.append(String.format("  Y: min=%3d, max=%3d, avg=%3d, nonzero=%d\n", yStats[0], yStats[1], yStats[2], yStats[3]));
            sb.append(String.format("  K: min=%3d, max=%3d, avg=%3d, nonzero=%d", kStats[0], kStats[1], kStats[2], kStats[3]));
            
            String msg = sb.toString();
            if (realtimeOutput) Log.i(TAG, msg);
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }

    
    /**
     * 记录覆盖率统计
     */
    public static void coverageStats(float cCoverage, float mCoverage, float yCoverage, float kCoverage) {
        if (!enabled || !logData) return;
        
        lock.lock();
        try {
            float total = cCoverage + mCoverage + yCoverage + kCoverage;
            String msg = String.format("%s 半色调覆盖率: C=%.1f%%, M=%.1f%%, Y=%.1f%%, K=%.1f%%, 总计=%.1f%%",
                    PREFIX_DATA, cCoverage, mCoverage, yCoverage, kCoverage, total);
            if (realtimeOutput) Log.i(TAG, msg);
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 记录TAC信息
     */
    public static void tacStats(float maxTac, float tacLimit, int overLimitPixels, int totalPixels) {
        if (!enabled || !logData) return;
        
        lock.lock();
        try {
            float overLimitPercent = totalPixels > 0 ? (overLimitPixels * 100.0f / totalPixels) : 0;
            String status = overLimitPixels > 0 ? "⚠ 超限" : "✓ 正常";
            
            String msg = String.format("%s TAC分析: 最大=%.0f%%, 限制=%.0f%%, 超限像素=%d (%.1f%%) %s",
                    PREFIX_DATA, maxTac, tacLimit, overLimitPixels, overLimitPercent, status);
            if (realtimeOutput) Log.i(TAG, msg);
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 记录错误信息(错误日志始终输出到 Logcat,不受 realtimeOutput 影响)
     * @param message 错误描述
     * @param e 异常对象(可为null)
     */
    public static void error(String message, Throwable e) {
        // 错误日志始终记录,不受 enabled 开关影响
        lock.lock();
        try {
            String msg = PREFIX_ERROR + " " + message;
            if (e != null) {
                msg += " | " + e.getClass().getSimpleName() + ": " + e.getMessage();
                Log.e(TAG, msg, e);  // 错误始终输出
            } else {
                Log.e(TAG, msg);  // 错误始终输出
            }
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 记录错误信息(无异常对象)
     * 错误日志始终输出到 Logcat,不受 realtimeOutput 影响
     * @param message 错误描述
     */
    public static void error(String message) {
        error(message, null);
    }
    
    /**
     * 记录普通信息 (INFO级别)
     * @param message 信息内容
     */
    public static void info(String message) {
        if (!enabled) return;
        
        lock.lock();
        try {
            if (realtimeOutput) Log.i(TAG, message);
            appendToSessionLogInternal(message);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 记录警告信息
     * @param message 警告描述
     */
    public static void warn(String message) {
        if (!enabled) return;
        
        lock.lock();
        try {
            String msg = "[WARN] " + message;
            if (realtimeOutput) Log.w(TAG, msg);
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 记录调试信息
     * @param message 调试信息
     */
    public static void debug(String message) {
        if (!enabled) return;
        
        lock.lock();
        try {
            if (realtimeOutput) Log.d(TAG, message);
            appendToSessionLogInternal(message);
        } finally {
            lock.unlock();
        }
    }
    
    // ======================== 配置方法 ========================
    
    /**
     * 设置是否启用日志
     */
    public static void setEnabled(boolean value) {
        enabled = value;
    }
    
    /**
     * 检查日志是否启用
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 设置各类日志的开关
     */
    public static void setLogLevels(boolean config, boolean flow, boolean stage, boolean perf, boolean data) {
        logConfig = config;
        logFlow = flow;
        logStage = stage;
        logPerf = perf;
        logData = data;
    }
    
    /**
     * 设置是否输出会话汇总
     */
    public static void setOutputSummary(boolean output) {
        outputSummary = output;
    }
    
    /**
     * 设置是否实时输出日志
     * 
     * @param realtime true: 每条日志立即输出到 Logcat(方便实时调试)
     *                 false: 日志仅缓存,在 endSession() 时一次性输出(方便复制完整日志)
     */
    public static void setRealtimeOutput(boolean realtime) {
        realtimeOutput = realtime;
    }
    
    /**
     * 检查是否实时输出日志
     */
    public static boolean isRealtimeOutput() {
        return realtimeOutput;
    }
    
    /**
     * 获取当前会话的完整日志
     * @return 会话日志字符串
     */
    public static String getSessionLog() {
        lock.lock();
        try {
            return sessionLog.toString();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 清除当前会话日志缓存
     */
    public static void clearSessionLog() {
        lock.lock();
        try {
            sessionLog.setLength(0);
        } finally {
            lock.unlock();
        }
    }

    
    // ======================== 内部方法 ========================
    
    /**
     * 追加到会话日志(线程安全版本,需要外部已持有锁)
     */
    private static void appendToSessionLogInternal(String msg) {
        if (sessionLog.length() > 0) {
            sessionLog.append("\n");
        }
        sessionLog.append(msg);
        
        // 检查日志大小,超过限制时截断前半部分
        if (sessionLog.length() > MAX_SESSION_LOG_SIZE) {
            int cutPoint = sessionLog.length() - MAX_SESSION_LOG_SIZE / 2;
            // 找到下一个换行符,避免截断在行中间
            int newlinePos = sessionLog.indexOf("\n", cutPoint);
            if (newlinePos > 0) {
                cutPoint = newlinePos + 1;
            }
            sessionLog.delete(0, cutPoint);
            sessionLog.insert(0, "[... 早期日志已截断 ...]\n");
        }
    }
    
    /**
     * 追加到会话日志(公共版本,自动加锁)
     */
    private static void appendToSessionLog(String msg) {
        lock.lock();
        try {
            appendToSessionLogInternal(msg);
        } finally {
            lock.unlock();
        }
    }
    
    private static String getIndent(int depth) {
        if (depth <= 0) return "";
        StringBuilder sb = new StringBuilder(depth * 2);
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }
    
    private static String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + "ms";
        } else if (ms < 60000) {
            return String.format(Locale.getDefault(), "%.2fs", ms / 1000.0);
        } else {
            long minutes = ms / 60000;
            long seconds = (ms % 60000) / 1000;
            return String.format(Locale.getDefault(), "%dm%ds", minutes, seconds);
        }
    }
    
    private static String formatMemory(long bytes) {
        if (bytes == 0) return "0";
        String sign = bytes > 0 ? "+" : "";
        long absBytes = Math.abs(bytes);
        if (absBytes < 1024) {
            return sign + bytes + "B";
        } else if (absBytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%s%.1fKB", sign, bytes / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%s%.1fMB", sign, bytes / (1024.0 * 1024.0));
        }
    }
    
    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * 计算数组统计信息
     * @return [min, max, avg, nonZeroCount]
     */
    private static int[] calcStats(byte[] data) {
        if (data == null || data.length == 0) {
            return new int[]{0, 0, 0, 0};
        }
        
        int min = data[0] & 0xFF;  // 修复:使用第一个元素初始化,并添加 & 0xFF
        int max = data[0] & 0xFF;
        long sum = 0;
        int nonZeroCount = 0;
        
        for (byte b : data) {
            int value = b & 0xFF;  // 必须添加 & 0xFF
            if (value < min) min = value;
            if (value > max) max = value;
            sum += value;
            if (value > 0) nonZeroCount++;
        }
        
        int avg = (int) (sum / data.length);
        return new int[]{min, max, avg, nonZeroCount};
    }

    
    /**
     * 输出会话汇总报告
     */
    private static void outputSessionSummary(long totalDuration) {
        StringBuilder summary = new StringBuilder();
        summary.append("\n---------- 会话汇总报告 ----------\n");
        
        // 配置摘要
        if (!configSnapshot.isEmpty()) {
            summary.append("【配置摘要】\n");
            int count = 0;
            for (Map.Entry<String, String> entry : configSnapshot.entrySet()) {
                if (count++ < 15) { // 显示前15个配置
                    summary.append(String.format("  %-25s: %s\n", 
                            truncate(entry.getKey(), 25), truncate(entry.getValue(), 40)));
                }
            }
            if (configSnapshot.size() > 15) {
                summary.append(String.format("  ... 还有 %d 项配置\n", configSnapshot.size() - 15));
            }
            summary.append("\n");
        }
        
        // 性能摘要
        if (!stageDurations.isEmpty()) {
            summary.append("【性能摘要】\n");
            long totalMethodTime = 0;
            for (Map.Entry<String, Long> entry : stageDurations.entrySet()) {
                summary.append(String.format("  %-40s: %10s\n", 
                        truncate(entry.getKey(), 40), formatDuration(entry.getValue())));
                totalMethodTime += entry.getValue();
            }
            summary.append(String.format("  %-40s: %10s\n", "方法耗时合计", formatDuration(totalMethodTime)));
            summary.append("\n");
        }
        
        // 总计
        summary.append("【总计】\n");
        summary.append(String.format("  总耗时: %s\n", formatDuration(totalDuration)));
        summary.append(String.format("  当前内存: %s\n", formatMemory(getUsedMemory())));
        summary.append("----------------------------------");
        
        Log.i(TAG, summary.toString());
    }
    
    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
}
