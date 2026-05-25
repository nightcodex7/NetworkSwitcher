package com.networkswitcher.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.networkswitcher.data.model.NetworkMode
import com.networkswitcher.data.model.PermissionState
import com.networkswitcher.data.model.SimInfo
import com.networkswitcher.data.repository.NetworkRepository
import com.networkswitcher.data.repository.SettingsRepository
import com.networkswitcher.manager.NetworkModeManager
import com.networkswitcher.manager.PermissionManager
import com.networkswitcher.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val context: Context,
    private val networkRepository: NetworkRepository,
    private val settingsRepository: SettingsRepository,
    private val permissionManager: PermissionManager,
    private val networkModeManager: NetworkModeManager
) : ViewModel() {

    data class UiState(
        val activeSims: List<SimInfo> = emptyList(),
        val selectedSimIndex: Int = 0,
        val permissionState: PermissionState = PermissionState.WRITE_SECURE_SETTINGS_MISSING
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _actionResult = MutableSharedFlow<Resource<String>>()
    val actionResult: SharedFlow<Resource<String>> = _actionResult.asSharedFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        val sims = networkRepository.getActiveSims()
        val perm = permissionManager.getPermissionState()
        
        val prevIndex = _uiState.value.selectedSimIndex
        val selectedIndex = if (prevIndex in sims.indices) prevIndex else 0

        _uiState.value = UiState(
            activeSims = sims,
            selectedSimIndex = selectedIndex,
            permissionState = perm
        )
    }

    fun selectSim(index: Int) {
        if (index in _uiState.value.activeSims.indices) {
            _uiState.value = _uiState.value.copy(selectedSimIndex = index)
        }
    }

    fun getSavedNetworkModeForSlot(slotIndex: Int): NetworkMode {
        return settingsRepository.getSavedNetworkMode(slotIndex)
    }

    fun applyNetworkMode(mode: NetworkMode) {
        val currentState = _uiState.value
        val index = currentState.selectedSimIndex
        if (index !in currentState.activeSims.indices) return

        val sim = currentState.activeSims[index]
        viewModelScope.launch {
            _actionResult.emit(Resource.Loading())
            val success = networkModeManager.applyNetworkMode(mode, sim.slotIndex, sim.subscriptionId)
            if (success) {
                settingsRepository.saveNetworkMode(sim.slotIndex, mode)
                refreshState()
                
                // Refresh Quick Settings Tile status
                try {
                    TileService.requestListeningState(
                        context,
                        ComponentName(context, "com.networkswitcher.service.NetworkSwitchTileService")
                    )
                } catch (e: Throwable) {
                    // Ignore background request failures
                }
                
                _actionResult.emit(Resource.Success("Successfully applied: ${mode.title} for ${sim.displayName}"))
            } else {
                _actionResult.emit(Resource.Error("Failed to apply. Verify permissions are granted."))
            }
        }
    }
}
