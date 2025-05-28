package com.example.fitnesstracker.utils

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.*

object SleepActivityMonitor {
    private val sleepStartHour = 23
    private val sleepEndHour = 7

    // Sleep segments (start, end in millis)
    private var sleepSegments = mutableListOf<Pair<Long, Long>>()

    fun getSleepSegments(): List<Pair<Long, Long>> = sleepSegments.toList()

    fun reset() {
        sleepSegments.clear()
    }

    fun collectSleepData(context: Context) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        if (!hasUsageAccess(context)) return

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        // Define the 23:00 of previous day
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, sleepStartHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        // Define 07:00 of current day
        val sleepEndCalendar = Calendar.getInstance()
        sleepEndCalendar.set(Calendar.HOUR_OF_DAY, sleepEndHour)
        sleepEndCalendar.set(Calendar.MINUTE, 0)
        sleepEndCalendar.set(Calendar.SECOND, 0)
        sleepEndCalendar.set(Calendar.MILLISECOND, 0)
        val maxEndTime = sleepEndCalendar.timeInMillis

        // Clamp the query end time to 07:00
        val clampedEndTime = minOf(endTime, maxEndTime)

        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            clampedEndTime
        )

        if (stats.isNullOrEmpty()) return

        val sortedStats = stats.sortedBy { it.lastTimeUsed }

        var lastUsedTime = startTime
        for (usage in sortedStats) {
            val idleStart = lastUsedTime
            val idleEnd = usage.lastTimeUsed

            if (idleEnd - idleStart >= MIN_SLEEP_INTERVAL) {
                sleepSegments.add(idleStart to idleEnd)
            }

            lastUsedTime = maxOf(lastUsedTime, usage.lastTimeUsed)
        }

        // Final segment if idle until end
        if (clampedEndTime - lastUsedTime >= MIN_SLEEP_INTERVAL) {
            sleepSegments.add(lastUsedTime to clampedEndTime)
        }
    }


    private const val MIN_SLEEP_INTERVAL = 30 * 60 * 1000L // 30 minutes

    fun hasUsageAccess(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    "android:get_usage_stats",
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    "android:get_usage_stats",
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }
}
