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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.macc_app.R
import com.example.macc_app.SensorView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.CircularProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen2(viewModel: ChatViewModel = viewModel()) {
    val context = LocalContext.current
    val messages = viewModel.messages

    var showPopup by remember { mutableStateOf(false) }
    var chatBubbleText by remember { mutableStateOf("") }

    var showExplanation by remember { mutableStateOf(false) }

    // Define the state variables to track whether the pitch, roll, and yaw are enabled
    var pitchEnabled by remember { mutableStateOf(true) }
    var rollEnabled by remember { mutableStateOf(true) }
    var yawEnabled by remember { mutableStateOf(true) }

    // Functions to handle the toggle actions
    val onPitchToggle: (Boolean) -> Unit = { isChecked ->
        pitchEnabled = isChecked
    }
    val onRollToggle: (Boolean) -> Unit = { isChecked ->
        rollEnabled = isChecked
    }
    val onYawToggle: (Boolean) -> Unit = { isChecked ->
        yawEnabled = isChecked
    }

    LaunchedEffect(Unit) {
        viewModel.initializeSpeechComponents(context)
    }


    // Request permissions
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    // Request permissions
    val internetPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Internet permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    // Request permissions
    val audioWriteFilePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Write file permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(Unit) {
        internetPermissionLauncher.launch(Manifest.permission.INTERNET)
    }

    LaunchedEffect(Unit) {
        audioWriteFilePermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Current chat") }, // Set the title
                actions = { Button(onClick = { showExplanation = true }) { Text("How to use 2D", fontWeight = MaterialTheme.typography.titleMedium.fontWeight) }}
            )
        }
    ) { paddingValues ->

        if(showExplanation) {
            ExplanationBox(
                onDismiss = { showExplanation = false },
                pitchEnabled = pitchEnabled,
                onPitchToggle = onPitchToggle,
                rollEnabled = rollEnabled,
                onRollToggle = onRollToggle,
                yawEnabled = yawEnabled,
                onYawToggle = onYawToggle
            )
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues) // Apply padding from Scaffold
        ) {
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
                            ChatBubble(
                                message,
                                Modifier.align(Alignment.Start),
                                translation = false,
                                onLongPress = { selectedText ->
                                    showPopup = true
                                    chatBubbleText = selectedText
                                }
                            )
                            if (isTranslation) {
                                ChatBubble(
                                    message,
                                    Modifier.align(Alignment.End),
                                    translation = true,
                                    onLongPress = { selectedText ->
                                        showPopup = true
                                        chatBubbleText = selectedText
                                    }
                                )
                            }
                        }

                    }
                }

                if (showPopup) {
                    AlertDialog(
                        onDismissRequest = { showPopup = false },
                        title = { Text("2D text view") },
                        text = {
                            AndroidView(
                                factory = { context ->
                                    SensorView(context, chatBubbleText, pitchEnabled, rollEnabled, yawEnabled)
                                }
                            )
                        },
                        confirmButton = {
                            Button(onClick = { showPopup = false }) {
                                Text("OK")
                            }
                        }
                    )
                }

                // Input Section
                MessageInput(
                    onTextSend = { text, timestamp ->
                        viewModel.sendMessage(
                            text,
                            type = MessageType.TEXT,
                            context = context,
                            targetLanguage = "it",
                            timestamp = timestamp
                        )
                    },
                    onSpeechStart = {
                        viewModel.startSpeechRecognition(
                            context,
                            onResult = { recognizedText ->
                                viewModel.sendMessage(
                                    recognizedText,
                                    type = MessageType.TEXT,
                                    context = context,
                                    targetLanguage = "it",
                                    timestamp = System.currentTimeMillis()
                                )
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
fun ChatBubble(message: Message, modifier: Modifier = Modifier, translation: Boolean, onLongPress: (String) -> Unit) {
    val bubbleColor = if (translation) Color(0xFFD1E8E2) else Color(0xFFACE0F9)
    val cornerRadius = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .background(bubbleColor, cornerRadius)
            .padding(12.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongPress(if(!translation) message.content else message.textContent.value) // Trigger the lambda when long-pressed
                    }
                )
            }
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

@Composable
fun ExplanationBox(
    onDismiss: () -> Unit,
    pitchEnabled: Boolean,
    onPitchToggle: (Boolean) -> Unit,
    rollEnabled: Boolean,
    onRollToggle: (Boolean) -> Unit,
    yawEnabled: Boolean,
    onYawToggle: (Boolean) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Use LaunchedEffect to control the visibility
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        isVisible = true
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.6f)) // Dim background
            .clickable { onDismiss() } // Dismiss when clicking outside the box
            .animateContentSize()
    ) {
        if (isLoading) {
            // Show loader while content is loading
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(durationMillis = 500)), // Fade-in animation
            exit = fadeOut(tween(durationMillis = 500))  // Fade-out if dismissed
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .background(color = Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp) // Padding inside the box
                    .alpha(
                        animateFloatAsState(
                            if (isVisible) 1f else 0f, animationSpec = tween(durationMillis = 2000),
                            label = ""
                        ).value
                    ) // Smooth fade-in effect
            ) {
                AlertDialog(
                    onDismissRequest = { onDismiss() },
                    title = { Text("How to use 2D") },
                    icon = {
                        AsyncImage(
                            model = R.drawable.rotate_mobile,
                            contentDescription = "Rotation Movement",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Long press on a message to explore it in 2D space. Tilt or rotate your device to move the message, following your hand's movements.",
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            // Toggle for Pitch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Enable Pitch", modifier = Modifier.weight(1f))
                                Switch(
                                    checked = pitchEnabled,
                                    onCheckedChange = onPitchToggle
                                )
                            }
                            // Toggle for Roll
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Enable Roll", modifier = Modifier.weight(1f))
                                Switch(
                                    checked = rollEnabled,
                                    onCheckedChange = onRollToggle
                                )
                            }
                            // Toggle for Yaw
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Enable Yaw", modifier = Modifier.weight(1f))
                                Switch(
                                    checked = yawEnabled,
                                    onCheckedChange = onYawToggle
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { onDismiss() }) {
                            Text("Got it")
                        }
                    },
                )
            }
        }
    }
}

