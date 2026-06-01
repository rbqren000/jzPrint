package com.mx.mxSdk;

public class Opcode {

    //打印头相关指令（0x0000~0x00FF）
    public static final int WritePrintTemperature = 0x0012;//设置打印头温度（0x0012),温度值2byte(int)
    public static final int ReadPrintTemperature = 0x0013;//读取打印头温度（0x0013),打印头索引1byte(uint_8),打印头索引：固定值，为 0
    public static final int ReadCartridgeId = 0x0014;
    public static final int ClearPrintHead = 0x0009;//清洗打印头
    //设备相关指令（0x0200~0x02FF）
    public static final int ReadSoftwareInfo = 0x0200;//读取设备信息（0x0200）数据 0byte
    public static final int Restart = 0x0201;//设备重启（0x0201）数据 0byte
    public static final int UpdateMcu = 0x0203;//MCU 升级（IAP）（0x0203）数据 0byte
    public static final int WriteLogoData = 0x0204;//烧录LOGO(0x0204)
    public static final int readPrinterHeadParameters = 0x0003;//读取打印机设置的参数
    public static final int WritePrinterHeadParameters = 0x0002; //打印机参数设置
    public static final int TransmitPictureData = 0x0100;//传输图片
    public static final int PrintPicture = 0x0300;//打印图片
    public static final int ReadBattery = 0x0018;//读取电量
    public static final int ReadRechargeState = 0x0019;//读取充电状态
    public static final int ReadBluetoothConnectState = 0x0202;//读取蓝牙连接状态
    public static final int WriteCirculationAndRepeatTimes = 0x0005;//循环次数和重复打印次数
    public static final int ReadCirculationAndRepeatTimes = 0x0006;//读取循环打印次数和重复打印次数
    public static final int WritePrintDirection = 0x0007;//打印方向
    public static final int ReadPrintDirection = 0x0008;//读取打印方向
    public static final int printStart = 0x1000;//打印开始
    public static final int printCompleted = 0x1001;//打印结束
    public static final int writePrinterConnectState = 0x0202;// 连接状态 0 已断开 1 已连接

    public static final int WriteSilentState = 0x0303;//静音模式指令
    public static final int ReadSilentState = 0x0304;//读取静音模式指令

    public static final int WriteAutoPowerOffState = 0x0305;//自动关机式指令
    public static final int ReadAutoPowerOffState = 0x0306;//读取自动关机状态式指令

    //和打印机上的打印按钮想同的功能
    public static final int WritePrintStartCommand = 0x0307;

    // 连续打印指令 0关闭 1 开启
    public static final int WriteContinuousPrint = 0x0308;
    // 读取连续打印状态 0关闭 1 开启
    public static final int ReadContinuousPrint = 0x0309;

    /**
     * 以下为OEM指令
     */
    public static final int OEMTransmitPictureData = 0x0101;// 传输 Stamp 图片至打印机
    public static final int OEMTransmitTestPictureData = 0x0205;//传输测试图 测试Stamp大小，不可大于131040Bytes
    public static final int OEMReadAllPrintCount = 0x0206;//获取总打印
    public static final int OEMReadPrintRecord = 0x0207;//获取打印记录
    public static final int OEMClearPrintRecord = 0x0208;//清除打印记录
    public static final int OEMPrintTestPicture = 0x0303;//打印测试图片

    public static final int OverTempTermination = 0x1010;//超温终止
    public static final int NotInstalledInCartridge  = 0x1011;//未安装墨盒
    public static final int IDVerificationFailed = 0x1012;//墨盒ID验证未通过而终止


}
