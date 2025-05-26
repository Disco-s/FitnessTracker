package com.example.fitnesstracker.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class FoodItem(
    @SerializedName("food_id") val foodId: String,
    @SerializedName("food_name") val foodName: String,
    @SerializedName("food_description") val foodDescription: String,
    @SerializedName("servings") val servings: Servings
) : Parcelable

@Parcelize
data class Servings(
    @SerializedName("serving") val serving: List<Serving>
) : Parcelable

@Parcelize
data class Serving(
    @SerializedName("calories") val calories: String,
    @SerializedName("protein") val protein: String,
    @SerializedName("fat") val fat: String,
    @SerializedName("carbohydrate") val carbohydrates: String,
    @SerializedName("serving_description") val servingDescription: String
) : Parcelable