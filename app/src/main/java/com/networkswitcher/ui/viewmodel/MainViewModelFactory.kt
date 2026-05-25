package com.networkswitcher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.networkswitcher.data.repository.NetworkRepository
import com.networkswitcher.data.repository.SettingsRepository
import com.networkswitcher.manager.NetworkModeManager
import com.networkswitcher.manager.PermissionManager

class MainViewModelFactory(
    private val networkRepository: NetworkRepository,
    private val settingsRepository: SettingsRepository,
    private val permissionManager: PermissionManager,
    private val networkModeManager: NetworkModeManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                networkRepository,
                settingsRepository,
                permissionManager,
                networkModeManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
