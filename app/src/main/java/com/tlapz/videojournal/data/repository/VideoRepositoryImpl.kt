package com.tlapz.videojournal.data.repository

import com.tlapz.videojournal.core.util.DateUtils
import com.tlapz.videojournal.data.database.VideoDao
import com.tlapz.videojournal.data.database.VideoEntry
import com.tlapz.videojournal.domain.model.VideoEntryModel
import com.tlapz.videojournal.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val dao: VideoDao,
) : VideoRepository {

    override fun getAllEntries(): Flow<List<VideoEntryModel>> =
        dao.getAllEntries().map { list -> list.map { it.toDomain() } }

    override fun getEntriesByDate(date: LocalDate): Flow<List<VideoEntryModel>> {
        val startMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.getByDateRange(startMs, endMs).map { list -> list.map { it.toDomain() } }
    }

    override fun getEntriesByMonth(yearMonth: YearMonth): Flow<List<VideoEntryModel>> {
        val start = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = yearMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.getByDateRange(start, end).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getEntryById(id: String): VideoEntryModel? =
        dao.getById(id)?.toDomain()

    override suspend fun upsertEntry(entry: VideoEntryModel) =
        dao.upsert(entry.toEntity())

    override suspend fun upsertEntries(entries: List<VideoEntryModel>) =
        dao.upsertAll(entries.map { it.toEntity() })

    override suspend fun deleteEntry(id: String) = dao.deleteById(id)

    override suspend fun deleteEntriesByIds(ids: List<String>) = dao.deleteByIds(ids)

    override suspend fun updateNote(id: String, note: String?) = dao.updateNote(id, note)

    override suspend fun updateMood(id: String, mood: String?) = dao.updateMood(id, mood)

    override suspend fun updateDuration(id: String, durationMs: Long) = dao.updateDuration(id, durationMs)

    override suspend fun markCompressed(id: String, newUri: String, newSize: Long) =
        dao.markCompressed(id, newUri, newSize)

    override suspend fun getAllIds(): Set<String> = dao.getAllIds().toSet()

    override fun getDatesWithEntries(yearMonth: YearMonth): Flow<List<LocalDate>> {
        val start = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = yearMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.getTimestampsInRange(start, end).map { timestamps ->
            timestamps.map { DateUtils.epochMsToLocalDate(it) }.distinct()
        }
    }

    override suspend fun getTotalSize(): Long = dao.getTotalSize()

    override suspend fun getEntryCount(): Int = dao.getEntryCount()

    // Mapping helpers
    private fun VideoEntry.toDomain() = VideoEntryModel(
        id = id, filePath = filePath, uriString = uriString, dateTimeMs = dateTimeMs,
        durationMs = durationMs, note = note, mood = mood,
        fileSizeBytes = fileSizeBytes, isCompressed = isCompressed,
    )

    private fun VideoEntryModel.toEntity() = VideoEntry(
        id = id, filePath = filePath, uriString = uriString, dateTimeMs = dateTimeMs,
        durationMs = durationMs, note = note, mood = mood,
        fileSizeBytes = fileSizeBytes, isCompressed = isCompressed,
    )
}
