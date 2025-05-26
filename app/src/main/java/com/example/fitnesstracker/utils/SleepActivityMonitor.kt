package com.example.fitnesstracker.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import java.util.*

object SleepActivityMonitor {
    private var wasActiveDuringSleepHours = false
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (isWithinSleepHours()) {
                    wasActiveDuringSleepHours = true
                }
            }
        }, filter)
    }

    fun wasInactiveDuringSleepHours(): Boolean {
        return !wasActiveDuringSleepHours
    }

    fun reset() {
        wasActiveDuringSleepHours = false
    }

    private fun isWithinSleepHours(): Boolean {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour in 23..23 || hour in 0..6
    }
}
