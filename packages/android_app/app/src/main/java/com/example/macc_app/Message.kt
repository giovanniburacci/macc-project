data class Message(
    val content: String, // Text content or audio file path
    val isAudio: Boolean, // True if the message is an audio file, false for text
    val timestamp: Long
)
