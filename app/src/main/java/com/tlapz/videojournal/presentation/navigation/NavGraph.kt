package com.tlapz.videojournal.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tlapz.videojournal.presentation.camera.CameraScreen
import com.tlapz.videojournal.presentation.calendar.CalendarScreen
import com.tlapz.videojournal.presentation.detail.EntryDetailScreen
import com.tlapz.videojournal.presentation.home.HomeScreen
import com.tlapz.videojournal.presentation.processing.ProcessingScreen
import com.tlapz.videojournal.presentation.settings.SettingsScreen
import com.tlapz.videojournal.presentation.setup.FolderSetupScreen

private const val NAV_ANIM_DURATION = 350

@Composable
fun TLapzNavGraph(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(NAV_ANIM_DURATION))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(NAV_ANIM_DURATION))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(NAV_ANIM_DURATION))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(NAV_ANIM_DURATION))
        },
    ) {
        composable(Screen.FolderSetup.route) {
            FolderSetupScreen(onFolderSelected = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.FolderSetup.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onOpenCamera = { navController.navigate(Screen.Camera.route) },
                onOpenEntry = { id -> navController.navigate(Screen.EntryDetail.createRoute(id)) },
                onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onDaySelected = { dateStr ->
                    navController.navigate(Screen.FilteredTimeline.createRoute(dateStr))
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() },
                onVideoSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.EntryDetail.route,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: return@composable
            EntryDetailScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProcessing = { mode, startMs, endMs ->
                    navController.navigate(Screen.Processing.createRoute(mode, startMs, endMs))
                },
            )
        }

        composable(
            route = "processing?mode={mode}&startMs={startMs}&endMs={endMs}",
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType; defaultValue = "compress" },
                navArgument("startMs") { type = NavType.LongType; defaultValue = 0L },
                navArgument("endMs") { type = NavType.LongType; defaultValue = Long.MAX_VALUE },
            ),
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "compress"
            val startMs = backStackEntry.arguments?.getLong("startMs") ?: 0L
            val endMs = backStackEntry.arguments?.getLong("endMs") ?: Long.MAX_VALUE
            ProcessingScreen(
                mode = mode,
                startMs = startMs,
                endMs = endMs,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.FilteredTimeline.route,
            arguments = listOf(navArgument("dateString") { type = NavType.StringType }),
        ) { backStackEntry ->
            val dateStr = backStackEntry.arguments?.getString("dateString") ?: return@composable
            HomeScreen(
                filterDate = dateStr,
                onOpenCamera = { navController.navigate(Screen.Camera.route) },
                onOpenEntry = { id -> navController.navigate(Screen.EntryDetail.createRoute(id)) },
                onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            )
        }
    }
}
