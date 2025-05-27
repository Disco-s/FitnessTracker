package com.example.fitnesstracker.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Product(
    val code: String?,
    val product_name: String?,
    val brands: String?,
    val nutriments: Nutriments?
) : Serializable

data class Nutriments(
    @SerializedName("energy-kcal_100g")
    val energyKcal100g: Double? = null,
    @SerializedName("fat_100g")
    val fat100g: Double? = null,
    @SerializedName("proteins_100g")
    val proteins100g: Double? = null,
    @SerializedName("carbohydrates_100g")
    val carbohydrates100g: Double? = null
)
