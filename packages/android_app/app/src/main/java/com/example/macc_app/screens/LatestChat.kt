package com.example.macc_app.screens

import com.example.macc_app.components.ChangeNameModal
import com.example.macc_app.ChatViewModel
import com.example.macc_app.MessageType
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.macc_app.R
import com.example.macc_app.SensorView
import com.example.macc_app.components.ExplanationBox
import com.example.macc_app.components.MessageInput
import com.example.macc_app.components.MessagesList
import com.example.macc_app.components.RecognizedTextDialog
import com.example.macc_app.data.remote.AddChatBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatestChat(viewModel: ChatViewModel) {
    val context = LocalContext.current
    val messages = viewModel.messages

    val showConfirmationPopup = viewModel.showConfirmationPopup
    val lastMessage = viewModel.lastMessage
    val isPublic = viewModel.lastChat.value?.is_public
    val chatId = viewModel.lastChat.value?.id
    val isDownloadingModel = viewModel.isDownloadingModel.value

    var showPopup by remember { mutableStateOf(false) }
    var chatBubbleText by remember { mutableStateOf("") }

    var showExplanation by remember { mutableStateOf(false) }

    // Define the state variables to track whether the pitch, roll, and yaw are enabled
    var pitchEnabled by remember { mutableStateOf(true) }
    var rollEnabled by remember { mutableStateOf(true) }
    var yawEnabled by remember { mutableStateOf(true) }

    var showModal by remember { mutableStateOf(false) }

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

    if(viewModel.lastChat.value == null) {
        // Show loader while content is loading
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val body = AddChatBody(
                                        name = "New Chat",
                                        is_public = false,
                                        user_id = viewModel.lastChat.value!!.user_id
                                    )
                                    viewModel.createChat(body, true)
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add new chat",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(viewModel.lastChat.value!!.name)
                            IconButton(
                                onClick = { showModal = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit chat name",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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
                                        painter = painterResource(if (isPublic !== null && !isPublic) R.drawable.outline_lock_24 else R.drawable.baseline_lock_open_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                                checked = if (isPublic !== null) isPublic else false,
                                onCheckedChange = {
                                    viewModel.updateIsChatPublic(viewModel.lastChat.value!!.id)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { showExplanation = true }) {
                                Text(
                                    "How to use 2D",
                                    fontWeight = MaterialTheme.typography.titleMedium.fontWeight
                                )
                            }
                        }

                    }
                )
            }
        ) { paddingValues ->

            if (showExplanation) {
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

            if (showModal) {
                ChangeNameModal(
                    currentName = viewModel.lastChat.value!!.name,
                    onDismiss = { showModal = false },
                    onSave = { newName ->
                        Log.d(
                            "ChatHistory",
                            "Saving new name: $newName for chat ${viewModel.lastChat.value!!.id}"
                        )
                        if (chatId !== null) {
                            viewModel.updateChatName(chatId, newName)
                        }
                        showModal = false
                    }
                )
            }

            if (showConfirmationPopup.value) {
                RecognizedTextDialog(
                    onDismiss = { showConfirmationPopup.value = false; lastMessage.value = null },
                    onConfirm = {
                        viewModel.sendMessage(
                            lastMessage.value!!.originalContent,
                            type = MessageType.TEXT,
                            context = context
                        )
                        showConfirmationPopup.value = false
                        lastMessage.value = null

                    },
                    showDialog = showConfirmationPopup.value,
                    text = if (lastMessage.value != null) lastMessage.value!!.originalContent else ""
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Apply padding from Scaffold
            ) {
                if(isDownloadingModel) {
                    Column(modifier = Modifier.align(Alignment.Center).zIndex(4f)) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally).zIndex(5f)
                        )
                        Text(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp).zIndex(5f), text = "Downloading model...")

                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp)
                ) {
                    MessagesList(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        messages,
                        onLongPressChatBubble = { selectedText ->
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
                                        SensorView(
                                            context,
                                            chatBubbleText,
                                            pitchEnabled,
                                            rollEnabled,
                                            yawEnabled
                                        )
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
                                context = context
                            )
                        },
                        onSpeechStart = {
                            viewModel.startSpeechRecognition(
                                onError = { error ->
                                    if (error == "7") {
                                        Toast.makeText(
                                            context,
                                            "No text was recognized, please try again",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
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
}
