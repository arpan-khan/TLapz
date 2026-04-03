package com.tlapz.videojournal.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tlapz.videojournal.data.storage.StorageManager
import com.tlapz.videojournal.domain.model.AppSettings
import com.tlapz.videojournal.domain.model.AppTheme
import com.tlapz.videojournal.domain.model.CompressionProfile
import com.tlapz.videojournal.domain.model.RecordingQuality
import com.tlapz.videojournal.domain.repository.SettingsRepository
import com.tlapz.videojournal.domain.repository.VideoRepository
import com.tlapz.videojournal.domain.usecase.SyncVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val folderDisplayPath: String = "Not selected",
    val totalVideos: Int = 0,
    val totalSizeBytes: Long = 0L,
    val isSyncing: Boolean = false,
    val syncResult: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val storageManager: StorageManager,
    private val videoRepository: VideoRepository,
    private val syncVideosUseCase: SyncVideosUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(folderDisplayPath = storageManager.getDisplayPath())
            }
        }
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val total = videoRepository.getEntryCount()
            val size = videoRepository.getTotalSize()
            _uiState.update { it.copy(totalVideos = total, totalSizeBytes = size) }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.updateTheme(theme.name) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDynamicColor(enabled) }
    }

    fun setRecordingQuality(quality: RecordingQuality) {
        viewModelScope.launch { settingsRepository.updateRecordingQuality(quality.name) }
    }

    fun setCompressionProfile(profile: CompressionProfile) {
        viewModelScope.launch { settingsRepository.updateCompressionProfile(profile.name) }
    }

    fun setAutoCompress(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoCompress(enabled) }
    }

    fun setCompactLayout(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateCompactLayout(enabled) }
    }

    fun setDebugLogging(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDebugLogging(enabled) }
    }

    fun onFolderChanged(uriString: String) {
        viewModelScope.launch {
            settingsRepository.updateFolderUri(uriString)
            _uiState.update { it.copy(folderDisplayPath = storageManager.getDisplayPath()) }
        }
    }

    fun manualRescan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncResult = null) }
            val result = syncVideosUseCase()
            loadStats()
            val msg = when (result) {
                is com.tlapz.videojournal.domain.model.SyncResult.Success ->
                    "Scan done: +${result.added} / -${result.removed}"
                is com.tlapz.videojournal.domain.model.SyncResult.Error -> "Scan error"
                com.tlapz.videojournal.domain.model.SyncResult.NoFolderSet -> "No folder selected"
            }
            _uiState.update { it.copy(isSyncing = false, syncResult = msg) }
        }
    }
}
