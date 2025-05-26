package com.example.fitnesstracker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FoodDao {

    @Insert
    suspend fun insert(entry: FoodEntry)

    @Query("SELECT * FROM food_entries WHERE userId = :userId AND date = :date AND mealType = :mealType")
    suspend fun getEntriesByDateAndMeal(date: String, mealType: String, userId: Int): List<FoodEntry>

    @Query("SELECT * FROM food_entries WHERE userId = :userId AND date = :date")
    suspend fun getAllEntriesByDate(date: String, userId: Int): List<FoodEntry>

    @Query("SELECT SUM(calories) FROM food_entries WHERE userId = :userId AND date = :date")
    suspend fun getCaloriesForDate(date: String, userId: Int): Int?

    @Query("""
        SELECT date, SUM(calories) as totalCalories
        FROM food_entries
        WHERE userId = :userId AND date BETWEEN :startDate AND :endDate
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getCaloriesForDateRange(startDate: String, endDate: String, userId: Int): List<CaloriesPerDay>
}
