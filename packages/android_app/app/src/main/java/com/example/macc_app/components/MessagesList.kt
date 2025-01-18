package com.example.macc_app.components

import Message
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.macc_app.data.remote.Comment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MessagesList(
    modifier: Modifier,
    messages: MutableList<Message>?,
    onLongPressChatBubble: (text: String) -> Unit,
    showExplanation: Boolean,
    showConfirmationPopup: Boolean,
    comments: SnapshotStateList<Comment>?
) {

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier,
        userScrollEnabled = true,
        state = listState
    ) {
        coroutineScope.launch {
            if (!messages.isNullOrEmpty()) {
                delay(250)
                // Animate scroll to the 10th item
                listState.animateScrollToItem(messages.size * 2)
            }
        }
        if (!messages.isNullOrEmpty()) {
            itemsIndexed(messages) { index, message ->
                Column(modifier = Modifier.fillMaxSize()) {
                    val isTranslation = message.translatedContent.value != "..."
                    val city = message.city.value
                    ChatBubble(
                        message,
                        Modifier.align(Alignment.End),
                        translation = false,
                        onLongPress = { selectedText ->
                            onLongPressChatBubble(selectedText)
                        },
                        showExplanation,
                        showConfirmationPopup,
                        city
                    )
                    if (isTranslation) {
                        ChatBubble(
                            message,
                            Modifier.align(Alignment.Start),
                            translation = true,
                            onLongPress = { selectedText ->
                                onLongPressChatBubble(selectedText)
                            },
                            showExplanation,
                            showConfirmationPopup,
                            city
                        )
                    }
                }
            }
        }
    }
    if (!comments.isNullOrEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp) // Fixed height for the comments section
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn {
                item {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        thickness = 1.dp
                    )
                }
                itemsIndexed(comments) { index, comment ->
                    CommentBubble(comment.username, comment.message)
                }
            }
        }
    }
}