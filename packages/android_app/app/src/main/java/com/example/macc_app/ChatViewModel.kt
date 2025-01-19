package com.example.macc_app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.Manifest
import android.location.Geocoder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import com.example.macc_app.data.remote.PythonAnywhereFactorAPI
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import androidx.lifecycle.ViewModelProvider
import com.example.macc_app.data.remote.AddChatBody
import com.example.macc_app.data.remote.AddChatMessage
import com.example.macc_app.data.remote.AddCommentBody
import com.example.macc_app.data.remote.AddUserBody
import com.example.macc_app.data.remote.ChangeNameBody
import com.example.macc_app.data.remote.ChatResponse
import com.example.macc_app.data.remote.ChatResponseWithUsername
import com.example.macc_app.data.remote.Comment
import com.example.macc_app.data.remote.MessageResponse
import retrofit2.Retrofit
import java.io.IOException

class ChatViewModelFactory(private val retrofit: Retrofit) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(retrofit) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
class ChatViewModel(retrofit: Retrofit): ViewModel() {

    private val myApiService: PythonAnywhereFactorAPI = retrofit.create(PythonAnywhereFactorAPI::class.java)

    val messages = mutableStateListOf<Message>()

    val readOnlyMessages = mutableStateListOf<Message>()

    val history = mutableStateListOf<ChatResponse>()

    val comments = mutableStateListOf<Comment>()

    val community = mutableStateListOf<ChatResponseWithUsername>()

    val showConfirmationPopup = mutableStateOf(false)
    val lastMessage = mutableStateOf<Message?>(null)

    val lastChat = mutableStateOf<ChatResponse?>(null)

    val readOnlyChat = mutableStateOf<ChatResponse?>(null)

    val isDownloadingModel = mutableStateOf(false)

    private var recognizer: SpeechRecognizer? = null
    private var locationProviderClient: FusedLocationProviderClient? = null

    companion object {
        const val DEFAULT_TARGET_LANGUAGE = "it"
    }

    // initializing speech components after main activity gets launched
    fun initializeSpeechComponents(context: Context) {
        // initialize recognizer
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)

