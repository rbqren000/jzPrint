package com.mx.mxSdk;

import com.mx.mxSdk.DataStore.DataStorageImpl;

public class Errors extends DataStorageImpl<Error> {

    private static final Errors MX_ERRORS = new Errors();

    public Errors() {
        init();
    }

    public static Errors Instance(){

        return MX_ERRORS;
    }

    private void init(){

        Error error101 = new Error(101,"CRC校验失败");
        Error error102 = new Error(102,"数据包无效");
        Error error103 = new Error(103,"命令码无效");
        Error error104 = new Error(104,"包长度无效");
        Error error105 = new Error(105,"数据值无效");
        Error error106 = new Error(106,4113,"墨盒超温");
        Error error107 = new Error(107,4112,"未安装墨盒");
        Error error141 = new Error(141,"MCU md5校验失败");
        Error error152 = new Error(152,"设备初始化失败");

        add(error101);
        add(error102);
        add(error103);
        add(error104);
        add(error105);
        add(error106);
        add(error107);
        add(error141);
        add(error152);
    }

    public String getDescribeByCode(int code){

        for (Error Error :this.data){
            if (code== Error.getCode()){
                return Error.getDescribe();
            }
        }
        return "未知异常";
    }

}
