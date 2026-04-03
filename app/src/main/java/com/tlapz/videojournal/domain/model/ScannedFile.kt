package com.tlapz.videojournal.domain.model

/** Represents a raw video file discovered during folder scanning. */
data class ScannedFile(
    val id: String,
    val uriString: String,
    val displayName: String,
    val dateTimeMs: Long,
    val fileSizeBytes: Long,
    val tag: String?,
)
