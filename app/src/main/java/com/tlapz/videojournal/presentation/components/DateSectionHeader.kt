package com.tlapz.videojournal.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DateSectionHeader(
    date: LocalDate,
    entryCount: Int,
    modifier: Modifier = Modifier,
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    val yearFormatter = DateTimeFormatter.ofPattern("yyyy", Locale.getDefault())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = date.format(formatter),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
            val year = date.format(yearFormatter)
            val currentYear = LocalDate.now().year.toString()
            if (year != currentYear) {
                Text(
                    text = year,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "$entryCount ${if (entryCount == 1) "entry" else "entries"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
