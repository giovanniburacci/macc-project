package com.example.macc_app.screens.history

import com.example.macc_app.ChatViewModel
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.macc_app.R
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistory(navController: NavController, viewModel: ChatViewModel) {
    val auth = FirebaseAuth.getInstance()
    viewModel.fetchHistory(auth.currentUser!!.uid)

    val history = viewModel.history

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Your History") })
        }
    ) { padding ->

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
            ) {
                itemsIndexed(history) { index, chat -> // Replace 20 with your data list size
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setReadOnlyChat(chat)
                                viewModel.fetchMessages(chat.id)
                                navController.navigate("chatHistory/${chat.id}") // Pass the cardId
                            },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = chat.name, style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.weight(1f))
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = { Text(if (!chat.is_public) "Private" else "Public") },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(if (!chat.is_public) R.drawable.outline_lock_24 else R.drawable.baseline_lock_open_24),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        disabledContainerColor = if (!chat.is_public) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                        disabledLabelColor = if (!chat.is_public) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                        disabledLeadingIconContentColor = if (!chat.is_public) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                )
                            }
                            Text(
                                text = formatDate(chat.creation_time),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = shortenText(if(!chat.preview.isNullOrEmpty()) chat.preview else "", 30
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

fun shortenText(text: String, maxLength: Int): String {
    return if (text.length > maxLength) {
        text.take(maxLength) + "..."
    } else {
        text
    }
}

fun formatDate(dateString: String): String {
    return try {
        Log.d("ChatHistory", "Date string: $dateString")
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val date = LocalDateTime.parse(dateString, inputFormatter).plusHours(1L)
        Log.d("ChatHistory", "Date parsed: $date")

        // Format the date for display
        val outputFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", Locale.ENGLISH)

        Log.d("ChatHistory", "Formatted date: ${outputFormatter.format(date)} from ${dateString}, ${Locale.getDefault()}")
        date.format(outputFormatter).toString()
    } catch (e: Exception) {
        // Return original date string if formatting fails
        Log.e("ChatHistory", "Error format date from $dateString")
        dateString
    }
}