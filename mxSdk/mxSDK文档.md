## 一、关于sdk的依赖说明

> **注意**: mxSdk 已内置以下依赖库，无需额外添加：
> - **OpenCV 4.13.0**: 图像处理库
> - **serialport**: 串口通信库

## 二、常用API列表
| 1 | <a href="#share()">share()</a> | 用于获取ConnectManager对象 |
| :---: | --- | --- |
| 2 | <a href="#init(@NonNull Application _application)">init(@NonNull Application _application)</a> | <font style="color:rgb(51, 51, 51);background-color:rgb(248, 248, 248);">用于初始化ConnectManager类中，必须调用</font> |
| 3 | <a href="#destroy()">destroy()</a> | <font style="color:rgb(51, 51, 51);">释放ConnectManager所有资源，再次使用调用必须重新初始化init方法</font> |
| 4 | <a href="#isEnable()">isEnable()</a> | <font style="color:rgb(51, 51, 51);background-color:rgb(248, 248, 248);">判断蓝牙是否打开</font> |
| 5 | <a href="#enable()">enable()</a> | <font style="color:rgb(51, 51, 51);">打开蓝牙</font> |
| 6 | <a href="#disable()">disable()</a> | <font style="color:rgb(51, 51, 51);background-color:rgb(248, 248, 248);">关闭蓝牙</font> |
| 7 | <a href="#discoverBluetoothDevice(float scanTime)">public void discoverBluetoothDevice(float scanTime)</a> | 扫描蓝牙设备 |
| 8 | <a href="#cancelDiscoveryBluetoothDevice()">public void cancelDiscoveryBluetoothDevice()</a> | 停止蓝牙设备扫描 |
| 9 | <a href="#isDiscoveringBluetoothDevice()">isDiscoveringBluetoothDevice()</a> | <font style="color:rgb(51, 51, 51);">判断是否正在扫描</font> |
| 10 | <a href="#discoverDistNetDevice()">public synchronized void discoverDistNetDevice()</a> | 扫描配网设备 |
| 11 | <a href="#cancelDiscoverDistNetDevice()">public synchronized void cancelDiscoverDistNetDevice()</a> | 停止配网设备扫描 |
| 12 | <a href="#distributionNetwork(DistNetDevice distNetDevice, String ssid, String password, float timeoutValue)">public synchronized void distributionNetwork(DistNetDevice distNetDevice, String ssid, String password, float timeoutValue)</a> | 对配网设备进行配网 |
| 13 | <a href="#discoverWifiDevice(float scanTime)">public void discoverWifiDevice(float scanTime)</a> | 扫描WiFi设备 |
| 14 | <a href="#cancelDiscoverWifiDevice()">public void cancelDiscoverWifiDevice()</a> | 停止WiFi设备扫描 |
| 15 | <a href="#getSerialDevices()">public List<Device> getSerialDevices()</a> | 获取串口设备 |
| 16 | <a href="#connect(Device device)">public void connect(Device device)</a> | 连接设备 |
| 17 | <a href="#disconnect()">public void disconnect()</a> | 断开设备连接 |
| 18 | <a href="#setWithSendMultiRowDataPacket(MultiRowData multiRowData)">public void setWithSendMultiRowDataPacket(MultiRowData multiRowData)</a> | 按默认协议设置并发送打印数据 |
| 19 | <a href="#setWithSendMultiRowDataPacket(MultiRowData multiRowData, int fh)">public void setWithSendMultiRowDataPacket(MultiRowData multiRowData, int fh)</a> | 按照指定的协议设置并发送打印<br/>数据 |
| 20 | <a href="#setWithSendOtaPacket(byte[] data)">public void setWithSendOtaPacket(byte[] data)</a> | 设置并发送ota数据 |
| 21 | <a href="#setWithSendOtaPacket(byte[] data, int fn)">public void setWithSendOtaPacket(byte[] data, int fn)</a> | 按照指定的协议设置并发送ota<br/>数据 |
| 22 | <a href="#setWithSendLogoPacket(LogoData logoData)">public void setWithSendLogoPacket(LogoData logoData)</a> | 按照默认协议设置并发送打印机默认打印内容 |
| 23 | <a href="#setWithSendLogoPacket(LogoData logoData, int fn)">public void setWithSendLogoPacket(LogoData logoData, int fn)</a> | 按照指定协议设置并发送打印机默认打印内容 |
| 24 | <a href="#isConnected()">public Boolean isConnected()</a> | 判断打印机是否连接 |
| 25 | <a href="#isConnectedDevice(Device device)">public Boolean isConnectedDevice(Device device)</a> | 判断某个设备是否连接 |
| 26 | <a href="#isConnectingDevice(Device device)">public Boolean isConnectingDevice(Device device)</a> | 判断某个设备是否正在连接 |
| 27 | <a href="#getConnectedDevice()">public Device getConnectedDevice()</a> | 获取已连接设备 |
| 28 | <a href="#readPrinterHeadParameters()">public void readPrinterHeadParameters()</a> | 读取打印头参数 |
| 29 | <a href="#readPrinterHeadParameters(int delayTime)">public void readPrinterHeadParameters(int delayTime)</a> | 延时delayTime毫秒读取打印头参数（事件单位毫秒） |
| 30 | <a href="#writePrinterHeadParameters(int printer_head, int pix)">public void writePrinterHeadParameters(int printer_head, int pix)</a> | 设置打印头及打印分辨率（打印头分1、2、3、4个序号） |
| 31 | <a href="#writePrinterHeadParameters(int printer_head, int pix, int delayTime)">public void writePrinterHeadParameters(int printer_head, int pix, int delayTime)</a> | 延时delayTime毫秒设置打印头及打印分辨率（打印头分1、2、3、4个序号，时间单位毫秒） |
| 32 | <a href="#writePrinterHeadParameters(int printer_head, int l_pix, int p_pix, int distance)">public void writePrinterHeadParameters(int printer_head, int l_pix, int p_pix, int distance)</a> | 设置打印头及打印分辨率及延迟打印距离（打印头分1、2、3、4个序号，时间单位毫秒） |
| 33 | <a href="#writePrinterHeadParameters(int printer_head, int l_pix, int p_pix, int distance, int delayTime)">public void writePrinterHeadParameters(int printer_head, int l_pix, int p_pix, int distance, int delayTime)</a> | 延时delayTime毫秒设置打印头及打印分辨率及延迟打印距离（打印头分1、2、3、4个序号，时间单位毫秒） |
| 34 | <a href="#readCirculationAndRepeatTime()">public void readCirculationAndRepeatTime()</a> | 读取打印机的打印循环次数和重复次数 |
| 35 | <a href="#readCirculationAndRepeatTime(int delayTime)">public void readCirculationAndRepeatTime(int delayTime)</a> | 延时delayTime毫秒读取打印机的打印循环次数和重复次数 |
| 36 | <a href="#writeCirculationAndRepeatTime(int circulation_time, int repeat_time)">public void writeCirculationAndRepeatTime(int circulation_time, int repeat_time)</a> | 设置打印机循环打印次数和重复打印次数 |
| 37 | <a href="#writeCirculationAndRepeatTime(int circulation_time, int repeat_time, int delayTime)">public void writeCirculationAndRepeatTime(int circulation_time, int repeat_time, int delayTime)</a> | 延时delayTime毫秒重复设置打印机循环打印次数和重复打印次数 |
| 38 | <a href="#readPrintDirection()">public void readPrintDirection()</a> | 读取打印方向 |
| 39 | <a href="#readPrintDirection(int delayTime)">public void readPrintDirection(int delayTime)</a> | 延时delayTime毫秒读取打印方向 |
| 40 | <a href="#writePrintDirection(int horizontalDirection, int verticalDirection)">public void writePrintDirection(int horizontalDirection, int verticalDirection)</a> | 设置水平和垂直打印方向 |
| 41 | <a href="#writePrintDirection(int horizontalDirection, int verticalDirection, int delayTime)">public void writePrintDirection(int horizontalDirection, int verticalDirection, int delayTime)</a> | 延时delayTime毫秒设置水平和垂直打印方向 |
| 42 | <a href="#readSoftwareInfo()">public void readSoftwareInfo()</a> | 读取软件信息 |
| 43 | <a href="#readSoftwareInfo(int delayTime)">public void readSoftwareInfo(int delayTime)</a> | 延时delayTime毫秒读取软件信息 |
| 44 | <a href="#readBattery()">public void readBattery()</a> | 读取电量信息 |
| 45 | <a href="#readBattery(int delayTime)">public void readBattery(int delayTime)</a> | 延时delayTime毫秒读取电量信息 |
| 46 | <a href="#LogoImage2Data(Context context, LogoImage logoImage, int threshold, boolean transparentToWhiteAuto, OnCreateLogoDataListener onCreateLogoDataListener)">public static void LogoImage2Data(Context context, LogoImage logoImage, int threshold, boolean transparentToWhiteAuto, OnCreateLogoDataListener onCreateLogoDataListener)</a> | 生成默认打印内容数据<font style="color:#080808;background-color:#ffffff;">LogoData</font>的方法，该方法内部执行线程，并以事件的形式返回生成结果 |
| 47 | <a href="#LogoImage2Data(Context context, LogoImage logoImage, int threshold, boolean transparentToWhiteAuto)">public static LogoData LogoImage2Data(Context context, LogoImage logoImage, int threshold, boolean transparentToWhiteAuto)</a> | 生成打印机默认打印数据<font style="color:#080808;background-color:#ffffff;">LogoData的</font>方法，图片处理过程为耗时任务，需要自己在外边创建线程来配合使用 |
| 48 | <a href="#bitmap2MultiRowData(Context context, MultiRowImage multiRowImage, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean flipHorizontally, boolean transparentToWhiteAuto, boolean thumbToSimulation, OnCreateMultiRowDataListener onCreateMultiRowDataListener)">public static void bitmap2MultiRowData(Context context, MultiRowImage multiRowImage, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean flipHorizontally, boolean transparentToWhiteAuto, boolean thumbToSimulation, OnCreateMultiRowDataListener onCreateMultiRowDataListener)</a> | 生成打印数据MultiRowData，该方法内部自己维护线程，并以事件的方式返回执行结果 |
| 49 | <a href="#bitmap2MultiRowData(Context context, MultiRowImage multiRowImage, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean flipHorizontally, boolean transparentToWhiteAuto, boolean thumbToSimulation)">public static MultiRowData bitmap2MultiRowData(Context context, MultiRowImage multiRowImage, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean flipHorizontally, boolean transparentToWhiteAuto, boolean thumbToSimulation)</a> | 生成打印数据MultiRowData，图片处理过程为耗时任务，需要在使用时创建线程来执行 |
| 50 | <a href="#image2MultiRowImage(Context context, Uri uri, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener)">public static void image2MultiRowImage(Context context, Uri uri, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener)</a> | 将一张普通的图片转成<font style="color:#080808;background-color:#ffffff;">MultiRowImage，参数context 上下文对象，uri图片Uri，rowLayoutDirection裁剪方向，onCreateMultiRowImageListener用于将处理的结果以事件的形式传递出来</font> |
| 51 | <a href="#image2MultiRowImage(Context context, Uri uri, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener)">public static void image2MultiRowImage(Context context, Uri uri, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener)</a> | 将一张普通的图片转成<font style="color:#080808;background-color:#ffffff;">MultiRowImage，参数context 上下文对象，uri图片Uri，rowLayoutDirection裁剪方向，ignoreLastRowIfHeightLess图片最后一拼忽略值，用于处理最后一拼尺寸太小打印意义不大的情况下进行忽略这一拼，onCreateMultiRowImageListener用于将处理的结果以事件的形式传递出来</font> |
| 52 | <a href="#image2MultiRowImage(Context context, String imagePath, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener)">public static void image2MultiRowImage(Context context, String imagePath, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener)</a> | 将一张普通的图片转成<font style="color:#080808;background-color:#ffffff;">MultiRowImage，参数context 上下文对象，imagePath图片的存储路径，rowLayoutDirection裁剪方向，onCreateMultiRowImageListener用于将处理的结果以事件的形式传递出来</font> |
| 53 | <a href="#image2MultiRowImage(Context context, String imagePath, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener)">public static void image2MultiRowImage(Context context, String imagePath, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener)</a> | 将一张普通的图片转成<font style="color:#080808;background-color:#ffffff;">MultiRowImage，参数context 上下文对象，imagePath图片的存储路径，rowLayoutDirection裁剪方向，ignoreLastRowIfHeightLess图片最后一拼忽略值，用于处理最后一拼尺寸太小打印意义不大的情况下进行忽略这一拼，onCreateMultiRowImageListener用于将处理的结果以事件的形式传递出来</font> |
| 54 | <a href="#image2MultiRowImage(Context context, Bitmap bitmap, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener)">public static void image2MultiRowImage(Context context, Bitmap bitmap, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener)</a> | 将一张普通的图片转成<font style="color:#080808;background-color:#ffffff;">MultiRowImage，参数context 上下文对象，bitmap要裁剪的图片，rowLayoutDirection裁剪方向，onCreateMultiRowImageListener用于将处理的结果以事件的形式传递出来</font> |
| 55 | <a href="#image2MultiRowImage(Context context, Bitmap bitmap, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener)">public static void image2MultiRowImage(Context context, Bitmap bitmap, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener)</a> | 将一张普通的图片转成<font style="color:#080808;background-color:#ffffff;">MultiRowImage，参数context 上下文对象，bitmap要裁剪的图片，rowLayoutDirection裁剪方向，ignoreLastRowIfHeightLess图片最后一拼忽略值，用于处理最后一拼尺寸太小打印意义不大的情况下进行忽略这一拼，onCreateMultiRowImageListener用于将处理的结果以事件的形式传递出来</font> |
| 56 | <a href="#startPrint()">public void startPrint()</a> | 发送打印指令，和打印机上打印按钮功能相同 |



