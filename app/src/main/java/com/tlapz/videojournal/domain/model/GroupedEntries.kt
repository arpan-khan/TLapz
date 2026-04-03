package com.tlapz.videojournal.domain.model

import java.time.LocalDate

data class GroupedEntries(
    val date: LocalDate,
    val entries: List<VideoEntryModel>,
)
