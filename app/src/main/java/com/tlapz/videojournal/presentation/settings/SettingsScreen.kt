package com.tlapz.videojournal.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tlapz.videojournal.core.util.DateUtils
import com.tlapz.videojournal.data.storage.StorageManager
import com.tlapz.videojournal.domain.model.AppTheme
import com.tlapz.videojournal.domain.model.CompressionProfile
import com.tlapz.videojournal.domain.model.RecordingQuality
import com.tlapz.videojournal.presentation.components.*
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProcessing: (mode: String, startMs: Long, endMs: Long) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val scrollState = rememberScrollState()

    // SAF folder re-picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.onFolderChanged(it.toString()) }
    }

    // Date range helper for batch tools
    fun currentMonthRange(): Pair<Long, Long> {
        val ym = YearMonth.now()
        val start = ym.atDay(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = ym.atEndOfMonth().atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return Pair(start, end)
    }

    fun currentYearRange(): Pair<Long, Long> {
        val year = java.time.Year.now().value
        val start = java.time.LocalDate.of(year, 1, 1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = java.time.LocalDate.of(year, 12, 31).atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return Pair(start, end)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .verticalScroll(scrollState),
        ) {

            // ── A. STORAGE ────────────────────────────────────────────────
            SettingsSectionHeader("Storage")

            // Storage stats card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        StorageStat(label = "Videos", value = uiState.totalVideos.toString())
                        StorageStat(label = "Total Size", value = DateUtils.formatFileSize(uiState.totalSizeBytes))
                        StorageStat(
                            label = "Avg Size",
                            value = if (uiState.totalVideos > 0)
                                DateUtils.formatFileSize(uiState.totalSizeBytes / uiState.totalVideos)
                            else "—"
                        )
                    }
                }
            }

            SettingsClickItem(
                title = "Video Folder",
                subtitle = uiState.folderDisplayPath,
                icon = Icons.Outlined.Folder,
                onClick = { folderPickerLauncher.launch(null) },
            )

            SettingsClickItem(
                title = "Rescan Folder",
                subtitle = if (uiState.isSyncing) "Scanning…" else uiState.syncResult ?: "Sync library with filesystem",
                icon = Icons.Outlined.Sync,
                onClick = { viewModel.manualRescan() },
                trailingContent = {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                },
            )

            SettingsDivider()

            // ── B. VIDEO ─────────────────────────────────────────────────
            SettingsSectionHeader("Video")

            // Recording quality segmented buttons
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Recording Quality",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    RecordingQuality.entries.forEachIndexed { i, q ->
                        SegmentedButton(
                            selected = settings.recordingQuality == q,
                            onClick = { viewModel.setRecordingQuality(q) },
                            shape = SegmentedButtonDefaults.itemShape(i, RecordingQuality.entries.size),
                            label = { Text(q.label) },
                        )
                    }
                }
            }

            // Compression profile
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Compression Profile",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CompressionProfile.entries.forEachIndexed { i, p ->
                        SegmentedButton(
                            selected = settings.compressionProfile == p,
                            onClick = { viewModel.setCompressionProfile(p) },
                            shape = SegmentedButtonDefaults.itemShape(i, CompressionProfile.entries.size),
                            label = { Text(p.label) },
                        )
                    }
                }
                Text(
                    text = settings.compressionProfile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            SettingsSwitchItem(
                title = "Auto-compress after recording",
                subtitle = "Apply compression profile immediately after recording",
                icon = Icons.Outlined.Compress,
                checked = settings.autoCompress,
                onCheckedChange = { viewModel.setAutoCompress(it) },
            )

            SettingsDivider()

            // ── C. BATCH TOOLS ───────────────────────────────────────────
            SettingsSectionHeader("Batch Tools")

            SettingsClickItem(
                title = "Compress All Unoptimized",
                subtitle = "Only compress videos that haven't been optimized yet",
                icon = Icons.Outlined.Compress,
                onClick = {
                    onNavigateToProcessing("uncompressed", 0L, Long.MAX_VALUE)
                },
            )
            SettingsClickItem(
                title = "Recompress This Month",
                subtitle = "Recompress all videos from current month",
                icon = Icons.Outlined.Archive,
                onClick = {
                    val (s, e) = currentMonthRange()
                    onNavigateToProcessing("compress", s, e)
                },
            )
            SettingsClickItem(
                title = "Recompress This Year",
                subtitle = "Recompress all videos from current year",
                icon = Icons.Outlined.Archive,
                onClick = {
                    val (s, e) = currentYearRange()
                    onNavigateToProcessing("compress", s, e)
                },
            )
            SettingsClickItem(
                title = "Merge This Month",
                subtitle = "Merge all videos from current month into one file",
                icon = Icons.Outlined.MergeType,
                onClick = {
                    val (s, e) = currentMonthRange()
                    onNavigateToProcessing("merge", s, e)
                },
            )
            SettingsClickItem(
                title = "Merge This Year",
                subtitle = "Merge all videos from current year into one file",
                icon = Icons.Outlined.MergeType,
                onClick = {
                    val (s, e) = currentYearRange()
                    onNavigateToProcessing("merge", s, e)
                },
            )

            SettingsDivider()

            // ── D. UI / APPEARANCE ───────────────────────────────────────
            SettingsSectionHeader("Appearance")

            // Theme radio group
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    "Theme",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                AppTheme.entries.forEach { theme ->
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                    ) {
                        RadioButton(
                            selected = settings.theme == theme,
                            onClick = { viewModel.setTheme(theme) },
                        )
                        Text(
                            text = theme.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }

            SettingsSwitchItem(
                title = "Dynamic Color",
                subtitle = "Use Material You colors from your wallpaper (Android 12+)",
                icon = Icons.Outlined.Palette,
                checked = settings.dynamicColor,
                onCheckedChange = { viewModel.setDynamicColor(it) },
            )

            SettingsSwitchItem(
                title = "Compact Layout",
                subtitle = "Smaller video cards in the timeline",
                icon = Icons.Outlined.ViewHeadline,
                checked = settings.compactLayout,
                onCheckedChange = { viewModel.setCompactLayout(it) },
            )

            SettingsDivider()

            // ── E. PRIVACY ───────────────────────────────────────────────
            SettingsSectionHeader("Privacy")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "All data is stored locally on your device. No internet access is used.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SettingsDivider()

            // ── F. ADVANCED ───────────────────────────────────────────────
            SettingsSectionHeader("Advanced")

            SettingsClickItem(
                title = "Filename Format",
                subtitle = "YYYY-MM-DD_HH-MM-SS.mp4",
                icon = Icons.Outlined.TextFormat,
                onClick = {},
            )

            SettingsSwitchItem(
                title = "Debug Logging",
                subtitle = "Enable verbose logging to logcat",
                icon = Icons.Outlined.BugReport,
                checked = settings.debugLogging,
                onCheckedChange = { viewModel.setDebugLogging(it) },
            )

            SettingsDivider()

            SettingsClickItem(
                title = "GitHub Repository",
                subtitle = "arpan-khan/TLapz",
                icon = Icons.Outlined.Code,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/arpan-khan/TLapz"))
                    context.startActivity(intent)
                },
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StorageStat(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
