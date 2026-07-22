package com.makstuff.minimalistcaloriecounter.health

/** Names each independent permission contract so optional features cannot gate core app status. */
internal enum class HealthConnectPermissionScope {
    CoreAppFeatures,
    WriteArchiveEntries,
    ReadNutrition,
    ReadGoalProfile,
    WriteGoalProfile,
    ExportReadableData,
    MutateNutritionRecords,
}
