package com.mx.mxSdk.Conditions;

import android.app.Activity;
import java.util.List;

public class MultiConditionAction implements ConditionAction {
    private final List<ConditionAction> actions;

    public MultiConditionAction(List<ConditionAction> actions) {
        this.actions = actions;
    }

    @Override
    public String getKey() {
        // 使用组合 key，便于标识
        return "MultiPermissionAction";
    }

    @Override
    public boolean isConditionMet(Activity activity) {
        // 遍历所有的 PermissionAction，如果有一个未通过，则返回 false
        for (ConditionAction action : actions) {
            if (!action.isConditionMet(activity)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onConditionMet() {

    }

    @Override
    public void requestCondition(Activity activity, ConditionManager conditionManager) {
        // 遍历所有未通过的 PermissionAction，并逐个请求权限
        for (ConditionAction action : actions) {
            if (!action.isConditionMet(activity)) {
                action.requestCondition(activity, conditionManager);
            }
        }
    }
}


