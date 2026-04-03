package com.tlapz.videojournal.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val TLapzShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

// Custom shapes for specific UI elements
val VideoCardShape = RoundedCornerShape(12.dp)
val ThumbnailShape = RoundedCornerShape(8.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val CalendarDayShape = RoundedCornerShape(8.dp)
val ChipShape = RoundedCornerShape(50.dp)
val RecordButtonShape = RoundedCornerShape(50.dp)
