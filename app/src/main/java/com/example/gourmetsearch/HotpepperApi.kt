package com.example.gourmetsearch

import retrofit2.http.GET
import retrofit2.http.Query

interface HotpepperApiService {
    @GET("hotpepper/gourmet/v1/?format=json")
    suspend fun searchRestaurants(
        @Query("key") apiKey: String,
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("range") range: Int,
        @Query("count") count: Int = 30
    ): ShopResponse
}

data class ShopResponse(
    val results: Results
)

data class Results(
    val shop: List<Shop>
)

data class Shop(
    val name: String,
    val address: String,
    val mobile_access: String,
    val open: String,
    val lat: Double,
    val lng: Double,
    val genre: Genre,
    val photo: Photo,
    val budget: Budget? = null
)

data class Budget(
    val code: String = "",
    val name: String = "",
    val average: String = ""
)

data class Genre(
    val name: String
)

data class Photo(
    val pc: PcPhoto,
    val mobile: MobilePhoto
)

data class PcPhoto(
    val l: String,
    val m: String,
    val s: String
)

data class MobilePhoto(
    val l: String
)