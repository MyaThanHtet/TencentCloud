package com.tencent.liteav.login.ui.utils;

import android.content.Context;

public class Utils {
    public static final String PACKAGE_NAME_TRTC_DEMO = "com.tencent.trtc";
    public static final String PACKAGE_NAME_LITEAV_DEMO = "com.tencent.liteav.demo";

    public static final boolean isTRTCDemo(Context context) {
        String packageName = context.getPackageName();

        return PACKAGE_NAME_TRTC_DEMO.equals(packageName);
    }
}
