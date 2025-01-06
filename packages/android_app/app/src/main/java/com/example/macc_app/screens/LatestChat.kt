package com.example.macc_app.screens

import ChatViewModel
import android.Manifest
import android.graphics.Paint.Align
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.macc_app.SensorView
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.res.painterResource
import com.example.macc_app.R
import com.example.macc_app.components.ChatBubble
import com.example.macc_app.components.ExplanationBox
import com.example.macc_app.components.MessageInput
import com.example.macc_app.components.MessagesList
import com.example.macc_app.components.RecognizedTextDialog
import com.example.macc_app.data.remote.AddChatBody
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatestChat(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val messages = viewModel.messages

    val showConfirmationPopup = viewModel.showConfirmationPopup
    val lastMessage = viewModel.lastMessage
    val isPublic = viewModel.lastChat.value!!.is_public

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

    LaunchedEffect(Unit) {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(Unit) {
        internetPermissionLauncher.launch(Manifest.permission.INTERNET)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val body = AddChatBody(name = "New Chat", is_public = false, user_id = viewModel.lastChat.value!!.user_id)
                                viewModel.createChat(body, true)
                            },
                            modifier = Modifier
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add new chat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text("Current chat")
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            thumbContent = {
                                Icon(
                                    painter = painterResource(if(!isPublic) R.drawable.outline_lock_24 else R.drawable.baseline_lock_open_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )},
                            checked = isPublic,
                            onCheckedChange = {
                                viewModel.updateIsChatPublic(viewModel.lastChat.value!!.id)
                            }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = { showExplanation = true }) {
                            Text("How to use 2D", fontWeight = MaterialTheme.typography.titleMedium.fontWeight)
                        }
                    }

                }
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

        if(showConfirmationPopup.value) {
            RecognizedTextDialog(
                onDismiss = {showConfirmationPopup.value = false; lastMessage.value = null},
                onConfirm = {
                    viewModel.sendMessage(
                        lastMessage.value!!.originalContent,
                        type = MessageType.TEXT,
                        targetLanguage = "it",
                        timestamp = System.currentTimeMillis(),
                        context = context
                    )
                    showConfirmationPopup.value = false
                    lastMessage.value = null

                },
                showDialog = showConfirmationPopup.value,
                text = if(lastMessage.value != null) lastMessage.value!!.originalContent else ""
            )
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues) // Apply padding from Scaffold
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)) {
                MessagesList(modifier = Modifier.
                weight(1f)
                    .padding(16.dp)
                    .fillMaxWidth(),
                    messages,
                    onLongPressChatBubble = {
                            selectedText ->
                        showPopup = true
                        chatBubbleText = selectedText

                    },
                    showExplanation,
                    showConfirmationPopup.value,
                    comments = null
                )
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
                            targetLanguage = "it",
                            timestamp = timestamp,
                            context = context
                        )
                    },
                    onSpeechStart = {
                        viewModel.startSpeechRecognition(
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
                        .padding(8.dp),
                    showExplanation,
                    showConfirmationPopup.value
                )
            }
        }
    }
}
