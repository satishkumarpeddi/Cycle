package com.cycle.speedometer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

        // Speed threshold check for buzz sound
        lifecycleScope.launch {
            var isSpeedAlertArmed = true
            gpsManager.speedFlow.collectLatest { currentSpeed ->
                val rideStats = rideTracker.statsFlow.value
                val isRideActive = rideStats.status == com.cycle.speedometer.tracking.RideStatus.ACTIVE
                
                if (isRideActive) {
                    if (currentSpeed > 25.0f && isSpeedAlertArmed) {
                        playLoudBuzz()
                        isSpeedAlertArmed = false
                    } else if (currentSpeed < 23.0f) {
                        isSpeedAlertArmed = true
                    }
                } else {
                    isSpeedAlertArmed = true
                }
            }
        }

        setContent {
            CycleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplashScreen by remember { mutableStateOf(true) }
                    
                    if (showSplashScreen) {
                        SplashScreen(
                            onAnimationFinished = {
                                showSplashScreen = false
                            }
                        )
                    } else {
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

    private fun playLoudBuzz() {
        try {
            val toneGenerator = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_ALARM,
                100
            )
            toneGenerator.startTone(android.media.ToneGenerator.TONE_SUP_ERROR, 800)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGenerator.release()
            }, 1000)

            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        android.os.VibrationEffect.createOneShot(
                            800,
                            android.os.VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(800)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(180.dp)
                    .border(2.dp, NeonGreen, RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

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

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    // Entrance animations
    val scale = remember { androidx.compose.animation.core.Animatable(0f) }
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
    
    // Heartbeat border / glow pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        // Run entrance animations concurrently
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
            )
        }
        
        // Show splash screen for 2.2 seconds
        delay(2200L)
        
        // Exit animation (smooth fade out)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer(
                scaleX = scale.value,
                scaleY = scale.value,
                alpha = alpha.value
            )
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .size(200.dp)
                    .border(
                        width = (2.dp * glowPulse),
                        color = NeonGreen.copy(alpha = glowPulse),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .shadow(
                        elevation = (12.dp * glowPulse),
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = NeonGreen,
                        spotColor = NeonGreen
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "REDEMPTION SPEEDOMETER",
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                modifier = Modifier.graphicsLayer(alpha = alpha.value)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Tracking Every Pedal Stroke",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.graphicsLayer(alpha = alpha.value * 0.7f)
            )
        }
    }
}
