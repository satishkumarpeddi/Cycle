package com.cycle.speedometer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cycle.speedometer.gps.SatelliteStatus
import com.cycle.speedometer.tracking.RideStats
import com.cycle.speedometer.tracking.RideStatus
import com.cycle.speedometer.ui.components.LiveStatCard
import com.cycle.speedometer.ui.components.OdometerDisplay
import com.cycle.speedometer.ui.components.RideTimerBlock
import com.cycle.speedometer.ui.components.SpeedometerGauge
import com.cycle.speedometer.ui.theme.*
import com.cycle.speedometer.utils.Formatters
import androidx.compose.ui.graphics.vector.path
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun DashboardScreen(
    rideStats: RideStats,
    satelliteStatus: SatelliteStatus,
    isGpsEnabled: Boolean,
    riderWeightKg: Double,
    onWeightChanged: (Double) -> Unit,
    onStartRide: () -> Unit,
    onPauseToggle: () -> Unit,
    onResetRide: () -> Unit,
    onStopRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWeightDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf(riderWeightKg.toString()) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var summaryStats by remember { mutableStateOf<RideStats?>(null) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. GPS Status Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Speedometer",
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Signal Icon
                    Icon(
                        imageVector = GpsSignalIconVector(),
                        contentDescription = "GPS Status",
                        tint = if (isGpsEnabled && satelliteStatus.hasSignal) NeonGreen else Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Text(
                        text = if (isGpsEnabled) {
                            "GPS: Yes (${satelliteStatus.satellitesUsedInFix}/${satelliteStatus.satellitesInView} Satellites)"
                        } else {
                            "GPS: No (0/0 Satellites)"
                        },
                        color = if (isGpsEnabled && satelliteStatus.hasSignal) TextWhite else TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 2. Circular speedometer gauge
            SpeedometerGauge(
                currentSpeed = rideStats.currentSpeedKmH,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
            )

            // 3. Odometer & Mode Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OdometerDisplay(distanceKm = rideStats.distanceKm)

                // Ride Mode Selector / Weight Settings
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .border(1.dp, CardOutline, RoundedCornerShape(20.dp))
                        .clickable {
                            weightInput = riderWeightKg.toString()
                            showWeightDialog = true
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = BicycleIconVector(),
                            contentDescription = "Cycling Mode",
                            tint = TextWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${riderWeightKg.toInt()}kg",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = DropdownArrowIconVector(),
                            contentDescription = "Rider settings",
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // 4. Timer Block
            val isTimerRunning = rideStats.status != RideStatus.IDLE
            val isTimerPaused = rideStats.status == RideStatus.PAUSED
            
            RideTimerBlock(
                elapsedTimeStr = Formatters.formatElapsedTime(rideStats.elapsedTimeSeconds),
                isRunning = isTimerRunning,
                isPaused = isTimerPaused,
                onPauseToggle = onPauseToggle,
                onReset = onResetRide
            )

            // 5. Grid of Statistics Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiveStatCard(
                    value = Formatters.formatDistance(rideStats.distanceKm),
                    unit = "km",
                    label = "Distance"
                )
                
                LiveStatCard(
                    value = Formatters.formatSpeed(rideStats.avgSpeedKmH),
                    unit = "km/h",
                    label = "Avg speed"
                )
                
                LiveStatCard(
                    value = Formatters.formatSpeed(rideStats.maxSpeedKmH),
                    unit = "km/h",
                    label = "Max speed"
                )
            }

            // Calories card & details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CardOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Calories Burned",
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MET Formula Dynamic Tracker",
                            color = TextMuted.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = rideStats.caloriesKcal.toString(),
                            color = NeonGreen,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "kcal",
                            color = NeonGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 6. Action Button (START / STOP)
            val isRideActive = rideStats.status == RideStatus.ACTIVE || rideStats.status == RideStatus.PAUSED
            
            Button(
                onClick = {
                    if (isRideActive) {
                        // Capture summary statistics before stopping
                        summaryStats = rideStats
                        onStopRide()
                        showSummaryDialog = true
                    } else {
                        onStartRide()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRideActive) Color(0xFFFF2B4B) else NeonGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(32.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isRideActive) "STOP" else "START",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isRideActive) Color.White else Color.Black
                    )
                    
                    Icon(
                        imageVector = if (isRideActive) StopArrowIconVector() else StartArrowIconVector(),
                        contentDescription = null,
                        tint = if (isRideActive) Color.White else Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Rider Weight Settings Dialog
        if (showWeightDialog) {
            AlertDialog(
                onDismissRequest = { showWeightDialog = false },
                title = { Text("Rider Settings", color = TextWhite) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Set your weight to calculate calories burned accurately:", color = TextMuted)
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (kg)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = CardOutline,
                                focusedLabelColor = NeonGreen,
                                unfocusedLabelColor = TextMuted
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val w = weightInput.toDoubleOrNull() ?: 70.0
                            onWeightChanged(w)
                            showWeightDialog = false
                        }
                    ) {
                        Text("SAVE", color = NeonGreen)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWeightDialog = false }) {
                        Text("CANCEL", color = TextMuted)
                    }
                },
                containerColor = CardBackground,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Ride Summary Dialog
        if (showSummaryDialog && summaryStats != null) {
            AlertDialog(
                onDismissRequest = {
                    showSummaryDialog = false
                    summaryStats = null
                },
                title = {
                    Text(
                        text = "Ride Summary",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Great job! Here are your telemetry stats:",
                            color = TextWhite,
                            textAlign = TextAlign.Center
                        )
                        
                        Divider(color = CardOutline)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Time", color = TextMuted, fontSize = 12.sp)
                                Text(Formatters.formatElapsedTime(summaryStats!!.elapsedTimeSeconds), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Distance", color = TextMuted, fontSize = 12.sp)
                                Text("${Formatters.formatDistance(summaryStats!!.distanceKm)} km", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Calories", color = TextMuted, fontSize = 12.sp)
                                Text("${summaryStats!!.caloriesKcal} kcal", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Avg Speed", color = TextMuted, fontSize = 12.sp)
                                Text("${Formatters.formatSpeed(summaryStats!!.avgSpeedKmH)} km/h", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Max Speed", color = TextMuted, fontSize = 12.sp)
                                Text("${Formatters.formatSpeed(summaryStats!!.maxSpeedKmH)} km/h", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSummaryDialog = false
                            summaryStats = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("DONE", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = CardBackground,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// Custom Vector icon path builders

@Composable
fun GpsSignalIconVector(): ImageVector {
    return ImageVector.Builder(
        name = "GpsSignal",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color.White),
            pathBuilder = {
                moveTo(12.0f, 8.0f)
                curveTo(9.79f, 8.0f, 8.0f, 9.79f, 8.0f, 12.0f)
                curveTo(8.0f, 14.21f, 9.79f, 16.0f, 12.0f, 16.0f)
                curveTo(14.21f, 16.0f, 16.0f, 14.21f, 16.0f, 12.0f)
                curveTo(16.0f, 9.79f, 14.21f, 8.0f, 12.0f, 8.0f)
                close()
                moveTo(12.0f, 2.0f)
                curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                curveTo(2.0f, 17.52f, 6.48f, 22.0f, 12.0f, 22.0f)
                curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
                curveTo(22.0f, 6.48f, 17.52f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(12.0f, 20.0f)
                curveTo(7.59f, 20.0f, 4.0f, 16.41f, 4.0f, 12.0f)
                curveTo(4.0f, 7.59f, 7.59f, 4.0f, 12.0f, 4.0f)
                curveTo(16.41f, 4.0f, 20.0f, 7.59f, 20.0f, 12.0f)
                curveTo(20.0f, 16.41f, 16.41f, 20.0f, 12.0f, 20.0f)
                close()
            }
        )
    }.build()
}

@Composable
fun BicycleIconVector(): ImageVector {
    return ImageVector.Builder(
        name = "Bicycle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color.White),
            pathBuilder = {
                moveTo(15.5f, 5.5f)
                curveTo(16.33f, 5.5f, 17.0f, 4.83f, 17.0f, 4.0f)
                curveTo(17.0f, 3.17f, 16.33f, 2.5f, 15.5f, 2.5f)
                curveTo(14.67f, 2.5f, 14.0f, 3.17f, 14.0f, 4.0f)
                curveTo(14.0f, 4.83f, 14.67f, 5.5f, 15.5f, 5.5f)
                close()
                moveTo(5.0f, 20.0f)
                curveTo(7.76f, 20.0f, 10.0f, 17.76f, 10.0f, 15.0f)
                curveTo(10.0f, 12.24f, 7.76f, 10.0f, 5.0f, 10.0f)
                curveTo(2.24f, 10.0f, 0.0f, 12.24f, 0.0f, 15.0f)
                curveTo(0.0f, 17.76f, 2.24f, 20.0f, 5.0f, 20.0f)
                close()
                moveTo(5.0f, 12.0f)
                curveTo(6.65f, 12.0f, 8.0f, 13.35f, 8.0f, 15.0f)
                curveTo(8.0f, 16.65f, 6.65f, 18.0f, 5.0f, 18.0f)
                curveTo(3.35f, 18.0f, 2.0f, 16.65f, 2.0f, 15.0f)
                curveTo(2.0f, 13.35f, 3.35f, 12.0f, 5.0f, 12.0f)
                close()
                moveTo(19.0f, 20.0f)
                curveTo(21.76f, 20.0f, 24.0f, 17.76f, 24.0f, 15.0f)
                curveTo(24.0f, 12.24f, 21.76f, 10.0f, 19.0f, 10.0f)
                curveTo(16.24f, 10.0f, 14.0f, 12.24f, 14.0f, 15.0f)
                curveTo(14.0f, 17.76f, 16.24f, 20.0f, 19.0f, 20.0f)
                close()
                moveTo(19.0f, 12.0f)
                curveTo(20.65f, 12.0f, 22.0f, 13.35f, 22.0f, 15.0f)
                curveTo(22.0f, 16.65f, 20.65f, 18.0f, 19.0f, 18.0f)
                curveTo(17.35f, 18.0f, 16.0f, 16.65f, 16.0f, 15.0f)
                curveTo(16.0f, 13.35f, 17.35f, 12.0f, 19.0f, 12.0f)
                close()
                moveTo(12.5f, 14.2f)
                lineTo(10.5f, 11.0f)
                horizontalLineTo(14.0f)
                lineTo(16.0f, 7.0f)
                horizontalLineTo(10.0f)
                lineTo(8.2f, 10.0f)
                horizontalLineTo(6.0f)
                verticalLineTo(8.0f)
                horizontalLineTo(7.2f)
                lineTo(9.0f, 5.0f)
                horizontalLineTo(15.0f)
                curveTo(15.8f, 5.0f, 16.5f, 5.5f, 16.8f, 6.2f)
                lineTo(18.9f, 11.0f)
                horizontalLineTo(21.0f)
                verticalLineTo(13.0f)
                horizontalLineTo(17.5f)
                lineTo(15.8f, 9.2f)
                lineTo(13.8f, 13.0f)
                horizontalLineTo(15.0f)
                verticalLineTo(15.0f)
                horizontalLineTo(11.0f)
                verticalLineTo(13.0f)
                horizontalLineTo(12.5f)
                close()
            }
        )
    }.build()
}

@Composable
fun DropdownArrowIconVector(): ImageVector {
    return ImageVector.Builder(
        name = "DropdownArrow",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color.White),
            pathBuilder = {
                moveTo(7.41f, 8.59f)
                lineTo(12.0f, 13.17f)
                lineTo(16.59f, 8.59f)
                lineTo(18.0f, 10.0f)
                lineTo(12.0f, 16.0f)
                lineTo(6.0f, 10.0f)
                close()
            }
        )
    }.build()
}

@Composable
fun StartArrowIconVector(): ImageVector {
    return ImageVector.Builder(
        name = "StartArrow",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color.Black),
            pathBuilder = {
                moveTo(5.88f, 4.12f)
                lineTo(13.76f, 12.0f)
                lineTo(5.88f, 19.88f)
                lineTo(8.0f, 22.0f)
                lineTo(18.0f, 12.0f)
                lineTo(8.0f, 2.0f)
                close()
            }
        )
    }.build()
}

@Composable
fun StopArrowIconVector(): ImageVector {
    return ImageVector.Builder(
        name = "StopArrow",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color.White),
            pathBuilder = {
                moveTo(6.0f, 6.0f)
                horizontalLineTo(18.0f)
                verticalLineTo(18.0f)
                horizontalLineTo(6.0f)
                close()
            }
        )
    }.build()
}
