package com.tlapz.videojournal.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM video_entries ORDER BY dateTimeMs DESC")
    fun getAllEntries(): Flow<List<VideoEntry>>

    @Query("SELECT * FROM video_entries WHERE id = :id")
    suspend fun getById(id: String): VideoEntry?

    @Query("""
        SELECT * FROM video_entries
        WHERE dateTimeMs >= :startMs AND dateTimeMs < :endMs
        ORDER BY dateTimeMs DESC
    """)
    fun getByDateRange(startMs: Long, endMs: Long): Flow<List<VideoEntry>>

    @Query("SELECT DISTINCT dateTimeMs FROM video_entries WHERE dateTimeMs >= :startMs AND dateTimeMs < :endMs")
    fun getTimestampsInRange(startMs: Long, endMs: Long): Flow<List<Long>>

    @Query("SELECT id FROM video_entries")
    suspend fun getAllIds(): List<String>

    @Upsert
    suspend fun upsert(entry: VideoEntry)

    @Upsert
    suspend fun upsertAll(entries: List<VideoEntry>)

    @Query("DELETE FROM video_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM video_entries WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE video_entries SET note = :note WHERE id = :id")
    suspend fun updateNote(id: String, note: String?)

    @Query("UPDATE video_entries SET mood = :mood WHERE id = :id")
    suspend fun updateMood(id: String, mood: String?)

    @Query("UPDATE video_entries SET durationMs = :durationMs WHERE id = :id")
    suspend fun updateDuration(id: String, durationMs: Long)

    @Query("UPDATE video_entries SET uriString = :newUri, fileSizeBytes = :newSize, isCompressed = 1 WHERE id = :id")
    suspend fun markCompressed(id: String, newUri: String, newSize: Long)

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM video_entries")
    suspend fun getTotalSize(): Long

    @Query("SELECT COUNT(*) FROM video_entries")
    suspend fun getEntryCount(): Int
}
