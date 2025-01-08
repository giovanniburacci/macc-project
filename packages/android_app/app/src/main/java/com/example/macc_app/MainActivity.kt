package com.example.macc_app

import ChatViewModel
import ChatViewModelFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.compose.AppTheme
import com.example.macc_app.screens.CameraOrGallery
import com.example.macc_app.screens.history.ChatHistory
import com.example.macc_app.screens.history.HistoryViewOnlyChat
import com.example.macc_app.screens.LatestChat
import com.example.macc_app.screens.community.Community
import com.example.macc_app.screens.community.ViewOnlyChatWithComments
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://ghinoads.pythonanywhere.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val viewModel: ChatViewModel by viewModels { ChatViewModelFactory(retrofit) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme  {
                val navController = rememberNavController()
                val context = LocalContext.current
                viewModel.initializeSpeechComponents(context = context)
                val auth = FirebaseAuth.getInstance()
                viewModel.fetchLastChat(auth.currentUser!!.uid)
                AppContent(navController, viewModel)
            }
        }
    }
}

@Composable
fun AppContent(navController: NavHostController, model: ChatViewModel) {
    val auth = FirebaseAuth.getInstance()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
        // Top Section: Dynamic Screens
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            NavHost(
                navController = navController,
                startDestination = "cameraOrGallery",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("cameraOrGallery") { CameraOrGallery(model, navController) }
                composable("latestChat") { LatestChat(model) }
                composable("chatHistory") { ChatHistory(navController, model) }
                composable("community") { Community(navController, model) }
                composable(
                    "chatHistory/{chatId}", // Define the route with a parameter placeholder
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                    HistoryViewOnlyChat(chatId, model) // Pass the parameter to chatHistory
                }
                composable(
                    "community/{chatId}", // Define the route with a parameter placeholder
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                    ViewOnlyChatWithComments(chatId, model) // Pass the parameter to chatHistory
                }
            }
        }

        // Floating Buttons for each screen
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).background(Color.Transparent).padding(bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = {
                    if (navController.currentBackStackEntry?.destination?.route !== "cameraOrGallery") {
                        navController.navigate("cameraOrGallery")
                    }
                },
                shape = CircleShape,
                modifier = Modifier.size(56.dp).offset(x = (-75).dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_add_a_photo_24),
                    contentDescription = "Open Camera",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            FloatingActionButton(
                onClick = {
                    if (navController.currentBackStackEntry?.destination?.route !== "latestChat") {
                        model.fetchLastChat(auth.currentUser!!.uid)
                        navController.navigate("latestChat")
                    }
                },
                shape = CircleShape,
                modifier = Modifier.size(56.dp).offset(x = (-25).dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_chat_24),
                    contentDescription = "Open Chat",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            FloatingActionButton(
                onClick = { if (navController.currentBackStackEntry?.destination?.route !== "chatHistory") {
                    navController.navigate("chatHistory")
                } },
                shape = CircleShape,
                modifier = Modifier.size(56.dp).offset(x = 25.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_history_24),
                    contentDescription = "View History",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            FloatingActionButton(
                onClick = { if (navController.currentBackStackEntry?.destination?.route !== "community") {
                    navController.navigate("community")
                } },
                shape = CircleShape,
                modifier = Modifier.size(56.dp).offset(x = 75.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_groups_24),
                    contentDescription = "View community",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

