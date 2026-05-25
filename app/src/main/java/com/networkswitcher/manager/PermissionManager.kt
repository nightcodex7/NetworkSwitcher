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
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return if (!isShizukuRunning()) {
            false
        } else {
            try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Throwable) {
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
