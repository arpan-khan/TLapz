package com.tlapz.videojournal.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tlapz.videojournal.core.util.Constants
import com.tlapz.videojournal.domain.model.AppSettings
import com.tlapz.videojournal.domain.model.AppTheme
import com.tlapz.videojournal.domain.model.CompressionProfile
import com.tlapz.videojournal.domain.model.RecordingQuality
import com.tlapz.videojournal.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val FOLDER_URI = stringPreferencesKey(Constants.PREF_FOLDER_URI)
        val THEME = stringPreferencesKey(Constants.PREF_THEME)
        val DYNAMIC_COLOR = booleanPreferencesKey(Constants.PREF_DYNAMIC_COLOR)
        val RECORDING_QUALITY = stringPreferencesKey(Constants.PREF_RECORDING_QUALITY)
        val COMPRESSION_PROFILE = stringPreferencesKey(Constants.PREF_COMPRESSION_PROFILE)
        val AUTO_COMPRESS = booleanPreferencesKey(Constants.PREF_AUTO_COMPRESS)
        val COMPACT_LAYOUT = booleanPreferencesKey(Constants.PREF_COMPACT_LAYOUT)
        val DEBUG_LOGGING = booleanPreferencesKey(Constants.PREF_DEBUG_LOGGING)
    }

    override val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        prefs.toSettings()
    }

    override suspend fun getSettings(): AppSettings = dataStore.data.first().toSettings()

    override suspend fun updateFolderUri(uri: String?) {
        dataStore.edit { prefs ->
            if (uri == null) prefs.remove(Keys.FOLDER_URI)
            else prefs[Keys.FOLDER_URI] = uri
        }
    }

    override suspend fun updateTheme(theme: String) {
        dataStore.edit { it[Keys.THEME] = theme }
    }

    override suspend fun updateDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    override suspend fun updateRecordingQuality(quality: String) {
        dataStore.edit { it[Keys.RECORDING_QUALITY] = quality }
    }

    override suspend fun updateCompressionProfile(profile: String) {
        dataStore.edit { it[Keys.COMPRESSION_PROFILE] = profile }
    }

    override suspend fun updateAutoCompress(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_COMPRESS] = enabled }
    }

    override suspend fun updateCompactLayout(enabled: Boolean) {
        dataStore.edit { it[Keys.COMPACT_LAYOUT] = enabled }
    }

    override suspend fun updateDebugLogging(enabled: Boolean) {
        dataStore.edit { it[Keys.DEBUG_LOGGING] = enabled }
    }

    private fun Preferences.toSettings() = AppSettings(
        folderUri = this[Keys.FOLDER_URI],
        theme = AppTheme.valueOf(this[Keys.THEME] ?: Constants.THEME_DARK),
        dynamicColor = this[Keys.DYNAMIC_COLOR] ?: false,
        recordingQuality = RecordingQuality.valueOf(this[Keys.RECORDING_QUALITY] ?: Constants.QUALITY_SD),
        compressionProfile = CompressionProfile.valueOf(this[Keys.COMPRESSION_PROFILE] ?: Constants.COMPRESSION_STANDARD),
        autoCompress = this[Keys.AUTO_COMPRESS] ?: false,
        compactLayout = this[Keys.COMPACT_LAYOUT] ?: false,
        debugLogging = this[Keys.DEBUG_LOGGING] ?: false,
    )
}
