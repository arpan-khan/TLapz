package com.tlapz.videojournal.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tlapz.videojournal.domain.model.GroupedEntries
import com.tlapz.videojournal.domain.model.SyncResult
import com.tlapz.videojournal.domain.usecase.GetTimelineUseCase
import com.tlapz.videojournal.domain.usecase.SyncVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val groups: List<GroupedEntries> = emptyList(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTimelineUseCase: GetTimelineUseCase,
    private val syncVideosUseCase: SyncVideosUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var filterDate: LocalDate? = null

    init {
        observeTimeline()
        sync()
    }

    fun setFilterDate(date: LocalDate?) {
        filterDate = date
        observeTimeline()
    }

    private fun observeTimeline() {
        viewModelScope.launch {
            val flow = if (filterDate != null) {
                getTimelineUseCase.forDate(filterDate!!)
            } else {
                getTimelineUseCase()
            }
            flow.catch { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }.collect { groups ->
                _uiState.update { it.copy(groups = groups, isLoading = false) }
            }
        }
    }

    fun sync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = null) }
            when (val result = syncVideosUseCase()) {
                is SyncResult.Success -> {
                    val msg = if (result.added > 0 || result.removed > 0)
                        "+${result.added} / -${result.removed}" else null
                    _uiState.update { it.copy(isSyncing = false, syncMessage = msg) }
                }
                is SyncResult.Error -> {
                    _uiState.update { it.copy(isSyncing = false, syncMessage = "Sync error") }
                }
                SyncResult.NoFolderSet -> {
                    _uiState.update { it.copy(isSyncing = false) }
                }
            }
        }
    }
}
