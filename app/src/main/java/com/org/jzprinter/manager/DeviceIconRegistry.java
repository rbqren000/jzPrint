package com.org.jzprinter.manager;

import static com.mx.mxSdk.DeviceDefinitionRegistry.DeviceType_CRYPTO_STAMP;
import static com.mx.mxSdk.DeviceDefinitionRegistry.DeviceType_INKSI_01;
import static com.mx.mxSdk.DeviceDefinitionRegistry.DeviceType_INKSI_01_Lite;
import static com.mx.mxSdk.DeviceDefinitionRegistry.DeviceType_MX_02;
import static com.mx.mxSdk.DeviceDefinitionRegistry.DeviceType_MX_06;

import com.org.jzprinter.R;

import java.util.HashMap;
import java.util.Map;

public class DeviceIconRegistry {
    private static final Map<Integer, Integer> map = new HashMap<>();
    //单例 DeviceIconMap
    private static DeviceIconRegistry instance;
    public static DeviceIconRegistry getInstance() {
        if (instance == null) {
            instance = new DeviceIconRegistry();
        }
        return instance;
    }

    private DeviceIconRegistry() {
        // 初始化设备图标映射
        map.put(DeviceType_MX_02, R.mipmap.ic_inksi_printer);
        map.put(DeviceType_MX_06, R.mipmap.ic_inksi_printer);
        map.put(DeviceType_INKSI_01,R.mipmap.ic_inksi01);
        map.put(DeviceType_INKSI_01_Lite,R.mipmap.ic_inksi01lite);
        map.put(DeviceType_CRYPTO_STAMP,R.mipmap.ic_inksi01);
    }

    public int getDeviceIcon(int deviceType) {
        return map.getOrDefault(deviceType, R.mipmap.ic_inksi_printer);
    }
}
