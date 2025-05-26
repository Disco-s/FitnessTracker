package com.example.fitnesstracker.ui.registration

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import com.example.fitnesstracker.R
import com.example.fitnesstracker.database.AppDatabase
import com.example.fitnesstracker.database.User
import com.example.fitnesstracker.databinding.ActivityRegistrationBinding
import com.example.fitnesstracker.ui.plans.PlanSelectionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.NumberFormatException

class RegistrationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistrationBinding // ViewBinding для доступа к элементам интерфейса
    private lateinit var db: AppDatabase // Ссылка на базу данных

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this)  // Получаем экземпляр базы данных

        setupUI() // Настраиваем интерфейс
    }

    private fun setupUI() {
        binding.btnContinue.setOnClickListener {
            if (validateInput()) {
                saveUserData()
            }
        }
    }

    // Проверка введённых пользователем данных
    private fun validateInput(): Boolean {
        var isValid = true

        with(binding) {
            // Валидация ФИО
            if (etFullName.text.isNullOrEmpty()) {
                tilFullName.error = getString(R.string.required_field)
                isValid = false
            } else {
                tilFullName.error = null
            }

            // Валидация роста
            try {
                etHeight.text.toString().toInt()
                tilHeight.error = null
            } catch (e: NumberFormatException) {
                tilHeight.error = getString(R.string.invalid_number)
                isValid = false
            }

            // Валидация веса
            try {
                etWeight.text.toString().toDouble()
                tilWeight.error = null
            } catch (e: NumberFormatException) {
                tilWeight.error = getString(R.string.invalid_number)
                isValid = false
            }

            // Валидация возраста
            try {
                etAge.text.toString().toInt()
                tilAge.error = null
            } catch (e: NumberFormatException) {
                tilAge.error = getString(R.string.invalid_number)
                isValid = false
            }

            // Валидация выбора цели
            if (rgGoal.checkedRadioButtonId == -1) {
                tvGoalError.isVisible = true
                isValid = false
            } else {
                tvGoalError.isVisible = false
            }

            // Пол
            if (binding.rgGender.checkedRadioButtonId == -1) {
                Toast.makeText(this@RegistrationActivity, "Пожалуйста, выберите пол", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            // Желаемый вес
            try {
                binding.etDesiredWeight.text.toString().toDouble()
                binding.tilDesiredWeight.error = null
            } catch (e: NumberFormatException) {
                binding.tilDesiredWeight.error = getString(R.string.invalid_number)
                isValid = false
            }
        }

        return isValid
    }

    // Сохраняем введённые данные пользователя в базу данных
    private fun saveUserData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = User(
                    fullName = binding.etFullName.text.toString(),
                    height = binding.etHeight.text.toString().toInt(),
                    weight = binding.etWeight.text.toString().toDouble(),
                    age = binding.etAge.text.toString().toInt(),
                    goalType = if (binding.rbLose.isChecked) "lose" else "gain",
                    isMale = binding.rgGender.checkedRadioButtonId == R.id.rbMale,
                    desiredWeight = binding.etDesiredWeight.text.toString().toDouble(),
                    planType = "not_selected" // По умолчанию план ещё не выбран
                )

                db.userDao().insert(user)  // Сохраняем пользователя

                runOnUiThread {
                    // Переход к выбору плана после успешной регистрации
                    startActivity(Intent(this@RegistrationActivity, PlanSelectionActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    // Обработка ошибок при сохранении
                    Toast.makeText(
                        this@RegistrationActivity,
                        "Ошибка сохранения: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}