## 三、API使用及示例 

#### 1、<a name="share()">share()</a>获取ConnectManager单例
```java
ConnectManager connectManager = ConnectManager.share();
```

#### 2、<a name="init(@NonNull Application _application)">init(@NonNull Application _application)</a><font style="color:rgb(51, 51, 51);">初始化ConnectManager方法</font>
```java
ConnectManager.share().init(this);
```

#### 3、<a name="destroy()">destroy()</a>资源释放
```java
ConnectManager.share().destroy();
```

#### 4、<a name="isEnable()">isEnable()</a>判断蓝牙是否打开
```java
ConnectManager.share().isEnable();
```

#### 5、<a name="enable()">enable()</a>打开蓝牙
```java
ConnectManager.share().enable();
//也可以使用intent来打开
Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
activity.startActivityForResult(intent, REQUEST_OPEN_BT_CODE);
```

#### 6、<a name="disable()">disable()</a> 关闭蓝牙
```java
ConnectManager.share().disable();
```

#### 7、<a name="discoverBluetoothDevice(float scanTime)">discoverBluetoothDevice(float scanTime)</a>扫描蓝牙设备
```java
//发现设备事件的注册
ConnectManager.share().registerDeviceDiscoverListener(onDeviceDiscoverListener);
//发现设备事件的注销
ConnectManager.share().unregisterDeviceDiscoverListener(onDeviceDiscoverListener);

//参数scanTime  扫描时长，单位为秒
ConnectManager.share().discoverBluetoothDevice(20);
//发现设备事件
private final ConnectManager.OnDeviceDiscoverListener onDeviceDiscoverListener = new ConnectManager.OnDeviceDiscoverListener() {
    @Override
    public void onStartDiscover() {
      RBQLog.i("开始扫描设备");
    }
    
    @Override
    public void onStopDiscover() {
      RBQLog.i("停止扫描蓝牙设备");
    }
    @Override
    public void onDiscovered(Device device) {
      
    }
};
```

