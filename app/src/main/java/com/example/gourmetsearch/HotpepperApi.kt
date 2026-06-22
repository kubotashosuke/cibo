package com.example.gourmetsearch

import retrofit2.http.GET
import retrofit2.http.Query

// データの受け皿
data class HotpepperResponse(val results: Results)
data class Results(val shop: List<Shop>)
data class Shop(
    val id: String,
    val name: String,
    val address: String,
    val mobile_access: String,
    val photo: Photo,
    val open: String
)
data class Photo(val mobile: MobilePhoto)
data class MobilePhoto(val l: String)

// API通信の設計図
interface HotpepperApiService {
    @GET("hotpepper/gourmet/v1/")
    suspend fun searchRestaurants(
        @Query("key") apiKey: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("range") range: Int,
        @Query("format") format: String = "json",
        @Query("count") count: Int = 20
    ): HotpepperResponse
}