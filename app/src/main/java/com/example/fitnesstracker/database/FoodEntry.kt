package com.example.fitnesstracker.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_entries",
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
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,  // <-- new field here
    val date: String, // Format "yyyy-MM-dd"
    val mealType: String, // "breakfast", "lunch", "dinner", "snack"
    val foodName: String,
    val quantity: Double, // in grams
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int
)