#### 8、<a name="cancelDiscoveryBluetoothDevice()">cancelDiscoveryBluetoothDevice()</a>取消扫描蓝牙设备
```java
ConnectManager.share().cancelDiscoveryBluetoothDevice();
```

#### 9、<a name="isDiscoveringBluetoothDevice()">isDiscoveringBluetoothDevice()</a>判断设备是否正在扫描蓝牙设备
```java
ConnectManager.share().isDiscoveringBluetoothDevice();
```

#### 10、<a name="discoverDistNetDevice()">discoverDistNetDevice()</a>搜索配网设备
```java
//注册搜索设备事件
ConnectManager.share().registerDistNetDeviceDiscoverListener(onDistNetDeviceDiscoverListener);
//注销配网设备事件
ConnectManager.share().unregisterDistNetDeviceDiscoverListener(onDistNetDeviceDiscoverListener);

//搜索配网设备
ConnectManager.share().discoverDistNetDevice();
//事件示例
ConnectManager.OnDistNetDeviceDiscoverListener onDistNetDeviceDiscoverListener = new ConnectManager.OnDistNetDeviceDiscoverListener() {
    @Override
    public void onDistNetDeviceDiscoverStart() {
      //开始搜索配网设备
    }

    @Override
    public void onDistNetDeviceDiscover(DistNetDevice device) {
      //搜索到配网设备
    }

    @Override
    public void onDistNetDeviceDiscoverCancel() {
      //取消配网设备搜索
    }
  };
```

#### 11、<a name="cancelDiscoverDistNetDevice()">cancelDiscoverDistNetDevice()</a>取消配网设备搜索
```java
//停止配网设备搜索
ConnectManager.share().cancelDiscoverDistNetDevice();
```

#### 12、<a name="distributionNetwork(DistNetDevice distNetDevice, String ssid, String password, float timeoutValue)">distributionNetwork(DistNetDevice distNetDevice, String ssid, String password, float timeoutValue)</a><font style="color:rgb(51, 51, 51);">对wifi设备进行配网</font>
```java
//事件注册
ConnectManager.share().registerDistributionNetworkListener(onDistributionNetworkListener);
//事件注销
ConnectManager.share().unregisterDistributionNetworkListener(onDistributionNetworkListener);

//启动配网  参数分别为  配网设备:DistNetDevice WiFi账号:SSID 密码:PASSWORD 超时时间: timeoutValue 单位s
ConnectManager.share().distributionNetwork(distNetDevice, SSID, PASSWORD, 100);

//事件
ConnectManager.OnDistributionNetworkListener onDistributionNetworkListener = new ConnectManager.OnDistributionNetworkListener() {
    @Override
    public void onDistributionNetworkStart() {
      //配网开始
    }

    @Override
    public void onDistributionNetworkSucceed(Device device) {
      //配网成功
    }

    @Override
    public void onDistributionNetworkFail() {
      //配网失败
    }

    @Override
    public void onDistributionNetworkTimeOut() {
      //配网超时
    }
  };
```

#### 13、<a name="discoverWifiDevice()">discoverWifiDevice()</a>扫描WiFi设备
```java
//事件注册
ConnectManager.share().registerDeviceDiscoverListener(onDeviceDiscoverListener);
//事件注销
ConnectManager.share().unregisterDeviceDiscoverListener(onDeviceDiscoverListener);

//搜索已配好网的WiFi设备
ConnectManager.share().discoverWifiDevice();
//事件
private final ConnectManager.OnDeviceDiscoverListener onDeviceDiscoverListener = new ConnectManager.OnDeviceDiscoverListener() {
    @Override
    public void onStartDiscover() {
        RBQLog.i("开始扫描设备");
    }

    @Override
    public void onStopDiscover() {
        RBQLog.i("停止扫描蓝牙设备");
    }
    @Override
    public void onDiscovered(Device device) {
        RBQLog.i("发现设备");
    }
};
```

#### 14、<a name="cancelDiscoverWifiDevice()">cancelDiscoverWifiDevice()</a> 取消WiFi设备扫描
```java
//取消WiFi设备搜索
ConnectManager.share().cancelDiscoverWifiDevice();
```

#### 15、<a name="getSerialDevices()">getSerialDevices()</a> 获取串口设备
```java
// 获取串口设备
List<Device> devices = ConnectManager.share().getSerialDevices();
```

#### 16、<a name="connect(Device device)">connect(Device device)</a>  连接设备
```java
//连接设备事件注册
ConnectManager.share().registerDeviceConnectListener(onDeviceConnectListener);
//连接设备事件注销
ConnectManager.share().unregisterDeviceConnectListener(onDeviceConnectListener);

//设备连接
ConnectManager.share().connect(device);

//连接设备事件示例
ConnectManager.OnDeviceConnectListener onDeviceConnectListener = new ConnectManager.OnDeviceConnectListener() {
    @Override
    public void onDeviceConnectStart(Device device) {
      
    }
    @Override
    public void onDeviceConnectSucceed(Device device) {
      
    }
    @Override
    public void onDeviceDisconnect(Device device) {
      
    }
    @Override
    public void onDeviceConnectFail(Device device, String error) {
      
    }
  };
```

#### 17、<a name="disconnect()">disconnect()</a><font style="color:rgb(51, 51, 51);">断开设备连接</font>
```java
ConnectManager.share().disconnect();
```

#### 18、<a name="setWithSendMultiRowDataPacket(MultiRowData multiRowData)">public void setWithSendMultiRowDataPacket(MultiRowData multiRowData)</a>按照默认的协议发送打印数据 
备注：MultiRowData   关于MultiRowData对象在会在后边的篇幅中详细讲解，它用来代表多张图片或者一张图裁剪成多张连续的拼接图生成的数据

```java
//注册数据传送事件
ConnectManager.share().registerDataProgressListener(onDataProgressListener);
//反注册数据传输事件
ConnectManager.share().unregisterDataProgressListener(onDataProgressListener);

//设置并发送数据
ConnectManager.share().setWithSendMultiRowDataPacket(multiRowData);
// 数据传输事件  注意：这里的时间均为时间戳
ConnectManager.OnDataProgressListener onDataProgressListener = new ConnectManager.OnDataProgressListener() {
        @Override
        public void onDataProgressStart(float size, int progress, long startTime) {
            
        }

        @Override
        public void onDataProgress(float size, int progress, long startTime, long currentTime) {
            
        }

        @Override
        public void onDataProgressFinish(float size, long startTime, long currentTime) {
            
        }

        @Override
        public void onDataProgressError(String error, int code) {
            
        }
    };
```

