package com.example.macc_app.components

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.macc_app.R
import kotlinx.coroutines.delay

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
        delay(200)
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
