# mxSdk SDK 混淆规则
# 只保留对外公开的 API，内部实现全部混淆

# ===== 保留核心 API 类 =====
-keep public class com.mx.mxSdk.ConnectManager {
    public *;
}

# ===== 保留所有监听器接口（ConnectManager 内部） =====
-keep public interface com.mx.mxSdk.ConnectManager$OnDataProgressListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnCommandWriteListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnDataWriteListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnDataSynchronizeListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnDeviceBluetoothStateListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnDeviceDiscoverListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnConnModelDiscoverListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnDeviceBondListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnDeviceConnectListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnReceiveMsgListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnReadGeneralCommandListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnReadPrintStartCommandListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnReadJsonListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnDistNetDeviceDiscoverListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnDistributionNetworkListener { *; }
-keep public interface com.mx.mxSdk.ConnectManager$OnPrintListener { *; }

# ===== 保留工厂类的监听器接口 =====
-keep public interface com.mx.mxSdk.LogoDataFactory$OnCreateLogoDataListener { *; }
-keep public interface com.mx.mxSdk.LogoDataFactory$OnCreateExtLogoDataListener { *; }
-keep public interface com.mx.mxSdk.MultiRowDataFactory$OnCreateMultiRowDataListener { *; }
-keep public interface com.mx.mxSdk.MultiRowDataFactory$OnCreateExtMultiRowDataListener { *; }
-keep public interface com.mx.mxSdk.MultiRowImageFactory$OnCreateMultiRowImageListener { *; }

# ===== 保留数据模型类 =====
-keep public class com.mx.mxSdk.Device { *; }
-keep public class com.mx.mxSdk.DistNetDevice { *; }

# ===== 保留图像数据类 =====
-keep public class com.mx.mxSdk.LogoImage { *; }
-keep public class com.mx.mxSdk.LogoData { *; }
-keep public class com.mx.mxSdk.MultiRowImage { *; }
-keep public class com.mx.mxSdk.MultiRowData { *; }
-keep public class com.mx.mxSdk.RowImage { *; }
-keep public class com.mx.mxSdk.RowData { *; }

# ===== 保留扩展数据类 =====
-keep public class com.mx.mxSdk.ExtLogoData { *; }
-keep public class com.mx.mxSdk.ExtMultiRowData { *; }
-keep public class com.mx.mxSdk.ExtRowData { *; }
-keep public class com.mx.mxSdk.ExtCMYKMultiRowData { *; }
-keep public class com.mx.mxSdk.ExtCMYKRowData { *; }

# ===== 保留 CMYK 数据类 =====
-keep public class com.mx.mxSdk.CMYKMultiRowData { *; }
-keep public class com.mx.mxSdk.CMYKRowData { *; }
-keep public class com.mx.mxSdk.CMYKChannel { *; }
-keep public class com.mx.mxSdk.CMYKType { *; }
-keep public class com.mx.mxSdk.GrayType { *; }

# ===== 保留工厂类 =====
-keep public class com.mx.mxSdk.LogoDataFactory {
    public *;
}
-keep public class com.mx.mxSdk.MultiRowDataFactory {
    public *;
}
-keep public class com.mx.mxSdk.MultiRowImageFactory {
    public *;
}
-keep public class com.mx.mxSdk.SimulationImageFactory {
    public *;
}

# ===== 保留枚举类 =====
-keep public class com.mx.mxSdk.ConnType { *; }
-keep public class com.mx.mxSdk.ConnModel { *; }
-keep public class com.mx.mxSdk.RowLayoutDirection { *; }
-keep public class com.mx.mxSdk.TransportProtocol { *; }

# ===== 保留常量类 =====
-keep public class com.mx.mxSdk.FactoryErrorCodes { *; }
-keep public class com.mx.mxSdk.Error { *; }
-keep public class com.mx.mxSdk.Errors { *; }

# ===== 保留配置类 =====
-keep public class com.mx.mxSdk.MxConfig { *; }

# ===== 保留设备相关 =====
-keep public class com.mx.mxSdk.DeviceModel { *; }
-keep public class com.mx.mxSdk.DeviceDefinitionRegistry { *; }
-keep public class com.mx.mxSdk.PrintRecord { *; }

# ===== 保留工具类（用户可能直接使用）=====
-keep public class com.mx.mxSdk.MxUtils {
    public *;
}
-keep public class com.mx.mxSdk.Compress {
    public *;
}

# ===== 保留其他公开工具类 =====
-keep public class com.mx.mxSdk.Opcode { *; }
-keep public class com.mx.mxSdk.Command { *; }
-keep public class com.mx.mxSdk.DataObj { *; }
-keep public class com.mx.mxSdk.CRC16 { *; }
-keep public class com.mx.mxSdk.MxImageUtils { *; }
-keep public class com.mx.mxSdk.ZlibUtils { *; }
-keep public class com.mx.mxSdk.CommandContext { *; }
-keep public class com.mx.mxSdk.DataObjContext { *; }
-keep public class com.mx.mxSdk.D72Obj { *; }
-keep public class com.mx.mxSdk.Utils.Arrays { *; }
-keep public class com.mx.mxSdk.Utils.RBQLog { *; }

