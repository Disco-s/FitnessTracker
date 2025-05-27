package com.example.fitnesstracker.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import java.util.*

object SleepActivityMonitor {
    // Список интервалов сна, каждый элемент — пара: начало и конец периода неактивности
    private val sleepSegments = mutableListOf<Pair<Long, Long>>()

    // Временная метка последней неактивности (экран выключен)
    private var lastInactiveTimestamp: Long? = null

    // Флаг, чтобы избежать повторной инициализации
    private var initialized = false

    // Часы, в пределах которых предполагается сон: с 23:00 до 07:00
    private val sleepStartHour = 23
    private val sleepEndHour = 7

    // Инициализация мониторинга: регистрирует BroadcastReceiver для отслеживания активности экрана
    fun initialize(context: Context) {
        if (initialized) return  // Если уже инициализировано — не повторять
        initialized = true

        // Настройка фильтра для прослушивания событий включения/выключения экрана
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)        // Экран включён
            addAction(Intent.ACTION_USER_PRESENT)     // Пользователь разблокировал экран
            addAction(Intent.ACTION_SCREEN_OFF)       // Экран выключен
        }

        // Регистрация анонимного BroadcastReceiver
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val now = System.currentTimeMillis() // Текущая метка времени

                // Обработка событий только в пределах "часов сна"
                if (!isWithinSleepHours(now)) return

                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        // Экран выключен — возможно, начало сна
                        if (lastInactiveTimestamp == null) {
                            lastInactiveTimestamp = now
                        }
                    }
                    Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                        // Экран включён или пользователь активен — возможно, окончание сна
                        lastInactiveTimestamp?.let { sleepStart ->
                            // Сохраняем интервал сна
                            sleepSegments.add(sleepStart to now)
                            lastInactiveTimestamp = null
                        }
                    }
                }
            }
        }, filter)
    }

    // Возвращает копию списка всех зафиксированных интервалов сна
    fun getSleepSegments(): List<Pair<Long, Long>> = sleepSegments.toList()


    // Сброс всех накопленных данных (используется после сохранения в БД)
    fun reset() {
        sleepSegments.clear()
        lastInactiveTimestamp = null
    }

    // Проверка, находится ли текущее время в пределах "ночного" окна сна (с 23:00 до 07:00)
    private fun isWithinSleepHours(timeMillis: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour >= sleepStartHour || hour < sleepEndHour
    }
}
