import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.*

class ChatViewModel : ViewModel() {

    private val _messages = mutableStateListOf<Message>()
    val messages: List<Message> get() = _messages

    private var textToSpeech: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    fun initializeTextToSpeech(context: Context) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
            }
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    }

    fun sendMessage(content: String, type: MessageType, context: Context, targetLanguage: String, timestamp: Long) {
        val message = Message(isSender = true, type = type, content = content, timestamp = timestamp)
        _messages.add(message)

        if (type == MessageType.TEXT) {
            processTextMessage(message, targetLanguage)
        } else if (type == MessageType.AUDIO) {
            transcribeAudio(content, message, context, targetLanguage)
        }
    }

    private fun processTextMessage(message: Message, targetLanguage: String) {
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(message.content)
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

        // Download the translation model if needed
        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                // Perform translation
                translator.translate(message.content)
                    .addOnSuccessListener { translatedText ->
                        message.textContent.value = translatedText
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

    private fun transcribeAudio(audioPath: String, message: Message, context: Context, targetLanguage: String) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognition", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("SpeechRecognition", "Speech beginning")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // This is called when the sound level in the room changes.
                // You can leave this empty if not needed.
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Called when more sound is received. You can leave this empty if not needed.
            }

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
                    message.textContent.value = text
                    processTextMessage(message, targetLanguage)
                } else {
                    Log.e("SpeechRecognition", "No recognizable speech")
                }
            }


            override fun onPartialResults(partialResults: Bundle?) {
                // Handle partial recognition results if needed
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Handle specific events if needed
            }
        })

        val audioUri = Uri.parse(audioPath)
        val audioIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        audioIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        this.recognizer?.startListening(audioIntent)
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.shutdown()
    }
}
