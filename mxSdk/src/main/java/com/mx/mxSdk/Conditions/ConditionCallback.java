package com.mx.mxSdk.Conditions;

import java.util.List;

public interface ConditionCallback {
    void onAllConditionsMet();
    void onConditionsUnmet(List<String> unmetConditions);
}


