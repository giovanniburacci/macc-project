package com.example.macc_app.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun RecognizedTextDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    showDialog: Boolean,
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.6f)) // Dim background
            .clickable { onDismiss() } // Dismiss when clicking outside the box
            .animateContentSize()
    ) {
        AnimatedVisibility(
            visible = showDialog,
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
                            if (showDialog) 1f else 0f, animationSpec = tween(durationMillis = 2000),
                            label = ""
                        ).value
                    ) // Smooth fade-in effect
            ) {
                AlertDialog(
                    onDismissRequest = { onDismiss() },
                    title = { Text("How to use 2D") },
                    icon = {
                    },
                    text = {
                        Column {
                            Row (modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "This is the recognized text: ",
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Would you like to translate it?")
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { onConfirm() }) {
                            Text("Translate")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {onDismiss()}) {
                            Text("Discard")
                        }
                    }
                )
            }
        }
    }
}
