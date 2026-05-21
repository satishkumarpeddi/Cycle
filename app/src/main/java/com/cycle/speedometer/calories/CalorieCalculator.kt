package com.cycle.speedometer.calories

object CalorieCalculator {
    
    /**
     * Estimates calories burned during cycling using the MET formula.
     * Calories = MET * Weight(kg) * Time(hours)
     */
    fun calculate(avgSpeedKmH: Float, weightKg: Double, durationSeconds: Long): Int {
        if (durationSeconds <= 0L) return 0
        
        // Determine MET based on speed
        val met = when {
            avgSpeedKmH < 15.0f -> 4.0 // Slow cycling
            avgSpeedKmH in 15.0f..22.0f -> 8.0 // Moderate cycling
            else -> 12.0 // Fast cycling
        }
        
        val timeHours = durationSeconds / 3600.0
        val calories = met * weightKg * timeHours
        return calories.toInt()
    }
}
