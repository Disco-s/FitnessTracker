package com.example.fitnesstracker.ui.plans

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.fitnesstracker.R
import com.example.fitnesstracker.database.AppDatabase
import com.example.fitnesstracker.databinding.ActivityPlanSelectionBinding
import com.example.fitnesstracker.ui.dashboard.DashboardActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlanSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlanSelectionBinding // ViewBinding для разметки
    private lateinit var db: AppDatabase // Ссылка на базу данных
    private var selectedPlan: String = "recommended" // Выбранный план по умолчанию

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlanSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this)

        setupPlanSelection() // Настройка выбора плана
        setupConfirmButton() // Настройка кнопки подтверждения
    }

    private fun setupPlanSelection() {
        // Обработчик выбора "быстрого" плана
        binding.cardFastPlan.setOnClickListener {
            selectedPlan = "fast"
            updateCardAppearance(binding.cardFastPlan, true)
            updateCardAppearance(binding.cardRecommendedPlan, false)
        }

        // Обработчик выбора "рекомендуемого" плана
        binding.cardRecommendedPlan.setOnClickListener {
            selectedPlan = "recommended"
            updateCardAppearance(binding.cardRecommendedPlan, true)
            updateCardAppearance(binding.cardFastPlan, false)
        }
    }

    // Обновление внешнего вида карточек при выборе
    private fun updateCardAppearance(card: com.google.android.material.card.MaterialCardView, isSelected: Boolean) {
        val strokeColor = if (isSelected) {
            ContextCompat.getColor(this, R.color.primary) // Цвет выделения
        } else {
            ContextCompat.getColor(this, R.color.surface_variant) // Цвет по умолчанию
        }

        card.strokeColor = strokeColor
        card.strokeWidth = if (isSelected) resources.getDimensionPixelSize(R.dimen.card_stroke_width) else 0
    }

    // Обработка нажатия кнопки подтверждения выбора плана
    private fun setupConfirmButton() {
        binding.btnConfirmPlan.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val user = db.userDao().getUser() // Получение текущего пользователя из БД
                    user?.let {
                        val updatedUser = it.copy(planType = selectedPlan) // Обновление выбранного плана
                        db.userDao().update(updatedUser) // Сохранение изменений

                        runOnUiThread {
                            // Переход к экрану Dashboard
                            startActivity(Intent(this@PlanSelectionActivity, DashboardActivity::class.java))
                            finish()
                        }
                    } ?: run {
                        runOnUiThread {
                            // Пользователь не найден
                            Toast.makeText(
                                this@PlanSelectionActivity,
                                "Ошибка: данные пользователя не найдены",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        // Обработка исключений
                        Toast.makeText(
                            this@PlanSelectionActivity,
                            "Ошибка: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}