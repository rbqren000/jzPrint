package com.mx.mxSdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mx.mxSdk.Utils.RBQLog;
import java.util.HashMap;
import java.util.Map;

public class DeviceDefinitionRegistry {

    public static final String DeviceModel_UNKNOWN = "INKSI";
    public static final String DeviceModel_MX_02 = "MX-02";
    public static final String DeviceModel_MX_06 = "MX-06";
    public static final String DeviceModel_INKSI_01 = "INKSI-01";

    public static final String DeviceModel_INKSI_01_Lite = "INKSI-01 Lite";

    public static final String DeviceModel_CRYPTO_STAMP = "crypto stamp";

    public static final int DeviceType_NONE = 0;
    public static final int DeviceType_MX_02 = 1;
    public static final int DeviceType_MX_06 = 2;
    public static final int DeviceType_INKSI_01 = 3;

    public static final int DeviceType_INKSI_01_Lite = 4;

    public static final int DeviceType_CRYPTO_STAMP = 5;

    private static final Map<String, String> modelMap = new HashMap<>();
    private static final Map<String, Integer> typeMap = new HashMap<>();

    //单例
    private DeviceDefinitionRegistry() {
        // 初始化设备型号映射
        modelMap.put("BX20",DeviceModel_INKSI_01);
        modelMap.put("BX21",DeviceModel_INKSI_01_Lite);
        modelMap.put("BX22",DeviceModel_CRYPTO_STAMP);
        // 初始化设备类型映射
        typeMap.put("BX20",DeviceType_INKSI_01);
        typeMap.put("BX21",DeviceType_INKSI_01_Lite);
        typeMap.put("BX22",DeviceType_CRYPTO_STAMP);
    }
    private static class SingletonHolder {
        private static final DeviceDefinitionRegistry INSTANCE = new DeviceDefinitionRegistry();
    }
    public static DeviceDefinitionRegistry getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public String getDeviceModelByDeviceModelData(String device_model_data_str) {
        if (device_model_data_str == null){
            return DeviceModel_UNKNOWN;
        }
        return modelMap.getOrDefault(device_model_data_str,DeviceModel_UNKNOWN);
    }

    // device_model_data_str
    public Integer getDeviceTypeByDeviceModelData(String device_model_data_str) {
        if (device_model_data_str == null) {
            return DeviceType_NONE;
        }
        return typeMap.getOrDefault(device_model_data_str,DeviceType_NONE);
    }

    // 工厂方法
    public DeviceModel createMX02DeviceModel(@Nullable String last4MacStr) {
        // 使用三元运算符简化赋值逻辑，直接内联模板
        String aliases = last4MacStr == null ? DeviceModel_MX_02 : String.format("%s_%s", DeviceModel_MX_02, last4MacStr);
        // 返回 DeviceModel 实例
        return new DeviceModel(DeviceType_MX_02, aliases,null);
    }

    public DeviceModel createMX06DeviceModel(@Nullable String last4MacStr) {
        String aliases = last4MacStr == null ? DeviceModel_MX_06 : String.format("%s_%s", DeviceModel_MX_06, last4MacStr);
        return new DeviceModel(DeviceType_MX_06, aliases,null);
    }

    public DeviceModel createNewDeviceModel(@NonNull String last4MacStr, @Nullable String device_model_data_str) {
        RBQLog.i("【createNewDeviceModel】last4MacStr:"+last4MacStr+"; device_model_data_str:"+device_model_data_str);
        int deviceType = getDeviceTypeByDeviceModelData(device_model_data_str);
        String deviceModel = getDeviceModelByDeviceModelData(device_model_data_str);
        String aliases = String.format("%s_%s", deviceModel, last4MacStr);
        return new DeviceModel(deviceType, aliases, deviceModel);
    }
}
