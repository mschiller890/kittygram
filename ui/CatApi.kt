package com.micik.kittygram.ui

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CatApi {
    val service: CatApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.thecatapi.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CatApiService::class.java)
    }
}
