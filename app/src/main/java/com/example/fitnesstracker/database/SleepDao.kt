package com.example.fitnesstracker.database

import androidx.room.*

@Dao
interface SleepDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sleepHistory: SleepHistory)

    @Query("SELECT * FROM sleep_history WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getSleepByDate(userId: Int, date: String): SleepHistory?

    @Query("SELECT * FROM sleep_history WHERE userId = :userId ORDER BY date DESC LIMIT 7")
    suspend fun getLastWeekSleep(userId: Int): List<SleepHistory>
}
