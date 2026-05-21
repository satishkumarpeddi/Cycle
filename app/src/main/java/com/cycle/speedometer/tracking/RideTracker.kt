package com.cycle.speedometer.tracking

import android.location.Location
import com.cycle.speedometer.calories.CalorieCalculator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RideTracker {

    private val trackerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    private val _statsFlow = MutableStateFlow(RideStats())
    val statsFlow: StateFlow<RideStats> = _statsFlow.asStateFlow()

    private var lastLocation: Location? = null
    var riderWeightKg: Double = 70.0
        set(value) {
            field = value
            updateCalories()
        }

    fun startRide() {
        val current = _statsFlow.value
        if (current.status == RideStatus.ACTIVE) return

        _statsFlow.value = current.copy(status = RideStatus.ACTIVE)
        lastLocation = null
        startTimer()
    }

    fun pauseRide() {
        val current = _statsFlow.value
        if (current.status != RideStatus.ACTIVE) return

        _statsFlow.value = current.copy(status = RideStatus.PAUSED)
        lastLocation = null
        stopTimer()
    }

    fun resumeRide() {
        val current = _statsFlow.value
        if (current.status != RideStatus.PAUSED) return

        _statsFlow.value = current.copy(status = RideStatus.ACTIVE)
        lastLocation = null
        startTimer()
    }

    fun resetRide() {
        stopTimer()
        lastLocation = null
        _statsFlow.value = RideStats()
    }

    fun onLocationUpdated(location: Location, currentSpeedKmH: Float) {
        val current = _statsFlow.value
        if (current.status != RideStatus.ACTIVE) return

        var addedDistanceMeters = 0.0
        
        // Filter out bad accuracy points (e.g. accuracy > 25 meters)
        // Also check if user is actually moving to prevent location drift at standstill
        val hasGoodAccuracy = location.accuracy <= 25.0f
        val isActuallyMoving = currentSpeedKmH > 1.0f // over 1 km/h

        if (lastLocation != null && hasGoodAccuracy && isActuallyMoving) {
            addedDistanceMeters = lastLocation!!.distanceTo(location).toDouble()
        }

        if (hasGoodAccuracy) {
            lastLocation = location
        }

        val newDistanceKm = current.distanceKm + (addedDistanceMeters / 1000.0)
        
        // Max Speed calculation
        val newMaxSpeed = if (currentSpeedKmH > current.maxSpeedKmH) currentSpeedKmH else current.maxSpeedKmH
        
        // Avg Speed calculation: total distance / total hours active
        val newAvgSpeed = if (current.elapsedTimeSeconds > 0) {
            val hours = current.elapsedTimeSeconds / 3600.0
            (newDistanceKm / hours).toFloat()
        } else {
            currentSpeedKmH
        }

        // Calorie Calculation
        val calories = CalorieCalculator.calculate(newAvgSpeed, riderWeightKg, current.elapsedTimeSeconds)

        _statsFlow.value = current.copy(
            distanceKm = newDistanceKm,
            currentSpeedKmH = currentSpeedKmH,
            maxSpeedKmH = newMaxSpeed,
            avgSpeedKmH = newAvgSpeed,
            caloriesKcal = calories
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = trackerScope.launch {
            while (isActive) {
                delay(1000L)
                val current = _statsFlow.value
                if (current.status == RideStatus.ACTIVE) {
                    val nextSeconds = current.elapsedTimeSeconds + 1
                    val newAvgSpeed = if (nextSeconds > 0) {
                        val hours = nextSeconds / 3600.0
                        (current.distanceKm / hours).toFloat()
                    } else {
                        current.currentSpeedKmH
                    }
                    val calories = CalorieCalculator.calculate(newAvgSpeed, riderWeightKg, nextSeconds)
                    
                    _statsFlow.value = current.copy(
                        elapsedTimeSeconds = nextSeconds,
                        avgSpeedKmH = newAvgSpeed,
                        caloriesKcal = calories
                    )
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateCalories() {
        val current = _statsFlow.value
        val calories = CalorieCalculator.calculate(current.avgSpeedKmH, riderWeightKg, current.elapsedTimeSeconds)
        _statsFlow.value = current.copy(caloriesKcal = calories)
    }

    fun clear() {
        stopTimer()
        trackerScope.cancel()
    }
}
