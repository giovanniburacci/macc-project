package com.example.macc_app.components

import Message
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun ChatBubble(message: Message, modifier: Modifier = Modifier, translation: Boolean, onLongPress: (String) -> Unit, showExplanation: Boolean, showConfirmationPopup: Boolean) {
    var bubbleColor = if (translation) Color(0xFFD1E8E2) else Color(0xFFACE0F9)
    bubbleColor = if(!showExplanation && !showConfirmationPopup) bubbleColor else bubbleColor.copy(alpha = 0.6f)
    val cornerRadius = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .background(bubbleColor, cornerRadius)
            .padding(12.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongPress(if(!translation) message.originalContent else message.translatedContent.value) // Trigger the lambda when long-pressed
                    }
                )
            }
    ) {
        if(!translation) {
            Text(text = message.originalContent, fontSize = 16.sp)
        }
        else {
            Text(text = message.translatedContent.value, fontSize = 16.sp)
        }

    }
}