package com.mx.mxSdk.UserPermission;

//定义一个观察者接口，声明更新方法
public interface UsePermissionObserver {
    //更新方法，接收被观察者作为参数
    void onUsePermissionChange(UsePermissionObservable observable);
}
