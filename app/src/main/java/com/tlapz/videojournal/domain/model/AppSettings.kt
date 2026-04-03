package com.tlapz.videojournal.domain.model

import com.tlapz.videojournal.core.util.Constants

data class AppSettings(
    val folderUri: String? = null,
    val theme: AppTheme = AppTheme.DARK,
    val dynamicColor: Boolean = false,
    val recordingQuality: RecordingQuality = RecordingQuality.SD,
    val compressionProfile: CompressionProfile = CompressionProfile.STANDARD,
    val autoCompress: Boolean = false,
    val compactLayout: Boolean = false,
    val debugLogging: Boolean = false,
)

enum class AppTheme { DARK, LIGHT, SYSTEM }

enum class RecordingQuality(val label: String, val height: Int) {
    SD("480p", Constants.HEIGHT_STANDARD),
    HD("720p", Constants.HEIGHT_HD),
}
