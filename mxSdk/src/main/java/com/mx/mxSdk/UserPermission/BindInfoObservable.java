package com.mx.mxSdk.UserPermission;

public interface BindInfoObservable {
    //注册观察者
    void registerObserver(BindInfoObserver observer);
    //注销观察者
    void unregisterObserver(BindInfoObserver observer);
    //通知观察者
    void notifyObservers();
}
