package com.example.macc_app.components

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService


@Composable
fun MessageInput(
    onTextSend: (String, Long) -> Unit,
    onSpeechStart: () -> Unit,
    onSpeechStop: () -> Unit,
    modifier: Modifier = Modifier,
    showExplanation: Boolean,
    showConfirmationPopup: Boolean
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var isRecording by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val backgroundColor = MaterialTheme.colorScheme.surface
    val inputFieldColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val sendIconColor = MaterialTheme.colorScheme.primary
    val micIconColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val micBackgroundColor = if (isRecording) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else inputFieldColor

    Row(
        modifier = modifier
            .padding(8.dp)
            .height(56.dp)
    ) {
        androidx.compose.material3.TextField(
            value = text.text,
            onValueChange = { text = TextFieldValue(it) },
            modifier = Modifier
                .weight(1f)
                .background(
                    if (!showExplanation && !showConfirmationPopup) backgroundColor else inputFieldColor,
                    RoundedCornerShape(12.dp)
                ),
            placeholder = { Text("Type a message...") },
            enabled = (!showExplanation && !showConfirmationPopup),
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = {
                if (text.text.isNotBlank()) {
                    onTextSend(text.text, System.currentTimeMillis())
                    text = TextFieldValue("") // Clear input field
                    focusManager.clearFocus()

                }
            },
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .alpha(if (showExplanation || showConfirmationPopup) 0.6f else 1.0f),
            enabled = (!showExplanation && !showConfirmationPopup)
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "Send message",
                tint = sendIconColor
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
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
                .background(micBackgroundColor, CircleShape)
                .padding(12.dp)
                .alpha(if (showExplanation || showConfirmationPopup) 0.6f else 1.0f),
            enabled = (!showExplanation && !showConfirmationPopup)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Phone else Icons.Outlined.Phone,
                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                tint = micIconColor
            )
        }

        if (showConfirmationPopup) {
            isRecording = false
        }
    }
}
