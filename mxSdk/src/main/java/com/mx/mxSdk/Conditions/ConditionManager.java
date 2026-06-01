package com.mx.mxSdk.Conditions;

import android.app.Activity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConditionManager {
    private final List<ConditionChecker> checkers = new ArrayList<>();
    private final Map<String, Boolean> conditionStatus = new HashMap<>();
    private ConditionCallback callback;

    public synchronized void addChecker(ConditionChecker checker) {
        if (checkers.contains(checker)){
            return;
        }
        checkers.add(checker);
        conditionStatus.put(checker.getConditionAction().getKey(), false); // 初始化状态为未处理
    }

    public synchronized void checkConditions(Activity activity, ConditionCallback callback) {
        this.callback = callback;
        // 检查所有权限
        for (ConditionChecker checker : checkers) {
            if (checker.getConditionAction().isConditionMet(activity)) {
                conditionStatus.put(checker.getConditionAction().getKey(), true); // 标记为已通过
                checker.getConditionAction().onConditionMet();
            } else {
                checker.getConditionAction().requestCondition(activity, this); // 请求权限
            }
        }

        // 如果所有权限都已通过，直接回调成功
        if (allConditionsProcessed(activity)) {
            notifyCallback();
        }
    }

    public synchronized void onConditionResult(Activity activity,String key, boolean granted) {
        conditionStatus.put(key, granted); // 更新权限状态
        for (ConditionChecker checker:checkers){
            if (checker.getConditionAction().getKey().equals(key)){
                if (granted){
                    checker.getConditionAction().onConditionMet();
                }
                return;
            }
        }
        // 检查是否所有权限都已处理
        if (allConditionsProcessed(activity)) {
            notifyCallback();
        }
    }

    private synchronized void olayCheckWithSetConditions(Activity activity) {
        // 检查所有权限
        for (ConditionChecker checker : checkers) {
            if (checker.getConditionAction().isConditionMet(activity)) {
                conditionStatus.put(checker.getConditionAction().getKey(), true); // 标记为已通过
            }
        }
    }

    public synchronized boolean allConditionsProcessed(Activity activity) {
        olayCheckWithSetConditions(activity);
        for (Boolean status : conditionStatus.values()) {
//            RBQLog.i("条件"+(status?"YES":"NO"));
            if (!status) {
                // 还有未处理的权限
                return false;
            }
        }
        return true;
    }

    private synchronized void notifyCallback() {
        if (callback == null) return;
        // 收集被拒绝的条件 Key
        List<String> unmetConditions = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : conditionStatus.entrySet()) {
            if (!entry.getValue()) {
                unmetConditions.add(entry.getKey());
            }
        }
        // 根据条件状态触发回调
        if (unmetConditions.isEmpty()) {
            callback.onAllConditionsMet();
        } else {
            callback.onConditionsUnmet(unmetConditions);
        }
    }
}












