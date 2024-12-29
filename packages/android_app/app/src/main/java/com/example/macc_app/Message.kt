import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

data class Message(
    val isSender: Boolean,
    val originalContent: String,
    val timestamp: Long,
    private val _translatedContent: String = "..." // default to content
) {
    var translatedContent = mutableStateOf(_translatedContent)
        private set

}

enum class MessageType {
    TEXT,
    AUDIO
}