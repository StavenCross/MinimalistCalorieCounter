package com.makstuff.minimalistcaloriecounter

import android.content.Context
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AppViewModelPersistenceActions(
    private val env: AppViewModelEnvironment,
    private val viewModel: AppViewModel,
) {
    fun updateDatabaseFromCsv(context: Context) {
        env.uiState.database.clear()
        env.uiState.database.addAll(env.csvStore.readDatabase(context))
        viewModel.databaseSortByName()
        viewModel.databaseQuickselectUpdate()
        viewModel.databaseLetterReset()
    }

    fun resetDatabaseCsv(overwriteIfExists: Boolean, context: Context) {
        env.csvStore.resetFromRawResource(context, "database.csv", R.raw.database, overwriteIfExists)
    }

    fun resetArchiveCsv(overwriteIfExists: Boolean, context: Context) {
        env.csvStore.resetFromRawResource(context, "archive.csv", R.raw.archive, overwriteIfExists)
    }

    fun resetDayCsv(overwriteIfExists: Boolean, context: Context) {
        env.csvStore.resetFromRawResource(context, "day.csv", R.raw.day, overwriteIfExists)
    }

    fun resetGoalsCsv(overwriteIfExists: Boolean, context: Context) {
        env.csvStore.resetGoals(context, overwriteIfExists)
    }

    fun writeDatabase(context: Context) {
        env.csvStore.writeDatabase(context, env.uiState.database)
    }

    fun writeDay(context: Context) {
        env.csvStore.writeDay(context, env.uiState.day)
    }

    fun updateDayFromCsv(context: Context) {
        env.state.update { currentState ->
            currentState.copy(day = env.csvStore.readDay(context))
        }
    }

    fun updateGoalsFromCsv(context: Context) {
        env.csvStore.readGoals(context)?.let { goals ->
            env.state.update { currentState ->
                currentState.copy(goals = goals)
            }
        }
    }

    fun writeGoals(context: Context) {
        env.csvStore.writeGoals(context, env.uiState.goals)
    }

    fun updateOptionsFromFile(context: Context) {
        val options = env.csvStore.readOptions(context) ?: return
        env.state.update { currentState ->
            currentState.copy(themeUserSetting = options.theme)
        }

        env.scope.launch {
            val granted = env.healthConnectManager.hasAllPermissions()
            env.state.update { currentState ->
                currentState.copy(
                    healthConnectSyncEnabled = (options.healthConnectSyncEnabled ?: currentState.healthConnectSyncEnabled) && granted,
                    healthConnectToastsEnabled = (options.healthConnectToastsEnabled ?: currentState.healthConnectToastsEnabled) && granted,
                )
            }
        }
    }

    fun writeOptions(context: Context) {
        env.csvStore.writeOptions(
            context = context,
            theme = env.uiState.themeUserSetting,
            syncEnabled = env.uiState.healthConnectSyncEnabled,
            toastsEnabled = env.uiState.healthConnectToastsEnabled,
        )
    }

    fun resetOptions(overwriteIfExists: Boolean, context: Context) {
        env.csvStore.resetOptions(context, overwriteIfExists)
    }

    fun writeArchive(context: Context) {
        env.csvStore.writeArchive(context, env.uiState.archive)
    }

    fun updateArchiveFromCsv(context: Context) {
        env.state.update { currentState ->
            currentState.copy(archive = env.csvStore.readArchive(context))
        }
    }
}
