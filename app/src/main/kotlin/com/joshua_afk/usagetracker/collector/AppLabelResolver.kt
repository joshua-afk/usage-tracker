package com.joshua_afk.usagetracker.collector

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

class AppLabelResolver(context: Context) {
    private val packageManager = context.packageManager
    private val cache = HashMap<String, String>()

    /** Falls back to the package name for uninstalled apps. */
    fun label(packageName: String): String = cache.getOrPut(packageName) {
        try {
            appInfo(packageName).loadLabel(packageManager).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun appInfo(packageName: String): ApplicationInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
}
