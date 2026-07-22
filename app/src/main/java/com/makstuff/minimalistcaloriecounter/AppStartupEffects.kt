package com.makstuff.minimalistcaloriecounter

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.makstuff.minimalistcaloriecounter.widget.NutritionWidgetRefreshScheduler
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
fun AppStartupEffects(
    uiState: AppUiState,
    viewModel: AppViewModel,
    fastWidgetLaunch: Boolean = false,
    onNavigate: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(uiState.automationRouteRequest) {
        val route = uiState.automationRouteRequest ?: return@LaunchedEffect
        onNavigate(route)
        viewModel.clearNavigationRequest(route)
    }

    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateHealthConnectPermissionsStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    LaunchedEffect(Unit) {
        NutritionWidgetRefreshScheduler.scheduleNext(context)
        if (fastWidgetLaunch) {
            viewModel.setLoadingToFalse()
            delay(350.milliseconds)
        }
        viewModel.updateHealthConnectPermissionsStatus()
        viewModel.databaseResetCSV(false, context)
        viewModel.archiveResetCSV(false, context)
        viewModel.dayResetCSV(false, context)
        viewModel.optionsResetFile(false, context)
        viewModel.goalsResetCSV(false, context)
        loadCsvBackedState(viewModel, context)
        if (!fastWidgetLaunch) delay(1000.milliseconds)
        viewModel.setLoadingToFalse()
    }
}

private fun loadCsvBackedState(viewModel: AppViewModel, context: android.content.Context) {
    runCsvLoad(context, context.getString(R.string.options)) { viewModel.optionsUpdateFromFile(context) }
    runCsvLoad(context, context.getString(R.string.archive)) { viewModel.archiveUpdateFromCSV(context) }
    runCsvLoad(context, context.getString(R.string.database)) { viewModel.databaseUpdateFromCSV(context) }
    runCsvLoad(context, context.getString(R.string.day)) { viewModel.dayUpdateFromCSV(context) }
    runCsvLoad(context, "Goals") { viewModel.goalsUpdateFromCSV(context) }
    runCsvLoad(context, "Add Meal outbox") { viewModel.quickImportOutboxUpdateFromCSV(context) }
}

private fun runCsvLoad(context: android.content.Context, label: String, block: () -> Unit) {
    try {
        block()
    } catch (e: IllegalStateException) {
        Toast.makeText(context, "$label CSV: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
