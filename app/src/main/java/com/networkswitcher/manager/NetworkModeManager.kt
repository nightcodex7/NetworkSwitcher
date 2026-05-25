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

    fun applyNetworkMode(mode: NetworkMode, slotId: Int, subId: Int): Boolean {
        var success = false

        // Method 1: Modify Settings directly via ContentResolver (if WRITE_SECURE_SETTINGS is granted)
        if (permissionManager.hasWriteSecureSettings()) {
            try {
                success = writeSettingsDirectly(mode, subId)
            } catch (e: Exception) {
                Log.e("NetworkModeManager", "Failed to write secure settings directly for subId $subId", e)
            }
        }

        // Method 2: Shizuku / Shell Command Execution (for fallback and redundancy)
        if (permissionManager.isShizukuRunning() && permissionManager.hasShizukuPermission()) {
            try {
                val shizukuSuccess = writeSettingsViaShizuku(mode, slotId, subId)
                success = success || shizukuSuccess
            } catch (e: Exception) {
                Log.e("NetworkModeManager", "Failed to write secure settings via Shizuku for slot $slotId", e)
            }
        }

        return success
    }

    private fun writeSettingsDirectly(mode: NetworkMode, subId: Int): Boolean {
        val resolver = context.contentResolver
        
        // Write to global key
        Settings.Global.putInt(resolver, "preferred_network_mode", mode.settingsValue)
        
        // Write to subscription-specific key
        if (subId != -1) {
            Settings.Global.putInt(resolver, "preferred_network_mode$subId", mode.settingsValue)
        }
        
        return true
    }

    private fun writeSettingsViaShizuku(mode: NetworkMode, slotId: Int, subId: Int): Boolean {
        var overallSuccess = true
        
        // Execute setting writes as shell user
        val cmdGlobal = "settings put global preferred_network_mode ${mode.settingsValue}"
        overallSuccess = overallSuccess && executeShellCommand(cmdGlobal)

        if (subId != -1) {
            val cmdSub = "settings put global preferred_network_mode$subId ${mode.settingsValue}"
            executeShellCommand(cmdSub)
        }

        // Write allowed network types bitmask via modern telephony cmd (Android 12 to 17)
        val bitmaskStr = java.lang.Long.toBinaryString(mode.allowedTypesBitmask).padStart(20, '0')
        val cmdAllowedTypes = "cmd phone set-allowed-network-types-for-users -s $slotId $bitmaskStr"
        val cmdRes = executeShellCommand(cmdAllowedTypes)
        overallSuccess = overallSuccess && cmdRes

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
