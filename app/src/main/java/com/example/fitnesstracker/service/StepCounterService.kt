package com.example.fitnesstracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.fitnesstracker.R
import com.example.fitnesstracker.database.AppDatabase
import com.example.fitnesstracker.database.StepHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var initialStepCount: Int? = null
    private var stepCount = 0
    private var currentUserId: Int = -1
    private lateinit var db: AppDatabase

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "step_counter_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()

        db = AppDatabase.getDatabase(applicationContext)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)

        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().getUser()
            if (user != null) {
                currentUserId = user.id
            }
        }
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val totalSteps = it.values[0].toInt()

            if (initialStepCount == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val today = getTodayDate()
                    val savedSteps = db.stepDao().getStepsByDate(today, currentUserId)?.steps ?: 0
                    initialStepCount = totalSteps - savedSteps.coerceAtLeast(0)
                    stepCount = savedSteps
                }
                return
            }

            val currentSteps = totalSteps - (initialStepCount ?: totalSteps)
            if (currentSteps != stepCount) {
                stepCount = currentSteps.coerceAtLeast(0)
                saveSteps()
                updateNotification()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveSteps() {
        CoroutineScope(Dispatchers.IO).launch {
            val today = getTodayDate()
            val existing = db.stepDao().getStepsByDate(today, currentUserId)
            if (existing != null) {
                db.stepDao().update(existing.copy(steps = stepCount))
            } else {
                db.stepDao().insert(StepHistory(userId = currentUserId, date = today, steps = stepCount))
            }
        }
    }

    private fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Step Counter Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Отслеживание шагов активно")
            .setContentText("Шаги: $stepCount")
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Отслеживание шагов активно")
            .setContentText("Шаги: $stepCount")
            .setOngoing(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
