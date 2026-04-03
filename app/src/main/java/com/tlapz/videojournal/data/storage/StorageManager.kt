package com.tlapz.videojournal.data.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.tlapz.videojournal.core.util.Constants
import com.tlapz.videojournal.core.util.DateUtils
import com.tlapz.videojournal.core.util.FileUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Gets the persisted root folder URI, or null if not set. */
    fun getRootUri(): Uri? {
        return context.contentResolver.persistedUriPermissions
            .firstOrNull { it.isReadPermission && it.isWritePermission }
            ?.uri
    }

    /** Gets the root folder as a DocumentFile, or null. */
    fun getRootDocumentFile(): DocumentFile? {
        val uri = getRootUri() ?: return null
        return DocumentFile.fromTreeUri(context, uri)
    }

    /** Persists read+write access to the user-selected folder URI. */
    fun persistFolderUri(uri: Uri) {
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
    }

    /** Releases all persisted URI permissions (folder unlink). */
    fun releaseFolderUri() {
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.persistedUriPermissions.forEach { perm ->
            try {
                context.contentResolver.releasePersistableUriPermission(perm.uri, flags)
            } catch (_: Exception) {}
        }
    }

    /**
     * Returns (or creates) the output DocumentFile for a new recording.
     * Path: root/YYYY/MM/YYYY-MM-DD_HH-MM-SS.mp4
     */
    fun createVideoOutputFile(timestampMs: Long = System.currentTimeMillis()): DocumentFile? {
        val root = getRootDocumentFile() ?: return null
        val (year, month) = DateUtils.getFolderParts(timestampMs)
        val yearDir = FileUtils.getOrCreateDirectory(root, year) ?: return null
        val monthDir = FileUtils.getOrCreateDirectory(yearDir, month) ?: return null
        val filename = DateUtils.generateFileName()
        return monthDir.createFile("video/mp4", filename.removeSuffix(".mp4"))
    }

    /** Resolves a URI string back to a DocumentFile. */
    fun documentFileFromUriString(uriString: String): DocumentFile? {
        return try {
            val uri = Uri.parse(uriString)
            DocumentFile.fromSingleUri(context, uri)
        } catch (_: Exception) { null }
    }

    /** Opens a ParcelFileDescriptor for writing to a DocumentFile URI. */
    fun openWriteDescriptor(uri: Uri) =
        context.contentResolver.openFileDescriptor(uri, "rw")

    /** Deletes a file by its URI string. */
    fun deleteFile(uriString: String): Boolean {
        val doc = documentFileFromUriString(uriString) ?: return true
        return FileUtils.deleteDocumentFile(doc)
    }

    /** Returns a human-readable display path for the root folder. */
    fun getDisplayPath(): String {
        val uri = getRootUri() ?: return "Not selected"
        return FileUtils.uriToDisplayPath(uri)
    }

    fun isFolderSelected(): Boolean = getRootUri() != null

    /** Creates a temp output location in the app cache for compression/merge output. */
    fun createCacheFile(name: String): java.io.File {
        val dir = java.io.File(context.cacheDir, "processing").also { it.mkdirs() }
        return java.io.File(dir, name)
    }

    /** Opens an input stream for any SAF URI. */
    fun openInputStream(uri: Uri) = context.contentResolver.openInputStream(uri)

    /** Opens an output stream for any SAF URI (truncate mode). */
    fun openOutputStream(uri: Uri) = context.contentResolver.openOutputStream(uri, "wt")
}
