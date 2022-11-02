package com.didichuxing.doraemonkit.kit.designcheck

import android.app.Activity
import android.content.Context
import com.didichuxing.doraemonkit.DoKit
import com.didichuxing.doraemonkit.R
import com.didichuxing.doraemonkit.config.DesignCheckConfig
import com.didichuxing.doraemonkit.kit.AbstractKit
import com.didichuxing.doraemonkit.util.ToastUtils
import com.google.auto.service.AutoService
import org.opencv.android.OpenCVLoader

@AutoService(AbstractKit::class)
class DesignCheckKit : AbstractKit() {
    override val name: Int
        get() = R.string.dk_kit_design_check
    override val icon: Int
        get() = R.mipmap.dk_design_check

    override val isInnerKit: Boolean
        get() = true

    override fun onClickWithReturn(activity: Activity): Boolean {
        return if (OpenCVLoader.initDebug()) {
            DoKit.launchFloating<DesignCheckInfoDoKitView>()
            DesignCheckConfig.setDesignCheckOpen(true)
            true
        } else {
            ToastUtils.showShort("Error in loading OpenCV lib")
            false
        }
    }

    override fun onAppInit(context: Context?) {
        DesignCheckConfig.setDesignCheckOpen(false)
    }

    override fun innerKitId(): String = "dokit_sdk_ui_ck_design_check"
}

