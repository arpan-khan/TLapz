package com.tlapz.videojournal.presentation.navigation

sealed class Screen(val route: String) {
    object FolderSetup : Screen("folder_setup")
    object Home : Screen("home")
    object Calendar : Screen("calendar")
    object Camera : Screen("camera")
    object Settings : Screen("settings")
    object Processing : Screen("processing?mode={mode}&startMs={startMs}&endMs={endMs}") {
        fun createRoute(mode: String, startMs: Long, endMs: Long) =
            "processing?mode=$mode&startMs=$startMs&endMs=$endMs"
    }
    object EntryDetail : Screen("entry_detail/{entryId}") {
        fun createRoute(entryId: String) = "entry_detail/$entryId"
    }
    object FilteredTimeline : Screen("filtered_timeline/{dateString}") {
        fun createRoute(dateString: String) = "filtered_timeline/$dateString"
    }
}
