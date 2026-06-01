package com.mx.mxSdk;

/**
 * 传输协议
 */
public final class TransportProtocol {

    //发送数据相关协议
    public static final byte SOH = 0x18;
    public static final byte STX = 0x19;
    public static final byte STX_A = 0x1A;//1024B
    public static final byte STX_B = 0x1B;//2KB
    public static final byte STX_C = 0x1C;
    public static final byte STX_D = 0x1D;
    public static final byte STX_E = 0x1E;

    //请求数据相关指令  N
    public static final byte C = 0x4E;
    //接收完毕，结束发送 D
    public static final byte EOT = 0x44;
    //重传当前数据包请求命令  R
    public static final byte NAK = 0x52;

}
