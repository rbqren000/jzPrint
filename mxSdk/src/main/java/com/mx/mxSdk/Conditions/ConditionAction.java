package com.mx.mxSdk.Conditions;

import android.app.Activity;

public interface ConditionAction {
    String getKey();
    boolean isConditionMet(Activity activity);
    void onConditionMet();
    void requestCondition(Activity activity, ConditionManager conditionManager);
}





