package com.example.fitnesstracker.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_history",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class WeightHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,  // <-- foreign key to User
    val date: String, // yyyy-MM-dd
    val weight: Double
)
