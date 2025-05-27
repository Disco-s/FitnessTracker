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

    // Менеджер сенсоров
    private lateinit var sensorManager: SensorManager

    // Сенсор для подсчета шагов
    private var stepSensor: Sensor? = null

    // Начальное значение счётчика шагов с момента последней перезагрузки устройства
    private var initialStepCount: Int? = null

    // Количество шагов, отображаемое пользователю
    private var stepCount = 0

    // ID текущего пользователя
    private var currentUserId: Int = -1

    // Ссылка на базу данных
    private lateinit var db: AppDatabase

    // Форматтер даты для сохранения шагов по дате
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        // Идентификатор канала уведомлений
        const val NOTIFICATION_CHANNEL_ID = "step_counter_channel"

        // Идентификатор уведомления
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()

        // Инициализация базы данных
        db = AppDatabase.getDatabase(applicationContext)

        // Получение менеджера сенсоров
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Получение сенсора шагомера
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)


        // Создание канала уведомлений и запуск сервиса в foreground режиме
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // Регистрация слушателя для сенсора
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)


        // Загрузка текущего пользователя из базы данных
        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().getUser()
            if (user != null) {
                currentUserId = user.id
            }
        }
    }

    override fun onDestroy() {
        // Отписка от сенсора при уничтожении сервиса
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    // Сервис не поддерживает привязку
    override fun onBind(intent: Intent?): IBinder? = null

    // Обработка событий от сенсора
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val totalSteps = it.values[0].toInt() // Общее количество шагов с момента включения устройства

            // Если начальное значение еще не установлено — инициализируем его
            if (initialStepCount == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val today = getTodayDate()
                    // Получаем шаги, сохранённые за сегодня
                    val savedSteps = db.stepDao().getStepsByDate(today, currentUserId)?.steps ?: 0
                    initialStepCount = totalSteps - savedSteps.coerceAtLeast(0)
                    stepCount = savedSteps
                }
                return
            }

            // Подсчет текущих шагов с момента запуска приложения
            val currentSteps = totalSteps - (initialStepCount ?: totalSteps)

            // Если количество шагов изменилось — обновляем и сохраняем
            if (currentSteps != stepCount) {
                stepCount = currentSteps.coerceAtLeast(0)
                saveSteps()
                updateNotification()
            }
        }
    }

    // Игнорируем изменение точности сенсора
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Сохранение шагов в базу данных
    private fun saveSteps() {
        CoroutineScope(Dispatchers.IO).launch {
            val today = getTodayDate()
            val existing = db.stepDao().getStepsByDate(today, currentUserId)
            if (existing != null) {
                // Обновляем, если уже есть запись на сегодня
                db.stepDao().update(existing.copy(steps = stepCount))
            } else {
                // Вставляем новую запись
                db.stepDao().insert(StepHistory(userId = currentUserId, date = today, steps = stepCount))
            }
        }
    }

    // Получение текущей даты в виде строки
    private fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    // Создание канала уведомлений (обязательно для Android 8+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Step Counter Service",
                NotificationManager.IMPORTANCE_LOW // Низкий приоритет, чтобы не мешать пользователю
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // Создание начального уведомления для foreground-сервиса
    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.directions_walk_24dp)
            .setContentTitle("Отслеживание шагов активно")
            .setContentText("Шаги: $stepCount")
            .setOngoing(true) // Уведомление нельзя удалить свайпом
            .build()
    }

    // Обновление текста уведомления при изменении количества шагов
    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.directions_walk_24dp)
            .setContentTitle("Отслеживание шагов активно")
            .setContentText("Шаги: $stepCount")
            .setOngoing(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