#### 19、<a name="setWithSendMultiRowDataPacket(MultiRowData multiRowData, int fh)">public void setWithSendMultiRowDataPacket(MultiRowData multiRowData, int fh)</a> 按照指定的协议发送打印数据
注意：sdk内支持多重协议，打印机具体支持那种协议，请根据当前所开发的打印机支持的实际协议来选择协议

```java
//注册数据传送事件
ConnectManager.share().registerDataProgressListener(onDataProgressListener);
//反注册数据传输事件
ConnectManager.share().unregisterDataProgressListener(onDataProgressListener);

//设置并发送数据 
ConnectManager.share().setWithSendMultiRowDataPacket(multiRowData,STX_A);
// 数据传输事件   注意：这里的时间均为时间戳
ConnectManager.OnDataProgressListener onDataProgressListener = new ConnectManager.OnDataProgressListener() {
        @Override
        public void onDataProgressStart(float size, int progress, long startTime) {
            
        }

        @Override
        public void onDataProgress(float size, int progress, long startTime, long currentTime) {
            
        }

        @Override
        public void onDataProgressFinish(float size, long startTime, long currentTime) {
            
        }

        @Override
        public void onDataProgressError(String error, int code) {
            
        }
    };
```

#### 20、 <a name="setWithSendOtaPacket(byte[] data)">public void  setWithSendOtaPacket(byte[] data) </a>使用默认的协议发送ota数据进行ota升级
```java
//注册数据传送事件
ConnectManager.share().registerDataProgressListener(onDataProgressListener);
//反注册数据传输事件
ConnectManager.share().unregisterDataProgressListener(onDataProgressListener);

//使用默认协议发送ota数据
ConnectManager.share().setWithSendOtaPacket(buffer);

// 数据传输事件   注意：这里的时间均为时间戳
ConnectManager.OnDataProgressListener onDataProgressListener = new ConnectManager.OnDataProgressListener() {
        @Override
        public void onDataProgressStart(float size, int progress, long startTime) {
            
        }

        @Override
        public void onDataProgress(float size, int progress, long startTime, long currentTime) {
            
        }

        @Override
        public void onDataProgressFinish(float size, long startTime, long currentTime) {
            
        }

        @Override
        public void onDataProgressError(String error, int code) {
            
        }
    };
```

#### 21、<a name="setWithSendOtaPacket(byte[] data, int fn)">public void  setWithSendOtaPacket(byte[] data, int fn)</a>  按照指定的协议发送ota数据进行ota升级
```java
//注册数据传送事件
ConnectManager.share().registerDataProgressListener(onDataProgressListener);
//反注册数据传输事件
ConnectManager.share().unregisterDataProgressListener(onDataProgressListener);

//使用STX_A协议发送ota数据
ConnectManager.share().setWithSendOtaPacket(buffer,STX_A);

// 数据传输事件   注意：这里的时间均为时间戳
ConnectManager.OnDataProgressListener onDataProgressListener = new ConnectManager.OnDataProgressListener() {
        @Override
        public void onDataProgressStart(float size, int progress, long startTime) {
            
        }

        @Override
        public void onDataProgress(float size, int progress, long startTime, long currentTime) {
            
        }

        @Override
        public void onDataProgressFinish(float size, long startTime, long currentTime) {
            
        }

        @Override
        public void onDataProgressError(String error, int code) {
            
        }
    };
```

#### 22、<a name="setWithSendLogoPacket(LogoData logoData)">public void setWithSendLogoPacket(LogoData logoData)</a> <font style="color:rgb(51, 51, 51);">使用默认协议修改默认打印内容</font>
```java
//注册数据传送事件
ConnectManager.share().registerDataProgressListener(onDataProgressListener);
//反注册数据传输事件
ConnectManager.share().unregisterDataProgressListener(onDataProgressListener);

//使用默认协议发送打印机默认打印内容  图片尺寸要求(2000x552) 在尺寸不足时sdk内部会去自动调整
ConnectManager.share().setWithSendLogoPacket(logoData);

// 数据传输事件   注意：这里的时间均为时间戳
ConnectManager.OnDataProgressListener onDataProgressListener = new ConnectManager.OnDataProgressListener() {
        @Override
        public void onDataProgressStart(float size, int progress, long startTime) {
            
        }

        @Override
        public void onDataProgress(float size, int progress, long startTime, long currentTime) {
            
        }

        @Override
        public void onDataProgressFinish(float size, long startTime, long currentTime) {
            
        }

        @Override
        public void onDataProgressError(String error, int code) {
            
        }
    };
```

#### 23、<a name="setWithSendLogoPacket(LogoData logoData, int fn)">public void setWithSendLogoPacket(LogoData logoData, int fn)</a><font style="color:rgb(51, 51, 51);">使用指定协议修改默认打印内容</font>
```java
//注册数据传送事件
ConnectManager.share().registerDataProgressListener(onDataProgressListener);
//反注册数据传输事件
ConnectManager.share().unregisterDataProgressListener(onDataProgressListener);

//使用STX_A协议发送打印机默认打印内容  图片尺寸要求(2000x552) 在尺寸不足时sdk内部会去自动调整
ConnectManager.share().setWithSendLogoPacket(logoData,STX_A);

// 数据传输事件   注意：这里的时间均为时间戳
ConnectManager.OnDataProgressListener onDataProgressListener = new ConnectManager.OnDataProgressListener() {
        @Override
        public void onDataProgressStart(float size, int progress, long startTime) {
            
        }

        @Override
        public void onDataProgress(float size, int progress, long startTime, long currentTime) {
            
        }

        @Override
        public void onDataProgressFinish(float size, long startTime, long currentTime) {
            
        }

        @Override
        public void onDataProgressError(String error, int code) {
            
        }
    };
```

#### 24、<a name="isConnected()">public Boolean isConnected()</a> 判断打印机是否连接
```java
ConnectManager.share().isConnected();
```

#### 25、<a name="isConnectedDevice(Device device)">public Boolean isConnectedDevice(Device device)</a> 判断某个打印是否已连接
```java
ConnectManager.share().isConnectedDevice(device)
```

#### 26、<a name="isConnectingDevice(Device device)">public Boolean isConnectingDevice(Device device)</a>判断该设备是否正在连接
```java
ConnectManager.share().isConnectingDevice(device);
```

#### 27、<a name="getConnectedDevice()">public Device getConnectedDevice()</a>获取已连接的设备，如果没有设备连接则，返回null
```java
Device connectedDevice = ConnectManager.share().getConnectedDevice();
```

#### 28、<a name="readPrinterHeadParameters()">public void readPrinterHeadParameters()</a>读取打印头参数
```java
//注册打印头读取事件
ConnectManager.share().registerReceiveMessageListener(onReceiveMsgListener);
//注销
ConnectManager.share().unregisterReceiveMessageListener(onReceiveMsgListener);

// 读取打印头参数
ConnectManager.share().readPrinterHeadParameters();

// 事件
ConnectManager.OnReceiveMsgListener onReceiveMsgListener = new ConnectManager.OnReceiveMsgListener() {
    @Override
    public void onReadPrinterHeadParameter(Device device, int headValue, int l_pix, int p_pix, int distance) {
      // 返回打印头参数数据
    }

    @Override
    public void onReadCirculationAndRepeatTime(Device device, int circulation_time, int repeat_time)    {
      // 返回循环次数和重复次数数据
    }

    @Override
    public void onReadDirection(Device device, int oldHorizontalDirection, int horizontalDirection, int oldVerticalDirection, int verticalDirection) {
      //返回打印方向数据  0和1
    }

    @Override
    public void onReadSoftwareInfo(Device device, String id, String name, String mcu_version, String mcu_date) {
      //返回软件版本
      
    }

    @Override
    public void onReadTemperature(Device device, int temp) {
        
      
    }

    @Override
    public void onReadBattery(Device device, int bat) {
      //读取电量
      
    }

    @Override
    public void onError(Device device, String error) {
      
    }
  };
```

