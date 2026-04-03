package com.tlapz.videojournal.domain.repository

import com.tlapz.videojournal.domain.model.VideoEntryModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface VideoRepository {

    fun getAllEntries(): Flow<List<VideoEntryModel>>

    fun getEntriesByDate(date: LocalDate): Flow<List<VideoEntryModel>>

    fun getEntriesByMonth(yearMonth: YearMonth): Flow<List<VideoEntryModel>>

    suspend fun getEntryById(id: String): VideoEntryModel?

    suspend fun upsertEntry(entry: VideoEntryModel)

    suspend fun upsertEntries(entries: List<VideoEntryModel>)

    suspend fun deleteEntry(id: String)

    suspend fun deleteEntriesByIds(ids: List<String>)

    suspend fun updateNote(id: String, note: String?)

    suspend fun updateMood(id: String, mood: String?)

    suspend fun updateDuration(id: String, durationMs: Long)

    suspend fun markCompressed(id: String, newUri: String, newSize: Long)

    /** Returns all IDs currently in the database */
    suspend fun getAllIds(): Set<String>

    /** Returns dates that have at least one entry, for calendar highlighting */
    fun getDatesWithEntries(yearMonth: YearMonth): Flow<List<LocalDate>>

    suspend fun getTotalSize(): Long

    suspend fun getEntryCount(): Int
}
