package com.mx.mxSdk;

public class DataObjContext {
    public DataObj dataObj;
    public DataObj.Callback callback;

    public DataObjContext(DataObj dataObj, DataObj.Callback callback) {
        this.callback = callback;
        this.dataObj = dataObj;
    }

    public void clear() {
        this.dataObj = null;
        this.callback = null;
    }
}
