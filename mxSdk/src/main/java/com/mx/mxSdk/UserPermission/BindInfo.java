package com.mx.mxSdk.UserPermission;

import java.util.ArrayList;
import java.util.List;

public class BindInfo implements BindInfoObservable {
    //观察者列表
    private final List<BindInfoObserver> observers;
    //变量值
    private boolean bindState;
    private long id;
    private String userName;
    private String password;

    //构造方法，初始化观察者列表和变量值
    public BindInfo(boolean bindState, long id, String userName, String password) {
        this.observers = new ArrayList<>();
        this.bindState = bindState;
        this.id = id;
        this.userName = userName;
        this.password = password;
    }

    //获取变量值
    public boolean isBindState() {
        return bindState;
    }

    public long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    //设置变量值，并通知观察者
    public void setBindState(boolean bindState) {
        this.bindState = bindState;
        notifyObservers();
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    //注册观察者
    @Override
    public void registerObserver(BindInfoObserver observer) {
        observers.add(observer);
    }

    //注销观察者
    @Override
    public void unregisterObserver(BindInfoObserver observer) {
        observers.remove(observer);
    }

    //通知观察者
    @Override
    public void notifyObservers() {
        for (BindInfoObserver observer : observers) {
            observer.onBindInfoChange(this);
        }
    }

}
