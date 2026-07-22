package com.makstuff.minimalistcaloriecounter

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makstuff.minimalistcaloriecounter.classes.MealImportLaunchRequest
import com.makstuff.minimalistcaloriecounter.classes.resolveMealImportLaunch
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import com.makstuff.minimalistcaloriecounter.ui.theme.DarkTheme
import com.makstuff.minimalistcaloriecounter.ui.theme.LocalTheme
import com.makstuff.minimalistcaloriecounter.widget.WidgetLaunchRequest
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {
    private var automationStarted = false
    private var pendingWidgetLaunchRequest by mutableStateOf<WidgetLaunchRequest?>(null)
    private var pendingMealImportLaunchRequest by mutableStateOf<MealImportLaunchRequest?>(null)
    private var fastExternalLaunch by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleExternalIntent(intent)
        
        enableEdgeToEdge()
        
        setContent {
            val viewModel: AppViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(viewModel, fastExternalLaunch) {
                if (fastExternalLaunch) delay(1_500)
                startDebugAutomation(viewModel)
            }
            LaunchedEffect(pendingWidgetLaunchRequest) {
                val request = pendingWidgetLaunchRequest
                if (request != null) {
                    viewModel.openAddMealFromWidget(
                        date = request.date,
                        targetRoute = request.targetRoute,
                        openDrawer = request.openAddMeal,
                    )
                    pendingWidgetLaunchRequest = null
                }
            }
            LaunchedEffect(pendingMealImportLaunchRequest) {
                val request = pendingMealImportLaunchRequest
                if (request != null) {
                    viewModel.openMealImport(request.import, request.targetRoute, request.openDrawer)
                    if (!request.requiresConfirmation) {
                        viewModel.quickImportCommit(applicationContext)
                    }
                    pendingMealImportLaunchRequest = null
                }
            }
            
            splashScreen.setKeepOnScreenCondition {
                uiState.loading && !fastExternalLaunch
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
                    App(viewModel, uiState, fastWidgetLaunch = fastExternalLaunch)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalIntent(intent)
    }

    /**
     * Resolves widget and meal-import intents in one place for cold and warm launches. Invalid
     * meal payloads stay recoverable and visible to the user instead of terminating the Activity.
     */
    private fun handleExternalIntent(intent: Intent) {
        pendingWidgetLaunchRequest = WidgetLaunchRequest.fromIntent(intent)
        val mealImport = resolveMealImportLaunch { MealImportLaunchRequest.fromIntent(intent) }
        pendingMealImportLaunchRequest = mealImport.request
        fastExternalLaunch = fastExternalLaunch ||
            pendingWidgetLaunchRequest != null ||
            pendingMealImportLaunchRequest != null
        if (mealImport.errorMessage != null) {
            Toast.makeText(
                this,
                getString(R.string.meal_import_failed),
                Toast.LENGTH_LONG,
            ).show()
        }
        if (pendingWidgetLaunchRequest != null || pendingMealImportLaunchRequest != null || mealImport.errorMessage != null) {
            setIntent(Intent(this, MainActivity::class.java))
        }
    }

    private fun startDebugAutomation(viewModel: AppViewModel) {
        if (automationStarted || !BuildConfig.DEBUG) return
        automationStarted = true
        runCatching {
            Class.forName("com.makstuff.minimalistcaloriecounter.automation.AutomationBootstrap")
                .getMethod("start", android.content.Context::class.java, AppViewModel::class.java)
                .invoke(null, applicationContext, viewModel)
        }
    }
}
