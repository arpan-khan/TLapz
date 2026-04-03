package com.tlapz.videojournal.domain.model

data class VideoEntryModel(
    val id: String,
    val filePath: String,
    val uriString: String,
    val dateTimeMs: Long,
    val durationMs: Long?,
    val note: String?,
    val mood: String?,
    val fileSizeBytes: Long,
    val isCompressed: Boolean,
)
