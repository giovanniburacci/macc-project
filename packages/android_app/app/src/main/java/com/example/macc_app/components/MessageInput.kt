package com.example.macc_app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp


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
                .background(if(!showExplanation && !showConfirmationPopup) Color.White else Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            enabled = (!showExplanation && !showConfirmationPopup)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = {
                if (text.text.isNotBlank()) {
                    onTextSend(text.text, System.currentTimeMillis())
                    text = TextFieldValue("") // Clear input field
                }
            },
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .alpha(if(showExplanation || showConfirmationPopup) 0.6f else 1.0f),
            enabled = (!showExplanation && !showConfirmationPopup)
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
                .alpha(if(showExplanation || showConfirmationPopup) 0.6f else 1.0f),
            enabled = (!showExplanation && !showConfirmationPopup)
        ) {
            Text(if (isRecording) "Stop" else "Talk")
        }

        if(showConfirmationPopup) { isRecording = false }
    }
}