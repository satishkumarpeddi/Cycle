package com.cycle.speedometer.tracking

enum class RideStatus {
    IDLE,
    ACTIVE,
    PAUSED
}

data class RideStats(
    val status: RideStatus = RideStatus.IDLE,
    val elapsedTimeSeconds: Long = 0L,
    val distanceKm: Double = 0.0,
    val currentSpeedKmH: Float = 0f,
    val avgSpeedKmH: Float = 0f,
    val maxSpeedKmH: Float = 0f,
    val caloriesKcal: Int = 0
)
