package com.networkswitcher

import android.app.Application
import com.networkswitcher.data.repository.NetworkRepository
import com.networkswitcher.data.repository.SettingsRepository
import com.networkswitcher.manager.NetworkModeManager
import com.networkswitcher.manager.PermissionManager

class NetworkSwitchApplication : Application() {

    lateinit var networkRepository: NetworkRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var permissionManager: PermissionManager
        private set
    lateinit var networkModeManager: NetworkModeManager
        private set

    override fun onCreate() {
        super.onCreate()

        networkRepository = NetworkRepository(this)
        settingsRepository = SettingsRepository(this)
        permissionManager = PermissionManager(this)
        networkModeManager = NetworkModeManager(this, permissionManager)
    }
}
