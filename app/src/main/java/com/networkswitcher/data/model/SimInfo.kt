package com.networkswitcher.data.model

data class SimInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val networkTypeName: String,
    val currentModeValue: Int,
    val resolvedMode: NetworkMode?
)
