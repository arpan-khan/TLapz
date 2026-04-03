package com.tlapz.videojournal.domain.model

sealed class SyncResult {
    data class Success(val added: Int, val removed: Int, val unchanged: Int) : SyncResult()
    data class Error(val message: String, val cause: Throwable? = null) : SyncResult()
    object NoFolderSet : SyncResult()
}
