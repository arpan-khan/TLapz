package com.tlapz.videojournal.domain.model

data class ProcessingProgress(
    val currentIndex: Int,
    val totalCount: Int,
    val currentFileName: String,
    val progressPercent: Float,         // 0f–1f for current file
    val isComplete: Boolean = false,
    val errorMessage: String? = null,
)
