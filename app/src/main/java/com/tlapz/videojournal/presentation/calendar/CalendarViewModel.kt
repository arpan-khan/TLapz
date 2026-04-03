package com.tlapz.videojournal.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tlapz.videojournal.domain.usecase.GetCalendarDatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val datesWithEntries: Set<LocalDate> = emptySet(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCalendarDatesUseCase: GetCalendarDatesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonth(YearMonth.now())
    }

    fun navigatePrevMonth() {
        val newMonth = _uiState.value.currentMonth.minusMonths(1)
        loadMonth(newMonth)
    }

    fun navigateNextMonth() {
        val newMonth = _uiState.value.currentMonth.plusMonths(1)
        loadMonth(newMonth)
    }

    private fun loadMonth(month: YearMonth) {
        _uiState.update { it.copy(currentMonth = month, isLoading = true) }
        viewModelScope.launch {
            getCalendarDatesUseCase(month)
                .catch { _uiState.update { s -> s.copy(isLoading = false) } }
                .collect { dates ->
                    _uiState.update { s ->
                        s.copy(datesWithEntries = dates.toSet(), isLoading = false)
                    }
                }
        }
    }
}
