package com.tlapz.videojournal.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    private val filenameFormatter = DateTimeFormatter.ofPattern(
        Constants.FILENAME_PATTERN, Locale.US
    )
    private val filenameRegex = Regex(Constants.FILENAME_REGEX)

    /** Generates a filename stem for a new recording, e.g. "2024-01-15_09-30-00" */
    fun generateFilenameStem(): String {
        return LocalDateTime.now().format(filenameFormatter)
    }

    /** Full filename with extension */
    fun generateFileName(tag: String? = null): String {
        val stem = generateFilenameStem()
        return if (tag.isNullOrBlank()) "$stem${Constants.VIDEO_EXTENSION}"
        else "${stem}_$tag${Constants.VIDEO_EXTENSION}"
    }

    /**
     * Parses a filename like "2024-01-15_09-30-00.mp4" or "2024-01-15_09-30-00_tag.mp4"
     * Returns null if the filename doesn't match the expected pattern.
     */
    fun parseFilename(filename: String): ParsedFilename? {
        val match = filenameRegex.find(filename) ?: return null
        val (year, month, day, hour, minute, second, tag) = match.destructured
        return try {
            val ldt = LocalDateTime.of(
                year.toInt(), month.toInt(), day.toInt(),
                hour.toInt(), minute.toInt(), second.toInt()
            )
            ParsedFilename(
                dateTime = ldt,
                epochMs = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                tag = tag.ifBlank { null }
            )
        } catch (e: Exception) {
            null
        }
    }

    fun epochMsToLocalDateTime(epochMs: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDateTime()

    fun epochMsToLocalDate(epochMs: Long): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()

    fun formatDisplayDate(epochMs: Long): String {
        val ldt = epochMsToLocalDateTime(epochMs)
        return ldt.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault()))
    }

    fun formatDisplayTime(epochMs: Long): String {
        val ldt = epochMsToLocalDateTime(epochMs)
        return ldt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    }

    fun formatDisplayDateTime(epochMs: Long): String {
        return "${formatDisplayDate(epochMs)} at ${formatDisplayTime(epochMs)}"
    }

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format(Locale.US, "%.2f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
            bytes >= 1_024 -> String.format(Locale.US, "%.0f KB", bytes / 1_024.0)
            else -> "$bytes B"
        }
    }

    /** Returns year/month folder names for a given epoch timestamp */
    fun getFolderParts(epochMs: Long): Pair<String, String> {
        val ldt = epochMsToLocalDateTime(epochMs)
        val year = ldt.format(DateTimeFormatter.ofPattern("yyyy"))
        val month = ldt.format(DateTimeFormatter.ofPattern("MM"))
        return Pair(year, month)
    }

    data class ParsedFilename(
        val dateTime: LocalDateTime,
        val epochMs: Long,
        val tag: String?
    )
}
