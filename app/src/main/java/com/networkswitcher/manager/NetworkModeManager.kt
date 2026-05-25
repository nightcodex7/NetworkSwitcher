package com.networkswitcher.manager

import android.content.Context
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.util.Log
import com.networkswitcher.data.model.NetworkMode
import rikka.shizuku.Shizuku

class NetworkModeManager(
    private val context: Context,
    private val permissionManager: PermissionManager
) {
    private val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

    fun applyNetworkMode(mode: NetworkMode): Boolean {
        var success = false

        // Method 1: Modify Global Settings directly via ContentResolver (if WRITE_SECURE_SETTINGS is granted)
        if (permissionManager.hasWriteSecureSettings()) {
            try {
                success = writeSettingsDirectly(mode)
            } catch (e: Exception) {
                Log.e("NetworkModeManager", "Failed to write secure settings directly", e)
            }
        }

        // Method 2: Shizuku / Shell Command Execution (for fallback and redundancy)
        if (permissionManager.isShizukuRunning() && permissionManager.hasShizukuPermission()) {
            try {
                val shizukuSuccess = writeSettingsViaShizuku(mode)
                success = success || shizukuSuccess
            } catch (e: Exception) {
                Log.e("NetworkModeManager", "Failed to write secure settings via Shizuku", e)
            }
        }

        return success
    }

    private fun writeSettingsDirectly(mode: NetworkMode): Boolean {
        val resolver = context.contentResolver
        
        // Write to default global setting key
        Settings.Global.putInt(resolver, "preferred_network_mode", mode.settingsValue)
        
        // Write to subscription-specific keys if present
        try {
            val activeList = subscriptionManager.activeSubscriptionInfoList
            if (activeList != null) {
                for (info in activeList) {
                    val subId = info.subscriptionId
                    Settings.Global.putInt(resolver, "preferred_network_mode$subId", mode.settingsValue)
                }
            }
        } catch (e: SecurityException) {
            Log.w("NetworkModeManager", "No READ_PHONE_STATE permission to retrieve SIM subIds", e)
        }
        
        return true
    }

    private fun writeSettingsViaShizuku(mode: NetworkMode): Boolean {
        var overallSuccess = true
        
        // Execute setting writes as shell user
        val cmd1 = "settings put global preferred_network_mode ${mode.settingsValue}"
        overallSuccess = overallSuccess && executeShellCommand(cmd1)

        try {
            val activeList = subscriptionManager.activeSubscriptionInfoList
            if (activeList != null && activeList.isNotEmpty()) {
                for (info in activeList) {
                    val subId = info.subscriptionId
                    val slotId = info.simSlotIndex
                    
                    // 1. Write subscription-specific preferred_network_mode
                    val cmdSub = "settings put global preferred_network_mode$subId ${mode.settingsValue}"
                    executeShellCommand(cmdSub)

                    // 2. Write allowed network types bitmask via modern telephony cmd (Android 12 to 17)
                    val cmdAllowedTypes = "cmd phone set-allowed-network-types-for-users -s $slotId ${mode.allowedTypesBitmask}"
                    val cmdRes = executeShellCommand(cmdAllowedTypes)
                    overallSuccess = overallSuccess && cmdRes
                }
            } else {
                // Fallback to slot 0 if subscription info is empty
                val cmdAllowedTypesFallback = "cmd phone set-allowed-network-types-for-users -s 0 ${mode.allowedTypesBitmask}"
                overallSuccess = overallSuccess && executeShellCommand(cmdAllowedTypesFallback)
            }
        } catch (e: SecurityException) {
            // If subscriptionManager cannot be accessed, try slot 0 & 1 blindly
            executeShellCommand("cmd phone set-allowed-network-types-for-users -s 0 ${mode.allowedTypesBitmask}")
            executeShellCommand("cmd phone set-allowed-network-types-for-users -s 1 ${mode.allowedTypesBitmask}")
        }

        return overallSuccess
    }

    private fun executeShellCommand(cmd: String): Boolean {
        return try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            Log.e("NetworkModeManager", "Shizuku exec failed for cmd: $cmd", e)
            false
        }
    }
}
