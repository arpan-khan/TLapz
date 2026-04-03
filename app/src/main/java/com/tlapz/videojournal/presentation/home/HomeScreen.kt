package com.tlapz.videojournal.presentation.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tlapz.videojournal.presentation.components.DateSectionHeader
import com.tlapz.videojournal.presentation.components.EmptyState
import com.tlapz.videojournal.presentation.components.VideoEntryCard
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onOpenCamera: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    filterDate: String? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Apply date filter if navigated from calendar
    LaunchedEffect(filterDate) {
        if (filterDate != null) {
            runCatching { viewModel.setFilterDate(LocalDate.parse(filterDate)) }
        }
    }

    // FAB visibility based on scroll direction
    var fabVisible by remember { mutableStateOf(true) }
    LaunchedEffect(listState.isScrollInProgress) {
        val isScrollingDown = listState.firstVisibleItemScrollOffset > 0 &&
                listState.canScrollForward
        fabVisible = !isScrollingDown || !listState.isScrollInProgress
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = if (filterDate != null) "Journal" else "TLapz",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                actions = {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar")
                    }
                    IconButton(onClick = { viewModel.sync() }) {
                        Icon(Icons.Outlined.Sync, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = scaleIn(tween(200)) + fadeIn(tween(200)),
                exit = scaleOut(tween(200)) + fadeOut(tween(200)),
            ) {
                ExtendedFloatingActionButton(
                    onClick = onOpenCamera,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Record") },
                    containerColor = MaterialTheme.colorScheme.primary,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = { viewModel.sync() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.groups.isEmpty() -> {
                    EmptyState(
                        title = "No videos yet",
                        subtitle = "Tap the Record button to create your first journal entry.",
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp, end = 12.dp,
                            top = 4.dp, bottom = 100.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        uiState.groups.forEach { group ->
                            stickyHeader(key = group.date.toString()) {
                                DateSectionHeader(
                                    date = group.date,
                                    entryCount = group.entries.size,
                                    modifier = Modifier.background(
                                        MaterialTheme.colorScheme.background
                                    ),
                                )
                            }
                            items(
                                items = group.entries,
                                key = { it.id },
                            ) { entry ->
                                VideoEntryCard(
                                    entry = entry,
                                    onClick = { onOpenEntry(entry.id) },
                                    modifier = Modifier.animateItem(),
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Sync result snackbar
    uiState.syncMessage?.let { message ->
        LaunchedEffect(message) {
            // Auto-clear after 2s
            kotlinx.coroutines.delay(2000)
            // Reset via ViewModel — simplified
        }
    }
}
