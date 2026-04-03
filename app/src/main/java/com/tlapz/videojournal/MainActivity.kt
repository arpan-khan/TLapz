package com.tlapz.videojournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.tlapz.videojournal.domain.model.AppTheme
import com.tlapz.videojournal.presentation.navigation.Screen
import com.tlapz.videojournal.presentation.navigation.TLapzNavGraph
import com.tlapz.videojournal.presentation.settings.SettingsViewModel
import com.tlapz.videojournal.presentation.theme.TLapzTheme
import com.tlapz.videojournal.data.storage.StorageManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var storageManager: StorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val uiState by settingsViewModel.uiState.collectAsState()
            val settings = uiState.settings

            val systemDark = isSystemInDarkTheme()
            val isDark = when (settings.theme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> systemDark
            }

            TLapzTheme(
                darkTheme = isDark,
                dynamicColor = settings.dynamicColor,
            ) {
                val navController = rememberNavController()
                val startDestination = if (storageManager.isFolderSelected()) {
                    Screen.Home.route
                } else {
                    Screen.FolderSetup.route
                }

                TLapzNavGraph(
                    navController = navController,
                    startDestination = startDestination,
                )
            }
        }
    }
}
