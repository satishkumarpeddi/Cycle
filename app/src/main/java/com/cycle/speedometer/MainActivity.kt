package com.cycle.speedometer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.cycle.speedometer.gps.GpsManager
import com.cycle.speedometer.gps.SatelliteStatus
import com.cycle.speedometer.tracking.RideStats
import com.cycle.speedometer.tracking.RideTracker
import com.cycle.speedometer.ui.DashboardScreen
import com.cycle.speedometer.ui.theme.CycleTheme
import com.cycle.speedometer.ui.theme.DarkBackground
import com.cycle.speedometer.ui.theme.NeonGreen
import com.cycle.speedometer.ui.theme.TextMuted
import com.cycle.speedometer.ui.theme.TextWhite
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var gpsManager: GpsManager
    private lateinit var rideTracker: RideTracker

    private var hasLocationPermission = mutableStateOf(false)

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission.value = fineGranted || coarseGranted
        if (hasLocationPermission.value) {
            gpsManager.startTracking()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        gpsManager = GpsManager(applicationContext)
        rideTracker = RideTracker()

        // Check current permissions state
        checkPermissions()

        // Core pipeline connecting GPS coordinates directly to telemetry stats
        lifecycleScope.launch {
            gpsManager.locationFlow.collectLatest { location ->
                if (location != null) {
                    val currentSpeedKmH = gpsManager.speedFlow.value
                    rideTracker.onLocationUpdated(location, currentSpeedKmH)
                }
            }
        }

        setContent {
            CycleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val permissionState by hasLocationPermission
                    
                    if (permissionState) {
                        // Gather telemetry states reactively
                        val rideStats by rideTracker.statsFlow.collectAsState()
                        val satelliteStatus by gpsManager.satellitesFlow.collectAsState()
                        val isGpsEnabled by gpsManager.isGpsEnabled.collectAsState()
                        
                        DashboardScreen(
                            rideStats = rideStats,
                            satelliteStatus = satelliteStatus,
                            isGpsEnabled = isGpsEnabled,
                            riderWeightKg = rideTracker.riderWeightKg,
                            onWeightChanged = { weight ->
                                rideTracker.riderWeightKg = weight
                            },
                            onStartRide = {
                                rideTracker.startRide()
                            },
                            onPauseToggle = {
                                val currentStats = rideTracker.statsFlow.value
                                if (currentStats.status == com.cycle.speedometer.tracking.RideStatus.ACTIVE) {
                                    rideTracker.pauseRide()
                                } else {
                                    rideTracker.resumeRide()
                                }
                            },
                            onResetRide = {
                                rideTracker.resetRide()
                            },
                            onStopRide = {
                                rideTracker.pauseRide()
                            }
                        )
                    } else {
                        // Beautiful Permission Request Landing UI
                        PermissionRequestScreen(
                            onRequestPermissions = {
                                requestPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasLocationPermission.value = fineGranted || coarseGranted
        if (hasLocationPermission.value) {
            gpsManager.startTracking()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermission.value) {
            gpsManager.startTracking()
        }
    }

    override fun onPause() {
        super.onPause()
        // We let the tracker continue running, but we can stop locations when app is destroyed
    }

    override fun onDestroy() {
        super.onDestroy()
        gpsManager.stopTracking()
        rideTracker.clear()
    }
}

@Composable
fun PermissionRequestScreen(
    onRequestPermissions: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Location Access Required",
                color = TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Cycle needs your phone's GPS location permission to track your cycling speed, compute total distances, and estimate calories burned accurately.",
                color = TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp)
            ) {
                Text(
                    text = "GRANT PERMISSION",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
