package com.example.macc_app.screens.community

import ChatViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.macc_app.screens.history.shortenText
import com.example.macc_app.R
import com.example.macc_app.data.remote.ChatResponse
import com.example.macc_app.screens.history.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Community(navController: NavController, viewModel: ChatViewModel) {

    viewModel.fetchCommunity()

    val community = viewModel.community

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Community") })
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)
        ) {
            itemsIndexed(community) { index, chat -> // Replace 20 with your data list size
                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setReadOnlyChat(ChatResponse(id = chat.id, name = chat.name, is_public = chat.is_public, creation_time = chat.creation_time, preview = chat.preview, last_update = chat.last_update, user_id = chat.user_id))
                            viewModel.fetchMessages(chat.id)
                            viewModel.fetchComments(chat.id)
                            navController.navigate("community/${chat.id}") // Pass the cardId
                        },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).align(Alignment.CenterHorizontally)
                    ) {
                        Column(modifier = Modifier.width(80.dp).align(Alignment.CenterVertically),
                            verticalArrangement = Arrangement.Center) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_person_24),
                                contentDescription = "User",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Text(
                                text = if(!chat.username.isNullOrEmpty()) chat.username else "",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(text = chat.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = formatDate(chat.creation_time),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = shortenText(
                                    if (!chat.preview.isNullOrEmpty()) chat.preview else "",
                                    30
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}