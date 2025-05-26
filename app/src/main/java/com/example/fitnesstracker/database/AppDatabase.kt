package com.example.fitnesstracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


// Аннотация @Database указывает Room, что это база данных с указанными сущностями
@Database(
    entities = [User::class, StepHistory::class, FoodEntry::class, WeightHistory::class, SleepHistory::class],
    version = 1 // Версия базы данных
)
abstract class AppDatabase : RoomDatabase() {
    // Абстрактные методы для получения DAO (объектов доступа к данным)
    abstract fun userDao(): UserDao
    abstract fun stepDao(): StepDao
    abstract fun foodDao(): FoodDao
    abstract fun weightDao(): WeightDao
    abstract fun sleepDao(): SleepDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null // Экземпляр базы данных (одиночка)

        // Метод для получения экземпляра базы данных
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) { // Гарантирует потокобезопасный доступ
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_db" // Имя файла базы данных
                )
                    .fallbackToDestructiveMigration() // Сброс базы при изменении схемы
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}