package com.example.macc_app.screens

import ChatViewModel
import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun Screen1(viewModel: ChatViewModel = viewModel(), navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val coroutineScope = rememberCoroutineScope() // Get the coroutine scope

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    var showRecognizedTextDialog by remember { mutableStateOf<Boolean>(false) }
    var recognizedText by remember { mutableStateOf<String>("") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    suspend fun recognizeTextFromBitmap(bitmap: Bitmap, context: Context) {
        // Convert the Bitmap to an InputImage
        val image = InputImage.fromBitmap(bitmap, 0)

        // Initialize the Text Recognizer
        val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        try {
            // Process the image
            val visionText = recognizer.process(image).await()
            // Handle the recognized text
            showRecognizedTextDialog = true
            recognizedText = visionText.text
        } catch (exception: Exception) {
            // Handle any error
            exception.printStackTrace()
            Toast.makeText(context, "Text recognition failed: ${exception.message}", Toast.LENGTH_SHORT).show()
        }

    }

    // Function to convert URI to Bitmap
    fun getBitmapFromUri(uri: Uri, context: Context): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }


    suspend fun takePicture(
        context: Context, // Current context
        imageCapture: ImageCapture? // ImageCapture instance for taking pictures
    ): Bitmap = suspendCancellableCoroutine { continuation ->
        if (imageCapture == null) {
            continuation.resumeWithException(Exception("Camera not ready"))
            return@suspendCancellableCoroutine
        }

        // Use the camera to capture the image
        val photoFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        try {
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        // Get image as Bitmap
                        val savedUri = Uri.fromFile(photoFile)
                        val bitmap = getBitmapFromUri(savedUri, context)
                        continuation.resume(bitmap) // Resume the coroutine with the bitmap
                    }

                    override fun onError(exception: ImageCaptureException) {
                        exception.printStackTrace()
                        continuation.resumeWithException(Exception("Failed to capture image: ${exception.message}"))
                    }
                }
            )
        } catch (exception: Exception) {
            exception.printStackTrace()
            continuation.resumeWithException(Exception("Failed to capture image: ${exception.message}"))
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // Handle gallery selection
        uri?.let { imageUri ->
            coroutineScope.launch {
                // Convert URI to Bitmap and perform text recognition
                val bitmap = getBitmapFromUri(imageUri, context)
                recognizeTextFromBitmap(bitmap, context)
            }

        }
    }
    // Request camera permission on initial composition
    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        if(showRecognizedTextDialog) {
            RecognizedTextDialog(
                onDismiss = {showRecognizedTextDialog = false; recognizedText = ""},
                onConfirm = {
                    viewModel.sendMessage(recognizedText, type = MessageType.TEXT, targetLanguage = "it", timestamp = System.currentTimeMillis())
                    showRecognizedTextDialog = false;
                    recognizedText = "";
                    navController.navigate("screen2")
                },
                showDialog = showRecognizedTextDialog,
                text = recognizedText
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        previewView = this
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Take Picture Button
        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val bitmap = takePicture(context, imageCapture)
                        // Perform text recognition on the captured Bitmap
                        recognizeTextFromBitmap(bitmap, context)
                    } catch (e: Exception) {
                        // Handle errors, e.g., camera not ready or failed to capture image
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Take Picture")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Upload from Gallery Button
        Button(
            onClick = {
                galleryLauncher.launch("image/*")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload from Gallery")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Display captured image URI
        capturedImageUri?.let { uri ->
            Text("Image saved at: $uri")
        }
    }

    // Camera initialization
    LaunchedEffect(previewView) {
        if (previewView != null) {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build()
                val imageCaptureBuilder = ImageCapture.Builder()
                imageCapture = imageCaptureBuilder.build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll() // Ensure previous bindings are cleared
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                preview.setSurfaceProvider(previewView!!.surfaceProvider)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to initialize camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}


@Composable
fun RecognizedTextDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    showDialog: Boolean,
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black.copy(alpha = 0.6f)) // Dim background
            .clickable { onDismiss() } // Dismiss when clicking outside the box
            .animateContentSize()
    ) {
        AnimatedVisibility(
            visible = showDialog,
            enter = fadeIn(tween(durationMillis = 500)), // Fade-in animation
            exit = fadeOut(tween(durationMillis = 500))  // Fade-out if dismissed
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .background(color = Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp) // Padding inside the box
                    .alpha(
                        animateFloatAsState(
                            if (showDialog) 1f else 0f, animationSpec = tween(durationMillis = 2000),
                            label = ""
                        ).value
                    ) // Smooth fade-in effect
            ) {
                AlertDialog(
                    onDismissRequest = { onDismiss() },
                    title = { Text("How to use 2D") },
                    icon = {
                    },
                    text = {
                        Column {
                            Row (modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "This is the recognized text: ",
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Would you like to translate it?")
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { onConfirm() }) {
                            Text("Translate")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {onDismiss()}) {
                            Text("Discard")
                        }
                    }
                )
            }
        }
    }
}
