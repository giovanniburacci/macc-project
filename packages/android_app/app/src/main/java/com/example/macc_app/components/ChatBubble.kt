package com.example.macc_app.components

import com.example.macc_app.Message
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatBubble(message: Message, modifier: Modifier = Modifier, translation: Boolean, onLongPress: (String) -> Unit, showExplanation: Boolean, showConfirmationPopup: Boolean, city: String) {
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
        Log.d("ChatBubble", "Original content: $message")
        if(!translation) {
            Column {
                // Main Text
                Text(
                    text = message.originalContent,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 4.dp) // Space between the main text and city
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.Start) // Align to the start of the column
                ) {
                    // City Text
                    if (city != "Unknown") {
                        // Small Icon for location
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Location Icon",
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )

                        Text(
                            text = city,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        else {
            Text(text = message.translatedContent.value, fontSize = 16.sp)
        }
    }
}