package com.tlapz.videojournal.presentation.processing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tlapz.videojournal.domain.model.CompressionProfile
import com.tlapz.videojournal.domain.model.ProcessingProgress
import com.tlapz.videojournal.domain.model.VideoEntryModel
import com.tlapz.videojournal.domain.repository.VideoRepository
import com.tlapz.videojournal.domain.repository.SettingsRepository
import com.tlapz.videojournal.domain.usecase.CompressVideoUseCase
import com.tlapz.videojournal.domain.usecase.MergeVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class ProcessingUiState(
    val mode: String = "compress",
    val entries: List<VideoEntryModel> = emptyList(),
    val progress: ProcessingProgress? = null,
    val isRunning: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null,
    val compressionProfile: CompressionProfile = CompressionProfile.STANDARD,
)

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val settingsRepository: SettingsRepository,
    private val compressVideoUseCase: CompressVideoUseCase,
    private val mergeVideosUseCase: MergeVideosUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    private var processingJob: Job? = null

    fun initialize(mode: String, startMs: Long, endMs: Long) {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            
            // If mode is "uncompressed", we gather all items where isCompressed is false
            // regardless of the date range passed.
            val entries = if (mode == "uncompressed") {
                videoRepository.getAllEntries().first()
                    .filter { !it.isCompressed }
                    .sortedBy { it.dateTimeMs }
            } else {
                videoRepository.getAllEntries().first()
                    .filter { it.dateTimeMs in startMs..endMs }
                    .sortedBy { it.dateTimeMs }
            }

            _uiState.update {
                it.copy(
                    mode = mode,
                    entries = entries,
                    compressionProfile = settings.compressionProfile,
                )
            }
        }
    }

    fun startProcessing() {
        val state = _uiState.value
        if (state.isRunning || state.entries.isEmpty()) return

        processingJob = viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true, error = null) }

            val flow = if (state.mode == "merge") {
                mergeVideosUseCase.invoke(state.entries)
            } else {
                compressVideoUseCase.invoke(state.entries, state.compressionProfile)
            }

            flow.catch { e ->
                _uiState.update { it.copy(error = e.message, isRunning = false) }
            }.collect { progress ->
                _uiState.update { it.copy(progress = progress, isComplete = progress.isComplete) }
            }

            _uiState.update { it.copy(isRunning = false) }
        }
    }

    fun cancel() {
        processingJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
    }

    fun updateProfile(profile: CompressionProfile) {
        _uiState.update { it.copy(compressionProfile = profile) }
    }
}
