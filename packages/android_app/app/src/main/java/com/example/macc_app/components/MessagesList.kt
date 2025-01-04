package com.example.macc_app.components

import Message
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MessagesList(modifier: Modifier, messages: List<Message>, onLongPressChatBubble: (text: String) -> Unit, showExplanation: Boolean, showConfirmationPopup: Boolean) {

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

        LazyColumn(
            modifier = modifier,
            userScrollEnabled = true,
            state = listState
        ) {
            coroutineScope.launch {
                delay(250)
                // Animate scroll to the 10th item
                listState.animateScrollToItem(messages.size * 2)
            }
            itemsIndexed(messages) { index, message ->
                Column(modifier = Modifier.fillMaxSize()) {
                    val isTranslation = message.translatedContent.value != "..."
                    ChatBubble(
                        message,
                        Modifier.align(Alignment.Start),
                        translation = false,
                        onLongPress = { selectedText ->
                            onLongPressChatBubble(selectedText)
                        },
                        showExplanation,
                        showConfirmationPopup
                    )
                    if (isTranslation) {
                        ChatBubble(
                            message,
                            Modifier.align(Alignment.End),
                            translation = true,
                            onLongPress = { selectedText ->
                                onLongPressChatBubble(selectedText)
                            },
                            showExplanation,
                            showConfirmationPopup
                        )
                    }
                }

            }
        }
    }