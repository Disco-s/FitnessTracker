package com.example.fitnesstracker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query


@Dao
interface WeightDao {
    @Insert
    suspend fun insert(weight: WeightHistory)

    @Query("SELECT * FROM weight_history WHERE userId = :userId AND date >= date('now', '-30 days') ORDER BY date ASC")
    suspend fun getLast30DaysWeights(userId: Int): List<WeightHistory>
}
