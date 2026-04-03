package com.tlapz.videojournal.presentation.setup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tlapz.videojournal.data.storage.StorageManager
import com.tlapz.videojournal.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderSetupUiState(
    val isLoading: Boolean = false,
    val folderSelected: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FolderSetupViewModel @Inject constructor(
    private val storageManager: StorageManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderSetupUiState())
    val uiState: StateFlow<FolderSetupUiState> = _uiState

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                storageManager.persistFolderUri(uri)
                settingsRepository.updateFolderUri(uri.toString())
                _uiState.value = _uiState.value.copy(isLoading = false, folderSelected = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to access folder: ${e.message}"
                )
            }
        }
    }
}
