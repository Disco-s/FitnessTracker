package com.example.fitnesstracker.network

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstracker.R
import com.example.fitnesstracker.model.Product

// Адаптер для отображения списка продуктов в RecyclerView
class FoodAdapter(
    private val mealType: String, // Тип приёма пищи (например, завтрак, обед)
    private val onItemClick: (Product) -> Unit // Коллбэк при клике на элемент
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    private var items: List<Product> = emptyList() // Список продуктов

    // Метод для обновления списка продуктов
    fun submitList(newItems: List<Product>) {
        items = newItems
        notifyDataSetChanged()
    }

    // ViewHolder для одного элемента списка
    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val foodName: TextView = itemView.findViewById(R.id.tvFoodName)
        private val brandName: TextView = itemView.findViewById(R.id.tvBrand)


        // Привязка данных к элементу
        fun bind(item: Product) {
            foodName.text = item.product_name ?: "Без названия"
            brandName.text = item.brands ?: ""

            // Обработка клика по элементу
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    // Создание ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    // Привязка данных к ViewHolder
    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(items[position])
    }

    // Количество элементов в списке
    override fun getItemCount(): Int = items.size
}
