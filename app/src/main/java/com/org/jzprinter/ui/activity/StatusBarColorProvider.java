package com.org.jzprinter.ui.activity;

import com.org.jzprinter.R;

public interface StatusBarColorProvider {
    default int getStatusBarColorResId() {
        return R.color.white; // 默认实现返回0或默认颜色ID
    }
    default int getStatusBarColorAttributeId() {
        return R.attr.app_normal_color; // 默认实现返回0或默认属性ID
    }
}
