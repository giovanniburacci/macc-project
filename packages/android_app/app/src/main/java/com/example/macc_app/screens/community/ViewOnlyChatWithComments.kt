package com.example.macc_app.screens.community

import com.example.macc_app.ChatViewModel
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.macc_app.SensorView
import com.example.macc_app.components.CommentInputBar
import com.example.macc_app.components.ExplanationBox
import com.example.macc_app.components.MessagesList
import com.example.macc_app.data.remote.AddCommentBody
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewOnlyChatWithComments(viewModel: ChatViewModel) {

    var showExplanation by remember { mutableStateOf(false) }


    // Define the state variables to track whether the pitch, roll, and yaw are enabled
    var pitchEnabled by remember { mutableStateOf(true) }
    var rollEnabled by remember { mutableStateOf(true) }
    var yawEnabled by remember { mutableStateOf(true) }
    var showPopup by remember { mutableStateOf(false) }
    var chatBubbleText by remember { mutableStateOf("") }

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
    val comments = viewModel.comments

    Log.d("ViewOnlyChatWithComments", "Comments: $comments")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.readOnlyChat.value!!.name) },
                actions = {
                    Button(onClick = { showExplanation = true }) {
                        Text(
                            "How to use 2D",
                            fontWeight = MaterialTheme.typography.titleMedium.fontWeight
                        )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 150.dp)
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
                    comments = viewModel.comments
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
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
            ) {
                CommentInputBar { commentText ->
                    run {
                        val body = AddCommentBody(
                            commentText,
                            FirebaseAuth.getInstance().currentUser!!.uid,
                            viewModel.readOnlyChat.value!!.id
                        )
                        viewModel.addComment(body)
                    }
                }
            }
        }

    }
}