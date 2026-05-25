package com.networkswitcher.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

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
}
