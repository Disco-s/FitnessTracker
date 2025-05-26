package com.example.fitnesstracker.network

import com.example.fitnesstracker.model.ProductDetailsResponse
import com.example.fitnesstracker.model.SearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApiService {

    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("ic") language: String = "ru",
        @Query("fields") fields: String = "product_name,brands,code",
        @Query("page_size") pageSize: Int = 20,
        @Query("nocache") nocache: Int = 1,
        @Query("json") json: Int = 1
    ): Response<SearchResponse>

    @GET("api/v2/product/{code}.json")
    suspend fun getProductByCode(
        @Path("code") code: String,
        @Query("fields") fields: String = "product_name,nutriments, energy-kcal_100g"
    ): Response<ProductDetailsResponse>
}
