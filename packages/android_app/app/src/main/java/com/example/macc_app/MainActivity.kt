package com.example.macc_app

import ChatViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.compose.AppTheme
import com.example.macc_app.screens.Screen1
import com.example.macc_app.screens.Screen2
import com.example.macc_app.screens.Screen3

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme  {
                val navController = rememberNavController()
                val viewModel: ChatViewModel = viewModel()
                val context = LocalContext.current
                viewModel.initializeSpeechComponents(context = context)
                AppContent(navController, viewModel)
            }
        }
    }
}

@Composable
fun AppContent(navController: NavHostController, model: ChatViewModel) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
        // Top Section: Dynamic Screens
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            NavHost(
                navController = navController,
                startDestination = "screen1",
                modifier = Modifier.fillMaxSize().padding(bottom = 64.dp)
            ) {
                composable("screen1") { Screen1(model, navController) }
                composable("screen2") { Screen2(model) }
                composable("screen3") { Screen3() }
            }
        }

        // Floating Buttons for each screen
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom=12.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = {
                    if (navController.currentBackStackEntry?.destination?.route !== "screen1") {
                        navController.navigate("screen1")
                    }
                },
                shape = CircleShape,
                modifier = Modifier.size(56.dp).offset(x = (-90).dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_add_a_photo_24),
                    contentDescription = "Open Camera",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            FloatingActionButton(
                onClick = {
                    if (navController.currentBackStackEntry?.destination?.route !== "screen2") {
                        navController.navigate("screen2")
                    }
                },
                shape = CircleShape,
                modifier = Modifier.size(56.dp).offset(x = 0.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_chat_24),
                    contentDescription = "Open Chat",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            FloatingActionButton(
                onClick = { if (navController.currentBackStackEntry?.destination?.route !== "screen3") {
                    navController.navigate("screen3")
                } },
                shape = CircleShape,
                modifier = Modifier.size(56.dp).offset(x = 90.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_groups_24),
                    contentDescription = "View History",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

