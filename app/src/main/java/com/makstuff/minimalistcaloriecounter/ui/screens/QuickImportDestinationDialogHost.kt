package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.AppViewModel
import com.makstuff.minimalistcaloriecounter.ui.reused.SheetTitle

private val AccentDatabase = Color(0xFFCE93D8)
private val AccentDay = Color(0xFFFFB74D)
private val AccentHealth = Color(0xFF4DD0E1)

/**
 * Owns the Add Meal destination dialog.
 *
 * The dialog changes local destination preferences immediately through the view model. Keeping it
 * outside the app shell lets the route host stay focused on navigation instead of feature settings.
 */
@Composable
fun QuickImportDestinationDialogHost(
    uiState: AppUiState,
    viewModel: AppViewModel,
) {
    if (uiState.quickImportSettingsVisible) {
        DestinationDialog(
            addDatabase = uiState.quickImportAddFoodsToDatabase,
            addDay = uiState.quickImportAddFoodsToDay,
            writeHealthConnect = uiState.quickImportWriteHealthConnect,
            onToggleAddDatabase = { viewModel.toggleQuickImportAddFoodsToDatabase() },
            onToggleAddDay = { viewModel.toggleQuickImportAddFoodsToDay() },
            onToggleHealthConnect = { viewModel.toggleQuickImportWriteHealthConnect() },
            onDismiss = { viewModel.updateQuickImportSettingsVisible(false) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationDialog(
    addDatabase: Boolean,
    addDay: Boolean,
    writeHealthConnect: Boolean,
    onToggleAddDatabase: () -> Unit,
    onToggleAddDay: () -> Unit,
    onToggleHealthConnect: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SheetTitle("Add Meal settings", "These stay on by default for your usual workflow.")
            DestinationToggle(
                checked = addDatabase,
                text = "Add foods to database",
                icon = { Icon(Icons.Default.Storage, contentDescription = null, tint = AccentDatabase) },
                onClick = onToggleAddDatabase,
                modifier = Modifier.testTag("quick_import_toggle_database"),
            )
            DestinationToggle(
                checked = addDay,
                text = "Add foods to current day",
                icon = { Icon(Icons.Default.Event, contentDescription = null, tint = AccentDay) },
                onClick = onToggleAddDay,
                modifier = Modifier.testTag("quick_import_toggle_day"),
            )
            DestinationToggle(
                checked = writeHealthConnect,
                text = "Write foods to Health Connect",
                icon = { Icon(Icons.Default.CloudDone, contentDescription = null, tint = AccentHealth) },
                onClick = onToggleHealthConnect,
                modifier = Modifier.testTag("quick_import_toggle_health"),
            )
        }
    }
}

@Composable
private fun DestinationToggle(
    checked: Boolean,
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                BorderStroke(
                    1.dp,
                    if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                ),
                RoundedCornerShape(8.dp),
            )
            .background(if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onClick() },
        )
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
