package com.networkswitcher.data.model

enum class NetworkMode(
    val title: String,
    val settingsValue: Int,
    val allowedTypesBitmask: Long
) {
    FIVE_G_ONLY("5G Only", 27, 524288L),              // 1 << 19 (NR)
    FIVE_G_PREFERRED("5G Preferred", 26, 1048575L),    // (1 << 20) - 1 (All types)
    FOUR_G_ONLY("4G Only", 11, 266240L)               // (1 << 12) | (1 << 18) (LTE + LTE_CA)
}
