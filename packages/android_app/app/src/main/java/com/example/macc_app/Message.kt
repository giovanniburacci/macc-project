import androidx.compose.runtime.mutableStateOf

data class Message(
    val originalContent: String,
    private val _translatedContent: String = "...", // default to content
    private val _city: String = "Unknown"
) {
    var translatedContent = mutableStateOf(_translatedContent)
        private set

    var city = mutableStateOf(_city)
        private set

}

enum class MessageType {
    TEXT,
    AUDIO
}