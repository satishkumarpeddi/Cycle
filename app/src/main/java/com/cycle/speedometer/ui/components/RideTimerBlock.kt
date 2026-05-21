package com.cycle.speedometer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cycle.speedometer.ui.theme.CardBackground
import com.cycle.speedometer.ui.theme.CardOutline
import com.cycle.speedometer.ui.theme.NeonGreen
import com.cycle.speedometer.ui.theme.TextMuted
import com.cycle.speedometer.ui.theme.TextWhite
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.PathFillType

@Composable
fun RideTimerBlock(
    elapsedTimeStr: String,
    isRunning: Boolean,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Flanking Left Button (Pause/Play)
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(CardBackground)
                .border(1.dp, CardOutline, CircleShape)
                .clickable(enabled = isRunning) { onPauseToggle() },
            contentAlignment = Alignment.Center
        ) {
            val iconColor = if (isRunning) (if (isPaused) NeonGreen else TextWhite) else TextMuted.copy(alpha = 0.3f)
            
            // Draw standard pause/play symbol
            if (isPaused) {
                // Play Icon
                Icon(
                    imageVector = PlayIconVector(),
                    contentDescription = "Resume Ride",
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                // Pause Icon
                Icon(
                    imageVector = PauseIconVector(),
                    contentDescription = "Pause Ride",
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Center Long Rounded Pill for Timer
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(CardBackground)
                .border(1.dp, CardOutline, RoundedCornerShape(27.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = elapsedTimeStr,
                color = TextWhite,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Flanking Right Button (Reset)
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(CardBackground)
                .border(1.dp, CardOutline, CircleShape)
                .clickable(enabled = isRunning || isPaused) { onReset() },
            contentAlignment = Alignment.Center
        ) {
            val iconColor = if (isRunning || isPaused) TextWhite else TextMuted.copy(alpha = 0.3f)
            Icon(
                imageVector = ResetIconVector(),
                contentDescription = "Reset Ride",
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Vector Custom Path Builders for clean, self-contained drawings

@Composable
fun PlayIconVector(): ImageVector {
    return ImageVector.Builder(
        name = "Play",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color.White),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Butt,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathBuilder = {
                moveTo(8.0f, 5.0f)
                lineTo(19.0f, 12.0f)
                lineTo(8.0f, 19.0f)
                close()
            }
        )
    }.build()
}

@Composable
fun PauseIconVector(): ImageVector {
    return ImageVector.Builder(
        name = "Pause",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color.White),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Butt,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathBuilder = {
                moveTo(6.0f, 19.0f)
                horizontalLineTo(10.0f)
                verticalLineTo(5.0f)
                horizontalLineTo(6.0f)
                verticalLineTo(19.0f)
                close()
                moveTo(14.0f, 5.0f)
                verticalLineTo(19.0f)
                horizontalLineTo(18.0f)
                verticalLineTo(5.0f)
                horizontalLineTo(14.0f)
                close()
            }
        )
    }.build()
}

@Composable
fun ResetIconVector(): ImageVector {
    return ImageVector.Builder(
        name = "Reset",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = androidx.compose.ui.graphics.SolidColor(Color.White),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Butt,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathBuilder = {
                moveTo(12.0f, 4.0f)
                verticalLineTo(1.0f)
                lineTo(8.0f, 5.0f)
                lineTo(12.0f, 9.0f)
                verticalLineTo(6.0f)
                curveTo(15.86f, 6.0f, 19.0f, 9.14f, 19.0f, 13.0f)
                curveTo(19.0f, 16.86f, 15.86f, 20.0f, 12.0f, 20.0f)
                curveTo(8.14f, 20.0f, 5.0f, 16.86f, 5.0f, 13.0f)
                horizontalLineTo(3.0f)
                curveTo(3.0f, 17.97f, 7.03f, 22.0f, 12.0f, 22.0f)
                curveTo(16.97f, 22.0f, 21.0f, 17.97f, 21.0f, 13.0f)
                curveTo(21.0f, 8.03f, 16.97f, 4.0f, 12.0f, 4.0f)
                close()
            }
        )
    }.build()
}
