package com.makstuff.minimalistcaloriecounter

import android.content.Context
import java.io.InputStream
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

    fun importDatabaseCsv(context: Context, inputStream: InputStream) {
        env.uiState.database.clear()
        env.uiState.database.addAll(env.csvStore.importDatabase(context, inputStream))
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
        env.scope.launch {
            val goals = runCatching { env.roomStore.readGoals() }.getOrNull()
                ?: env.csvStore.readGoals(context)?.also { csvGoals ->
                    env.launchRoomWrite { writeGoals(csvGoals) }
                }
                ?: return@launch
            env.state.update { currentState ->
                currentState.copy(goals = goals)
            }
        }
    }

    fun updateQuickImportOutboxFromCsv(context: Context) {
        env.scope.launch {
            val roomItems = runCatching { env.roomStore.readQuickImportOutbox() }.getOrDefault(emptyList())
            val items = if (roomItems.isNotEmpty()) {
                roomItems
            } else {
                env.csvStore.readQuickImportOutbox(context).also { csvItems ->
                    env.launchRoomWrite { seedQuickImportOutbox(csvItems) }
                }
            }
            env.state.update { currentState ->
                currentState.copy(quickImportOutbox = items)
            }
        }
    }

    fun writeGoals(context: Context) {
        env.csvStore.writeGoals(context, env.uiState.goals)
        env.launchRoomWrite {
            writeGoals(env.uiState.goals)
        }
    }

    fun updateOptionsFromFile(context: Context) {
        env.scope.launch {
            val options = runCatching { env.roomStore.readOptions() }.getOrNull()
                ?: env.csvStore.readOptions(context)?.also { csvOptions ->
                    env.launchRoomWrite {
                        writeOptions(
                            theme = csvOptions.theme,
                            syncEnabled = csvOptions.healthConnectSyncEnabled ?: false,
                            toastsEnabled = csvOptions.healthConnectToastsEnabled ?: false,
                        )
                    }
                }
                ?: return@launch
            val granted = env.healthConnectManager.hasArchiveSyncPermissions()
            env.state.update { currentState ->
                currentState.copy(
                    themeUserSetting = options.theme,
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
        env.launchRoomWrite {
            writeOptions(
                theme = env.uiState.themeUserSetting,
                syncEnabled = env.uiState.healthConnectSyncEnabled,
                toastsEnabled = env.uiState.healthConnectToastsEnabled,
            )
        }
    }

    fun resetOptions(overwriteIfExists: Boolean, context: Context) {
        env.csvStore.resetOptions(context, overwriteIfExists)
        env.csvStore.readOptions(context)?.let { options ->
            env.launchRoomWrite {
                writeOptions(
                    theme = options.theme,
                    syncEnabled = options.healthConnectSyncEnabled ?: false,
                    toastsEnabled = options.healthConnectToastsEnabled ?: false,
                )
            }
        }
    }

    fun writeArchive(context: Context) {
        env.csvStore.writeArchive(context, env.uiState.archive)
    }

    fun updateArchiveFromCsv(context: Context) {
        env.state.update { currentState ->
            currentState.copy(archive = env.csvStore.readArchive(context))
        }
    }

    fun importArchiveCsv(context: Context, inputStream: InputStream) {
        env.state.update { currentState ->
            currentState.copy(archive = env.csvStore.importArchive(context, inputStream))
        }
    }
}
