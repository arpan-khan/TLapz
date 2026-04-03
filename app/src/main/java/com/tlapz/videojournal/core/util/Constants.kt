package com.tlapz.videojournal.core.util

object Constants {

    // Folder structure
    const val DATE_FORMAT_YEAR = "yyyy"
    const val DATE_FORMAT_MONTH = "MM"
    const val FILENAME_PATTERN = "yyyy-MM-dd_HH-mm-ss"
    const val VIDEO_EXTENSION = ".mp4"
    const val FILENAME_REGEX = """^(\d{4})-(\d{2})-(\d{2})_(\d{2})-(\d{2})-(\d{2})(?:_(.+))?\.mp4$"""
    const val MERGED_PREFIX = "merged_"

    // DataStore keys
    const val PREF_FOLDER_URI = "pref_folder_uri"
    const val PREF_THEME = "pref_theme"
    const val PREF_DYNAMIC_COLOR = "pref_dynamic_color"
    const val PREF_RECORDING_QUALITY = "pref_recording_quality"
    const val PREF_COMPRESSION_PROFILE = "pref_compression_profile"
    const val PREF_AUTO_COMPRESS = "pref_auto_compress"
    const val PREF_COMPACT_LAYOUT = "pref_compact_layout"
    const val PREF_DEBUG_LOGGING = "pref_debug_logging"

    // Video quality settings
    const val QUALITY_SD = "SD"      // 480p
    const val QUALITY_HD = "HD"      // 720p

    // Compression profiles
    const val COMPRESSION_STANDARD = "STANDARD"   // 480p ~1.5 Mbps
    const val COMPRESSION_ULTRA = "ULTRA"         // 360p ~800 Kbps
    const val COMPRESSION_EXTREME = "EXTREME"     // 240p ~400 Kbps

    // Bitrate values (bps)
    const val BITRATE_STANDARD = 1_500_000
    const val BITRATE_ULTRA = 800_000
    const val BITRATE_EXTREME = 400_000

    // Resolution heights
    const val HEIGHT_STANDARD = 480
    const val HEIGHT_ULTRA = 360
    const val HEIGHT_EXTREME = 240
    const val HEIGHT_HD = 720

    // Theme values
    const val THEME_DARK = "DARK"
    const val THEME_LIGHT = "LIGHT"
    const val THEME_SYSTEM = "SYSTEM"

    // Mood options
    val MOOD_OPTIONS = listOf("😊", "😐", "😔", "😤", "🤩", "😴", "🤔", "❤️")

    // Database
    const val DATABASE_NAME = "tlapz_video_journal.db"
    const val DATABASE_VERSION = 1
}
