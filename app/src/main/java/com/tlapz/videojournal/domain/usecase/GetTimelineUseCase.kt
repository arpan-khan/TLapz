package com.tlapz.videojournal.domain.usecase

import com.tlapz.videojournal.domain.model.GroupedEntries
import com.tlapz.videojournal.domain.repository.VideoRepository
import com.tlapz.videojournal.core.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class GetTimelineUseCase @Inject constructor(
    private val repository: VideoRepository,
) {
    /** Returns all entries grouped by calendar date, newest first. */
    operator fun invoke(): Flow<List<GroupedEntries>> {
        return repository.getAllEntries().map { entries ->
            entries
                .sortedByDescending { it.dateTimeMs }
                .groupBy { DateUtils.epochMsToLocalDate(it.dateTimeMs) }
                .map { (date, dayEntries) ->
                    GroupedEntries(date = date, entries = dayEntries.sortedByDescending { it.dateTimeMs })
                }
                .sortedByDescending { it.date }
        }
    }

    /** Returns entries for a specific date. */
    fun forDate(date: LocalDate): Flow<List<GroupedEntries>> {
        return repository.getEntriesByDate(date).map { entries ->
            listOf(GroupedEntries(date = date, entries = entries.sortedByDescending { it.dateTimeMs }))
        }
    }
}
