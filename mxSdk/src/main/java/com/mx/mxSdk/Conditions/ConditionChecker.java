package com.mx.mxSdk.Conditions;

import android.app.Activity;

public interface ConditionChecker {
    ConditionAction getConditionAction();
    void checkCondition(Activity activity);
}


