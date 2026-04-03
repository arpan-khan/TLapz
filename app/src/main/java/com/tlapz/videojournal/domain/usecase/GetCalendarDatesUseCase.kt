package com.tlapz.videojournal.domain.usecase

import com.tlapz.videojournal.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class GetCalendarDatesUseCase @Inject constructor(
    private val repository: VideoRepository,
) {
    operator fun invoke(yearMonth: YearMonth): Flow<List<LocalDate>> {
        return repository.getDatesWithEntries(yearMonth)
    }
}
