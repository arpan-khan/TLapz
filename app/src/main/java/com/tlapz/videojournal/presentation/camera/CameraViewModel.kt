package com.tlapz.videojournal.presentation.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tlapz.videojournal.data.processing.VideoCompressor
import com.tlapz.videojournal.data.storage.StorageManager
import com.tlapz.videojournal.domain.model.CompressionProfile
import com.tlapz.videojournal.domain.model.RecordingQuality
import com.tlapz.videojournal.domain.repository.SettingsRepository
import com.tlapz.videojournal.domain.repository.VideoRepository
import com.tlapz.videojournal.domain.usecase.SyncVideosUseCase
import com.tlapz.videojournal.domain.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val isRecording: Boolean = false,
    val isCompressing: Boolean = false,
    val compressionProgress: Float = 0f,
    val videoSaved: Boolean = false,
    val qualityLabel: String = "480p",
    val error: String? = null,
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val storageManager: StorageManager,
    private val settingsRepository: SettingsRepository,
    private val syncVideosUseCase: SyncVideosUseCase,
    private val videoCompressor: VideoCompressor,
    private val videoRepository: VideoRepository,
) : ViewModel() {

    private val TAG = "CameraViewModel"

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private var activeRecording: Recording? = null
    private var openPfd: android.os.ParcelFileDescriptor? = null
    private var lastOutputUriString: String? = null

    init {
        viewModelScope.launch {
            val appSettings = settingsRepository.getSettings()
            _settings.value = appSettings
            _uiState.update { it.copy(qualityLabel = appSettings.recordingQuality.label) }
        }
    }

    fun startRecording(context: Context, videoCapture: androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>) {
        if (activeRecording != null || _uiState.value.isRecording) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRecording = true) }

                val outputFile = storageManager.createVideoOutputFile()
                if (outputFile == null) {
                    _uiState.update { it.copy(isRecording = false, error = "Could not create file. Folder might not be set.") }
                    return@launch
                }

                val pfd = storageManager.openWriteDescriptor(outputFile.uri)
                if (pfd == null) {
                    _uiState.update { it.copy(isRecording = false, error = "Could not open file descriptor") }
                    return@launch
                }
                
                openPfd = pfd
                lastOutputUriString = outputFile.uri.toString()

                // Start actual capture
                val outputOptions = FileDescriptorOutputOptions.Builder(pfd).build()
                
                activeRecording = videoCapture.output
                    .prepareRecording(context, outputOptions)
                    .withAudioEnabled()
                    .start(ContextCompat.getMainExecutor(context)) { event ->
                        when (event) {
                            is VideoRecordEvent.Start -> {
                                _uiState.update { it.copy(isRecording = true, error = null) }
                            }
                            is VideoRecordEvent.Finalize -> {
                                openPfd?.close()
                                openPfd = null
                                if (event.hasError()) {
                                    Log.e(TAG, "Recording finalize error: ${event.error}")
                                    _uiState.update {
                                        it.copy(
                                            isRecording = false,
                                            error = "Recording error: ${event.error}",
                                        )
                                    }
                                } else {
                                    val savedUri = lastOutputUriString
                                    _uiState.update { it.copy(isRecording = false) }
                                    
                                    viewModelScope.launch {
                                        syncVideosUseCase()
                                        
                                        val settings = settingsRepository.getSettings()
                                        if (settings.autoCompress && savedUri != null) {
                                            _uiState.update { it.copy(isCompressing = true) }
                                            val result = videoCompressor.compress(
                                                inputUriString = savedUri,
                                                profile = settings.compressionProfile,
                                                onProgress = { p -> 
                                                    _uiState.update { it.copy(compressionProgress = p) }
                                                }
                                            )
                                            _uiState.update { it.copy(isCompressing = false) }
                                            if (result != null) {
                                                val latest = videoRepository.getAllEntries().first()
                                                    .firstOrNull { it.uriString == result.newUriString }
                                                
                                                if (latest != null) {
                                                    videoRepository.markCompressed(
                                                        id = latest.id,
                                                        newUri = result.newUriString,
                                                        newSize = result.newSizeBytes
                                                    )
                                                }
                                                syncVideosUseCase()
                                            }
                                        }
                                        _uiState.update { it.copy(videoSaved = true) }
                                    }
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to start recording: ${e.message}"
                )
            }
        }
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    fun flipCamera(current: CameraSelector): CameraSelector {
        return if (current == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeRecording?.stop()
        openPfd?.close()
    }
}
