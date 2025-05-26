package com.example.fitnesstracker.model

import com.google.gson.annotations.SerializedName

data class ProductDetailsResponse(
    @SerializedName("product") val product: Product?,
    @SerializedName("status") val status: Int
)
