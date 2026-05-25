package com.networkswitcher.data.repository

import android.content.Context
import com.networkswitcher.data.model.NetworkMode

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("network_switcher_prefs", Context.MODE_PRIVATE)

    fun getSavedNetworkMode(): NetworkMode = getSavedNetworkMode(0)

    fun getSavedNetworkMode(slotIndex: Int): NetworkMode {
        val name = prefs.getString("last_mode_slot_$slotIndex", NetworkMode.FIVE_G_PREFERRED.name)
        return try {
            NetworkMode.valueOf(name ?: NetworkMode.FIVE_G_PREFERRED.name)
        } catch (e: Exception) {
            NetworkMode.FIVE_G_PREFERRED
        }
    }

    fun saveNetworkMode(mode: NetworkMode) = saveNetworkMode(0, mode)

    fun saveNetworkMode(slotIndex: Int, mode: NetworkMode) {
        prefs.edit().putString("last_mode_slot_$slotIndex", mode.name).apply()
    }
}
