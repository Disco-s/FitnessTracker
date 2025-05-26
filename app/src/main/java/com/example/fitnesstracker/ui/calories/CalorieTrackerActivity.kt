package com.example.fitnesstracker.ui.calories

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.fitnesstracker.R
import com.example.fitnesstracker.database.AppDatabase
import com.example.fitnesstracker.databinding.ActivityCalorieTrackerBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import com.example.fitnesstracker.ui.dashboard.DashboardActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter


class CalorieTrackerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalorieTrackerBinding
    private lateinit var db: AppDatabase
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currentUserId: Int = -1

    companion object {
        const val REQUEST_CODE_ADD_FOOD = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalorieTrackerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            val user = db.userDao().getUser()
            user?.let {
                currentUserId = it.id
                loadDailyData()
                loadWeeklyChartData()
            } ?: run {
                showError("User not found")
            }
        }

        setupClickListeners() // Обработчики кнопок
        loadDailyData()       // Загрузка калорий на сегодня
        loadWeeklyChartData() // Загрузка данных за неделю для графика
    }

    private fun loadWeeklyChartData() {
        lifecycleScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val endDate = dateFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                val startDate = dateFormat.format(calendar.time)

                val weeklyData = db.foodDao().getCaloriesForDateRange(startDate, endDate, currentUserId)


                val dateToCalories = weeklyData.associateBy({ it.date }, { it.totalCalories })

                val entries = mutableListOf<Entry>()
                val labels = mutableListOf<String>()

                for (i in 0..6) {
                    val date = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -6 + i)
                    }
                    val formattedDate = dateFormat.format(date.time)
                    val calories = dateToCalories[formattedDate] ?: 0
                    entries.add(Entry(i.toFloat(), calories.toFloat()))
                    labels.add(SimpleDateFormat("EEE", Locale.getDefault()).format(date.time)) // Пн, Вт, ...
                }

                runOnUiThread {
                    drawChart(entries, labels)
                }
            } catch (e: Exception) {
                showError("Ошибка загрузки графика")
            }
        }
    }

    private fun drawChart(entries: List<Entry>, labels: List<String>) {
        val dataSet = LineDataSet(entries, "Калории")
        dataSet.color = ContextCompat.getColor(this, R.color.primary)
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.on_surface)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawValues(false)

        val lineData = LineData(dataSet)
        binding.caloriesChart.data = lineData


        val xAxis = binding.caloriesChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels) // метки оси X: дни недели
        xAxis.granularity = 1f
        xAxis.position = XAxis.XAxisPosition.BOTTOM


        binding.caloriesChart.axisRight.isEnabled = false
        binding.caloriesChart.description.isEnabled = false
        binding.caloriesChart.legend.isEnabled = false
        binding.caloriesChart.invalidate()
    }



    private fun setupClickListeners() {
        binding.btnAddBreakfast.setOnClickListener { openFoodSearch("breakfast") }
        binding.btnAddLunch.setOnClickListener { openFoodSearch("lunch") }
        binding.btnAddDinner.setOnClickListener { openFoodSearch("dinner") }
        binding.btnAddSnack.setOnClickListener { openFoodSearch("snack") }
        binding.btnBack.setOnClickListener{val intent = Intent(this, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()}
    }

    private fun openFoodSearch(mealType: String) {
        val intent = Intent(this, FoodSearchActivity::class.java).apply {
            putExtra("MEAL_TYPE", mealType)
        }
        startActivityForResult(intent, REQUEST_CODE_ADD_FOOD)
    }

    private fun loadDailyData() {
        lifecycleScope.launch {
            try {
                val today = dateFormat.format(Date())
                val entries = db.foodDao().getAllEntriesByDate(today, currentUserId)

                val totals = mutableMapOf(
                    "breakfast" to 0,
                    "lunch" to 0,
                    "dinner" to 0,
                    "snack" to 0
                )

                entries.forEach { entry ->
                    totals[entry.mealType] = totals[entry.mealType]!! + entry.calories
                }

                runOnUiThread {
                    binding.tvBreakfastTotal.text = formatCalories(totals["breakfast"]!!)
                    binding.tvLunchTotal.text = formatCalories(totals["lunch"]!!)
                    binding.tvDinnerTotal.text = formatCalories(totals["dinner"]!!)
                    binding.tvSnackTotal.text = formatCalories(totals["snack"]!!)
                    binding.tvTotalCalories.text = formatCalories(totals.values.sum())
                }

            } catch (e: Exception) {
                showError("Ошибка загрузки данных")
            }
        }
    }

    private fun formatCalories(calories: Int): String {
        return "$calories ккал"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ADD_FOOD && resultCode == RESULT_OK) {
            loadDailyData() // обновляем данные после добавления еды
        }
    }


    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}