#### 29、<a name="readPrinterHeadParameters(int delayTime)">public void readPrinterHeadParameters(int delayTime)</a>按照指定的延时时间，读取打印头参数（事件单位毫秒）
```java
//注册打印头读取事件
ConnectManager.share().registerReceiveMessageListener(onReceiveMsgListener);
//注销
ConnectManager.share().unregisterReceiveMessageListener(onReceiveMsgListener);

// 读取打印头参数 delayTime毫秒后自动发出指令并进行读取
ConnectManager.share().readPrinterHeadParameters(delayTime);

// 事件
ConnectManager.OnReceiveMsgListener onReceiveMsgListener = new ConnectManager.OnReceiveMsgListener() {
    @Override
    public void onReadPrinterHeadParameter(Device device, int headValue, int l_pix, int p_pix, int distance) {
      // 返回打印头参数数据
    }

    @Override
    public void onReadCirculationAndRepeatTime(Device device, int circulation_time, int repeat_time)    {
      // 返回循环次数和重复次数数据
    }

    @Override
    public void onReadDirection(Device device, int oldHorizontalDirection, int horizontalDirection, int oldVerticalDirection, int verticalDirection) {
      //返回打印方向数据  0和1
    }

    @Override
    public void onReadSoftwareInfo(Device device, String id, String name, String mcu_version, String mcu_date) {
      //返回软件版本
      
    }

    @Override
    public void onReadTemperature(Device device, int temp) {
        
      
    }

    @Override
    public void onReadBattery(Device device, int bat) {
      //读取电量
      
    }

    @Override
    public void onError(Device device, String error) {
      
    }
  };
```

#### 30、<a name="writePrinterHeadParameters(int printer_head, int pix)">public void writePrinterHeadParameters(int printer_head, int pix)</a>设置打印头及打印分辨率（打印头分1、2、3、4个序号，分辨率支持 600x600 1200x1200） 
```java
/**
* printer_head 打印头编号
* pix 分辨率
*/
ConnectManager.share().writePrinterHeadParameters(int printer_head, int pix);
// 事件注册和监听  同  readPrinterHeadParameters()
```

#### 31、<a name="writePrinterHeadParameters(int printer_head, int pix, int delayTime)">public void writePrinterHeadParameters(int printer_head, int pix, int delayTime)</a> 延时delayTime毫秒设置打印头及打印分辨率（打印头分1、2、3、4个序号，时间单位毫秒）
```java
/**
* printer_head 打印头编号
* pix 分辨率
*/
ConnectManager.share().writePrinterHeadParameters(int printer_head, int pix, int delayTime);
// 事件注册和监听  同  readPrinterHeadParameters()
```

#### 32、<a name="writePrinterHeadParameters(int printer_head, int l_pix, int p_pix, int distance)">public void writePrinterHeadParameters(int printer_head, int l_pix, int p_pix, int distance)</a>设置打印头及打印分辨率及延迟打印距离（打印头分1、2、3、4个序号，时间单位毫秒，distance单位像素）
```java
ConnectManager.share().writePrinterHeadParameters(int printer_head, int l_pix, int p_pix, int distance);
// 事件注册和监听  同  readPrinterHeadParameters()
```

#### 33、<a name="writePrinterHeadParameters(int printer_head, int l_pix, int p_pix, int distance, int delayTime)">public void writePrinterHeadParameters(int printer_head, int l_pix, int p_pix, int distance, int delayTime)</a>延时delayTime毫秒设置打印头及打印分辨率及延迟打印距离（打印头分1、2、3、4个序号，时间单位毫秒）
```java
ConnectManager.share().writePrinterHeadParameters(int printer_head, int l_pix, int p_pix, int distance,int delayTime);
```

#### 34、<a name="readCirculationAndRepeatTime()">public void readCirculationAndRepeatTime()</a>读取打印机的打印循环次数和重复次数
```java
ConnectManager.share().readCirculationAndRepeatTime();
// 事件注册和监听  同  readPrinterHeadParameters()
```

#### 35、<a name="readCirculationAndRepeatTime(int delayTime)">public void readCirculationAndRepeatTime(int delayTime)</a> 延时delayTime毫秒读取打印机的打印循环次数和重复次数
```java
ConnectManager.share().readCirculationAndRepeatTime(int delayTime);
// 事件注册和监听  同  readPrinterHeadParameters()
```

#### 36、<a name="writeCirculationAndRepeatTime(int circulation_time, int repeat_time)">public void writeCirculationAndRepeatTime(int circulation_time, int repeat_time)</a> 设置打印机循环打印次数和重复打印次数
```java
ConnectManager.share().writeCirculationAndRepeatTime(int circulation_time, int repeat_time);
```

#### 37、<a name="writeCirculationAndRepeatTime(int circulation_time, int repeat_time, int delayTime)">public void writeCirculationAndRepeatTime(int circulation_time, int repeat_time, int delayTime)</a> 延时delayTime毫秒重复设置打印机循环打印次数和重复打印次数
```java
ConnectManager.share().writeCirculationAndRepeatTime(int circulation_time, int repeat_time,int delayTime);
```

#### 38、<a name="readPrintDirection()">public void readPrintDirection()</a>读取打印方向
```java
ConnectManager.share().readPrintDirection();
// 事件注册和监听  同  readPrinterHeadParameters()
```

#### 39、<a name="readPrintDirection(int delayTime)">public void readPrintDirection(int delayTime)</a> 延时delayTime毫秒读取打印方向
```java
ConnectManager.share().readPrintDirection(int delayTime);
// 事件注册和监听  同  readPrinterHeadParameters()
```

#### 40、<a name="writePrintDirection(int horizontalDirection, int verticalDirection)">public void writePrintDirection(int horizontalDirection, int verticalDirection)</a>设置水平和垂直打印方向
```java
/** 
* 设置打印方向  仅支持 0和1，其他值将视为无效
* horizontalDirection 0、1
* verticalDirection  0、1
*/
ConnectManager.share().writePrintDirection(int horizontalDirection, int verticalDirection) 
```

#### 41、<a name="writePrintDirection(int horizontalDirection, int verticalDirection, int delayTime)">public void writePrintDirection(int horizontalDirection, int verticalDirection, int delayTime)</a> 延时delayTime毫秒设置水平和垂直打印方向
```java
/** 
* 设置打印方向  仅支持 0和1，其他值将视为无效
* horizontalDirection 0、1
* verticalDirection  0、1
* delayTime  延时时间  单位  毫秒
*/
ConnectManager.share().writePrintDirection(int horizontalDirection, int verticalDirection, int delayTime)
```

#### 42、<a name="readSoftwareInfo()">public void readSoftwareInfo()</a>读取打印机软件信息  
```java
ConnectManager.share().readSoftwareInfo();
// 事件注册和监听  同  readPrinterHeadParameters()
```

#### 43、<a name="readSoftwareInfo(int delayTime)">public void readSoftwareInfo(int delayTime)</a>延时delayTime毫秒读取打印机软件信息  
```java
ConnectManager.share().readSoftwareInfo(delayTime);
// 事件注册和监听  同  readPrinterHeadParameters()
```

#### 44、<a name="readBattery()">public void readBattery()</a> 读取电量信息
```java
ConnectManager.share().readBattery();
// 事件注册和监听  同  readPrinterHeadParameters()
```

