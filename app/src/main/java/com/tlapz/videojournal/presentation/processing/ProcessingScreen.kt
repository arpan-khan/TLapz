package com.tlapz.videojournal.presentation.processing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tlapz.videojournal.domain.model.CompressionProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    mode: String,
    startMs: Long,
    endMs: Long,
    onNavigateBack: () -> Unit,
    viewModel: ProcessingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val title = if (mode == "merge") "Merge Videos" else "Compress Videos"

    LaunchedEffect(mode, startMs, endMs) {
        viewModel.initialize(mode, startMs, endMs)
    }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            kotlinx.coroutines.delay(2000)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isRunning) viewModel.cancel()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // Summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("${uiState.entries.size} videos selected", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (mode == "merge") "Videos will be merged in chronological order."
                        else "Videos will be recompressed in-place.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Compression profile selector (only for compress mode)
            if (mode != "merge") {
                Text(
                    "Compression Profile",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CompressionProfile.entries.forEachIndexed { index, profile ->
                        SegmentedButton(
                            selected = uiState.compressionProfile == profile,
                            onClick = { viewModel.updateProfile(profile) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = CompressionProfile.entries.size
                            ),
                            label = { Text(profile.label) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = uiState.compressionProfile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }

            // Progress section
            val progress = uiState.progress
            if (progress != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (uiState.isComplete) "✓ Complete!" else progress.currentFileName,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = {
                                if (progress.totalCount > 0)
                                    progress.currentIndex.toFloat() / progress.totalCount
                                else 0f
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${progress.currentIndex} / ${progress.totalCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        progress.errorMessage?.let { err ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Error: $err",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Action buttons
            if (!uiState.isComplete) {
                if (uiState.isRunning) {
                    OutlinedButton(
                        onClick = { viewModel.cancel() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text("Cancel")
                    }
                } else {
                    Button(
                        onClick = { viewModel.startProcessing() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = uiState.entries.isNotEmpty(),
                    ) {
                        Text(if (mode == "merge") "Start Merge" else "Start Compression")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
