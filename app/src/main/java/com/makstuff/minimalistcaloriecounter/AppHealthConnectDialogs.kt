package com.makstuff.minimalistcaloriecounter

import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.makstuff.minimalistcaloriecounter.ui.reused.ButtonText

@Composable
fun HealthConnectSyncDialogs(
    uiState: AppUiState,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    onDismissError: () -> Unit,
) {
    val context = LocalContext.current
    if (uiState.healthConnectSyncProgress != null) {
        val window = context.findActivity()?.window
        DisposableEffect(Unit) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
        AlertDialog(
            onDismissRequest = { },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = {
                Text(
                    text = stringResource(R.string.please_wait),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(
                            R.string.syncing_health_connect,
                            uiState.healthConnectSyncCurrentCount,
                            uiState.healthConnectSyncTotalCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    LinearProgressIndicator(
                        progress = { uiState.healthConnectSyncProgress },
                        modifier = Modifier.fillMaxWidth(),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
            },
            confirmButton = {
                if (uiState.healthConnectSyncProgress >= 1f) {
                    ButtonText(text = stringResource(R.string.button_finish), onClick = onFinish)
                } else {
                    ButtonText(text = stringResource(R.string.button_cancel), onClick = onCancel)
                }
            },
        )
    }
    if (uiState.healthConnectSyncMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
            confirmButton = {
                ButtonText(text = stringResource(R.string.button_understood), onClick = onDismissError)
            },
            title = { Text(stringResource(R.string.confirmation)) },
            text = { Text(stringResource(R.string.health_connect_sync_error, uiState.healthConnectSyncMessage)) },
        )
    }
}
