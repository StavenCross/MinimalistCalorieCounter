package com.makstuff.minimalistcaloriecounter

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.health.connect.client.HealthConnectClient
import com.makstuff.minimalistcaloriecounter.health.HealthConnectManager
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppRoutes
import com.makstuff.minimalistcaloriecounter.ui.reused.ButtonText

@Composable
fun AppConfirmationDialogs(
    uiState: AppUiState,
    viewModel: AppViewModel,
    context: Context,
    fileLaunchers: AppFileLaunchers,
    healthConnectManager: HealthConnectManager,
    healthConnectRequestPermissionLauncher: ManagedActivityResultLauncher<Set<String>, Set<String>>,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    when {
        uiState.alertDialogArchiveReset -> ArchiveResetDialog(viewModel, context)
        uiState.alertDialogDatabaseReset -> DatabaseResetDialog(viewModel, context)
        uiState.alertDialogArchiveImport -> ArchiveImportDialog(viewModel, fileLaunchers)
        uiState.alertDialogDatabaseImport -> DatabaseImportDialog(viewModel, fileLaunchers)
        uiState.alertDialogHealthConnectSync -> HealthConnectSyncDialog(viewModel)
        uiState.alertDialogHealthConnectPermissions -> HealthConnectPermissionsDialog(
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            healthConnectManager = healthConnectManager,
            healthConnectRequestPermissionLauncher = healthConnectRequestPermissionLauncher,
        )
        uiState.alertDialogHealthConnectActivation -> InformationDialog(
            text = stringResource(R.string.dialog_health_connect_activation),
            onDismiss = { viewModel.setAlertDialogHealthConnectActivation(false) },
        )
        uiState.alertDialogHealthConnectToasts -> InformationDialog(
            text = stringResource(R.string.dialog_health_connect_toasts),
            onDismiss = { viewModel.setAlertDialogHealthConnectToasts(false) },
        )
        uiState.alertDialogDayReset -> DayResetDialog(viewModel, context)
        uiState.alertDialogArchiveDelete -> ArchiveDeleteDialog(uiState, viewModel, context, onNavigate)
        uiState.alertDialogDatabaseDelete -> DatabaseDeleteDialog(uiState, viewModel, context, onNavigateBack)
    }
}

@Composable
private fun ArchiveResetDialog(viewModel: AppViewModel, context: Context) {
    ConfirmationDialog(
        text = stringResource(R.string.dialog_archive_clear),
        onDismiss = { viewModel.setAlertDialogArchiveReset(false) },
        onConfirm = {
            viewModel.archiveResetCSV(true, context)
            viewModel.archiveUpdateFromCSV(context)
            viewModel.setAlertDialogArchiveReset(false)
        },
    )
}

@Composable
private fun DatabaseResetDialog(viewModel: AppViewModel, context: Context) {
    ConfirmationDialog(
        text = stringResource(R.string.dialog_database_reset),
        onDismiss = { viewModel.setAlertDialogDatabaseReset(false) },
        onConfirm = {
            viewModel.databaseResetCSV(true, context)
            viewModel.databaseUpdateFromCSV(context)
            viewModel.setAlertDialogDatabaseReset(false)
        },
    )
}

@Composable
private fun ArchiveImportDialog(viewModel: AppViewModel, fileLaunchers: AppFileLaunchers) {
    ConfirmationDialog(
        text = stringResource(R.string.dialog_archive_import),
        onDismiss = { viewModel.setAlertDialogArchiveImport(false) },
        onConfirm = {
            fileLaunchers.archiveImporter.launch(arrayOf("text/comma-separated-values"))
            viewModel.setAlertDialogArchiveImport(false)
        },
    )
}

@Composable
private fun DatabaseImportDialog(viewModel: AppViewModel, fileLaunchers: AppFileLaunchers) {
    ConfirmationDialog(
        text = stringResource(R.string.dialog_database_import),
        onDismiss = { viewModel.setAlertDialogDatabaseImport(false) },
        onConfirm = {
            fileLaunchers.databaseImporter.launch(arrayOf("text/comma-separated-values"))
            viewModel.setAlertDialogDatabaseImport(false)
        },
    )
}

