package com.tlapz.videojournal.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_entries")
data class VideoEntry(
    @PrimaryKey val id: String,
    val filePath: String,
    val uriString: String,
    val dateTimeMs: Long,
    val durationMs: Long?,
    val note: String?,
    val mood: String?,
    val fileSizeBytes: Long,
    val isCompressed: Boolean,
)
