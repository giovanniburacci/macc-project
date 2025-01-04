package com.example.macc_app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface PythonAnywhereAPI {
    //@GET("api/v1/data/factor/{number}")
    @GET("?format=json")
    //suspend fun getFactors(@Path("number") number: Long): FactorDBResponse
    suspend fun getFactors(@Query("query") number: Long): PythonAnywhereResponse
}

data class PythonAnywhereResponse(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("factors") val factors: List<List<String>>
)