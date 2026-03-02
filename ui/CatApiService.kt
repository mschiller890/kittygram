package com.micik.kittygram.ui

import retrofit2.http.GET
import retrofit2.http.Query

interface CatApiService {
    @GET("images/search")
    suspend fun getCatImages(
        @Query("limit") limit: Int = 10,
        @Query("breed_ids") breedIds: String? = null,
        @Query("api_key") apiKey: String? = null
    ): List<CatImage>
}
