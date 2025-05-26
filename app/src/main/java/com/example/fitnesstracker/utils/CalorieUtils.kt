package com.example.fitnesstracker.utils

object CalorieUtils {

    fun calculateCaloriesBurnedFromSteps(steps: Int, weightKg: Double): Int {
        val stepLengthMeters = 0.78
        val speedMPerS = 1.4
        val totalMeters = steps * stepLengthMeters
        val durationHours = (totalMeters / speedMPerS) / 3600.0
        val met = 3.5

        val caloriesBurned = met * weightKg * durationHours
        return caloriesBurned.toInt()
    }
}
