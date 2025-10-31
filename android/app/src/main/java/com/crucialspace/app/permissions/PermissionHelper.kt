package com.crucialspace.app.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

fun requestPostNotificationsIfNeeded(activity: Activity, launcher: ActivityResultLauncher<String>) {
    if (Build.VERSION.SDK_INT >= 33) {
        val granted = ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

fun requestIgnoreBatteryOptimizations(activity: Activity) {
    val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
    val pkg = activity.packageName
    if (!pm.isIgnoringBatteryOptimizations(pkg)) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$pkg")
            }
            activity.startActivity(intent)
        } catch (_: Exception) { }
    }
}


