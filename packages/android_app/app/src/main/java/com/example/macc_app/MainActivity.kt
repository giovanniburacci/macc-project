package com.example.macc_app

import ChatViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.macc_app.screens.Screen1
import com.example.macc_app.screens.Screen2
import com.example.macc_app.screens.Screen3
import com.example.macc_app.ui.theme.CarouselAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CarouselAppTheme {
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
    Column(modifier = Modifier.fillMaxSize()) {
        // Top Section: Dynamic Screens
        Box(
            modifier = Modifier
                .weight(1f) // This section takes the remaining vertical space
                .fillMaxWidth()
        ) {
            NavHost(
                navController = navController,
                startDestination = "screen1",
                modifier = Modifier.fillMaxSize()
            ) {
                composable("screen1") { Screen1(model) }
                composable("screen2") { Screen2(model) }
                composable("screen3") { Screen3() }
            }
        }

        // Bottom Section: Buttons (Always Visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { navController.navigate("screen1") }) {
                Text("Screen 1")
            }
            Button(onClick = { navController.navigate("screen2") }) {
                Text("Screen 2")
            }
            Button(onClick = { navController.navigate("screen3") }) {
                Text("Screen 3")
            }
        }
    }
}

