package com.example.fitnesstracker.test

import com.example.fitnesstracker.database.AppDatabase
import com.example.fitnesstracker.database.FoodEntry
import com.example.fitnesstracker.database.SleepHistory
import com.example.fitnesstracker.database.StepHistory
import com.example.fitnesstracker.database.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object DummyDataSeeder {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)


    fun seed(database: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            // Insert dummy user
            val dummyUser = User(
                fullName = "Визирякин Денис Дмитриевич",
                height = 198,
                weight = 140.0,
                age = 24,
                goalType = "lose",
                planType = "fast",
                desiredWeight = 100.0,
                isMale = true
            )
            val existingUser = database.userDao().getUser()
            val userId = if (existingUser == null) {
                database.userDao().insert(dummyUser)
                database.userDao().getUser()?.id ?: return@launch
            } else {
                existingUser.id
            }

            // Prepare dates: today and previous two days
            val today = Calendar.getInstance()
            val dates = (0..2).map {
                val cal = today.clone() as Calendar
                cal.add(Calendar.DAY_OF_YEAR, -it)
                dateFormat.format(cal.time)
            }.reversed() // Oldest first (2 days ago, yesterday, today)

            // Only seed if step data for today doesn't exist
            val existingSteps = database.stepDao().getStepsByDate(dates.last(), userId)
            if (existingSteps != null) return@launch

            // STEP ENTRIES (dummy steps per date)
            val stepEntries = listOf(
                StepHistory(date = dates[0], steps = 9500, userId = userId),
                StepHistory(date = dates[1], steps = 8700, userId = userId),
                StepHistory(date = dates[2], steps = 2000, userId = userId)
            )
            stepEntries.forEach { database.stepDao().insert(it) }

            // FOOD ENTRIES (dummy meals per date)
            val foodEntries = listOf(
                // dates[0] (2 days ago)
                FoodEntry(date = dates[0], mealType = "breakfast", foodName = "Oatmeal", quantity = 200.0, calories = 350, protein = 10, fat = 5, carbs = 60, userId = userId),
                FoodEntry(date = dates[0], mealType = "lunch", foodName = "Grilled Chicken", quantity = 150.0, calories = 450, protein = 40, fat = 10, carbs = 20, userId = userId),
                FoodEntry(date = dates[0], mealType = "dinner", foodName = "Pasta", quantity = 250.0, calories = 700, protein = 20, fat = 15, carbs = 90, userId = userId),
                FoodEntry(date = dates[0], mealType = "snack", foodName = "Nuts", quantity = 50.0, calories = 500, protein = 15, fat = 40, carbs = 10, userId = userId),

                // dates[1] (yesterday)
                FoodEntry(date = dates[1], mealType = "breakfast", foodName = "Eggs and Toast", quantity = 180.0, calories = 400, protein = 25, fat = 20, carbs = 30, userId = userId),
                FoodEntry(date = dates[1], mealType = "lunch", foodName = "Beef Bowl", quantity = 200.0, calories = 600, protein = 35, fat = 25, carbs = 50, userId = userId),
                FoodEntry(date = dates[1], mealType = "dinner", foodName = "Rice & Veggies", quantity = 250.0, calories = 600, protein = 15, fat = 10, carbs = 80, userId = userId),
                FoodEntry(date = dates[1], mealType = "snack", foodName = "Protein Shake", quantity = 300.0, calories = 400, protein = 30, fat = 5, carbs = 20, userId = userId),

                // dates[2] (today)
                FoodEntry(date = dates[2], mealType = "lunch", foodName = "Chicken Wrap", quantity = 180.0, calories = 500, protein = 30, fat = 10, carbs = 40, userId = userId)
            )
            foodEntries.forEach { database.foodDao().insert(it) }


            fun toMillis(dateStr: String, timeStr: String): Long {
                // Combine date + time and parse
                return dateTimeFormat.parse("$dateStr $timeStr")?.time ?: 0L
            }

// SLEEP ENTRIES (dummy sleep for each date)
            val sleepEntries = listOf(
                SleepHistory(
                    userId = userId,
                    date = dates[0],
                    sleepStart = toMillis(dates[0], "23:00"),
                    sleepEnd = toMillis(dates[1], "07:00"),  // next day morning
                    durationMinutes = 480
                ),
                SleepHistory(
                    userId = userId,
                    date = dates[1],
                    sleepStart = toMillis(dates[1], "22:45"),
                    sleepEnd = toMillis(dates[2], "06:45"),
                    durationMinutes = 480
                ),
                SleepHistory(
                    userId = userId,
                    date = dates[2],
                    sleepStart = toMillis(dates[2], "23:30"),
                    sleepEnd = toMillis(dates[2], "07:30"),  // adjust if needed
                    durationMinutes = 480
                )
            )

            sleepEntries.forEach { database.sleepDao().insert(it) }
    }
}
}
