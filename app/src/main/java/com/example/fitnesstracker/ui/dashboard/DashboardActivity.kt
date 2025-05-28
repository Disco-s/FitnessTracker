package com.example.fitnesstracker.ui.dashboard

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.fitnesstracker.R
import com.example.fitnesstracker.database.AppDatabase
import com.example.fitnesstracker.database.SleepHistory
import com.example.fitnesstracker.database.StepHistory
import com.example.fitnesstracker.databinding.ActivityDashboardBinding
import com.example.fitnesstracker.service.StepCounterService
import com.example.fitnesstracker.ui.calories.CalorieTrackerActivity
import com.example.fitnesstracker.ui.weight.UpdateWeightActivity
import com.example.fitnesstracker.utils.BMRCalculator
import com.example.fitnesstracker.utils.CalorieGoalCalculator
import com.example.fitnesstracker.utils.CalorieUtils
import com.example.fitnesstracker.utils.GoalDateEstimator
import com.example.fitnesstracker.utils.SleepActivityMonitor
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class DashboardActivity : AppCompatActivity() {

    // Привязка к макету через View Binding
    private lateinit var binding: ActivityDashboardBinding

    // Менеджер сенсоров и шагомер
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    // Ссылка на базу данных
    private lateinit var db: AppDatabase

    // Кол-во шагов и их начальное значение
    private var stepCount = 0
    private var initialStepCount: Int? = null

    // Формат даты для записи текущего дня
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Идентификатор текущего пользователя
    private var currentUserId: Int = -1

    // Мьютекс для защиты от одновременных сохранений шагов
    private val stepSaveMutex = Mutex()

    // Данные по калориям
    private var calorieGoal = 0
    private var caloriesConsumed = 0
    private var caloriesBurned = 0

    private val POST_NOTIFICATIONS_PERMISSION_REQUEST_CODE = 1001
    private var lastStepUpdateDate: String? = null

    // Слушатель событий сенсора шагов
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val totalSteps = it.values[0].toInt()
                val today = getTodayDate()

                // Проверка изменился ли день с последнего добавления шагов
                if (lastStepUpdateDate != today) {
                    // Если день новый сброс кол-во шагов
                    CoroutineScope(Dispatchers.IO).launch {
                        val savedSteps = db.stepDao().getStepsByDate(today, currentUserId)?.steps ?: 0
                        initialStepCount = totalSteps - savedSteps.coerceAtLeast(0)
                        stepCount = savedSteps
                        lastStepUpdateDate = today
                        runOnUiThread {
                            updateStepUI()
                            updateCaloriesUI()
                        }
                    }
                    return
                }


                if (initialStepCount == null) {

                    CoroutineScope(Dispatchers.IO).launch {
                        val savedSteps = db.stepDao().getStepsByDate(today, currentUserId)?.steps ?: 0
                        initialStepCount = totalSteps - savedSteps.coerceAtLeast(0)
                        stepCount = savedSteps
                        runOnUiThread {
                            updateStepUI()
                            updateCaloriesUI()
                        }
                    }
                    return
                }

                val currentSteps = totalSteps - (initialStepCount ?: totalSteps)
                if (currentSteps != stepCount && currentSteps >= 0) {
                    stepCount = currentSteps
                    saveSteps()
                    runOnUiThread {
                        updateStepUI()
                        updateCaloriesUI()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkUsageAccessPermission()

        // Инициализация системы отслеживания сна
        SleepActivityMonitor.collectSleepData(applicationContext)

        // Получаем ссылку на базу данных и шагомер
        db = AppDatabase.getDatabase(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)


        // Проверка наличия датчика
        if (stepSensor == null) {
            Toast.makeText(this, "Датчик шагов не найден!", Toast.LENGTH_LONG).show()
        }

        // Запускаем фоновый сервис шагомера, если разрешения даны
        val permissionGranted = intent.getBooleanExtra("PERMISSION_GRANTED", false)
        if (permissionGranted) {
            startStepCounterService()
        }

        checkAndRequestNotificationPermission()
        // Заполнение базы данных тестовыми данными
        com.example.fitnesstracker.test.DummyDataSeeder.seed(db)


        // Настройка пользовательского интерфейса
        setupUI()
    }

    private fun startStepCounterService() {
        val intent = Intent(this, StepCounterService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun setupUI() {
        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().getUser()
            user?.let {
                currentUserId = it.id

                // Автоматическое отслеживание сна
                autoTrackSleep(it.id)

                // Установка цели по шагам в зависимости от плана
                val userPlan = it.planType ?: "recommended"
                val stepGoal = if (userPlan == "fast") 12000 else 8000

                // Расчет дневной калорийной цели
                calorieGoal = CalorieGoalCalculator.calculateDailyCalories(
                    weight = it.weight,
                    height = it.height,
                    age = it.age,
                    isMale = it.isMale,
                    goalType = if (it.weight > it.desiredWeight) "lose" else "gain",
                    planType = userPlan
                )

                // Получение шагов и калорий за сегодня
                val today = getTodayDate()
                val savedSteps = db.stepDao().getStepsByDate(today, currentUserId)?.steps ?: 0
                stepCount = savedSteps


                caloriesConsumed = db.foodDao().getCaloriesForDate(today, currentUserId) ?: 0
                caloriesBurned = CalorieUtils.calculateCaloriesBurnedFromSteps(stepCount, it.weight)

                // Отображение данных в UI
                runOnUiThread {
                    binding.progressSteps.max = stepGoal
                    updateStepUI()
                    updateCaloriesUI()

                    binding.tvWeight.text = "${it.weight} кг"

                    // Оценка даты достижения цели
                    val goalDate = GoalDateEstimator.estimateGoalDate(
                        it.weight, it.desiredWeight, it.planType
                    )
                    binding.tvGoalDate.text = "Ожидаемая дата достижения цели: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(goalDate)}"

                    loadChartData()
                }
            }
        }

        // Обработка нажатий на карточки
        binding.cardCalories.setOnClickListener {
            startActivity(Intent(this, CalorieTrackerActivity::class.java))
            finish()
        }

        binding.btnUpdateWeight.setOnClickListener {
            startActivity(Intent(this, UpdateWeightActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Регистрация слушателя шагомера
        if (stepSensor != null) {
            sensorManager.registerListener(sensorListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Отписка от слушателя при паузе
        sensorManager.unregisterListener(sensorListener)
    }

    // Модификация калорийной цели с учетом плана и цели
    fun applyPlanFactor(baseCalories: Int, goalType: String, planType: String): Int {
        val factor = when (planType) {
            "fast" -> if (goalType == "lose") 0.75 else 1.25
            else -> if (goalType == "lose") 0.85 else 1.15
        }
        return (baseCalories * factor).toInt()
    }

    // Обновление UI для шагов
    private fun updateStepUI() {
        binding.progressSteps.progress = stepCount
        binding.tvSteps.text = "$stepCount / ${binding.progressSteps.max} шагов"
    }

    // Обновление UI для калорий
    private fun updateCaloriesUI() {
        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().getUser() ?: return@launch
            val todaySleep = db.sleepDao().getSleepByDate(user.id, getTodayDate())
            val fullBMR = BMRCalculator.calculateBMR(
                weight = user.weight,
                height = user.height,
                age = user.age,
                isMale = user.isMale
            )
            val sleepMinutes = todaySleep?.durationMinutes ?: 0
            val sleepHours = sleepMinutes / 60

            val sleepBMR = (fullBMR / 1440.0) * sleepMinutes
            val awakeBMR = (fullBMR / 1440.0) * (1440 - sleepMinutes)
            val stepCalories = CalorieUtils.calculateCaloriesBurnedFromSteps(stepCount, user.weight)

            val totalBurned = (sleepBMR + awakeBMR + stepCalories).toInt()

            val goalType = if (user.weight > user.desiredWeight) "lose" else "gain"
            val adjustedGoal = applyPlanFactor(totalBurned, goalType, user.planType ?: "recommended")

            runOnUiThread {
                caloriesBurned = totalBurned
                binding.tvCaloriesNet.text =
                    "Потреблено: $caloriesConsumed ккал\n" +
                            "Сожжено (шаги): ${stepCalories.toInt()} ккал\n" +
                            "ПБМ во сне: ${sleepBMR.toInt()} ккал\n" +
                            "Время во сне: ${sleepHours.toInt()} часов\n" +
                            "ПБМ в бодрствовании: ${awakeBMR.toInt()} ккал\n" +
                            "Цель на сегодня: $caloriesConsumed / $adjustedGoal ккал"
            }
        }
    }


    // Сохраняем шаги в базу данных
    private fun saveSteps() {
        CoroutineScope(Dispatchers.IO).launch {
            stepSaveMutex.withLock {
                val today = getTodayDate()
                val existing = db.stepDao().getStepsByDate(today, currentUserId)
                if (existing != null) {
                    db.stepDao().update(existing.copy(steps = stepCount))
                } else {
                    db.stepDao().insert(StepHistory(userId = currentUserId, date = today, steps = stepCount))
                }
            }
        }
    }

    // Получаем текущую дату
    private fun getTodayDate(): String = dateFormat.format(Date())

    // Загрузка данных для отображения графика шагов
    private fun loadChartData() {
        CoroutineScope(Dispatchers.IO).launch {
            val stepsData = db.stepDao().getLastWeekSteps(currentUserId)
            val calendar = Calendar.getInstance()
            val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            val stepsMapByDate = stepsData.associateBy { it.date }

            val entries = mutableListOf<BarEntry>()
            calendar.add(Calendar.DAY_OF_YEAR, -3)
            for (i in 0..6) {
                val dateStr = dateFormat.format(calendar.time)
                val steps = stepsMapByDate[dateStr]?.steps?.toFloat() ?: 0f
                entries.add(BarEntry(i.toFloat(), steps))
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            runOnUiThread {
                val dataSet = BarDataSet(entries, "Шаги").apply {
                    color = ContextCompat.getColor(this@DashboardActivity, R.color.primary)
                    valueTextColor = ContextCompat.getColor(this@DashboardActivity, R.color.on_surface)
                    setDrawValues(true)
                    valueTextSize = 14f
                }

                val barData = BarData(dataSet).apply {
                    barWidth = 0.5f
                }

                val dayLabels = mutableListOf<String>()
                val dayFormat = SimpleDateFormat("E", Locale("ru"))
                calendar.add(Calendar.DAY_OF_YEAR, -3)

                for (i in 0..6) {
                    dayLabels.add(dayFormat.format(calendar.time))
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                with(binding.stepsChart) {
                    data = barData
                    setFitBars(true)
                    description.isEnabled = false
                    axisRight.isEnabled = false
                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        granularity = 1f
                        setDrawGridLines(true)
                        axisMinimum = 0f
                        axisMaximum = 6f
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return dayLabels.getOrNull(value.toInt()) ?: ""
                            }
                        }
                    }
                    axisLeft.axisMinimum = 0f
                    invalidate()
                }
            }
        }
    }

    // Автоматическое определение сна и запись в базу
    private suspend fun autoTrackSleep(userId: Int) {
        val today = getTodayDate()
        val existing = db.sleepDao().getSleepByDate(userId, today)
        if (existing != null) return

        val segments = SleepActivityMonitor.getSleepSegments()
        if (segments.isEmpty()) return

        // Trim segments to fall within 23:00 - 07:00
        val dateFormatFull = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sleepStartWindow = dateFormatFull.parse("${getPreviousDate()} 23:00")!!.time
        val sleepEndWindow = dateFormatFull.parse("$today 07:00")!!.time

        var totalDuration = 0
        for ((start, end) in segments) {
            val trimmedStart = maxOf(start, sleepStartWindow)
            val trimmedEnd = minOf(end, sleepEndWindow)
            if (trimmedEnd > trimmedStart) {
                totalDuration += ((trimmedEnd - trimmedStart) / (60 * 1000)).toInt()
            }
        }

        if (totalDuration > 0) {
            val sleep = SleepHistory(
                userId = userId,
                date = today,
                sleepStart = sleepStartWindow,
                sleepEnd = sleepEndWindow,
                durationMinutes = totalDuration
            )
            db.sleepDao().insert(sleep)
        }

        // Сброс сегментов сна после записи
        SleepActivityMonitor.reset()
    }

    private fun getPreviousDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(cal.time)
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    POST_NOTIFICATIONS_PERMISSION_REQUEST_CODE
                )
            } else {
                startStepCounterService()
            }
        } else {
            startStepCounterService()
        }
    }

    private fun checkUsageAccessPermission() {
        if (!SleepActivityMonitor.hasUsageAccess(this)) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            Toast.makeText(this, "Please enable 'Usage Access' for sleep tracking to work", Toast.LENGTH_LONG).show()
        } else {
            SleepActivityMonitor.collectSleepData(this)
        }
    }

}
