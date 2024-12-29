import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.*

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

        if (type == MessageType.TEXT) {
            processTextMessage(message, targetLanguage)
        } else if (type == MessageType.AUDIO) {
            transcribeAudio(message, targetLanguage)
        }
    }

    private fun processTextMessage(message: Message, targetLanguage: String) {
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(message.originalContent)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    Log.e("ChatViewModel", "Language not identified")
                } else {
                    translateText(message, languageCode, targetLanguage)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Language identification failed", e)
            }
    }

    private fun translateText(message: Message, sourceLanguage: String, targetLanguage: String) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLanguage) ?: TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLanguage) ?: TranslateLanguage.ENGLISH)
            .build()

        val translator = Translation.getClient(options)

        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(message.originalContent)
                    .addOnSuccessListener { translatedText ->
                        message.translatedContent.value = translatedText
                        Log.d("ChatViewModel", "Translated text: $translatedText")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatViewModel", "Translation failed", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ChatViewModel", "Model download failed", e)
            }
    }

    private fun transcribeAudio(message: Message, targetLanguage: String) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
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
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.joinToString(separator = " ") ?: "No speech recognized"

                if (text.isNotEmpty()) {
                    message.translatedContent.value = text
                    processTextMessage(message, targetLanguage)
                } else {
                    Log.e("SpeechRecognition", "No recognizable speech")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val audioIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        audioIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        recognizer?.startListening(audioIntent)
    }

    fun startSpeechRecognition(context: Context, onResult: (String) -> Unit, onError: (String) -> Unit) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
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
                onError("Speech recognition error: $error")
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.joinToString(separator = " ") ?: ""
                if(text.isNotEmpty()) {
                    showConfirmationPopup.value = true
                    lastMessage.value = Message(isSender = true, originalContent = text, timestamp = System.currentTimeMillis())
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        recognizer?.startListening(intent)
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
