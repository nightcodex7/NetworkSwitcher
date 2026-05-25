package com.networkswitcher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.networkswitcher.data.model.NetworkMode
import com.networkswitcher.data.model.PermissionState
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
    private val networkRepository: NetworkRepository,
    private val settingsRepository: SettingsRepository,
    private val permissionManager: PermissionManager,
    private val networkModeManager: NetworkModeManager
) : ViewModel() {

    data class UiState(
        val carrierName: String = "",
        val networkTypeName: String = "",
        val activeSimCount: Int = 1,
        val selectedMode: NetworkMode = NetworkMode.FIVE_G_PREFERRED,
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
        val savedMode = settingsRepository.getSavedNetworkMode()
        val carrier = networkRepository.getCarrierName()
        val netType = networkRepository.getCurrentNetworkTypeName()
        val simCount = networkRepository.getActiveSimCount()
        val perm = permissionManager.getPermissionState()

        _uiState.value = UiState(
            carrierName = carrier,
            networkTypeName = netType,
            activeSimCount = simCount,
            selectedMode = savedMode,
            permissionState = perm
        )
    }

    fun applyNetworkMode(mode: NetworkMode) {
        viewModelScope.launch {
            _actionResult.emit(Resource.Loading())
            val success = networkModeManager.applyNetworkMode(mode)
            if (success) {
                settingsRepository.saveNetworkMode(mode)
                refreshState()
                _actionResult.emit(Resource.Success("Successfully applied: ${mode.title}"))
            } else {
                _actionResult.emit(Resource.Error("Failed to apply. Verify permissions are granted."))
            }
        }
    }
}
