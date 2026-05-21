package com.cycle.speedometer.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GpsManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
        
    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow: StateFlow<Location?> = _locationFlow.asStateFlow()

    private val _speedFlow = MutableStateFlow(0f) // in km/h
    val speedFlow: StateFlow<Float> = _speedFlow.asStateFlow()

    private val _satellitesFlow = MutableStateFlow(SatelliteStatus())
    val satellitesFlow: StateFlow<SatelliteStatus> = _satellitesFlow.asStateFlow()

    private val _isGpsEnabled = MutableStateFlow(false)
    val isGpsEnabled: StateFlow<Boolean> = _isGpsEnabled.asStateFlow()

    private var isTracking = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            _locationFlow.value = location
            
            // Speed from GPS is in meters/second. Convert to km/h (1 m/s = 3.6 km/h)
            val speedKmH = if (location.hasSpeed()) {
                location.speed * 3.6f
            } else {
                0f
            }
            _speedFlow.value = speedKmH
        }

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            _isGpsEnabled.value = locationAvailability.isLocationAvailable
        }
    }

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val count = status.satelliteCount
            var usedCount = 0
            for (i in 0 until count) {
                if (status.usedInFix(i)) {
                    usedCount++
                }
            }
            _satellitesFlow.value = SatelliteStatus(
                satellitesInView = count,
                satellitesUsedInFix = usedCount,
                hasSignal = usedCount > 0
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        isTracking = true

        // Check if GPS provider is enabled
        _isGpsEnabled.value = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).apply {
            setMinUpdateIntervalMillis(500L)
            setMinUpdateDistanceMeters(0f)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            
            // Register GNSS status updates for satellite counts
            locationManager.registerGnssStatusCallback(
                gnssStatusCallback,
                Handler(Looper.getMainLooper())
            )
        } catch (e: SecurityException) {
            isTracking = false
            _isGpsEnabled.value = false
        }
    }

    fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        } catch (e: Exception) {
            // Safe removal
        }
        
        _speedFlow.value = 0f
        _locationFlow.value = null
        _satellitesFlow.value = SatelliteStatus()
    }
}
