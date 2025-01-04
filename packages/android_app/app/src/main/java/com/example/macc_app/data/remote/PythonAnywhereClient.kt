package com.example.macc_app.data.remote

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object PythonAnywhereClient {
    private const val BASE_URL = ""

    init {
        Log.d("PythonAnywhereClient", "PythonAnywhereClient initialized")
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        }

    val api: PythonAnywhereAPI by lazy {
        retrofit.create(PythonAnywhereAPI::class.java)
       }
}
