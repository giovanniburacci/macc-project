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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient

class ChatViewModel : ViewModel() {

    private val _messages = mutableStateListOf<Message>()
    val messages: List<Message> get() = _messages

    val showConfirmationPopup = mutableStateOf(false)
    val lastMessage = mutableStateOf<Message?>(null)

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

    fun sendMessage(content: String, type: MessageType, targetLanguage: String, timestamp: Long, context: Context) {
        val message = Message(isSender = true, originalContent = content, timestamp = timestamp)
        _messages.add(message)

        viewModelScope.launch {
            if (type == MessageType.TEXT) {
                processTextMessage(message, targetLanguage)
            } else if (type == MessageType.AUDIO) {
                transcribeAudio(message, targetLanguage)
            }
        }

        obtainPosition(context)
    }

    private fun processTextMessage(message: Message, targetLanguage: String) {
        viewModelScope.launch {
            try {
                val languageCode = identifyLanguage(message.originalContent)
                if (languageCode != "und") {
                    translateText(message, languageCode, targetLanguage)
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

    private suspend fun translateText(message: Message, sourceLanguage: String, targetLanguage: String) {
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
                    Log.d("ChatViewModel", "Translated text: $translatedText")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Translation failed", e)
            }
        }
    }

    private fun transcribeAudio(message: Message, targetLanguage: String) {
        viewModelScope.launch {
            try {
                val text = recognizeSpeech()
                if (text.isNotEmpty()) {
                    message.translatedContent.value = text
                    processTextMessage(message, targetLanguage)
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
                    lastMessage.value = Message(isSender = true, originalContent = text, timestamp = System.currentTimeMillis())
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

fun obtainPosition(context: Context) {
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
        fetchLocation(context, fusedLocationClient)
    }
}

private fun fetchLocation(context: Context, fusedLocationClient: FusedLocationProviderClient) {
    try {
        fusedLocationClient.lastLocation.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val location = task.result
                if (location != null) {

                    Log.d("ChatViewModel", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                    Toast.makeText(context, "Lat: ${location.latitude}, Lng: ${location.longitude}", Toast.LENGTH_LONG).show()
                    val cityName = getCityName(location.latitude, location.longitude, context)
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