import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ChatViewModel : ViewModel() {
    // StateFlow to hold the list of messages
    private val _chatState = MutableStateFlow(ChatState(messages = emptyList()))
    val chatState: StateFlow<ChatState> get() = _chatState

    // Function to send a new message
    fun sendMessage(content: String, isAudio: Boolean, timestamp: Long) {
        _chatState.update { currentState ->
            val newMessage = Message(content = content, isAudio = isAudio, timestamp = timestamp)
            currentState.copy(messages = currentState.messages + newMessage)
        }
    }
}

// State class to hold the chat's UI state
data class ChatState(
    val messages: List<Message> // List of all messages
)
