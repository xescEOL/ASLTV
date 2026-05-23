package com.xesc.asltv.utils

import android.content.Context
import android.content.pm.PackageManager

object DeviceDetector {
    fun isAndroidTV(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}