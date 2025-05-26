package com.example.fitnesstracker.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val height: Int,
    val weight: Double,
    val age: Int,
    val goalType: String, // "lose" или "gain"
    val planType: String,  // "fast" или "recommended"
    val desiredWeight: Double, // kg
    val isMale: Boolean // true = male, false = female

)