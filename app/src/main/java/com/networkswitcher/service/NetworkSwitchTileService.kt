package com.networkswitcher.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.networkswitcher.NetworkSwitchApplication
import com.networkswitcher.data.model.NetworkMode

class NetworkSwitchTileService : TileService() {

    private val app by lazy { application as NetworkSwitchApplication }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        
        val currentMode = app.settingsRepository.getSavedNetworkMode()
        val nextMode = when (currentMode) {
            NetworkMode.FIVE_G_PREFERRED -> NetworkMode.FIVE_G_ONLY
            NetworkMode.FIVE_G_ONLY -> NetworkMode.FOUR_G_ONLY
            NetworkMode.FOUR_G_ONLY -> NetworkMode.FIVE_G_PREFERRED
        }

        val success = app.networkModeManager.applyNetworkMode(nextMode)
        if (success) {
            app.settingsRepository.saveNetworkMode(nextMode)
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val mode = app.settingsRepository.getSavedNetworkMode()
        
        tile.label = "Net: ${mode.title}"
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }
}
