package com.example.fitnesstracker.utils

object BMRCalculator {
    fun calculateBMR(weight: Double, height: Int, age: Int, isMale: Boolean): Double {
        return if (isMale) {
            88.362 + (13.397 * weight) + (4.799 * height) - (5.677 * age)
        } else {
            447.593 + (9.247 * weight) + (3.098 * height) - (4.330 * age)
        }
    }
}

object CalorieGoalCalculator {
    fun calculateDailyCalories(
        weight: Double,
        height: Int,
        age: Int,
        isMale: Boolean,
        goalType: String,
        planType: String
    ): Int {
        val bmr = BMRCalculator.calculateBMR(weight, height, age, isMale)
        val factor = when (planType) {
            "fast" -> if (goalType == "lose") 0.75 else 1.25
            else -> if (goalType == "lose") 0.85 else 1.15
        }
        return (bmr * factor).toInt()
    }
}
