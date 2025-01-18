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

    @GET("/user/{uid}")
    suspend fun getUser(@Path("uid") uid: String): UserResponse

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

    @GET("/comment/{chatId}")
    suspend fun fetchComments(@Path("chatId") chatId: Long): List<Comment>

    @GET("/chat/from-user/{uid}")
    suspend fun fetchHistory(@Path("uid") uid: String): List<ChatResponse>

    @GET("/chat/community")
    suspend fun fetchCommunity(): List<ChatResponseWithUsername>

    @PUT("/chat/{chatId}")
    suspend fun updateIsChatPublic(@Path("chatId") chatId: Long): ChatResponse

    @Headers("Content-Type: application/json")
    @PUT("/chat/change-name")
    suspend fun updateChatName(@Body body: ChangeNameBody): Object

    @Headers("Content-Type: application/json")
    @POST("/comment")
    suspend fun addComment(@Body body: AddCommentBody): Comment

}

data class PythonAnywhereFactorResponse(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("factors") val factors: List<List<String>>
)

data class UserResponse(
    @SerializedName("creation_time") val creation_time: String,
    @SerializedName("email") val email: String,
    @SerializedName("last_update") val last_update: String,
    @SerializedName("uid") val uid: String,
    @SerializedName("username") val username: String,
    @SerializedName("target_language") val target_language: String
)

data class ChatResponse(
    @SerializedName("creation_time") val creation_time: String,
    @SerializedName("id") val id: Long,
    @SerializedName("is_public") val is_public: Boolean,
    @SerializedName("last_update") val last_update: String,
    @SerializedName("name") var name: String,
    @SerializedName("user_id") val user_id: String,
    @SerializedName("preview") val preview: String?,
)

data class ChatResponseWithUsername(
    @SerializedName("creation_time") val creation_time: String,
    @SerializedName("id") val id: Long,
    @SerializedName("is_public") val is_public: Boolean,
    @SerializedName("last_update") val last_update: String,
    @SerializedName("name") var name: String,
    @SerializedName("user_id") val user_id: String,
    @SerializedName("preview") val preview: String?,
    @SerializedName("username") val username: String?,
)
data class AddChatBody(
    @SerializedName("name") val name: String,
    @SerializedName("is_public") val is_public: Boolean,
    @SerializedName("user_id") val user_id: String,
)

data class AddChatMessage(
    @SerializedName("message") val message: String,
    @SerializedName("translation") val translation: String,
    @SerializedName("city") val city: String?,
    @SerializedName("chat_id") val chat_id: Long,
)

data class AddUserBody(
    @SerializedName("uid") val uid: String,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("target_language") val target_language: String
)

data class MessageResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("message") val message: String,
    @SerializedName("translation") val translation: String,
    @SerializedName("city") val city: String,
    @SerializedName("chat_id") val chat_id: Long,
    @SerializedName("creation_time") val creation_time: String,
    @SerializedName("last_update") val last_update: String,
)

data class ChangeNameBody(
    @SerializedName("chat_id") val chat_id: Long,
    @SerializedName("name") val name: String,
)

data class Comment(
    @SerializedName("id") val id: Long,
    @SerializedName("chat_id") val chat_id: Long,
    @SerializedName("creation_time") val creation_time: String,
    @SerializedName("last_update") val last_update: String,
    @SerializedName("username") val username: String,
    @SerializedName("message") val message: String,
    @SerializedName("user_id") val user_id: String,
)

data class AddCommentBody(
    @SerializedName("message") val message: String,
    @SerializedName("user_id") val user_id: String,
    @SerializedName("chat_id") val chat_id: Long,
)