@Composable
private fun HealthConnectSyncDialog(viewModel: AppViewModel) {
    ConfirmationDialog(
        text = stringResource(R.string.dialog_health_connect_sync),
        onDismiss = { viewModel.setAlertDialogHealthConnectSync(false) },
        onConfirm = {
            viewModel.syncHealthConnect()
            viewModel.setAlertDialogHealthConnectSync(false)
        },
    )
}

@Composable
private fun HealthConnectPermissionsDialog(
    uiState: AppUiState,
    viewModel: AppViewModel,
    context: Context,
    healthConnectManager: HealthConnectManager,
    healthConnectRequestPermissionLauncher: ManagedActivityResultLauncher<Set<String>, Set<String>>,
) {
    ConfirmationDialog(
        text = stringResource(R.string.dialog_health_connect_disclosure),
        onDismiss = { viewModel.setAlertDialogHealthConnectPermissions(false) },
        onConfirm = {
            if (uiState.healthConnectAnyPermissionsGranted) {
                try {
                    context.startActivity(
                        Intent("androidx.health.connect.action.MANAGE_HEALTH_PERMISSIONS").apply {
                            putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                        },
                    )
                } catch (_: Exception) {
                    context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
                }
            } else {
                healthConnectRequestPermissionLauncher.launch(healthConnectManager.permissions)
            }
            viewModel.setAlertDialogHealthConnectPermissions(false)
        },
    )
}

@Composable
private fun DayResetDialog(viewModel: AppViewModel, context: Context) {
    ConfirmationDialog(
        text = stringResource(R.string.dialog_reset_day),
        onDismiss = { viewModel.setAlertDialogDayReset(false) },
        onConfirm = {
            viewModel.dayReset(context)
            viewModel.setAlertDialogDayReset(false)
        },
    )
}

@Composable
private fun ArchiveDeleteDialog(
    uiState: AppUiState,
    viewModel: AppViewModel,
    context: Context,
    onNavigate: (String) -> Unit,
) {
    ConfirmationDialog(
        text = stringResource(R.string.dialog_archive_delete),
        onDismiss = { viewModel.setAlertDialogArchiveDelete(false) },
        onConfirm = {
            if (uiState.indexArchiveDelete != -1) {
                viewModel.archiveDeleteEntry(uiState.indexArchiveDelete, context)
                onNavigate(AppRoutes.ARCHIVE_HOME)
            }
            viewModel.setAlertDialogArchiveDelete(false)
        },
    )
}

@Composable
private fun DatabaseDeleteDialog(
    uiState: AppUiState,
    viewModel: AppViewModel,
    context: Context,
    onNavigateBack: () -> Unit,
) {
    ConfirmationDialog(
        text = stringResource(R.string.dialog_database_delete),
        onDismiss = { viewModel.setAlertDialogDatabaseDelete(false) },
        onConfirm = {
            if (uiState.indexDatabaseDelete != -1) {
                viewModel.databaseDeleteEntry(uiState.indexDatabaseDelete, true, context)
                onNavigateBack()
            }
            viewModel.setAlertDialogDatabaseDelete(false)
        },
    )
}

@Composable
private fun ConfirmationDialog(
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { ButtonText(text = stringResource(R.string.button_continue), onClick = onConfirm) },
        dismissButton = { ButtonText(text = stringResource(R.string.button_cancel), onClick = onDismiss) },
        text = { Text(text) },
        title = { Text(stringResource(R.string.confirmation)) },
    )
}

@Composable
private fun InformationDialog(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
        confirmButton = { ButtonText(text = stringResource(R.string.button_understood), onClick = onDismiss) },
        text = { Text(text) },
        title = { Text(stringResource(R.string.information)) },
    )
}
