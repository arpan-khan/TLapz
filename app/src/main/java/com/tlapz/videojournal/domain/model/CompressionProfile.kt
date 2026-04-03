package com.tlapz.videojournal.domain.model

import com.tlapz.videojournal.core.util.Constants

enum class CompressionProfile(
    val label: String,
    val description: String,
    val height: Int,
    val bitrateBps: Int,
) {
    STANDARD(
        label = "Standard",
        description = "480p · ~1.5 Mbps",
        height = Constants.HEIGHT_STANDARD,
        bitrateBps = Constants.BITRATE_STANDARD,
    ),
    ULTRA(
        label = "Ultra",
        description = "360p · ~800 Kbps",
        height = Constants.HEIGHT_ULTRA,
        bitrateBps = Constants.BITRATE_ULTRA,
    ),
    EXTREME(
        label = "Extreme",
        description = "240p · ~400 Kbps",
        height = Constants.HEIGHT_EXTREME,
        bitrateBps = Constants.BITRATE_EXTREME,
    ),
}
