import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

data class Message(
    val isSender: Boolean,
    val type: MessageType,
    val content: String,
    val timestamp: Long,
    private val _textContent: String = "..." // default to content
) {
    var textContent = mutableStateOf(_textContent)
        private set // Only allow internal updates to this property

    // You can also expose a read-only property if needed:
    val text: State<String> get() = textContent
}

enum class MessageType {
    TEXT,
    AUDIO
}