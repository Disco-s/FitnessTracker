package com.example.fitnesstracker.ui.dashboard

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
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

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var db: AppDatabase
    private var stepSensor: Sensor? = null
    private var stepCount = 0
    private var initialStepCount: Int? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currentUserId: Int = -1
    private val stepSaveMutex = Mutex()

    // Calories tracking
    private var calorieGoal = 0
    private var caloriesConsumed = 0
    private var caloriesBurned = 0

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val totalSteps = it.values[0].toInt()
                val today = getTodayDate()

                // Initialize offset once per day on first event
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

                // Calculate steps from offset
                val currentSteps = totalSteps - (initialStepCount ?: totalSteps)
                if (currentSteps != stepCount && currentSteps >= 0) {
                    stepCount = currentSteps

                    saveSteps()

                    runOnUiThread {
                        updateStepUI()
                    }

                    runOnUiThread { updateCaloriesUI() }
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SleepActivityMonitor.initialize(applicationContext)

        db = AppDatabase.getDatabase(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Toast.makeText(this, "Датчик шагов не найден!", Toast.LENGTH_LONG).show()
        }

        // Start StepCounterService if permission was granted in LauncherActivity
        val permissionGranted = intent.getBooleanExtra("PERMISSION_GRANTED", false)
        if (permissionGranted) {
            startStepCounterService()
        }

        com.example.fitnesstracker.test.DummyDataSeeder.seed(db)

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
                autoTrackSleep(it.id)
                val userPlan = it.planType ?: "recommended"
                val stepGoal = if (userPlan == "fast") 12000 else 8000

                calorieGoal = CalorieGoalCalculator.calculateDailyCalories(
                    weight = it.weight,
                    height = it.height,
                    age = it.age,
                    isMale = it.isMale,
                    goalType = if (it.weight > it.desiredWeight) "lose" else "gain",
                    planType = userPlan
                )

                // Load today's saved steps to sync UI and initialStepCount offset
                val today = getTodayDate()
                val savedSteps = db.stepDao().getStepsByDate(today, currentUserId)?.steps ?: 0
                stepCount = savedSteps
                //initialStepCount = null // Reset here to force offset recalculation on first sensor event

                caloriesConsumed = db.foodDao().getCaloriesForDate(today, currentUserId) ?: 0
                caloriesBurned = CalorieUtils.calculateCaloriesBurnedFromSteps(stepCount, it.weight)

                runOnUiThread {
                    binding.progressSteps.max = stepGoal
                    updateStepUI()
                    updateCaloriesUI()

                    binding.tvWeight.text = "${it.weight} кг"

                    val goalDate = GoalDateEstimator.estimateGoalDate(
                        it.weight, it.desiredWeight, it.planType
                    )
                    binding.tvGoalDate.text = "Ожидаемая дата достижения цели: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(goalDate)}"

                    loadChartData()
                }
            }
        }

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
        if (stepSensor != null) {
            sensorManager.registerListener(sensorListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorListener)
    }

    fun applyPlanFactor(baseCalories: Int, goalType: String, planType: String): Int {
        val factor = when (planType) {
            "fast" -> if (goalType == "lose") 0.75 else 1.25
            else -> if (goalType == "lose") 0.85 else 1.15
        }
        return (baseCalories * factor).toInt()
    }

    private fun updateStepUI() {
        binding.progressSteps.progress = stepCount
        binding.tvSteps.text = "$stepCount / ${binding.progressSteps.max} шагов"
    }

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
                            "ПБМ в бодрствовании: ${awakeBMR.toInt()} ккал\n" +
                            "Цель на сегодня: $caloriesConsumed / $adjustedGoal ккал"
            }
        }
    }


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

    private fun getTodayDate(): String = dateFormat.format(Date())

    private fun loadChartData() {
        CoroutineScope(Dispatchers.IO).launch {
            val stepsData = db.stepDao().getLastWeekSteps(currentUserId)
            val calendar = Calendar.getInstance()
            val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Sunday=1 ... Saturday=7

            val stepsMap = stepsData.associateBy {
                val date = dateFormat.parse(it.date)
                Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_WEEK)
            }

            val entries = mutableListOf<BarEntry>()
            for (i in -3..3) {
                val dayIndex = ((todayDayOfWeek - 1 + i + 7) % 7) + 1
                val steps = stepsMap[dayIndex]?.steps?.toFloat() ?: 0f
                entries.add(BarEntry(i + 3f, steps))
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

    private suspend fun autoTrackSleep(userId: Int) {
        val today = getTodayDate()
        val existing = db.sleepDao().getSleepByDate(userId, today)
        if (existing != null) return

        if (!SleepActivityMonitor.wasInactiveDuringSleepHours()) {
            return // user was active, don't log sleep
        }

        val cal = Calendar.getInstance()
        cal.time = dateFormat.parse(today)!!
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val prevDay = dateFormat.format(cal.time)

        val sleepStart = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse("$prevDay 23:00")!!.time
        val sleepEnd = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse("$today 07:00")!!.time
        val durationMinutes = ((sleepEnd - sleepStart) / (60 * 1000)).toInt()

        val sleep = SleepHistory(
            userId = userId,
            date = today,
            sleepStart = sleepStart,
            sleepEnd = sleepEnd,
            durationMinutes = durationMinutes
        )

        db.sleepDao().insert(sleep)
        SleepActivityMonitor.reset() // Reset for next day
    }

}
