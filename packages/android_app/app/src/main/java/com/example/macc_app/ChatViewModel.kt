import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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
import android.app.Activity
import android.location.Geocoder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.example.macc_app.data.remote.PythonAnywhereFactorAPI
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.macc_app.data.remote.AddChatBody
import com.example.macc_app.data.remote.AddChatMessage
import com.example.macc_app.data.remote.AddUserBody
import com.example.macc_app.data.remote.ChatResponse
import com.example.macc_app.data.remote.MessageResponse
import retrofit2.Retrofit

class ChatViewModelFactory(private val retrofit: Retrofit) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(retrofit) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
class ChatViewModel(private val retrofit: Retrofit): ViewModel() {

    private val myApiService: PythonAnywhereFactorAPI = retrofit.create(PythonAnywhereFactorAPI::class.java)

    val messages = mutableStateListOf<Message>()

    val history = mutableStateListOf<ChatResponse>()

    val showConfirmationPopup = mutableStateOf(false)
    val lastMessage = mutableStateOf<Message?>(null)

    val lastChat = mutableStateOf<ChatResponse?>(null)


    private var textToSpeech: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null

    fun initializeSpeechComponents(context: Context) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
            }
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    }

    fun mapMessageResponseListToMessageList(messageResponses: List<MessageResponse>): List<Message> {
        return messageResponses.map { mapMessageResponseToMessage(it) }
    }

    private fun mapMessageResponseToMessage(messageResponse: MessageResponse): Message {
        val mess = Message(
            originalContent = messageResponse.message
        )
        mess.translatedContent.value = messageResponse.translation
        return mess
    }

    fun fetchHistory(uid: String) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Fetch history with uid: $uid")
                val response = myApiService.fetchHistory(uid)
                history.clear()
                history.addAll(response)

                Log.d("ChatViewModel", "Response from history API: $response")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error fetching chats history", e)
            }
        }
    }

    fun fetchMessages(chatId: Long) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Fetch messages with chatId: $chatId")
                val response = myApiService.fetchMessages(chatId)
                messages.clear()
                messages.addAll(mapMessageResponseListToMessageList(response).toMutableList())

                Log.d("ChatViewModel", "Response from API: $response")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error fetching messages", e)
            }
        }
    }

    fun fetchLastChat(uid: String) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Uid: $uid")
                val response = myApiService.getLastChatFromUser(uid)
                lastChat.value = response
                fetchMessages(lastChat.value!!.id)
                Log.d("ChatViewModel", "Response from API: $response")
            } catch (e: Exception) {
                if(uid.isNotEmpty()) {
                    val body = AddChatBody(name = "New Chat", is_public = false, user_id = uid)
                    createChat(body)
                }
                Log.e("ChatViewModel", "Error fetching last chat", e)
            }
        }
    }

    fun createChat(body: AddChatBody) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Uid: $body")
                val response = myApiService.addChat(body)
                lastChat.value = response
                Log.d("ChatViewModel", "Response from addChat API: $response")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error creating chat", e)
            }
        }
    }

    fun addMessage(body: AddChatMessage) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Uid: $body")
                val response = myApiService.addMessage(body)
                Log.d("ChatViewModel", "Response from addMessage API: $response")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error adding message", e)
            }
        }
    }

    fun createUser(body: AddUserBody) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Uid: $body")
                val response = myApiService.addUser(body)
                Log.d("ChatViewModel", "Response from API: $response")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error creating user", e)
            }
        }
    }

    fun sendMessage(content: String, type: MessageType, targetLanguage: String, timestamp: Long, context: Context) {
        val message = Message(originalContent = content)
        messages.add(message)
        viewModelScope.launch {
            if (type == MessageType.TEXT) {
                obtainPosition(context, { cityName -> processTextMessage(message, targetLanguage, cityName)})
            } else if (type == MessageType.AUDIO) {
                obtainPosition(context, {cityName ->  transcribeAudio(message, targetLanguage, cityName)})
            }
        }
    }

    private fun processTextMessage(message: Message, targetLanguage: String, cityName: String) {
        viewModelScope.launch {
            try {
                val languageCode = identifyLanguage(message.originalContent)
                if (languageCode != "und") {
                    translateText(message, languageCode, targetLanguage, cityName)
                } else {
                    Log.e("ChatViewModel", "Language not identified")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Language identification failed", e)
            }
        }
    }

    private suspend fun identifyLanguage(text: String): String = withContext(Dispatchers.IO) {
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

    private suspend fun translateText(message: Message, sourceLanguage: String, targetLanguage: String, cityName: String) {
        withContext(Dispatchers.IO) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLanguage) ?: TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLanguage) ?: TranslateLanguage.ENGLISH)
                .build()

            val translator = Translation.getClient(options)

            try {
                translator.downloadModelIfNeeded().await()
                val translatedText = translator.translate(message.originalContent).await()
                withContext(Dispatchers.Main) {
                    message.translatedContent.value = translatedText
                    val body = AddChatMessage(message = message.originalContent, translation = translatedText, city = cityName, chat_id = lastChat.value!!.id)
                    addMessage(body)
                    Log.d("ChatViewModel", "Translated text: $translatedText")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Translation failed", e)
            }
        }
    }

    private fun transcribeAudio(message: Message, targetLanguage: String, cityName: String) {
        viewModelScope.launch {
            try {
                val text = recognizeSpeech()
                if (text.isNotEmpty()) {
                    message.translatedContent.value = text
                    processTextMessage(message, targetLanguage, cityName)
                } else {
                    Log.e("SpeechRecognition", "No recognizable speech")
                }
            } catch (e: Exception) {
                Log.e("SpeechRecognition", "Error during speech recognition", e)
            }
        }
    }

    private suspend fun recognizeSpeech(): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            var isContinuationResumed = false // Flag to ensure single resumption

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
                        isContinuationResumed = true
                        Log.e("SpeechRecognition", "Error occurred: $error")
                        continuation.resumeWithException(RuntimeException("Speech recognition error: $error"))
                    } else {
                        Log.w("SpeechRecognition", "onError called after continuation already resumed.")
                    }
                }

                override fun onResults(results: Bundle?) {
                    if (!isContinuationResumed) {
                        isContinuationResumed = true
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.joinToString(separator = " ") ?: ""
                        continuation.resume(text)
                    } else {
                        Log.w("SpeechRecognition", "onResults called after continuation already resumed.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }

            recognizer?.setRecognitionListener(listener)

            val audioIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
            recognizer?.startListening(audioIntent)

            continuation.invokeOnCancellation {
                recognizer?.stopListening()
                recognizer?.cancel()
            }
        }
    }

    fun startSpeechRecognition(onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val text = recognizeSpeech()
                if (text.isNotEmpty()) {
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
        textToSpeech?.shutdown()
        recognizer?.destroy()
    }
}

fun obtainPosition(context: Context, callback: (cityName: String) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            context as Activity, // Replace `this` with your Activity reference
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1001
        )
    } else {
        // Permission already granted, fetch location
        fetchLocation(context, fusedLocationClient, callback)
    }
}

private fun fetchLocation(context: Context, fusedLocationClient: FusedLocationProviderClient, callback: (cityName: String) -> Unit) {
    try {
        fusedLocationClient.lastLocation.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val location = task.result
                if (location != null) {

                    Log.d("ChatViewModel", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                    Toast.makeText(context, "Lat: ${location.latitude}, Lng: ${location.longitude}", Toast.LENGTH_LONG).show()
                    val cityName = getCityName(location.latitude, location.longitude, context)
                    if(!cityName.isNullOrEmpty()) {
                        callback(cityName)
                    }
                    Log.d("ChatViewModel", "City: $cityName")
                    Toast.makeText(context, "City: $cityName", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Location is null. Try again later.", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.d("Screen2", "No Location")
                Toast.makeText(context, "Unable to fetch location", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "Permission error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun getCityName(lat: Double,long: Double, context: Context): String?{
    var cityName: String?
    val geoCoder = Geocoder(context, Locale.getDefault())
    val address = geoCoder.getFromLocation(lat,long,1)
    cityName = address?.get(0)?.adminArea
    if (cityName == null){
        cityName = address?.get(0)?.locality
        if (cityName == null){
            cityName = address?.get(0)?.subAdminArea
        }
    }
    return cityName
}