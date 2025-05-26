package com.example.fitnesstracker.ui.weight

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.fitnesstracker.R
import com.example.fitnesstracker.database.AppDatabase
import com.example.fitnesstracker.database.WeightHistory
import com.example.fitnesstracker.databinding.ActivityUpdateWeightBinding
import com.example.fitnesstracker.ui.dashboard.DashboardActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class UpdateWeightActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateWeightBinding
    private lateinit var db: AppDatabase
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateWeightBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        loadCurrentWeight() // Загрузка текущего веса
        loadWeightHistory() // Загрузка истории веса

        lifecycleScope.launch {
            val user = db.userDao().getUser()
            user?.let {
                currentUserId = it.id
            }
        }

        binding.btnBack.setOnClickListener{val intent = Intent(this, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()}

        binding.btnSaveWeight.setOnClickListener {
            val weightInput = binding.etNewWeight.text.toString().toDoubleOrNull()
            if (weightInput != null) {
                saveNewWeight(weightInput) // Сохранение нового веса
            } else {
                Toast.makeText(this, "Введите корректный вес", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCurrentWeight() {
        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().getUser() // Получение текущего пользователя из БД
            user?.let {
                runOnUiThread {
                    binding.tvCurrentWeight.text = "${it.weight} кг" // Отображение текущего веса
                }
            }
        }
    }

    private fun saveNewWeight(newWeight: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().getUser() // Получение пользователя для обновления данных
            if (user != null) {
                // Обновление данных пользователя
                db.userDao().update(user.copy(weight = newWeight))

                // Сохранение нового веса в историю
                val today = dateFormat.format(Date())
                db.weightDao().insert(WeightHistory(date = today, weight = newWeight, userId = currentUserId))

                runOnUiThread {
                    Toast.makeText(this@UpdateWeightActivity, "Вес обновлен", Toast.LENGTH_SHORT).show()
                    binding.tvCurrentWeight.text = "$newWeight кг" // Обновление текущего веса на экране
                    binding.etNewWeight.text.clear() // Очистка поля ввода
                    loadWeightHistory() // Обновление графика
                }
            }
        }
    }

    private fun loadWeightHistory() {
        CoroutineScope(Dispatchers.IO).launch {
            val history = db.weightDao().getLast30DaysWeights(currentUserId) // Получение данных веса за последние 30 дней
            val entries = history.map {
                val date = dateFormat.parse(it.date)
                val day = Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_MONTH)
                Entry(day.toFloat(), it.weight.toFloat()) // Создание записи для графика
            }

            runOnUiThread {
                val dataSet = LineDataSet(entries, "Вес").apply {
                    color = ContextCompat.getColor(this@UpdateWeightActivity, R.color.primary) // Установка цвета графика
                    valueTextColor = ContextCompat.getColor(this@UpdateWeightActivity, R.color.on_surface)
                    setDrawValues(true)   // Отображение значений на графике
                    setDrawCircles(true)  // Отображение кругов на графике
                }

                with(binding.weightChart) {
                    description.isEnabled = false // Отключение описания графика
                    xAxis.position = XAxis.XAxisPosition.BOTTOM // Расположение оси X
                    axisRight.isEnabled = false // Отключение правой оси
                    xAxis.setDrawLabels(true) // Отображение меток на оси X
                    axisLeft.setDrawLabels(true) // Отображение меток на оси Y
                    legend.isEnabled = true // Включение легенды
                    data = LineData(dataSet) // Установка данных для графика
                    invalidate() // Перерисовка графика
                }
            }
        }
    }
}
