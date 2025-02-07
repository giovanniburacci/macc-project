package com.example.macc_app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.macc_app.ui.theme.AppTheme
import com.example.macc_app.data.remote.PythonAnywhereClient
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

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(PythonAnywhereClient.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val viewModel: ChatViewModel by viewModels { ChatViewModelFactory(retrofit) }

    // List of permissions to request
    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Permission launcher
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }

        permissions.entries.forEach { permission ->
            if (permission.value) {
                // Permission granted
                Log.d("MainActivity", "${permission.key} granted")
            } else {
                // Permission denied
                Log.d("MainActivity", "${permission.key} denied")
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request permissions
        checkAndRequestPermissions()

        setContent {
            AppTheme  {
                val navController = rememberNavController()
                val auth = FirebaseAuth.getInstance()
                Log.d("MainActivity", "User logged with uid ${auth.currentUser!!.uid}")
                viewModel.fetchLastChat(auth.currentUser!!.uid)
                AppContent(navController, viewModel)
            }
        }
    }
}

@Composable
fun AppContent(navController: NavHostController, model: ChatViewModel) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        model.initializeSpeechComponents(context)
    }

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
                ) {
                    HistoryViewOnlyChat(model) // Pass the parameter to chatHistory
                }
                composable(
                    "community/{chatId}", // Define the route with a parameter placeholder
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                ) {
                    ViewOnlyChatWithComments(model) // Pass the parameter to chatHistory
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
                        model.fetchMessages(model.lastChat.value!!.id)
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
                    model.fetchHistory(auth.currentUser!!.uid)
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
                    model.fetchCommunity()
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
