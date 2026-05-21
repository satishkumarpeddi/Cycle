package com.cycle.speedometer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cycle.speedometer.ui.theme.CardBackground
import com.cycle.speedometer.ui.theme.CardOutline
import com.cycle.speedometer.ui.theme.DigitalGreen
import com.cycle.speedometer.ui.theme.DigitalInactive
import com.cycle.speedometer.ui.theme.TextMuted

@Composable
fun OdometerDisplay(
    distanceKm: Double,
    modifier: Modifier = Modifier
) {
    // Format distance: standard distance in units (e.g., if 4.12 km, display 4)
    // Let's display actual meters as part of the odometer! E.g. total meters or kilometers as a 6-digit integer
    val distanceInt = distanceKm.toInt()
    val digitsStr = String.format("%06d", distanceInt.coerceIn(0, 999999))

    Row(
        modifier = modifier
            .background(CardBackground.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
            .border(1.dp, CardOutline, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        var leadingZero = true

        digitsStr.forEachIndexed { index, char ->
            if (char != '0') {
                leadingZero = false
            }
            
            // The last digit is always active even if it's zero
            val isActive = !leadingZero || index == digitsStr.length - 1
            val textColor = if (isActive) DigitalGreen else DigitalInactive

            Box(
                modifier = Modifier
                    .size(width = 18.dp, height = 28.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(CardOutline.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char.toString(),
                    color = textColor,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = "km",
            color = DigitalGreen,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
