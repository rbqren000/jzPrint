package com.mx.mxSdk.Conditions;

import android.app.Activity;

public class ConditionCheckerImpl implements ConditionChecker {
    private final ConditionManager conditionManager;
    private final ConditionAction conditionAction;

    public ConditionCheckerImpl(ConditionManager conditionManager, ConditionAction conditionAction) {
        this.conditionManager = conditionManager;
        this.conditionAction = conditionAction;
    }

    @Override
    public ConditionAction getConditionAction() {
        return conditionAction;
    }

    @Override
    public void checkCondition(Activity activity) {
        if (conditionAction != null) {
            if (!conditionAction.isConditionMet(activity)) {
                conditionAction.requestCondition(activity, conditionManager);
            } else {
                conditionManager.onConditionResult(activity,conditionAction.getKey(), true);
            }
        }
    }

}





