package com.example.fitnesstracker.utils

import java.util.*

object GoalDateEstimator {
    fun estimateGoalDate(currentWeight: Double, targetWeight: Double, planType: String): Date {
        val weightDiff = kotlin.math.abs(currentWeight - targetWeight)
        val kgPerWeek = if (planType == "fast") 1.0 else 0.5
        val weeks = (weightDiff / kgPerWeek).toInt()
        return Calendar.getInstance().apply {
            add(Calendar.WEEK_OF_YEAR, weeks)
        }.time
    }
}
