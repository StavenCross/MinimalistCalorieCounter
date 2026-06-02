package com.makstuff.minimalistcaloriecounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import com.makstuff.minimalistcaloriecounter.ui.theme.DarkTheme
import com.makstuff.minimalistcaloriecounter.ui.theme.LocalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            val viewModel: AppViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            
            splashScreen.setKeepOnScreenCondition {
                uiState.loading
            }

            val darkTheme = when (uiState.themeUserSetting) {
                AppTheme.MODE_AUTO -> DarkTheme(isSystemInDarkTheme())
                AppTheme.MODE_DAY -> DarkTheme(false)
                AppTheme.MODE_NIGHT -> DarkTheme(true)
            }

            LaunchedEffect(darkTheme.isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme.isDark },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { darkTheme.isDark }
                )
            }

            CompositionLocalProvider(LocalTheme provides darkTheme) {
                AppTheme(useDarkTheme = LocalTheme.current.isDark) {
                    App(viewModel, uiState)
                }
            }
        }
    }
}
