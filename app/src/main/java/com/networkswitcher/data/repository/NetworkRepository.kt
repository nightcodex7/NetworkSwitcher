package com.networkswitcher.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.networkswitcher.data.model.NetworkMode
import com.networkswitcher.data.model.SimInfo

class NetworkRepository(private val context: Context) {
    
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun getCarrierName(): String {
        return try {
            telephonyManager.networkOperatorName.ifEmpty { "Unknown Carrier" }
        } catch (e: Exception) {
            "Unknown Carrier"
        }
    }

    fun getCurrentNetworkTypeName(): String {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return "Disconnected"
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "Disconnected"
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "Not Mobile Data"
            }
            
            // Check underlying data network type
            val networkType = try {
                telephonyManager.dataNetworkType
            } catch (e: SecurityException) {
                TelephonyManager.NETWORK_TYPE_UNKNOWN
            }
            
            when (networkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G (LTE)"
                TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                else -> "Connected (Cellular)"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getActiveSimCount(): Int {
        return try {
            subscriptionManager.activeSubscriptionInfoCount
        } catch (e: Exception) {
            1
        }
    }

    fun getActiveSims(): List<SimInfo> {
        val simList = mutableListOf<SimInfo>()
        try {
            val activeList = subscriptionManager.activeSubscriptionInfoList
            if (activeList != null) {
                for (info in activeList) {
                    val subId = info.subscriptionId
                    val slotId = info.simSlotIndex
                    val displayName = info.displayName?.toString() ?: "SIM ${slotId + 1}"
                    val carrierName = info.carrierName?.toString() ?: "Unknown Carrier"
                    
                    // Get network type for this specific subId
                    val specificTelephony = telephonyManager.createForSubscriptionId(subId)
                    val networkType = try {
                        specificTelephony.dataNetworkType
                    } catch (e: SecurityException) {
                        TelephonyManager.NETWORK_TYPE_UNKNOWN
                    }
                    
                    val networkTypeName = when (networkType) {
                        TelephonyManager.NETWORK_TYPE_NR -> "5G"
                        TelephonyManager.NETWORK_TYPE_LTE -> "4G (LTE)"
                        TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA,
                        TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                        TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                        else -> "No Service / Unknown"
                    }
                    
                    // Get current mode value from settings
                    val resolver = context.contentResolver
                    val currentModeValue = android.provider.Settings.Global.getInt(
                        resolver, 
                        "preferred_network_mode$subId", 
                        android.provider.Settings.Global.getInt(resolver, "preferred_network_mode", -1)
                    )
                    
                    val resolvedMode = when (currentModeValue) {
                        27 -> NetworkMode.FIVE_G_ONLY
                        26 -> NetworkMode.FIVE_G_PREFERRED
                        11 -> NetworkMode.FOUR_G_ONLY
                        else -> null
                    }
                    
                    simList.add(
                        SimInfo(
                            subscriptionId = subId,
                            slotIndex = slotId,
                            displayName = displayName,
                            carrierName = carrierName,
                            networkTypeName = networkTypeName,
                            currentModeValue = currentModeValue,
                            resolvedMode = resolvedMode
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // Read phone state permission missing
        } catch (e: Exception) {
            // Other exceptions
        }
        
        // If the list is empty, fallback to virtual SIM 1
        if (simList.isEmpty()) {
            val carrier = getCarrierName()
            val netType = getCurrentNetworkTypeName()
            val resolver = context.contentResolver
            val modeVal = android.provider.Settings.Global.getInt(resolver, "preferred_network_mode", -1)
            val resolved = when (modeVal) {
                27 -> NetworkMode.FIVE_G_ONLY
                26 -> NetworkMode.FIVE_G_PREFERRED
                11 -> NetworkMode.FOUR_G_ONLY
                else -> null
            }
            simList.add(
                SimInfo(
                    subscriptionId = -1,
                    slotIndex = 0,
                    displayName = "SIM 1",
                    carrierName = carrier,
                    networkTypeName = netType,
                    currentModeValue = modeVal,
                    resolvedMode = resolved
                )
            )
        }
        
        return simList
    }
}
