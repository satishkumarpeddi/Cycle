package com.cycle.speedometer.gps

data class SatelliteStatus(
    val satellitesInView: Int = 0,
    val satellitesUsedInFix: Int = 0,
    val hasSignal: Boolean = false
)
