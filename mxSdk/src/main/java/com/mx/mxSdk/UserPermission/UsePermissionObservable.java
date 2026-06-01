package com.mx.mxSdk.UserPermission;

public interface UsePermissionObservable {
    //注册观察者
    void registerObserver(UsePermissionObserver observer);
    //注销观察者
    void unregisterObserver(UsePermissionObserver observer);
    //通知观察者
    void notifyObservers();
}
