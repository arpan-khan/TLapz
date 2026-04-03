package com.tlapz.videojournal.presentation.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.tlapz.videojournal.presentation.theme.RecordRed
import com.tlapz.videojournal.presentation.theme.RecordRedDim
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    onVideoSaved: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    LaunchedEffect(uiState.videoSaved) {
        if (uiState.videoSaved) {
            onVideoSaved()
        }
    }

    // Manual CameraX State
    var cameraSelector by remember { mutableStateOf(androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA) }
    val previewView = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    val videoCaptureState = remember { mutableStateOf<androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>?>(null) }

    LaunchedEffect(lifecycleOwner, cameraSelector, settings.recordingQuality, uiState.isCompressing) {
        if (uiState.isCompressing) return@LaunchedEffect // Don't bind while processing
        
        val cameraProvider = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context).get()
        
        val quality = when (settings.recordingQuality) {
            com.tlapz.videojournal.domain.model.RecordingQuality.SD -> Quality.SD
            com.tlapz.videojournal.domain.model.RecordingQuality.HD -> Quality.HD
        }

        val recorder = androidx.camera.video.Recorder.Builder()
            .setQualitySelector(QualitySelector.from(quality))
            .build()
            
        // FORCE SDR HERE - This is the fix for the Mali DataSpace conflict
        val videoCapture = androidx.camera.video.VideoCapture.Builder(recorder)
            .setDynamicRange(androidx.camera.core.DynamicRange.SDR)
            .build()
        
        val preview = androidx.camera.core.Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )
            videoCaptureState.value = videoCapture
        } catch (e: Exception) {
            Log.e("CameraScreen", "Use case binding failed", e)
        }
    }

    // Timer state for recording
    var elapsedSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            val start = System.currentTimeMillis()
            while (uiState.isRecording) {
                elapsedSeconds = ((System.currentTimeMillis() - start) / 1000).toInt()
                delay(500)
            }
        } else {
            elapsedSeconds = 0
        }
    }

    // Pulsing animation for record dot
    val infiniteTransition = rememberInfiniteTransition(label = "record_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        // Camera preview
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Camera permission required", color = Color.White)
            }
        }

        // Top controls
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Close button
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }

            // Timer / Quality Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (uiState.isRecording) {
                    Surface(
                        color = RecordRed.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = formatTimer(elapsedSeconds),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = uiState.qualityLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // Flip camera
            IconButton(
                onClick = { 
                    cameraSelector = if (cameraSelector == androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA) {
                        androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
            ) {
                Icon(Icons.Filled.FlipCameraAndroid, contentDescription = "Flip camera", tint = Color.White)
            }
        }

        // Bottom controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.2f))
                .navigationBarsPadding()
                .padding(bottom = 32.dp, top = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Record button
            val recordScale = if (uiState.isRecording) pulseScale else 1f
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(recordScale)
                    .clip(CircleShape)
                    .background(if (uiState.isRecording) RecordRedDim else Color.White)
                    .border(
                        width = 4.dp,
                        color = if (uiState.isRecording) RecordRed else Color.White.copy(alpha = 0.6f),
                        shape = CircleShape,
                    )
                    .clickable {
                        if (uiState.isRecording) {
                            viewModel.stopRecording()
                        } else {
                            videoCaptureState.value?.let { vc ->
                                viewModel.startRecording(context, vc)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.isRecording) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(Color.White),
                    )
                }
            }
        }

        // Processing overlay
        if (uiState.isCompressing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { uiState.compressionProgress },
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                    )
                    Text(
                        text = "Optimizing Journal Entry...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(uiState.compressionProgress * 100).toInt()}%",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Error overlay
        if (uiState.error != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun formatTimer(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}
