package com.cycle.speedometer.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cycle.speedometer.ui.theme.CardOutline
import com.cycle.speedometer.ui.theme.DarkBackground
import com.cycle.speedometer.ui.theme.NeonGreen
import com.cycle.speedometer.ui.theme.TextMuted
import com.cycle.speedometer.ui.theme.TextWhite
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerGauge(
    currentSpeed: Float,
    modifier: Modifier = Modifier,
    maxSpeedDisplay: Float = 60f
) {
    // Animate speed for a ultra-smooth needle sweep motion
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed.coerceIn(0f, maxSpeedDisplay),
        animationSpec = tween(durationMillis = 300),
        label = "NeedleSweep"
    )

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            val width = size.width
            val height = size.height
            val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
            
            // Layout dimension calculations
            val outerRadius = width / 2.1f
            val trackRadius = outerRadius - 16.dp.toPx()
            val tickRadius = trackRadius - 16.dp.toPx()
            val textRadius = tickRadius - 20.dp.toPx()

            // Angles definition (0 km/h is bottom-left, 60 km/h is bottom-right)
            val startAngle = 150f
            val totalSweep = 240f

            // 1. Draw outer gauge track arc (dark tech grey)
            drawArc(
                color = CardOutline,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(center.x - trackRadius, center.y - trackRadius),
                size = androidx.compose.ui.geometry.Size(trackRadius * 2, trackRadius * 2),
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Draw active speed track glow (semi-translucent neon green arc)
            if (animatedSpeed > 0.1f) {
                val activeSweep = (animatedSpeed / maxSpeedDisplay) * totalSweep
                drawArc(
                    color = NeonGreen.copy(alpha = 0.15f),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - trackRadius, center.y - trackRadius),
                    size = androidx.compose.ui.geometry.Size(trackRadius * 2, trackRadius * 2),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Draw tick marks & speed labels
            val textPaint = Paint().apply {
                color = TextMuted.toArgb()
                textSize = 13.dp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            
            val activeTextPaint = Paint().apply {
                color = TextWhite.toArgb()
                textSize = 14.dp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }

            // Draw ticks from 0 to 60
            for (speed in 0..maxSpeedDisplay.toInt() step 2) {
                val fraction = speed / maxSpeedDisplay
                val angleDeg = startAngle + (fraction * totalSweep)
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val isMajor = speed % 10 == 0
                val tickLength = if (isMajor) 14.dp.toPx() else 8.dp.toPx()
                val tickWidth = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx()
                
                // Active speed portion glowing tick color
                val isTickActive = speed <= animatedSpeed
                val tickColor = if (isTickActive) NeonGreen else TextMuted.copy(alpha = 0.4f)

                val startX = center.x + (tickRadius - tickLength) * cos(angleRad).toFloat()
                val startY = center.y + (tickRadius - tickLength) * sin(angleRad).toFloat()
                val endX = center.x + tickRadius * cos(angleRad).toFloat()
                val endY = center.y + tickRadius * sin(angleRad).toFloat()

                drawLine(
                    color = tickColor,
                    start = androidx.compose.ui.geometry.Offset(startX, startY),
                    end = androidx.compose.ui.geometry.Offset(endX, endY),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )

                // Draw Text labels for 0, 10, 20, 30, 40, 50, 60
                if (isMajor) {
                    val labelX = center.x + textRadius * cos(angleRad).toFloat()
                    val labelY = center.y + textRadius * sin(angleRad).toFloat()
                    
                    val activeLabel = animatedSpeed >= speed - 2f && animatedSpeed <= speed + 2f
                    val paintToUse = if (activeLabel) activeTextPaint else textPaint

                    // Adjust Y for text baseline centering
                    val adjustedY = labelY - ((paintToUse.descent() + paintToUse.ascent()) / 2f)
                    drawContext.canvas.nativeCanvas.drawText(
                        speed.toString(),
                        labelX,
                        adjustedY,
                        paintToUse
                    )
                }
            }

            // 4. Draw Center Pivot Core (Concentric circles matching screenshot)
            val centerRadius1 = 16.dp.toPx()
            val centerRadius2 = 10.dp.toPx()
            
            // Outer glow circle
            drawCircle(
                color = NeonGreen.copy(alpha = 0.2f),
                radius = centerRadius1 + 8.dp.toPx(),
                center = center
            )
            // Outer dark ring
            drawCircle(
                color = CardOutline,
                radius = centerRadius1,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
            // Inner green pivot dot
            drawCircle(
                color = NeonGreen,
                radius = centerRadius2,
                center = center
            )
            // Pivot hollow center
            drawCircle(
                color = DarkBackground,
                radius = centerRadius2 - 3.dp.toPx(),
                center = center
            )

            // 5. Draw mechanical speed needle pointing towards the speed angle
            val needleAngleDeg = startAngle + ((animatedSpeed / maxSpeedDisplay) * totalSweep)
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
            
            val needleLength = tickRadius - 20.dp.toPx()
            val needleEndX = center.x + needleLength * cos(needleAngleRad).toFloat()
            val needleEndY = center.y + needleLength * sin(needleAngleRad).toFloat()

            // Glowing line style needle
            drawLine(
                color = NeonGreen,
                start = center,
                end = androidx.compose.ui.geometry.Offset(needleEndX, needleEndY),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Draw a thin core highlights inside the needle
            drawLine(
                color = Color.White,
                start = center,
                end = androidx.compose.ui.geometry.Offset(needleEndX, needleEndY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 6. Centered digital speed readout overlay
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 55.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%.0f", currentSpeed),
                color = TextWhite,
                fontSize = 58.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "km/h",
                color = TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.offset(y = (-4).dp)
            )
        }
    }
}
