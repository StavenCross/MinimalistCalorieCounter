package com.makstuff.minimalistcaloriecounter

import android.app.Application
import android.content.Context
import com.makstuff.minimalistcaloriecounter.health.HealthConnectManager
import com.makstuff.minimalistcaloriecounter.persistence.AppCsvStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

internal class AppViewModelEnvironment(
    val application: Application,
    val state: MutableStateFlow<AppUiState>,
    val scope: CoroutineScope,
    val healthConnectManager: HealthConnectManager,
    val csvStore: AppCsvStore,
) {
    val context: Context
        get() = application.applicationContext

    val uiState: AppUiState
        get() = state.value
}
