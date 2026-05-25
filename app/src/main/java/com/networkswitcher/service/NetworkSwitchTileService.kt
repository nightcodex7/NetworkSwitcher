package com.networkswitcher.service

import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.SubscriptionManager
import com.networkswitcher.NetworkSwitchApplication
import com.networkswitcher.data.model.NetworkMode
import com.networkswitcher.data.model.PermissionState
import com.networkswitcher.data.model.SimInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkSwitchTileService : TileService() {

    private val app by lazy { application as NetworkSwitchApplication }
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        
        serviceScope.launch {
            if (app.permissionManager.getPermissionState() != PermissionState.GRANTED) {
                withContext(Dispatchers.Main) {
                    val tile = qsTile
                    if (tile != null) {
                        tile.label = "Need Permission"
                        tile.state = Tile.STATE_INACTIVE
                        tile.updateTile()
                    }
                }
                return@launch
            }

            val sim = getTargetSim()
            val currentMode = sim.resolvedMode ?: app.settingsRepository.getSavedNetworkMode(sim.slotIndex)
            val nextMode = when (currentMode) {
                NetworkMode.FIVE_G_PREFERRED -> NetworkMode.FIVE_G_ONLY
                NetworkMode.FIVE_G_ONLY -> NetworkMode.FOUR_G_ONLY
                NetworkMode.FOUR_G_ONLY -> NetworkMode.FIVE_G_PREFERRED
            }

            val success = app.networkModeManager.applyNetworkMode(nextMode, sim.slotIndex, sim.subscriptionId)
            if (success) {
                app.settingsRepository.saveNetworkMode(sim.slotIndex, nextMode)
            }
            
            withContext(Dispatchers.Main) {
                updateTileState()
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        
        if (app.permissionManager.getPermissionState() != PermissionState.GRANTED) {
            tile.label = "Need Permission"
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
            return
        }

        val sim = getTargetSim()
        val mode = sim.resolvedMode ?: app.settingsRepository.getSavedNetworkMode(sim.slotIndex)
        
        tile.label = "${sim.displayName}: ${mode.title}"
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }

    private fun getTargetSim(): SimInfo {
        val activeSims = app.networkRepository.getActiveSims()
        val defaultDataSubId = try {
            SubscriptionManager.getDefaultDataSubscriptionId()
        } catch (e: Throwable) {
            -1
        }

        if (defaultDataSubId != -1) {
            val matchingSim = activeSims.find { it.subscriptionId == defaultDataSubId }
            if (matchingSim != null) {
                return matchingSim
            }
        }
        return activeSims.firstOrNull() ?: SimInfo(-1, 0, "SIM 1", "Unknown", "Unknown", -1, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
