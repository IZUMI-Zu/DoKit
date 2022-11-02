package com.didichuxing.doraemonkit.config;

import com.didichuxing.doraemonkit.constant.SharedPrefsKey;
import com.didichuxing.doraemonkit.util.DoKitSPUtil;

public class DesignCheckConfig {
    public static boolean isDesignCheckOpen() {
        return DoKitSPUtil.getBoolean(SharedPrefsKey.DESIGN_CHECK_OPEN, false);
    }

    public static void setDesignCheckOpen(boolean open) {
        DoKitSPUtil.putBoolean(SharedPrefsKey.DESIGN_CHECK_OPEN, open);
    }
}
