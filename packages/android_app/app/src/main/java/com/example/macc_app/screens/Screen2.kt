package com.example.macc_app.screens

import ChatViewModel
import Message
import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun Screen2(viewModel: ChatViewModel = viewModel()) {
    val context = LocalContext.current
    val messages = viewModel.messages

    LaunchedEffect(Unit) {
        viewModel.initializeSpeechComponents(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
                .fillMaxWidth(),
            userScrollEnabled = true
        ) {
            itemsIndexed(messages) { index, message ->
                Column(modifier = Modifier.fillMaxSize()) {
                    val isTranslation = message.textContent.value != "..."
                    ChatBubble(message, Modifier.align(Alignment.Start), translation = false)
                    if (isTranslation) {
                        ChatBubble(message, Modifier.align(Alignment.End), translation = true)
                    }
                }
            }
        }

        // Input Section
        MessageInput(
            onTextSend = { text, timestamp ->
                viewModel.sendMessage(text, type = MessageType.TEXT, context = context, targetLanguage = "it", timestamp = timestamp)
            },
            onSpeechStart = {
                viewModel.startSpeechRecognition(
                    context,
                    onResult = { recognizedText ->
                        viewModel.sendMessage(recognizedText, type = MessageType.TEXT, context = context, targetLanguage = "it", timestamp = System.currentTimeMillis())
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onSpeechStop = {
                viewModel.stopSpeechRecognition()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}


fun findFileFromTimestamp(context: Context, timestamp: Long): File? {
    val musicDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath)
    if (musicDir.exists()) {
        val files = musicDir.listFiles()
        if (files.isNullOrEmpty()) {
            Log.d("AudioDebug", "No files found in the directory.")
        } else {
            files.forEach { file ->
                Log.d("AudioDebug", "File found: ${file.absolutePath}")
                if(file.absolutePath.contains(timestamp.toString())) {
                    return file
                } else {
                    Log.d("AudioDebug", "Not this audio.")

                }
            }
        }
    } else {
        Log.d("AudioDebug", "Directory does not exist.")
    }
    return null
}

@Composable
fun ChatBubble(message: Message, modifier: Modifier = Modifier, translation: Boolean) {
    val bubbleColor = if (translation) Color(0xFFD1E8E2) else Color(0xFFACE0F9)
    val cornerRadius = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .background(bubbleColor, cornerRadius)
            .padding(12.dp)
    ) {
        if(!translation) {
            if (message.type === MessageType.AUDIO) {
                AudioPlayer(uri = message.content, timestamp = message.timestamp)
            } else {
                Text(text = message.content, fontSize = 16.sp)
            }
        }
        else {
            Text(text = message.textContent.value, fontSize = 16.sp)
        }

    }
}

@Composable
fun AudioPlayer(uri: String, timestamp: Long) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember { MediaPlayer() }

    Button(onClick = {
        if (isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.reset()
            isPlaying = false
        } else {
            val audioFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath
            )
            if(audioFile.exists()) {
                val getFile = findFileFromTimestamp(context, timestamp) // Debugging files
                if(getFile?.exists() == true) {
                    mediaPlayer.setDataSource(getFile.absolutePath)
                    mediaPlayer.prepare()
                    mediaPlayer.setOnCompletionListener {
                        mediaPlayer.stop()
                        mediaPlayer.reset()
                        isPlaying = false
                    }
                    mediaPlayer.start()
                    isPlaying = true
                } else {
                    Toast.makeText(context, "Failed to play audio again: ${audioFile.absolutePath}", Toast.LENGTH_LONG).show()

                }
            } else {
                Toast.makeText(context, "Failed to play audio: ${audioFile.absolutePath}", Toast.LENGTH_LONG).show()
            }

        }
    }) {
        Text(if (isPlaying) "Stop" else "Play")
    }
}

@Composable
fun MessageInput(
    onTextSend: (String, Long) -> Unit,
    onSpeechStart: () -> Unit,
    onSpeechStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var isRecording by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .padding(8.dp)
            .height(56.dp)
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = {
                if (text.text.isNotBlank()) {
                    onTextSend(text.text, System.currentTimeMillis())
                    text = TextFieldValue("") // Clear input field
                }
            },
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Text("Send")
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = {
                if (!isRecording) {
                    onSpeechStart()
                } else {
                    onSpeechStop()
                }
                isRecording = !isRecording
            },
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .background(if (isRecording) Color.Red else Color.Gray, CircleShape)
                .padding(12.dp)
        ) {
            Text(if (isRecording) "Stop" else "Talk")
        }
    }
}


