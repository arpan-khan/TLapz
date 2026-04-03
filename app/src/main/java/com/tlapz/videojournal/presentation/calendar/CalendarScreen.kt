package com.tlapz.videojournal.presentation.calendar

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tlapz.videojournal.presentation.theme.CalendarDayShape
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onDaySelected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            // Month navigation header
            MonthNavigationHeader(
                yearMonth = uiState.currentMonth,
                onPrev = { viewModel.navigatePrevMonth() },
                onNext = { viewModel.navigateNextMonth() },
            )

            Spacer(Modifier.height(8.dp))

            // Day-of-week labels
            DayOfWeekRow()

            Spacer(Modifier.height(4.dp))

            // Calendar grid
            CalendarGrid(
                yearMonth = uiState.currentMonth,
                datesWithEntries = uiState.datesWithEntries,
                onDayClick = { date -> onDaySelected(date.toString()) },
            )

            if (uiState.datesWithEntries.isEmpty() && !uiState.isLoading) {
                Spacer(Modifier.height(40.dp))
                Text(
                    text = "No entries this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MonthNavigationHeader(
    yearMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
        }
        Text(
            text = yearMonth.format(formatter),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun DayOfWeekRow() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val days = DayOfWeek.values()
        days.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    datesWithEntries: Set<LocalDate>,
    onDayClick: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val firstDayOfMonth = yearMonth.atDay(1)
    // ISO: Monday=1, so offset = dayOfWeek.value - 1 (0 = Mon, 6 = Sun)
    val startOffset = (firstDayOfMonth.dayOfWeek.value - 1).coerceIn(0, 6)
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1
                    if (dayNumber < 1 || dayNumber > daysInMonth) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val date = yearMonth.atDay(dayNumber)
                        val hasEntry = date in datesWithEntries
                        val isToday = date == today
                        DayCell(
                            day = dayNumber,
                            hasEntry = hasEntry,
                            isToday = isToday,
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    hasEntry: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = when {
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        hasEntry -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)
    }
    val textColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        hasEntry -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CalendarDayShape)
            .background(bgColor)
            .clickable(enabled = hasEntry) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isToday || hasEntry) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp,
                ),
                color = textColor,
            )
            if (hasEntry) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}
