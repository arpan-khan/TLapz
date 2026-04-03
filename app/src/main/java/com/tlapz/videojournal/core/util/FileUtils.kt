package com.tlapz.videojournal.core.util

import android.content.ContentResolver
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object FileUtils {

    /**
     * Traverses a SAF DocumentFile tree and yields all .mp4 files recursively.
     */
    fun collectMp4Files(root: DocumentFile): List<DocumentFile> {
        val results = mutableListOf<DocumentFile>()
        collectRecursively(root, results)
        return results
    }

    private fun collectRecursively(dir: DocumentFile, out: MutableList<DocumentFile>) {
        if (!dir.isDirectory) return
        dir.listFiles().forEach { child ->
            when {
                child.isDirectory -> collectRecursively(child, out)
                child.isFile && child.name?.endsWith(Constants.VIDEO_EXTENSION, ignoreCase = true) == true -> {
                    out.add(child)
                }
            }
        }
    }

    /**
     * Gets or creates a subdirectory under a parent DocumentFile.
     */
    fun getOrCreateDirectory(parent: DocumentFile, name: String): DocumentFile? {
        return parent.findFile(name)?.takeIf { it.isDirectory }
            ?: parent.createDirectory(name)
    }

    /**
     * Generates a stable ID for a video entry from its URI string (use SHA-1 hex short form).
     */
    fun generateEntryId(uriString: String): String {
        return uriString.hashCode().toLong().and(0xFFFFFFFFL).toString(16).padStart(8, '0')
    }

    /**
     * Returns human-readable display path from a SAF tree URI.
     */
    fun uriToDisplayPath(uri: Uri): String {
        return uri.lastPathSegment
            ?.replace("primary:", "Internal Storage/")
            ?.replace(":", "/")
            ?: uri.toString()
    }

    /**
     * Reads a DocumentFile into a ByteArray (for copying to/from cache).
     */
    fun readDocumentFile(resolver: ContentResolver, uri: Uri): ByteArray? {
        return try {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Writes bytes to a DocumentFile (used when moving compressed vids back to SAF).
     */
    fun writeDocumentFile(resolver: ContentResolver, uri: Uri, data: ByteArray): Boolean {
        return try {
            resolver.openOutputStream(uri, "wt")?.use { it.write(data) }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deletes a DocumentFile safely, returns true if deleted or already missing.
     */
    fun deleteDocumentFile(file: DocumentFile?): Boolean {
        if (file == null || !file.exists()) return true
        return try { file.delete() } catch (e: Exception) { false }
    }
}
