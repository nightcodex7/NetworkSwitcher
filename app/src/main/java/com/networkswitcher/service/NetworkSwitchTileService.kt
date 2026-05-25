package com.networkswitcher.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.networkswitcher.NetworkSwitchApplication
import com.networkswitcher.data.model.NetworkMode
import com.networkswitcher.data.model.SimInfo

class NetworkSwitchTileService : TileService() {

    private val app by lazy { application as NetworkSwitchApplication }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        
        val activeSims = app.networkRepository.getActiveSims()
        val sim = activeSims.firstOrNull() ?: SimInfo(-1, 0, "SIM 1", "Unknown", "Unknown", -1, null)

        val currentMode = app.settingsRepository.getSavedNetworkMode(sim.slotIndex)
        val nextMode = when (currentMode) {
            NetworkMode.FIVE_G_PREFERRED -> NetworkMode.FIVE_G_ONLY
            NetworkMode.FIVE_G_ONLY -> NetworkMode.FOUR_G_ONLY
            NetworkMode.FOUR_G_ONLY -> NetworkMode.FIVE_G_PREFERRED
        }

        val success = app.networkModeManager.applyNetworkMode(nextMode, sim.slotIndex, sim.subscriptionId)
        if (success) {
            app.settingsRepository.saveNetworkMode(sim.slotIndex, nextMode)
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val activeSims = app.networkRepository.getActiveSims()
        val sim = activeSims.firstOrNull() ?: SimInfo(-1, 0, "SIM 1", "Unknown", "Unknown", -1, null)
        val mode = app.settingsRepository.getSavedNetworkMode(sim.slotIndex)
        
        tile.label = "Net: ${mode.title}"
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }
}
