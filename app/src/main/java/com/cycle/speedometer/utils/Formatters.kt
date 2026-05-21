package com.cycle.speedometer.utils

import java.util.Locale

object Formatters {

    /**
     * Formats duration in seconds to HH:MM:SS format.
     */
    fun formatElapsedTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    /**
     * Formats speed in km/h to standard single decimal place.
     */
    fun formatSpeed(speedKmH: Float): String {
        return String.format(Locale.US, "%.1f", speedKmH)
    }

    /**
     * Formats distance in km to standard two decimal places.
     */
    fun formatDistance(distanceKm: Double): String {
        return String.format(Locale.US, "%.2f", distanceKm)
    }
}
