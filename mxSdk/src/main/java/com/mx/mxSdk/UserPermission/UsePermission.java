package com.mx.mxSdk.UserPermission;

import java.util.ArrayList;
import java.util.List;

public class UsePermission implements UsePermissionObservable {
    //观察者列表
    private final List<UsePermissionObserver> observers;
    //变量值
    private boolean allowUse;

    //构造方法，初始化观察者列表和变量值
    public UsePermission(boolean allowUse) {
        this.observers = new ArrayList<>();
        this.allowUse = allowUse;
    }

    //获取变量值
    public boolean isAllowUse() {
        return allowUse;
    }

    //设置变量值，并通知观察者
    public void setAllowUse(boolean allowUse) {
        this.allowUse = allowUse;
        notifyObservers();
    }

    //注册观察者
    @Override
    public void registerObserver(UsePermissionObserver observer) {
        observers.add(observer);
    }

    //注销观察者
    @Override
    public void unregisterObserver(UsePermissionObserver observer) {
        observers.remove(observer);
    }

    //通知观察者
    @Override
    public void notifyObservers() {
        for (UsePermissionObserver observer : observers) {
            observer.onUsePermissionChange(this);
        }
    }

}
