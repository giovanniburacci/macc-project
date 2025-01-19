package com.example.macc_app.screens.history

import com.example.macc_app.components.ChangeNameModal
import com.example.macc_app.ChatViewModel
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.macc_app.R
import com.example.macc_app.SensorView
import com.example.macc_app.components.ExplanationBox
import com.example.macc_app.components.MessagesList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryViewOnlyChat(viewModel: ChatViewModel) {

    var showExplanation by remember { mutableStateOf(false) }

    // Define the state variables to track whether the pitch, roll, and yaw are enabled
    var pitchEnabled by remember { mutableStateOf(true) }
    var rollEnabled by remember { mutableStateOf(true) }
    var yawEnabled by remember { mutableStateOf(true) }
    var showPopup by remember { mutableStateOf(false) }
    var chatBubbleText by remember { mutableStateOf("") }

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

    val showConfirmationPopup = viewModel.showConfirmationPopup

    val messages = viewModel.readOnlyMessages
    val chat = viewModel.readOnlyChat.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(viewModel.readOnlyChat.value!!.name)
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
                }, // Set the title
                actions = {
                    Row(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            thumbContent = {
                                if (chat != null) {
                                    Icon(
                                        painter = painterResource(if (!chat.is_public) R.drawable.outline_lock_24 else R.drawable.baseline_lock_open_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                }
                            },
                            checked = chat?.is_public ?: false,
                            onCheckedChange = {
                                viewModel.updateIsChatPublic(viewModel.readOnlyChat.value!!.id)
                            }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = { showExplanation = true }) {
                            Text(
                                "How to use 2D",
                                fontWeight = MaterialTheme.typography.titleMedium.fontWeight
                            )
                        }
                    }

                })
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
                currentName = viewModel.readOnlyChat.value!!.name,
                onDismiss = { showModal = false },
                onSave = { newName ->
                    Log.d(
                        "ChatHistory",
                        "Saving new name: $newName for chat ${viewModel.readOnlyChat.value!!.id}"
                    )
                    viewModel.updateChatName(viewModel.readOnlyChat.value!!.id, newName)
                    showModal = false
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
        ) {
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
            }
        }
    }
}