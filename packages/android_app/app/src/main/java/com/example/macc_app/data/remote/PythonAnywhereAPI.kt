package com.example.macc_app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PythonAnywhereFactorAPI {
    @GET("/user")
    suspend fun getUsers(): List<UserResponse>

    @GET("/chat/last-from-user/{uid}")
    suspend fun getLastChatFromUser(@Path("uid") uid: String): ChatResponse?

    @Headers("Content-Type: application/json")
    @POST("/chat")
    suspend fun addChat(@Body body: AddChatBody): ChatResponse

    @Headers("Content-Type: application/json")
    @POST("/message")
    suspend fun addMessage(@Body body: AddChatMessage): Object

    @Headers("Content-Type: application/json")
    @POST("/user")
    suspend fun addUser(@Body body: AddUserBody): Object

    @GET("/message/{chatId}")
    suspend fun fetchMessages(@Path("chatId") chatId: Long): List<MessageResponse>

    @GET("/chat/from-user/{uid}")
    suspend fun fetchHistory(@Path("uid") uid: String): List<ChatResponse>

    @PUT("/chat/{chatId}")
    suspend fun updateIsChatPublic(@Path("chatId") chatId: Long): ChatResponse

}

data class PythonAnywhereFactorResponse(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("factors") val factors: List<List<String>>
)

data class UserResponse(
    @SerializedName ("creation_time") val creation_time: String,
    @SerializedName ("email") val email: String,
    @SerializedName ("last_update") val last_update: String,
    @SerializedName ("uid") val uid: String,
    @SerializedName ("username") val username: String,
)

data class ChatResponse(
    @SerializedName ("creation_time") val creation_time: String,
    @SerializedName ("id") val id: Long,
    @SerializedName ("is_public") val is_public: Boolean,
    @SerializedName ("last_update") val last_update: String,
    @SerializedName ("name") val name: String,
    @SerializedName ("user_id") val user_id: String,
)

data class AddChatBody(
    @SerializedName ("name") val name: String,
    @SerializedName ("is_public") val is_public: Boolean,
    @SerializedName ("user_id") val user_id: String,
)

data class AddChatMessage(
    @SerializedName ("message") val message: String,
    @SerializedName ("translation") val translation: String,
    @SerializedName ("city") val city: String,
    @SerializedName ("chat_id") val chat_id: Long,
)

data class AddUserBody(
    @SerializedName ("uid") val uid: String,
    @SerializedName ("username") val username: String,
    @SerializedName ("email") val email: String,
)

data class MessageResponse(
    @SerializedName ("id") val id: Long,
    @SerializedName ("message") val message: String,
    @SerializedName ("translation") val translation: String,
    @SerializedName ("city") val city: String,
    @SerializedName ("chat_id") val chat_id: Long,
    @SerializedName ("creation_time") val creation_time: String,
    @SerializedName ("last_update") val last_update: String,
)