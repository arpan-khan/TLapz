package com.tlapz.videojournal.domain.repository

import com.tlapz.videojournal.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun updateFolderUri(uri: String?)
    suspend fun updateTheme(theme: String)
    suspend fun updateDynamicColor(enabled: Boolean)
    suspend fun updateRecordingQuality(quality: String)
    suspend fun updateCompressionProfile(profile: String)
    suspend fun updateAutoCompress(enabled: Boolean)
    suspend fun updateCompactLayout(enabled: Boolean)
    suspend fun updateDebugLogging(enabled: Boolean)
    suspend fun getSettings(): AppSettings
}