#### 45、<a name="readBattery(int delayTime)">public void readBattery(int delayTime)</a> 延时delayTime毫秒读取电量信息
```java
ConnectManager.share().readBattery();
// 事件注册和监听  同  readPrinterHeadParameters(int delayTime)
```

#### 46、<a name="LogoImage2Data(Context context, LogoImage logoImage, int threshold, boolean transparentToWhiteAuto, OnCreateLogoDataListener onCreateLogoDataListener)">public static void LogoImage2Data(Context context, LogoImage logoImage, int threshold, boolean transparentToWhiteAuto, OnCreateLogoDataListener onCreateLogoDataListener)</a>生成默认打印内容数据工具方法，该方法内部执行线程，并以事件的形式返回生成结果
```java
// 打印机默认打印内容(logo)对象，该对象通过LogoImage类的工厂方法createInstance来生成
LogoImage logoImage = LogoImage.createInstance("xxx/xxx.png");
/**
 *
 * @param context  上下文对象
 * @param logoImage logo图片对象
 * @param threshold  二值化阈值
 * @param transparentToWhiteAuto 是否自动将透明背景图转化为白色背景图(通常在明确知道图片不为透明图片的情况下不建议打开，检测图片透明会降低运行效率) 
 * @param onCreateLogoDataListener  logo图片对象处理事件
 */
LogoDataFactory.LogoImage2Data(this, logoImage, 127, false, new LogoDataFactory.OnCreateLogoDataListener() {
    @Override
    public void onCreateLogoDataStart() {
        //开始处理logo图片
    }

    @Override
    public void onCreateLogoDataComplete(LogoData logoData) {
        //完成图片转化，并返回LogoData对象
    }

    @Override
    public void onCreateLogoDataError(int code) {
        //发生异常报错
    }
});
```

#### 47、<a name="LogoImage2Data(Context context, LogoImage logoImage, int threshold, boolean transparentToWhiteAuto)">public static LogoData LogoImage2Data(Context context, LogoImage logoImage, int threshold, boolean transparentToWhiteAuto)</a>生成默认打印内容数据工具方法<font style="color:#DF2A3F;">（需用户自己创建线程来执行）</font>
```java
// 打印机默认打印内容(logo)对象，该对象通过LogoImage类的工厂方法createInstance来生成
LogoImage logoImage = LogoImage.createInstance("xxx/xxx.png");
/**
 *
 * @param context  上下文对象
 * @param logoImage logo图片对象
 * @param threshold  二值化阈值
 * @param transparentToWhiteAuto 是否自动将透明背景图转化为白色背景图(通常在明确知道图片不为透明图片的情况下不建议打开，检测图片透明会降低运行效率)
 */
LogoData logoData = LogoDataFactory.LogoImage2Data(this, logoImage, 127,false);
```

#### 48、<a name="bitmap2MultiRowData(Context context, MultiRowImage multiRowImage, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean flipHorizontally, boolean transparentToWhiteAuto, boolean thumbToSimulation, OnCreateMultiRowDataListener onCreateMultiRowDataListener)">public static void bitmap2MultiRowData(Context context, MultiRowImage multiRowImage, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean flipHorizontally, boolean transparentToWhiteAuto, boolean thumbToSimulation, OnCreateMultiRowDataListener onCreateMultiRowDataListener)</a> 生成打印数据，该方法内部自己维护线程，并以事件的方式返回执行结果
```java
// rowImage 单拼图像对象
RowImage rowImage = RowImage.createInstance("xxx/xxx.png");
String thumbPath = "xxx/xxx.png";
ArrayList<RowImage> rowImages = new ArrayList<>();
rowImages.add(rowImage);
// multiRowImage  多拼图像对象
MultiRowImage multiRowImage = MultiRowImage.createInstance(rowImages,thumbPath);
/**
 *
 * @param context  上下文对象
 * @param multiRowImage 多拼图像对象
 * @param threshold  二值化阈值
 * @param clearBackground 是否移除背景色
 * @param dithering 是否开启抖动算法
 * @param compress 是否启用压缩
 * @param flipHorizontally 是否横向翻转图像
 * @param transparentToWhiteAuto 是否将透明图片转为白色背景图
 * @param thumbToSimulation 缩略图是否转为可预览的打印效果图
 * @param onCreateMultiRowDataListener 图像转化事件
 */
MultiRowDataFactory.bitmap2MultiRowData(this, multiRowImage, 127, false, true, false, false, false, false, new MultiRowDataFactory.OnCreateMultiRowDataListener() {
    @Override
    public void onCreateMultiRowDataStart() {
        //开始事件
    }

    @Override
    public void onCreateMultiRowDataComplete(MultiRowData multiRowData) {
        //图片处理完成，并在该事件返回MultiRowData对象
    }

    @Override
    public void onCreateMultiRowDataError(int code) {
        //转化过程生成异常
    }
});

```

#### 49、<a name="bitmap2MultiRowData(Context context, MultiRowImage multiRowImage, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean flipHorizontally, boolean transparentToWhiteAuto, boolean thumbToSimulation)">public static MultiRowData bitmap2MultiRowData(Context context, MultiRowImage multiRowImage, int threshold, boolean clearBackground, boolean dithering, boolean compress, boolean flipHorizontally, boolean transparentToWhiteAuto, boolean thumbToSimulation)</a> 生成打印数据，图片处理过程为耗时任务，需要在使用时创建线程来执行
```java
// rowImage 单拼图像对象
RowImage rowImage = RowImage.createInstance("xxx/xxx.png");
String thumbPath = "xxx/xxx.png";
ArrayList<RowImage> rowImages = new ArrayList<>();
rowImages.add(rowImage);
// multiRowImage  多拼图像对象
MultiRowImage multiRowImage = MultiRowImage.createInstance(rowImages,thumbPath);
/**
 *
 * @param context  上下文对象
 * @param multiRowImage 多拼图像对象
 * @param threshold  二值化阈值
 * @param clearBackground 是否移除背景色
 * @param dithering 是否开启抖动算法
 * @param compress 是否启用压缩
 * @param flipHorizontally 是否横向翻转图像
 * @param transparentToWhiteAuto 是否将透明图片转为白色背景图
 * @param thumbToSimulation 缩略图是否转为可预览的打印效果图
 */
MultiRowData multiRowData1 =  MultiRowDataFactory.bitmap2MultiRowData(this, multiRowImage, 127, false, true, false, false, false, false);
```

#### 50、 <a name="image2MultiRowImage(Context context, Uri uri, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener)">public static void image2MultiRowImage(Context context, Uri uri, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener)</a>用来生成 MultiRowImage
```java
/**
     *
     * @param context 上下文对象
     * @param uri 图片的uri
     * @param rowLayoutDirection 裁剪方向
     * @param onCreateMultiRowImageListener 事件
     */
MultiRowImageFactory.image2MultiRowImage(context, uri, rowLayoutDirection, new MultiRowImageFactory.OnCreateMultiRowImageListener() {
            @Override
            public void onCreateMultiRowImageStart() {
                
            }

            @Override
            public void onCreateMultiRowImageComplete(MultiRowImage multiRowImage) {
                //生成multiRowImage
            }

            @Override
            public void onCreateMultiRowImageError(int code) {
                
            }
        });

```

#### 51、<a name="image2MultiRowImage(Context context, Uri uri, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener)">public static void image2MultiRowImage(Context context, Uri uri, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener)</a></a>用来生成 MultiRowImage

