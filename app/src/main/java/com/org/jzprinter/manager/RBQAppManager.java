package com.org.jzprinter.manager;

/**
 * Created by rbq on 2016/11/1.
 */
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.mx.mxSdk.ConnectManager;
import com.mx.mxSdk.Utils.RBQLog;
import com.tencent.bugly.crashreport.CrashReport;
import com.org.jzprinter.database.AppDatabase;
import com.org.jzprinter.database.dao.PrintTaskDao;
import com.org.jzprinter.repository.PrintProgressRepository;
import com.org.jzprinter.repository.PrintTaskRepository;
import com.org.jzprinter.print.MaterialLoader;
import com.org.jzprinter.print.PrintEngine;
import com.org.jzprinter.print.PrintConfig;
import com.org.jzprinter.print.PrintProgressManager;
import com.org.jzprinter.print.TaskRecoveryManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Executors;
import androidx.annotation.NonNull;
import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class RBQAppManager extends Application {

    public static final String TAG = RBQAppManager.class.getSimpleName();

    private boolean serviceStarted;
    private boolean serviceConnected;
    private static RBQAppManager This;

    /** 存放Activity栈 */
    public static Stack<Activity> mActivityStack = new Stack<Activity>();
    private long c_id = -1;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    //主线程执行
    public void runOnUiThread(Runnable runnable) {
        //判断如果在主线程，则直接执行，如果不在主线程则在主线程执行
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    // 文件操作完成监听器（移动、复制、删除等操作后通知刷新）
    public interface OnFileOperationListener {
        void onFileOperationComplete();
    }
    private final List<OnFileOperationListener> onFileOperationListeners = new ArrayList<>();
    
    public void registerFileOperationListener(OnFileOperationListener listener) {
        if (!onFileOperationListeners.contains(listener)) {
            onFileOperationListeners.add(listener);
        }
    }
    
    public void unregisterFileOperationListener(OnFileOperationListener listener) {
        onFileOperationListeners.remove(listener);
    }
    
    public void notifyFileOperationComplete() {
        runOnUiThread(() -> {
            for (OnFileOperationListener listener : onFileOperationListeners) {
                listener.onFileOperationComplete();
            }
        });
    }

    protected final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            onAppServiceConnected(name, service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            onAppServiceDisconnected(name);
        }
    };
    
    public long getC_id() {
        return c_id;
    }

    public Boolean isReadyForUploadDownload(){
        return true;
    }
    
    /** 隐藏构造方法 */
    public RBQAppManager() {
        super();
    }
    
    @SuppressLint("RestrictedApi")
    @Override
    public void onCreate() {
        super.onCreate();
        This = this;
        Log.i(TAG, "onCreate: 应用创建");

        //初始化Bugly崩溃统计
        CrashReport.initCrashReport(getApplicationContext(), "64c6b86b2a", false);
        CustomActivityOnCrash.install(getApplicationContext());
        
        //初始化打印机连接管理器
        ConnectManager.share().destroy();
        ConnectManager.share().init(this);
        AutoConnectManager.share().init(this);
        
        //初始化数据库和打印引擎
        initPrintEngine();

        //清理打印临时文件
        PrintTempFileManager.getInstance().cleanAllTempFiles(this);
    }

    private void initPrintEngine() {
        AppDatabase db = AppDatabase.getInstance(this);
        PrintTaskDao taskDao = db.printTaskDao();
        PrintProgressRepository progressRepo = db.printProgressRepository();
        PrintTaskRepository taskRepo = new PrintTaskRepository(taskDao);
        MaterialLoader materialLoader = new MaterialLoader();

        PrintEngine engine = PrintEngine.init(this, taskRepo, materialLoader);
        engine.setOddPageOnRight(PrintConfig.isOddPageOnRight(this));

        PrintProgressManager progressManager = new PrintProgressManager(
            taskRepo, progressRepo, engine.getDbExecutor());
        engine.setProgressManager(progressManager);

        TaskRecoveryManager recoveryManager = new TaskRecoveryManager(taskRepo);
        recoveryManager.recoverOnStartup();
    }
    /**
     * 启动LightService
     *
     * @param clazz 要启动的service
     */
    public void startAppService(Class<? extends RBQAppService> clazz) {

        if (this.serviceStarted || this.serviceConnected)
            return;

        this.serviceStarted = true;

        Intent service = new Intent(this, clazz);
        this.bindService(service, this.mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    /**
     * 停止LightService
     */
    public void stopAppService() {

        if (!this.serviceStarted)
            return;

        this.serviceStarted = false;

        if (this.serviceConnected) {
            this.unbindService(this.mServiceConnection);
        }
    }

    protected void onAppServiceConnected(ComponentName name, IBinder service) {

        RBQLog.i("service connected --> " + name.getShortClassName());
        this.serviceConnected = true;
//        this.dispatchEvent(ServiceEvent.newInstance(this, ServiceEvent.SERVICE_CONNECTED, service));
    }

    protected void onAppServiceDisconnected(ComponentName name) {

        RBQLog.i("service disconnected --> " + name.getShortClassName());
        this.serviceConnected = false;
//        this.dispatchEvent(ServiceEvent.newInstance(this, ServiceEvent.SERVICE_DISCONNECTED, null));
    }

    @Override
    public void onTerminate() {
        // 程序终止的时候执行
        Log.d(TAG, "onTerminate");
        super.onTerminate();
        ConnectManager.share().destroy();
        this.stopAppService();
        this.serviceStarted = false;
        this.serviceConnected = false;

    }

    @Override
    public void onLowMemory() {
        // 低内存的时候执行
        Log.d(TAG, "onLowMemory");
        super.onLowMemory();
    }
    @Override
    public void onTrimMemory(int level) {
        // 程序在内存清理的时候执行
        Log.d(TAG, "onTrimMemory");
        super.onTrimMemory(level);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

    public static RBQAppManager share() {
        return This;
    }

    /**
     * 获取Activity栈
     *
     * @return
     */
    public Stack<Activity> getActivityStack() {

        return mActivityStack;
    }

    /**
     * 添加Activity
     *
     * @param activity
     */
    public void addActivity(Activity activity) {

        mActivityStack.push(activity);
    }

    /**
     * 杀死指定的Activity
     *
     * @param activity
     */
    public void killActivity(Activity activity) {

        mActivityStack.remove(activity);
        activity.finish();
    }

    /**
     * 杀死指定的Activity
     *
     * @param className 目标Activity的ClassName
     */
    public void killActivity(String className) {
        if (TextUtils.isEmpty(className)) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(className);
            killActivity(clazz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 杀死指定的Activity
     *
     * @param clazz 目标Activity的Class
     */
    public void killActivity(Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        killActivityBySimpleName(clazz.getSimpleName());
    }

    /**
     * 杀死指定的Activity
     *
     * @param simpleName 目标Activity的SimpleName
     */
    public void killActivityBySimpleName(String simpleName) {
        if (TextUtils.isEmpty(simpleName)) {
            return;
        }
        for (Activity activity : mActivityStack) {
            if (activity.getClass().getSimpleName().equals(simpleName)) {
                killActivity(activity);
            }
        }
    }


    /**
     * 返回到指定的Activity，如果Activity栈中不存在该Activity，则不会操作
     *
     * @param clazz 目标Activity的Class
     */
    public void navigateToActivity(Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        navigateToActivityBySimpleName(clazz.getSimpleName());
    }

    /**
     * 返回到指定的Activity，如果Activity栈中不存在该Activity，则不会操作
     *
     * @param className 目标Activity的ClassName
     */
    public void navigateToActivity(String className) {
        if (TextUtils.isEmpty(className)) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(className);
            navigateToActivity(clazz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回到指定的Activity，如果Activity栈中不存在该Activity，则不会操作
     *
     * @param simpleName 目标Activity的SimpleName
     */
    public void navigateToActivityBySimpleName(String simpleName) {
        if (TextUtils.isEmpty(simpleName) || mActivityStack.isEmpty()) {
            return;
        }
        Iterator<Activity> iterator = mActivityStack.iterator();
        boolean foundTarget = false;
        while (iterator.hasNext()) {
            Activity activity = iterator.next();
            if (activity.getClass().getSimpleName().equals(simpleName)) {
                foundTarget = true;
                break;
            }
        }
        if (!foundTarget) {
            return; // 目标Activity不存在，直接返回
        }
        // 清理目标Activity之上的所有Activity
        while (!mActivityStack.isEmpty()) {
            Activity topActivity = mActivityStack.peek();
            if (topActivity.getClass().getSimpleName().equals(simpleName)) {
                break; // 遇到目标Activity，停止清理
            } else {
                topActivity.finish();
                mActivityStack.pop();
            }
        }
    }



    /**
     * 返回到指定的Activity，如果Activity栈中不存在该Activity，则不会操作
     *
     * @param className 目标Activity的ClassName
     */
    public void killCurrentWithNavigateToActivity(String className) {
        if (TextUtils.isEmpty(className)) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(className);
            killCurrentWithNavigateToActivity(clazz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回到指定的Activity，如果Activity栈中不存在该Activity，则不会操作
     *
     * @param clazz 目标Activity的Class
     */
    public void killCurrentWithNavigateToActivity(Class<?> clazz) {
        if (clazz == null){
            return;
        }
        killCurrentWithNavigateToActivityBySimpleName(clazz.getSimpleName());
    }

    /**
     * 返回到指定的Activity，如果Activity栈中不存在该Activity，则不会操作
     *
     * @param simpleName 目标Activity的SimpleName
     */
    public void killCurrentWithNavigateToActivityBySimpleName(String simpleName) {
        if (TextUtils.isEmpty(simpleName)) {
            // 如果传入的参数为null，则结束当前的Activity
            mActivityStack.peek().finish();
            mActivityStack.pop();
            return;
        }

        Iterator<Activity> iterator = mActivityStack.iterator();
        boolean foundTarget = false;
        while (iterator.hasNext()) {
            Activity activity = iterator.next();
            if (activity.getClass().getSimpleName().equals(simpleName)) {
                foundTarget = true;
                break;
            }
        }
        if (!foundTarget) {
            return; // 目标Activity不存在，直接返回
        }
        // 清理目标Activity之上的所有Activity
        while (!mActivityStack.isEmpty()) {
            Activity topActivity = mActivityStack.peek();
            if (topActivity.getClass().getSimpleName().equals(simpleName)) {
                break; // 遇到目标Activity，停止清理
            } else {
                topActivity.finish();
                mActivityStack.pop();
            }
        }
    }


    /**
     * 杀死当前Activity并返回到包含传入参数的Activity类型，并清理目标Activity及其之上的所有Activity
     *
     * @param clazz 目标Activity的Class
     */
    public void killCurrentToTargetWithTarget(Class<?> clazz) {
        if (clazz == null){
            return;
        }
        String simpleName = clazz.getSimpleName();
        killCurrentToTargetWithTargetBySimpleName(simpleName);
    }

    /**
     * 杀死当前Activity并返回到包含传入参数的Activity类名，并清理目标Activity及其之上的所有Activity
     *
     * @param className 目标Activity的ClassName
     */
    public void killCurrentToTargetWithTarget(String className) {
        if (TextUtils.isEmpty(className)) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(className);
            killCurrentToTargetWithTarget(clazz);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 杀死当前Activity并返回到包含传入参数的Activity简单类名，并清理目标Activity及其之上的所有Activity
     *
     * @param simpleName 目标Activity的SimpleName
     */
    public void killCurrentToTargetWithTargetBySimpleName(String simpleName) {
        if (TextUtils.isEmpty(simpleName)) {
            mActivityStack.peek().finish();
            mActivityStack.pop();
            return;
        }
        Iterator<Activity> iterator = mActivityStack.iterator();
        boolean foundTarget = false;
        while (iterator.hasNext()) {
            Activity activity = iterator.next();
            if (activity.getClass().getSimpleName().equals(simpleName)) {
                foundTarget = true;
                break;
            }
        }
        if (!foundTarget) {
            return; // 目标Activity不存在，直接返回
        }
        // 清理目标Activity及其之上的所有Activity
        while (!mActivityStack.isEmpty()) {
            Activity topActivity = mActivityStack.peek();
            if (topActivity.getClass().getSimpleName().equals(simpleName)) {
                topActivity.finish(); // 清理目标Activity
                mActivityStack.pop();
                break; // 遇到目标Activity，停止清理
            } else {
                topActivity.finish();
                mActivityStack.pop();
            }
        }
    }


    /**
     * 获取栈顶的Activity
     *
     * @return
     */
    public Activity getTopActivity() {
        return mActivityStack.peek();
    }

    /**
     * 杀死栈顶Activity
     */
    public void killTopActivity() {
        killActivity(getTopActivity());
    }

    /**
     * 杀死栈中所有activity
     */

    public static void killAllActivity() {
        while (!mActivityStack.isEmpty()) {
            Activity activity = mActivityStack.pop();
            activity.finish();
            System.out.println(activity.getClass().getSimpleName() + "is finishing...");
        }
    }

    /**
     * 退出应用
     */

    public void exit(Context context) {
        killAllActivity();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager==null) return;
        activityManager.killBackgroundProcesses(context.getPackageName());
    }
    
    public void setCId(long c_id) {
        this.c_id = c_id;
    }
    
    //是否控制，不登录能否操作文件（税票打印不需要登录限制）
    public boolean getEnableFileOpt() {
        return true;
    }
}

