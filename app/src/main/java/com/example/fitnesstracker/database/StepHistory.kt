package com.example.fitnesstracker.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "steps",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class StepHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,          // Add this
    val date: String,        // Format "yyyy-MM-dd"
    val steps: Int
)
