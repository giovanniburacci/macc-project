package com.example.macc_app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CommentInputBar(
    onCommentPosted: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Text Input
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text("Write a comment...") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            // Publish Button
            Button(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onCommentPosted(commentText)
                        commentText = "" // Clear the input field
                    }
                },
                enabled = commentText.isNotBlank()
            ) {
                Text("Post")
            }
        }
    }
}