```java
/**
     *
     * @param context 上下文
     * @param uri 图片的uri
     * @param rowLayoutDirection 裁剪方向
     * @param ignoreLastRowIfHeightLess 忽略参数，最后一拼低于该值将直接被忽略
     * @param onCreateMultiRowImageListener 事件
     */
MultiRowImageFactory.image2MultiRowImage(context, uri, rowLayoutDirection, ignoreLastRowIfHeightLess, new MultiRowImageFactory.OnCreateMultiRowImageListener() {
            @Override
            public void onCreateMultiRowImageStart() {
                
            }

            @Override
            public void onCreateMultiRowImageComplete(MultiRowImage multiRowImage) {
                //生成multiRowImage
            }

            @Override
            public void onCreateMultiRowImageError(int code) {
                
            }
        });
```

#### 52、<a name="image2MultiRowImage(Context context, String imagePath, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener)">public static void image2MultiRowImage(Context context, String imagePath, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener)</a></a>用来生成 MultiRowImage

```java
/**
     *
     * @param context 上下文
     * @param imagePath 图片的路径
     * @param rowLayoutDirection 裁剪方向
     * @param onCreateMultiRowImageListener 事件
     */
 MultiRowImageFactory.image2MultiRowImage(context, imagePath, rowLayoutDirection, new MultiRowImageFactory.OnCreateMultiRowImageListener() {
            @Override
            public void onCreateMultiRowImageStart() {
            }

            @Override
            public void onCreateMultiRowImageComplete(MultiRowImage multiRowImage) {
            }

            @Override
            public void onCreateMultiRowImageError(int code) {
            }
        });
```

#### 53、<a name="image2MultiRowImage(Context context, String imagePath, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener)">public static void image2MultiRowImage(Context context, String imagePath, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener)</a></a>用来生成 MultiRowImage

```java
/**
     *
     * @param context 上下文
     * @param imagePath 图片的路径
     * @param rowLayoutDirection 裁剪方向
     * @param ignoreLastRowIfHeightLess 忽略参数，最后一拼低于该值将直接被忽略
     * @param onCreateMultiRowImageListener 事件
     */
 MultiRowImageFactory.image2MultiRowImage(context, imagePath, rowLayoutDirection, ignoreLastRowIfHeightLess, new MultiRowImageFactory.OnCreateMultiRowImageListener() {
            @Override
            public void onCreateMultiRowImageStart() {
            }

            @Override
            public void onCreateMultiRowImageComplete(MultiRowImage multiRowImage) {
                progressDialog.dismiss();
                createMultiRowData(multiRowImage);
            }

            @Override
            public void onCreateMultiRowImageError(int code) {
            }
        });
```

#### 54、<a name="image2MultiRowImage(Context context, Bitmap bitmap, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener)">public static void image2MultiRowImage(Context context, Bitmap bitmap, RowLayoutDirection rowLayoutDirection, OnCreateMultiRowImageListener onCreateMultiRowImageListener)</a></a>用来生成 MultiRowImage

```java
/**
     *
     * @param context 上下文
     * @param bitmap 图片bitmap对象
     * @param rowLayoutDirection 裁剪方向
     * @param onCreateMultiRowImageListener 事件
     */
MultiRowImageFactory.image2MultiRowImage(context, bitmap, rowLayoutDirection, new MultiRowImageFactory.OnCreateMultiRowImageListener() {
            @Override
            public void onCreateMultiRowImageStart() {
            }

            @Override
            public void onCreateMultiRowImageComplete(MultiRowImage multiRowImage) {
            }

            @Override
            public void onCreateMultiRowImageError(int code) {
            }
        });
```

#### 55、<a name="image2MultiRowImage(Context context, Bitmap bitmap, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener)">public static void image2MultiRowImage(Context context, Bitmap bitmap, RowLayoutDirection rowLayoutDirection, int ignoreLastRowIfHeightLess, OnCreateMultiRowImageListener onCreateMultiRowImageListener)</a></a>用来生成 MultiRowImage

```java
/**
     *
     * @param context 上下文
     * @param bitmap 图片bitmap对象
     * @param rowLayoutDirection 裁剪方向
     * @param ignoreLastRowIfHeightLess 忽略参数，最后一拼低于该值将直接被忽略
     * @param onCreateMultiRowImageListener 事件
     */
MultiRowImageFactory.image2MultiRowImage(context, bitmap, rowLayoutDirection, ignoreLastRowIfHeightLess, new MultiRowImageFactory.OnCreateMultiRowImageListener() {
            @Override
            public void onCreateMultiRowImageStart() {
            }

            @Override
            public void onCreateMultiRowImageComplete(MultiRowImage multiRowImage) {
            }

            @Override
            public void onCreateMultiRowImageError(int code) {
            }
        });
```

#### 56、<a name="startPrint()">public void startPrint() </a>发送打印指令，和打印机上打印按钮功能想同

```java
ConnectManager.share().startPrint();
//注册监听
ConnectManager.share().registerReadPrintStartCommandListener(onReadPrintStartCommandListener);
//注销监听
ConnectManager.share().unregisterReadPrintStartCommandListener(onReadPrintStartCommandListener);
//该指令发送到打印机收到反馈的监听
ConnectManager.OnReadPrintStartCommandListener onReadPrintStartCommandListener = new ConnectManager.OnReadPrintStartCommandListener() {
        @Override
        public void onReadStartPrintCommand() {
            RBQLog.i("收到打印机反馈的打印指令");
        }
    };
```



## 四、常用类讲解
#### 1、<font style="color:#080808;background-color:#ffffff;">com.mx.mxSdk.</font>Device类：打印机设备
该对象通常为打印机设备，这里包括蓝牙打印机设备、wifi设备打印机设备、及直接串口连接的设备，通常无需自己创建，只需要调用相应的扫描或者get方法即可获得该对象。如：discoverBluetoothDevice（扫描蓝牙设备）、discoverWifiDevice（扫描wifi设备）和getSerialDevices（获取串口设备），其示例见上边api列表中discoverBluetoothDevice方法的使用示例。

#### 2、<font style="color:#080808;background-color:#ffffff;">com.mx.mxSdk.</font>DistNetDevice类：配网设备
该对象为配网设备，既调用discoverDistNetDevice方法搜索配网设备，由它的事件OnDistNetDeviceDiscoverListener返回的对象，该对象也无需自己创建。其示例见上边api列表中 discoverDistNetDevice方法的使用示例。

#### 3、<font style="color:#080808;background-color:#ffffff;">com.mx.mxSdk.LogoImage类：打印机默认打印内容（logo）</font>
```java

public class LogoImage implements Parcelable {

    private final String imagePath;
}
// 该对象仅提供一个工厂方法createInstance来创建该对象，需要传入logo图片的存储路径
LogoImage logoImage = LogoImage.createInstance("xxx/xxx.png");
```

#### <font style="color:#080808;background-color:#ffffff;">4、com.mx.mxSdk.LogoData类 打印默认图片生成的数据对象，该对象通常无需自己创建，调用LogoDataFactory工厂类的LogoImage2Data方法生成</font>
```java
public class LogoData implements Parcelable {
    /** 数据长度 */
    private final int dataLength;
    /** 数据存储的路径 */
    private final String dataPath;
    /** 生成的预览图存储的路径 */
    private final String imagePath;
}
```

