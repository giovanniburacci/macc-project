import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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

    fun sendMessage(content: String, type: MessageType, targetLanguage: String, timestamp: Long) {
        val message = Message(isSender = true, originalContent = content, timestamp = timestamp)
        _messages.add(message)

        viewModelScope.launch {
            if (type == MessageType.TEXT) {
                processTextMessage(message, targetLanguage)
            } else if (type == MessageType.AUDIO) {
                transcribeAudio(message, targetLanguage)
            }
        }
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
                    Log.e("SpeechRecognition", "Error occurred: $error")
                    continuation.resumeWithException(RuntimeException("Speech recognition error: $error"))
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.joinToString(separator = " ") ?: ""
                    continuation.resume(text)
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
