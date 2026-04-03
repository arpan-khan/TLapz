package com.tlapz.videojournal.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tlapz.videojournal.domain.model.VideoEntryModel
import com.tlapz.videojournal.domain.repository.VideoRepository
import com.tlapz.videojournal.domain.usecase.DeleteEntryUseCase
import com.tlapz.videojournal.domain.usecase.UpdateEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val entry: VideoEntryModel? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val updateEntryUseCase: UpdateEntryUseCase,
    private val deleteEntryUseCase: DeleteEntryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadEntry(id: String) {
        viewModelScope.launch {
            val entry = videoRepository.getEntryById(id)
            _uiState.update { it.copy(entry = entry, isLoading = false) }
        }
    }

    fun updateNote(note: String) {
        val id = _uiState.value.entry?.id ?: return
        viewModelScope.launch {
            updateEntryUseCase.updateNote(id, note.takeIf { it.isNotBlank() })
            _uiState.update { state ->
                state.copy(entry = state.entry?.copy(note = note.takeIf { it.isNotBlank() }))
            }
        }
    }

    fun updateMood(mood: String?) {
        val id = _uiState.value.entry?.id ?: return
        viewModelScope.launch {
            updateEntryUseCase.updateMood(id, mood)
            _uiState.update { state ->
                state.copy(entry = state.entry?.copy(mood = mood))
            }
        }
    }

    fun deleteEntry() {
        val entry = _uiState.value.entry ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val success = deleteEntryUseCase(entry.id, entry.uriString)
            if (success) {
                _uiState.update { it.copy(isDeleted = true, isSaving = false) }
            } else {
                _uiState.update { it.copy(error = "Failed to delete entry", isSaving = false) }
            }
        }
    }
}
