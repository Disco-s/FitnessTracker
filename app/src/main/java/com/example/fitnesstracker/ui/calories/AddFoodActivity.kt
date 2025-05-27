package com.example.fitnesstracker.ui.calories

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fitnesstracker.database.AppDatabase
import com.example.fitnesstracker.database.FoodEntry
import com.example.fitnesstracker.databinding.ActivityAddFoodBinding
import com.example.fitnesstracker.model.Product
import com.example.fitnesstracker.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AddFoodActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddFoodBinding // Привязка к layout
    private lateinit var db: AppDatabase // Экземпляр базы данных
    private lateinit var mealType: String // Тип приема пищи
    private var product: Product? = null // Данные продукта
    private var currentUserId: Int = -1

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Формат даты

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFoodBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getDatabase(this) // Получение БД

        val code = intent.getStringExtra("PRODUCT_CODE") // Получение кода продукта из интента
        mealType = intent.getStringExtra("MEAL_TYPE") ?: "breakfast" // Получение типа еды

        if (code == null) {
            Toast.makeText(this, "No product code provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchProductDetails(code) // Загрузка информации о продукте

        lifecycleScope.launch {
            val user = db.userDao().getUser()
            user?.let {
                currentUserId = it.id
            } ?: run {
                showError("User not found")
            }
        }

        // Обновление нутриентов при вводе количества
        binding.etQuantity.setOnEditorActionListener { _, _, _ ->
            updateNutritionDisplay()
            false
        }

        // Сохранение еды при нажатии кнопки
        binding.btnAdd.setOnClickListener {
            saveEntryToDatabase()
        }
    }

    // Загрузка деталей продукта с сервера
    private fun fetchProductDetails(code: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getProductByCode(code, "product_name,nutriments")

                if (response.isSuccessful) {
                    val productDetails = response.body()?.product
                    if (productDetails != null) {
                        product = productDetails
                        displayProductData()
                    } else {
                        showError("Failed to load product details.")
                    }
                } else {
                    showError("Failed to load product details: ${response.message()}")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    // Отображение информации о продукте на экране
    private fun displayProductData() {
        product?.let { product ->
            binding.tvFoodName.text = product.product_name ?: "Без названия"
            updateNutritionDisplay()
        }
    }

    // Обновление информации о калориях и нутриентах
    private fun updateNutritionDisplay() {
        val quantity = binding.etQuantity.text.toString().toDoubleOrNull() ?: 100.0
        val factor = quantity / 100.0

        val nutriments = product?.nutriments

        val calories = (nutriments?.energyKcal100g ?: 0.0) * factor
        val protein = (nutriments?.proteins100g ?: 0.0) * factor
        val fat = (nutriments?.fat100g ?: 0.0) * factor
        val carbs = (nutriments?.carbohydrates100g ?: 0.0) * factor

        binding.tvCalories.text = "Калории: ${calories.toInt()} ккал"
        binding.tvProtein.text = "Белки: ${protein.toInt()} г"
        binding.tvFat.text = "Жиры: ${fat.toInt()} г"
        binding.tvCarbs.text = "Углеводы: ${carbs.toInt()} г"
    }

    // Сохранение введенной еды в БД
    private fun saveEntryToDatabase() {
        val quantity = binding.etQuantity.text.toString().toDoubleOrNull()
        if (quantity == null || quantity <= 0) {
            showError("Введите количество в граммах")
            return
        }

        val factor = quantity / 100.0
        val nutriments = product?.nutriments ?: return

        val entry = FoodEntry(
            date = dateFormat.format(Date()), // Сегодняшняя дата
            mealType = mealType, // Тип приема пищи
            foodName = product?.product_name ?: "Продукт", // Название
            quantity = quantity, // Количество
            calories = ((nutriments.energyKcal100g ?: 0.0) * factor).toInt(),
            protein = ((nutriments.proteins100g ?: 0.0) * factor).toInt(),
            fat = ((nutriments.fat100g ?: 0.0) * factor).toInt(),
            carbs = ((nutriments.carbohydrates100g ?: 0.0) * factor).toInt(),
            userId = currentUserId
        )

        lifecycleScope.launch {
            db.foodDao().insert(entry) // Вставка в БД
            setResult(Activity.RESULT_OK) // Устанавливаем результат для родительской Activity
            finish() // Закрываем экран
        }
    }

    // Показ тоста с ошибкой
    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
