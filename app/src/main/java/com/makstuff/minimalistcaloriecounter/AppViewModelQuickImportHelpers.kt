package com.makstuff.minimalistcaloriecounter

import android.content.Context
import androidx.compose.runtime.toMutableStateList
import com.makstuff.minimalistcaloriecounter.classes.CustomWeights
import com.makstuff.minimalistcaloriecounter.classes.DatabaseEntry
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import com.makstuff.minimalistcaloriecounter.classes.QuickImportDatabaseEntryDraft
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMeal
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutbox
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxItem
import com.makstuff.minimalistcaloriecounter.persistence.room.LocalMealBackupMapper
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

internal data class QuickImportMealTypeSelection(
    val dateTime: LocalDateTime,
    val mealType: QuickImportMealType,
    val snackOverride: Boolean,
)

internal fun QuickImportDatabaseEntryDraft.toDatabaseEntry(context: Context): DatabaseEntry {
    return DatabaseEntry(
        name = name,
        nutrients = Nutrients(nutrientsPer100g.toAppValues().toMutableStateList(), context = context),
        customWeights = CustomWeights(context = context),
        quickselect = true,
        context = context,
    )
}

internal fun quickImportResultText(
    databaseEntriesAdded: Int,
    dayFoodsAdded: Int,
    healthWriteResult: QuickImportHealthWriteResult?,
    localDestinationsSkipped: Boolean = false,
): String {
    val localText = if (localDestinationsSkipped) {
        "Saved serving nutrition without legacy gram conversion."
    } else {
        "Added $databaseEntriesAdded database foods and $dayFoodsAdded day foods."
    }
    val healthText = when (healthWriteResult) {
        null -> "Health Connect skipped."
        QuickImportHealthWriteResult.Success -> "Health Connect write succeeded."
        QuickImportHealthWriteResult.HealthConnectUnavailable -> "Health Connect is unavailable."
        QuickImportHealthWriteResult.PermissionsMissing -> "Health Connect permissions are missing."
        is QuickImportHealthWriteResult.Failed -> "Health Connect failed: ${healthWriteResult.message}"
    }
    return "$localText $healthText"
}

/**
 * Applies an explicit meal label without changing its timestamp. Time windows choose the automatic
 * label only; moving the timestamp can create a future Health Connect record that Android rejects.
 */
internal fun AppUiState.withMealTypeOverride(mealType: QuickImportMealType): AppUiState {
    val selection = quickImportMealTypeSelection(inputQuickImportDateTime, mealType)
    return copy(
        inputQuickImportDateTime = selection.dateTime,
        quickImportMealTypeOverride = selection.mealType,
        quickImportSnackOverride = selection.snackOverride,
    )
}

/** Builds the explicit label state while retaining the timestamp selected by the user. */
internal fun quickImportMealTypeSelection(
    currentDateTime: LocalDateTime,
    mealType: QuickImportMealType,
): QuickImportMealTypeSelection = QuickImportMealTypeSelection(
    dateTime = currentDateTime,
    mealType = mealType,
    snackOverride = mealType == QuickImportMealType.Snack,
)

internal fun AppViewModelEnvironment.writeQuickImportOutboxItem(context: Context, item: QuickImportOutboxItem?) {
    if (item == null) return
    val items = QuickImportOutbox.upsert(uiState.quickImportOutbox, item)
    csvStore.writeQuickImportOutbox(context, items)
    launchRoomWrite {
        writeQuickImportOutboxItem(item)
    }
    state.update { currentState ->
        currentState.copy(quickImportOutbox = items)
    }
}

internal suspend fun AppViewModelEnvironment.writeLocalMealBackup(
    meal: QuickImportMeal,
    dateTime: LocalDateTime,
    mealType: QuickImportMealType,
    clientRecordIds: List<String?> = emptyList(),
) {
    roomStore.writeLocalMealBackups(
        LocalMealBackupMapper.toEntities(
            meal = meal,
            dateTime = dateTime,
            mealType = mealType,
            clientRecordIds = clientRecordIds,
            createdAt = LocalDateTime.now(),
        )
    )
}
