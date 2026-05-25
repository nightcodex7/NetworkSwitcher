package com.networkswitcher.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.networkswitcher.data.model.PermissionState
import rikka.shizuku.Shizuku

class PermissionManager(private val context: Context) {

    fun hasWriteSecureSettings(): Boolean {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    fun isShizukuRunning(): Boolean {
        return try {
            val running = Shizuku.pingBinder()
            android.util.Log.d("PermissionManager", "isShizukuRunning: pingBinder=$running")
            running
        } catch (e: Throwable) {
            android.util.Log.e("PermissionManager", "isShizukuRunning: exception", e)
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return if (!isShizukuRunning()) {
            android.util.Log.d("PermissionManager", "hasShizukuPermission: Shizuku not running")
            false
        } else {
            try {
                val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                android.util.Log.d("PermissionManager", "hasShizukuPermission: checkSelfPermission result granted=$granted")
                granted
            } catch (e: Throwable) {
                android.util.Log.e("PermissionManager", "hasShizukuPermission: exception", e)
                false
            }
        }
    }

    fun getPermissionState(): PermissionState {
        if (hasWriteSecureSettings()) {
            return PermissionState.GRANTED
        }
        return if (isShizukuRunning()) {
            if (hasShizukuPermission()) {
                PermissionState.GRANTED
            } else {
                PermissionState.SHIZUKU_DENIED
            }
        } else {
            PermissionState.SHIZUKU_NOT_RUNNING
        }
    }
}