# ===== 保留其他公共工具类（续） =====
-keep public class com.mx.mxSdk.JsonStreamAssembler {
    public *;
}
-keep public interface com.mx.mxSdk.JsonStreamAssembler$OnJsonCompleteListener { *; }
-keep public class com.mx.mxSdk.RepeatingTask {
    public *;
}
-keep public interface com.mx.mxSdk.RepeatingTask$Callback { *; }
-keep public class com.mx.mxSdk.RepeatingTaskWithTimeout {
    public *;
}
-keep public interface com.mx.mxSdk.RepeatingTaskWithTimeout$Callback { *; }
-keep public class com.mx.mxSdk.DataStore.** { *; }
-keep public class com.mx.mxSdk.Model.** { *; }
-keep public class com.mx.mxSdk.OpencvUtils.** { *; }
-keep public class com.mx.mxSdk.Safe.** { *; }
-keep public class com.mx.mxSdk.Views.** { *; }

# ===== 保留BLE相关类 =====
-keep public class com.mx.mxSdk.BleCenter.BleManager { *; }
-keep public class com.mx.mxSdk.BleCenter.BleDevice { *; }
-keep public class com.mx.mxSdk.BleCenter.gatt.data.ServiceInfo { *; }
-keep public class com.mx.mxSdk.BleCenter.gatt.data.CharacteristicInfo { *; }
-keep public class com.mx.mxSdk.ConnModel { *; }

# ===== 保留Packet类 =====
-keep public class com.mx.mxSdk.Packet.MultiRowDataPacket { *; }
-keep public class com.mx.mxSdk.Packet.OtaPacket { *; }
-keep public class com.mx.mxSdk.Packet.LogoPacket { *; }
-keep public class com.mx.mxSdk.Packet.CMYKMultiRowDataPacket { *; }

# ===== 保留BLE回调接口 =====
-keep public interface com.mx.mxSdk.BleCenter.gatt.callback.** { *; }
-keep public interface com.mx.mxSdk.BleCenter.scan.** { *; }

# ===== 第三方库保留（必要） =====
-keep class org.opencv.** { *; }
-keep class tp.xmaihh.serialport.** { *; }
-keep class android_serialport_api.** { *; }
-keep class com.hjq.permissions.** { *; }

# ===== 保留 Parcelable 和 Serializable =====
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===== 保留 JSON 相关 =====
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-dontwarn org.json.**
-keep class org.json.** { *; }

# ===== 保留所有其他 mxSdk 公共类（兜底规则） =====
-keep public class com.mx.mxSdk.** {
    public *;
}

# ===== 保留 Native 方法 =====
-keepclasseswithmembernames class * {
    native <methods>;
}

# ===== 保留可能被反射使用的类 =====
-keepclassmembers class * {
    public <init>();
    public <fields>;
    public <methods>;
}

# ===== 优化配置 =====
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===== 生成映射文件（用于问题定位） =====
-printmapping mapping.txt

# ===== 日志处理（可选：发布时删除日志） =====
# 取消注释可删除调试日志
# -assumenosideeffects class com.mx.mxSdk.Utils.RBQLog {
#     public static *** d(...);
#     public static *** v(...);
#     public static *** i(...);
# }

# ===== 补充遗漏的保留规则 =====

# 保留图像压缩模块（完整保留，包含 Builder 模式）
-keep public class com.mx.mxSdk.imageCompress.** { *; }

# 保留条件管理模块
-keep public class com.mx.mxSdk.Conditions.** { *; }

# 保留 SimulationImageFactory 监听器接口
-keep public interface com.mx.mxSdk.SimulationImageFactory$OnCreateSimulationImageListener { *; }
-keep public interface com.mx.mxSdk.SimulationImageFactory$OnCreateGrayTypeSimulationImageListener { *; }
-keep public interface com.mx.mxSdk.SimulationImageFactory$OnCreateGrayTypeSimulationImageWithSaveListener { *; }

# 保留 Command 和 DataObj 回调接口
-keep public interface com.mx.mxSdk.Command$Callback { *; }
-keep public interface com.mx.mxSdk.DataObj$Callback { *; }

# 保留 BLE 相关监听器
-keep public interface com.mx.mxSdk.BleCenter.BleReceiver$BluetoothStateChangedListener { *; }
-keep class com.mx.mxSdk.BleCenter.gatt.BleHandlerThread { *; }

# 保留 SPP 相关监听器接口
-keep public interface com.mx.mxSdk.SppCenter.BluetoothDiscoverUtils$OnBluetoothDeviceListener { *; }
-keep public interface com.mx.mxSdk.SppCenter.ReadThread$OnReadDataListener { *; }
-keep public interface com.mx.mxSdk.SppCenter.BluetoothSocketThread$OnSocketConnListener { *; }
-keep public interface com.mx.mxSdk.SppCenter.ConnectA2dpThread$OnA2dpConnListener { *; }
-keep public interface com.mx.mxSdk.SppCenter.ServiceThread$OnServiceListener { *; }
-keep public interface com.mx.mxSdk.SppCenter.A2dpProfileProxyUtils$OnA2dpServiceConnStatedListener { *; }

# 保留 WiFi 相关监听器接口
-keep public interface com.mx.mxSdk.WifiCenter.TcpClientThread$OnTCPConnectListener { *; }
-keep public interface com.mx.mxSdk.WifiCenter.UdpServiceThread$OnUpdMonitorListener { *; }

# 保留串口相关监听器接口
-keep public interface com.mx.mxSdk.Serial.SerialThread$OnSerialConnectListener { *; }