        // initialize location provider
        locationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    }
    // mapping messages from backend format to client data class
    private fun mapMessageResponseListToMessageList(messageResponses: List<MessageResponse>): List<Message> {
        return messageResponses.map { mapMessageResponseToMessage(it) }
    }

    // message mapper
    private fun mapMessageResponseToMessage(messageResponse: MessageResponse): Message {
        val mess = Message(
            originalContent = messageResponse.message
        )
        // setting mutable state to update the composable ui
        mess.translatedContent.value = messageResponse.translation
        mess.city.value = messageResponse.city
        return mess
    }

    // fetching history with network call
    fun fetchHistory(uid: String) {

        // launching coroutine with IO dispatcher for network
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("com.example.macc_app.ChatViewModel", "Fetch history with uid: $uid")
                Log.d("ChatViewModel", "Fetch history with uid: $uid")

                // retrieving response
                val response = myApiService.fetchHistory(uid)

                // resetting history and setting again
                history.clear()
                history.addAll(response)

                Log.d("com.example.macc_app.ChatViewModel", "Response from history API: $response")
            } catch (e: Exception) {
                Log.e("com.example.macc_app.ChatViewModel", "Error fetching chats history", e)
            }
        }
    }

    // fetching community with network call
    fun fetchCommunity() {

        // launching coroutine with IO dispatcher for network
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("com.example.macc_app.ChatViewModel", "Fetch community")

                // retrieving response
                val response = myApiService.fetchCommunity()

                // resetting history and setting again
                community.clear()
                community.addAll(response)
                Log.d("com.example.macc_app.ChatViewModel", "Response from community API: $response")

            } catch (e: Exception) {
                Log.e("com.example.macc_app.ChatViewModel", "Error fetching chats community", e)
            }
        }
    }

    // updating chat name with network call
    fun updateChatName(chatId: Long, name: String) {

        // launching coroutine with IO dispatcher for network
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("com.example.macc_app.ChatViewModel", "Update chat name with name: $name for chat $chatId")

                // defining payload for network call
                val body = ChangeNameBody(chat_id = chatId, name = name)

                // retrieving response
                val resp = myApiService.updateChatName(body)
                Log.d("com.example.macc_app.ChatViewModel", "Response from API: $resp")

                // updating chat name in state for last chat screen
                if(lastChat.value?.id == chatId) {
                    val updatedChat = lastChat.value!!.copy()
                    updatedChat.name = name
                    lastChat.value = updatedChat
                }

                // updating chat name in state for history screen
                if(readOnlyChat.value?.id == chatId) {
                    val updatedChat = readOnlyChat.value!!.copy()
                    updatedChat.name = name
                    readOnlyChat.value = updatedChat
                }
            } catch (e: Exception) {
                Log.e("com.example.macc_app.ChatViewModel", "Error changing name", e)
            }
        }
    }

    // fetching messages with network call
    fun fetchMessages(chatId: Long) {
        // launching coroutine with IO dispatcher for network
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("com.example.macc_app.ChatViewModel", "Fetch messages with chatId: $chatId")
                // retrieving response
                val response = myApiService.fetchMessages(chatId)
                messages.clear()
                messages.addAll(mapMessageResponseListToMessageList(response).toMutableList())
                Log.d("com.example.macc_app.ChatViewModel", "Response from fetchMessages API: $response")
            } catch (e: Exception) {
                Log.e("com.example.macc_app.ChatViewModel", "Error fetching messages", e)
            }
        }
    }

    // fetching messages with network call
    fun fetchReadOnlyMessages(chatId: Long) {
        // launching coroutine with IO dispatcher for network
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("com.example.macc_app.ChatViewModel", "Fetch read only messages with chatId: $chatId")
                // retrieving response
                val response = myApiService.fetchMessages(chatId)
                readOnlyMessages.clear()
                readOnlyMessages.addAll(mapMessageResponseListToMessageList(response).toMutableList())
                Log.d("com.example.macc_app.ChatViewModel", "Response from fetchMessages API: $response")
            } catch (e: Exception) {
                Log.e("com.example.macc_app.ChatViewModel", "Error fetching messages", e)
            }
        }
    }

    // fetching comments with network call
    fun fetchComments(chatId: Long) {
        // launching coroutine with IO dispatcher for network
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("com.example.macc_app.ChatViewModel", "Fetch comments with chatId: $chatId")
                // retrieving response
                val response = myApiService.fetchComments(chatId)
                comments.clear()
                comments.addAll(response)
                Log.d("com.example.macc_app.ChatViewModel", "Response from fetchComments API: $response")
            } catch (e: Exception) {
                Log.e("com.example.macc_app.ChatViewModel", "Error fetching comments", e)
            }
        }
    }

    // adding comment with network call
    fun addComment(body: AddCommentBody) {
        // launching coroutine with IO dispatcher for network
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("com.example.macc_app.ChatViewModel", "Uid: $body")
                // retrieving response
                val response = myApiService.addComment(body)

                // fetching again comments
                fetchComments(body.chat_id)
                Log.d("com.example.macc_app.ChatViewModel", "Response from addComment API: $response")
            } catch (e: Exception) {
                Log.e("com.example.macc_app.ChatViewModel", "Error adding comment", e)
            }
        }
    }


    // setting chat for read-only screens like history and community
    fun setReadOnlyChat(chat: ChatResponse) {
        readOnlyChat.value = chat
    }

    // updating chat privacy with network call
    fun updateIsChatPublic(chatId: Long) {
        // launching coroutine with IO dispatcher for network
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("com.example.macc_app.ChatViewModel", "chatId: $chatId")
                // retrieving response
                val response = myApiService.updateIsChatPublic(chatId)

                // updating chat privacy in state for last chat screen
                if(lastChat.value?.id == chatId) {
                    lastChat.value = response
                }

                // updating chat privacy in state for history screen
                if(readOnlyChat.value?.id == chatId){
                    readOnlyChat.value = response
                }
                Log.d("com.example.macc_app.ChatViewModel", "Response from API updateIsChatPublic: $response")
            } catch (e: Exception) {
                Log.e("com.example.macc_app.ChatViewModel", "Error updateIsChatPublic", e)
            }
        }
    }

    // fetching last chat with network call
    fun fetchLastChat(uid: String) {
        // launching coroutine with IO dispatcher for network
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("com.example.macc_app.ChatViewModel", "Uid: $uid")
                // retrieving response
                val response = myApiService.getLastChatFromUser(uid)
                lastChat.value = response

                // fetching messages for given chatId
                fetchMessages(lastChat.value!!.id)
                Log.d("com.example.macc_app.ChatViewModel", "Response from fetchLastChat API: $response")
            } catch (e: Exception) {
                if(uid.isNotEmpty()) {
                    val body = AddChatBody(name = "New Chat", is_public = false, user_id = uid)
                    createChat(body, false)
                }
                Log.e("com.example.macc_app.ChatViewModel", "Error fetching last chat", e)
            }
        }
    }

    // creating new chat with network call
    fun createChat(body: AddChatBody, clearMessages: Boolean) {
        // launching coroutine with IO dispatcher for network
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("com.example.macc_app.ChatViewModel", "Uid: $body")
                // retrieving response
                val response = myApiService.addChat(body)

                // clearing last chat data
                lastChat.value = response
                if(clearMessages) {
                    messages.clear()
                }
                Log.d("com.example.macc_app.ChatViewModel", "Response from addChat API: $response")
            } catch (e: Exception) {
                Log.e("com.example.macc_app.ChatViewModel", "Error creating chat", e)
            }
        }
    }

    // adding new message with network call
    private fun addMessage(body: AddChatMessage) {
        // launching coroutine with IO dispatcher for network
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("com.example.macc_app.ChatViewModel", "Uid: $body")
                // retrieving response
                val response = myApiService.addMessage(body)
                Log.d("com.example.macc_app.ChatViewModel", "Response from addMessage API: $response")
            } catch (e: Exception) {
                Log.e("com.example.macc_app.ChatViewModel", "Error adding message", e)
            }
        }
    }

    // creating user in mysql db with network call
    fun createUser(body: AddUserBody) {
        // launching coroutine with IO dispatcher for network
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("com.example.macc_app.ChatViewModel", "Uid: $body")
                // retrieving response
                val response = myApiService.addUser(body)
                Log.d("com.example.macc_app.ChatViewModel", "Response from API: $response")
            } catch (e: Exception) {
                Log.e("com.example.macc_app.ChatViewModel", "Error creating user", e)
            }
        }
    }

    fun sendMessage(content: String, type: MessageType, context: Context) {
        val message = Message(originalContent = content)

        // showing new message in the ui
        messages.add(message)

        // retrieving target language from preferences
        val targetLanguage = getTargetLanguage(context)

        // launching coroutine with Default dispatcher for processing purpose, CPU-intensive
        viewModelScope.launch(Dispatchers.Default) {

            // location permission has not been given
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (type == MessageType.TEXT) {
                    // if message is text, process it directly
                    processTextMessage(message, targetLanguage, "Unknown", context)
                } else if (type == MessageType.AUDIO) {
                    // otherwise, first transcribe it
                    transcribeAudio(context, message, targetLanguage, "Unknown")
                }
            } else {
                // Permission already granted, fetch location
                if (type == MessageType.TEXT) {
                    fetchLocation(
                        context,
                        locationProviderClient!!
                    ) { cityName -> viewModelScope.launch(Dispatchers.Default) {processTextMessage(message, targetLanguage, cityName, context) }}
                } else if (type == MessageType.AUDIO) {
                    fetchLocation(
                        context,
                        locationProviderClient!!
                    ) { cityName -> viewModelScope.launch(Dispatchers.Default) {transcribeAudio(context, message, targetLanguage, cityName) }}
                }
            }
        }
    }

    // utility function to retrieve target language from preferences
    private fun getTargetLanguage(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("targetLanguage", DEFAULT_TARGET_LANGUAGE) ?: DEFAULT_TARGET_LANGUAGE
    }

    // suspend function launched from a coroutine to perform the translation
    private suspend fun processTextMessage(message: Message, targetLanguage: String, cityName: String, context: Context) {
        try {

            // identify source language of the message using ml kit
            val languageCode = identifyLanguage(message.originalContent)

            // language has been recognized
            if (languageCode != "und") {
                translateText(message, languageCode, targetLanguage, cityName)
            } else {
                // language has not been recognized, translation is not performed
                // and the translated content is equal to the original
                message.translatedContent.value = message.originalContent
                message.city.value = cityName
                val body = AddChatMessage(message = message.originalContent, translation = message.originalContent, city = cityName, chat_id = lastChat.value!!.id)

                addMessage(body)
                Toast.makeText(context, "Language not identified, please try again", Toast.LENGTH_LONG).show()
                Log.e("com.example.macc_app.ChatViewModel", "Language not identified")
            }
        } catch (e: Exception) {
            Log.e("com.example.macc_app.ChatViewModel", "Language identification failed", e)
        }
    }

    // function to identify language using ml kit
    private suspend fun identifyLanguage(text: String): String = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            val languageIdentifier = LanguageIdentification.getClient()
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { languageCode ->
                    continuation.resume(languageCode)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    // suspend function to translate text using ml kit
    private suspend fun translateText(message: Message, sourceLanguage: String, targetLanguage: String, cityName: String) {
        // options function to set source and target languages
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLanguage) ?: TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLanguage) ?: TranslateLanguage.ENGLISH)
            .build()

        // creating translator
        val translator = Translation.getClient(options)

        try {
            // isDownloadingModel is used to show a loading indicator
            isDownloadingModel.value = true

            // downloading model if not previously downloaded
            translator.downloadModelIfNeeded().await()
            isDownloadingModel.value = false

            // obtaining translated text
            val translatedText = translator.translate(message.originalContent).await()

            // message translated content and city are updated
            message.translatedContent.value = translatedText
            message.city.value = cityName

            // creating payload
            val body = AddChatMessage(message = message.originalContent, translation = translatedText, city = cityName, chat_id = lastChat.value!!.id)

            // invoking api
            addMessage(body)
            Log.d("com.example.macc_app.ChatViewModel", "Translated text: $translatedText")

        } catch (e: Exception) {
            Log.e("com.example.macc_app.ChatViewModel", "Translation failed", e)
        }
    }

    // transcribe audio to text
    private suspend fun transcribeAudio(context: Context, message: Message, targetLanguage: String, cityName: String) {
        try {
            // start listening for speech
            val text = recognizeSpeech()

            if (text.isNotEmpty()) {
                // text has been recognized
                message.translatedContent.value = text
                // processing function is invoked with recognized text
                processTextMessage(message, targetLanguage, cityName, context)
            } else {
                Log.e("SpeechRecognition", "No recognizable speech")
            }
        } catch (e: Exception) {
            Log.e("SpeechRecognition", "Error during speech recognition", e)
        }
    }

    // speech recognition
    private suspend fun recognizeSpeech(): String = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            // flag to ensure single resumption, same recognition is not performed twice
            var isContinuationResumed = false

            // defines listener object with functions for recognition
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("SpeechRecognition", "Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("SpeechRecognition", "Speech beginning")
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d("SpeechRecognition", "Speech ended")
                }

                override fun onError(error: Int) {
                    if (!isContinuationResumed) {
                        // can't resume twice after error occurrance
                        isContinuationResumed = true
                        Log.e("SpeechRecognition", "Error occurred: $error")
                        continuation.resumeWithException(RuntimeException("$error"))
                    } else {
                        Log.w("SpeechRecognition", "onError called after continuation already resumed.")
                    }
                }

                override fun onResults(results: Bundle?) {
                    // check that coroutine is not resuming from error state
                    if (!isContinuationResumed) {
                        isContinuationResumed = true

                        // recognized words array
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                        // creates string from array of words
                        val text = matches?.joinToString(separator = " ") ?: ""

                        // resume coroutine
                        continuation.resume(text)
                    } else {
                        Log.w("SpeechRecognition", "onResults called after continuation already resumed.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }

            // set listeners object to recognizer
            recognizer?.setRecognitionListener(listener)

            // defines intent
            val audioIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }

            // start listening
            recognizer?.startListening(audioIntent)

            // on coroutine cancellation, stop listening and reset
            continuation.invokeOnCancellation {
                recognizer?.stopListening()
                recognizer?.cancel()
            }
        }
    }

    // start recognition launching a new coroutine
    fun startSpeechRecognition(onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // invoke suspend fun to recognize speech
                val text = recognizeSpeech()
                if (text.isNotEmpty()) {
                    // popup showing recognized text is first shown
                    showConfirmationPopup.value = true
                    lastMessage.value = Message(originalContent = text)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun stopSpeechRecognition() {
        recognizer?.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        recognizer?.destroy()
    }
}

// location function to fetch city name
private fun fetchLocation(context: Context, fusedLocationClient: FusedLocationProviderClient, callback: (cityName: String) -> Unit) {
    try {
        // adding listeners for onComplete
        fusedLocationClient.lastLocation.addOnCompleteListener { task ->
            // task is a return object for the onComplete event
            if (task.isSuccessful) {
                val location = task.result
                if (location != null) {
                    Log.d("com.example.macc_app.ChatViewModel",
                        "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                    // gets city name from location data, lat and long
                    val cityName = getCityName(location.latitude, location.longitude, context)
                    if(!cityName.isNullOrEmpty()) {
                        callback(cityName)
                    }
                    Log.d("com.example.macc_app.ChatViewModel", "City: $cityName")
                } else {
                    // location is null for some reason, text will be translated
                    // indicating 'unkown' as location
                    callback("Unknown")
                }
            } else {
                // something has gone wrong during location recognition
                Toast.makeText(context, "Unable to fetch location", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "Permission error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// utility function exploiting geocoder for retrieving city name from lat and long
private fun getCityName(lat: Double,long: Double, context: Context): String?{
    return try {
        // instantiating geocoder with locale for city name
        val geocoder = Geocoder(context, Locale.getDefault())

        // getting one address from data
        val addresses = geocoder.getFromLocation(lat, long, 1) // Get one result
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            // Return the locality for the city name
            return address.locality ?: address.subAdminArea // Use subAdminArea as fallback if locality is null
        } else {
            // No address found
            return null
        }
    } catch (e: IOException) {
        // Handle Geocoder service not available
        e.printStackTrace()
        null
    }
}