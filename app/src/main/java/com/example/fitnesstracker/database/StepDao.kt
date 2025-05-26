package com.example.fitnesstracker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface StepDao {
    @Insert
    suspend fun insert(stepHistory: StepHistory)

    @Query("SELECT * FROM steps WHERE userId = :userId AND date = :date")
    suspend fun getStepsByDate(date: String, userId: Int): StepHistory?

    @Query("SELECT * FROM steps WHERE userId = :userId ORDER BY date DESC LIMIT 7")
    suspend fun getLastWeekSteps(userId: Int): List<StepHistory>

    @Update
    suspend fun update(stepHistory: StepHistory)
}
