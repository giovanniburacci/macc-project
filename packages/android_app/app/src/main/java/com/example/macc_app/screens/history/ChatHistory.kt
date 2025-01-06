package com.example.macc_app.screens.history

import ChatViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
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

        if(history.isEmpty()) {
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
                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)
            ) {
                itemsIndexed(history) { index, chat -> // Replace 20 with your data list size
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setChat(chat)
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
                                Spacer(modifier = Modifier.width(8.dp))
                                AssistChip(
                                    onClick = {},
                                    label = { Text(if(!chat.is_public) "Private" else "Public")},
                                    leadingIcon = {Icon(
                                        painter = painterResource(if(!chat.is_public) R.drawable.outline_lock_24 else R.drawable.baseline_lock_open_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )}
                                )
                            }
                            Text(text = chat.creation_time, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This is a preview of the card content for item $index.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}