package com.example.fitnesstracker.ui.calories

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitnesstracker.databinding.ActivityFoodSearchBinding
import com.example.fitnesstracker.network.RetrofitInstance
import com.example.fitnesstracker.network.FoodAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

private var searchJob: Job? = null


class
FoodSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFoodSearchBinding
    private lateinit var mealType: String
    private lateinit var adapter: FoodAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получение типа приёма пищи из Intent (например, завтрак, обед и т.п.)
        mealType = intent.getStringExtra("MEAL_TYPE") ?: "breakfast"

        setupRecyclerView()
        setupSearchView()
    }

    companion object {
        const val REQUEST_CODE_ADD_FOOD = 1002
    }

    // Настройка списка продуктов (RecyclerView) и обработка выбора элемента
    private fun setupRecyclerView() {
        adapter = FoodAdapter(mealType) { product ->
            openAddFoodActivity(product.code ?: "")
            Log.d("FoodSearchActivity", "Product Code from search: ${product.code}")
        }

        binding.rvFoodResults.apply {
            layoutManager = LinearLayoutManager(this@FoodSearchActivity)
            adapter = this@FoodSearchActivity.adapter
        }
    }

    // Настройка поля поиска с отложенным запросом (debounce)
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Отмена предыдущего поиска, если пользователь ещё вводит текст
                searchJob?.cancel()

                // Задержка перед выполнением поиска (500 мс)
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    newText?.let {
                        if (it.length >= 2) {
                            searchFoods(it)
                        }
                    }
                }

                return true
            }
        })
    }


    // Выполнение сетевого запроса на поиск продуктов
    private fun searchFoods(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.searchProducts(query, language = "ru", pageSize = 20, nocache = 1)
                if (response.isSuccessful) {
                    val products = response.body()?.products ?: emptyList()
                    runOnUiThread {
                        if (products.isEmpty()) {
                            Toast.makeText(this@FoodSearchActivity, "No products found", Toast.LENGTH_SHORT).show()
                        } else {
                            products.forEach { product ->
                                Log.d("FoodSearchActivity", "Product: ${product}, Code: ${product.code}")
                            }
                            adapter.submitList(products)
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@FoodSearchActivity, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@FoodSearchActivity, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Обработка результата возврата из активности добавления продукта
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ADD_FOOD && resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }


    // Открытие экрана добавления продукта с передачей кода продукта и типа приёма пищи
    private fun openAddFoodActivity(productCode: String) {
        val intent = Intent(this, AddFoodActivity::class.java).apply {
            putExtra("PRODUCT_CODE", productCode)
            putExtra("MEAL_TYPE", mealType)
        }
        startActivityForResult(intent, REQUEST_CODE_ADD_FOOD)
    }
}