#### 5、<font style="color:#080808;background-color:#ffffff;">com.mx.mxSdk.LogoDataFactory类 用于将LogoImage转为打印机默认打印数据LogoData</font>
```java
String logoImagePath = "xxx/xxx.png";
LogoImage logoImage = LogoImage.createInstance(logoImagePath);

/** 调用该方法，则可直接返回logoData 但是需要自己在外部创建线程来维持该耗时任务 */
LogoData logoData = LogoDataFactory.LogoImage2Data(this,logoImage,127,false);
/** 调用该方法，传入一个事件，结果以事件的形式返回，无需外边单独维持线程 */
LogoDataFactory.LogoImage2Data(this, logoImage, 127, false, new LogoDataFactory.OnCreateLogoDataListener() {
    @Override
    public void onCreateLogoDataStart() {
        //开始处理logo图片
    }

    @Override
    public void onCreateLogoDataComplete(LogoData logoData) {
        //完成logo图片处理，并返回 logoData 对象
    }

    @Override
    public void onCreateLogoDataError(int code) {
        //处理异常
    }
});

//发送打印数据
ConnectManager.share().setWithSendLogoPacket(logoData);
```

#### <font style="color:#080808;background-color:#ffffff;">6、com.mx.mxSdk.MultiRowImage类：多拼图或者单张大图裁切形成的连续图</font>
```java
public class MultiRowImage implements Parcelable {

    private final ArrayList<RowImage> rowImages;
    /** 缩略图地址*/
    private final String thumbPath;
    /** 多张图的排布方向 RowLayoutDirectionVertical 从上到下排列 RowLayoutDirectionHorizontal从左到右排列   */
    private final RowLayoutDirection rowLayoutDirection;
    /** 图片是否为连续图（既由一张大图裁切而来的连续图片） */
    private final boolean isContiguousCroppedImages;
}
// 创建MultiRowImage对象示例
MultiRowImage multiRowImage = MultiRowImage.createInstance(rowImages,thumbPath);
MultiRowImage multiRowImage1 = MultiRowImage.createInstance(rowImages,thumbPath, RowLayoutDirectionHorizontal,true);
```

#### 7、<font style="color:#080808;background-color:#ffffff;">com.mx.mxSdk.MultiRowData类 多拼图或者单张大图裁切形成的连续图生成的打印数据</font>
```java
public class MultiRowData implements Parcelable {
    /** 多拼图或者单张大图裁切形成的连续图生成的打印数据 */
    private final ArrayList<RowData> rowDataArr;
    /** 多拼图或者单张大图裁切形成的连续图生成的预览图的存储地址 */
    private final ArrayList<String> imagePaths;
    /** 缩略图生成的地址 thumbPath有可能为null，
     * 是否将缩略图生成预览图需要看MultiRowDataFactory的bitmap2MultiRowData方法传入的thumbToSimulation的值
     * thumbToSimulation为true则生成预览图，thumbToSimulation为false则不将缩略图生成预览图*/
    private final String thumbPath;
    /** 是否压缩打印数据 有损压缩 */
    private final boolean compress;
    /** 多图的排布方向，该值和MultiRowImage的rowLayoutDirection保持一致 */
    private final RowLayoutDirection rowLayoutDirection;
}
```

#### 8、<font style="color:#080808;background-color:#ffffff;">com.mx.mxSdk.MultiRowDataFactory 类 用于将多拼打印图MultiRowImage生成打印数据的工具类</font>
```java
String imagePath = "xxx/xxx.png";
String thumbPath = "xxx/xxx.png";
RowImage rowImage = RowImage.createInstance(imagePath);
ArrayList<RowImage> rowImages = new ArrayList<RowImage>();
rowImages.add(rowImage);
MultiRowImage multiRowImage = MultiRowImage.createInstance(rowImages,thumbPath);
// 该方法将多拼打印图转化为打印数据，直接返回multiRowData的值，该耗时事件需要用户自己维持线程
MultiRowData multiRowData = MultiRowDataFactory.bitmap2MultiRowData(this, multiRowImage, 127, true, false, false, false,false,false);
//该方法将多拼打印图转化为打印数据，该方法内部自己维护线程，无需用户单独外部创建线程，通过事件的形式返回结果
MultiRowDataFactory.bitmap2MultiRowData(this, multiRowImage, 127, true, false, false, false
        , false, false, new MultiRowDataFactory.OnCreateMultiRowDataListener() {
            @Override
            public void onCreateMultiRowDataStart() {

            }

            @Override
            public void onCreateMultiRowDataComplete(MultiRowData multiRowData) {

            }

            @Override
            public void onCreateMultiRowDataError(int code) {

            }
        });

//发送打印数据
ConnectManager.share().setWithSendMultiRowDataPacket(multiRowData);
```

### 五、关于打印开始和完成事件的监听

```java
//事件注册
ConnectManager.share().registerPrintListener(onPrintListener);
//事件注销
ConnectManager.share().unregisterPrintListener(onPrintListener);
//事件监听
ConnectManager.OnPrintListener onPrintListener = new ConnectManager.OnPrintListener() {
        @Override
        public void onPrintStart(int beginIndex, int endIndex, int currentIndex) {
            RBQLog.i("打印开始事件 beginIndex:"+beginIndex+"; endIndex:"+endIndex+"; currentIndex:"+currentIndex);
        }

        @Override
        public void onPrintComplete(int beginIndex, int endIndex, int currentIndex, String cartridgeId) {
            RBQLog.i("打印结束事件 beginIndex:"+beginIndex+"; endIndex:"+endIndex+"; currentIndex:"+currentIndex+"; 墨盒cartridgeId:"+cartridgeId);
        }
    };
```



### 六、<font style="color:rgb(51, 51, 51);">TransportProtocol（传输）协议类型简介</font>

<font style="color:rgb(51, 51, 51);">以下常亮分别代表了协议的类型，数据结构为 fn + ~fn + data + crc 。实际长度为1 + 1 + data.length + 2，</font>

<font style="color:rgb(51, 51, 51);">蓝牙类型的打印机：在安卓中使用的是spp协议，推荐使用STX_A ，在iOS中使用尽可能使用 STX_E (具体开发还需和我们沟通，不同打印机所支持的协议种类存存在差异)</font>

<font style="color:rgb(51, 51, 51);">wifi类型的打印机：推荐使用 STX_A 协议进行传输</font>

```java
public final class TransportProtocol {
    // 每包有效数据长度为128byte (打印机未支持)
    public static final byte SOH = 0x18;
    // 每包有效数据长度为512byte 
    public static final byte STX = 0x19;
    // 每包有效数据长度为 1024byte
    public static final byte STX_A = 0x1A;
    // 每包有效数据长度为 2KB
    public static final byte STX_B = 0x1B;
    // 未使用和验证
    public static final byte STX_C = 0x1C;
    // 打印机mcu暂时未支持
    public static final byte STX_D = 0x1D;
    // 打印机mcu支持
    public static final byte STX_E = 0x1E;
}
```

## 七、工厂类中的错误码含义
```java
public class FactoryErrorCodes {
    /**上下文对象为null*/
    public static final int Context_NULL_ERROR = 1 << 0;
    /**图片路径为null*/
    public static final int IMAGE_PATH_NULL_ERROR = 1 << 1;
    /**图片为null*/
    public static final int IMAGE_NULL_ERROR = 1 << 2;
    /**RowImage为null*/
    public static final int ROW_IMAGE_NULL_ERROR = 1 << 3;
    /**MultiRowImage为null*/
    public static final int MULTI_ROW_IMAGE_NULL_ERROR = 1 << 4;
    /**MultiRowImage创建失败null*/
    public static final int MULTI_ROW_IMAGE_CREATION_FAILED = 1 << 5;
    /**文件没有找到*/
    public static final int FILE_NOT_FOUND = 1 << 6;
    /**io异常*/
    public static final int IOException_ERROR = 1 << 7;  // 16
}
```

