package com.makstuff.minimalistcaloriecounter

import android.content.Context
import androidx.compose.runtime.toMutableStateList
import com.makstuff.minimalistcaloriecounter.classes.CustomWeights
import com.makstuff.minimalistcaloriecounter.classes.DatabaseEntry
import com.makstuff.minimalistcaloriecounter.classes.Nutrients
import com.makstuff.minimalistcaloriecounter.classes.QuickImportDatabaseEntryDraft
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult

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
): String {
    val localText = "Added $databaseEntriesAdded database foods and $dayFoodsAdded day foods."
    val healthText = when (healthWriteResult) {
        null -> "Health Connect skipped."
        QuickImportHealthWriteResult.Success -> "Health Connect write succeeded."
        QuickImportHealthWriteResult.HealthConnectUnavailable -> "Health Connect is unavailable."
        QuickImportHealthWriteResult.PermissionsMissing -> "Health Connect permissions are missing."
        is QuickImportHealthWriteResult.Failed -> "Health Connect failed: ${healthWriteResult.message}"
    }
    return "$localText $healthText"
}
