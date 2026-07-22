package com.makstuff.minimalistcaloriecounter.health

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord

internal val healthConnectNutritionWritePermissions = setOf(
    HealthPermission.getWritePermission(NutritionRecord::class),
)

internal val healthConnectNutritionReadPermissions = setOf(
    HealthPermission.getReadPermission(NutritionRecord::class),
)

internal val healthConnectGoalProfileWritePermissions = setOf(
    HealthPermission.getWritePermission(HeightRecord::class),
    HealthPermission.getWritePermission(WeightRecord::class),
)

internal val healthConnectGoalProfileReadPermissions = setOf(
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getReadPermission(HeightRecord::class),
    HealthPermission.getReadPermission(BodyFatRecord::class),
    HealthPermission.getReadPermission(BodyWaterMassRecord::class),
    HealthPermission.getReadPermission(BoneMassRecord::class),
    HealthPermission.getReadPermission(LeanBodyMassRecord::class),
)

/** Permissions required for the app's meal, archive, and Goals features to be operational. */
internal val coreHealthConnectPermissions: Set<String> = healthConnectNutritionWritePermissions +
    healthConnectNutritionReadPermissions +
    healthConnectGoalProfileReadPermissions +
    healthConnectGoalProfileWritePermissions

/** Initial activation stays limited to permissions required by everyday app features. */
internal val defaultHealthConnectPermissions: Set<String> = coreHealthConnectPermissions

/** Returns the exact grant set needed by a single Health Connect capability. */
internal fun healthConnectPermissionsFor(scope: HealthConnectPermissionScope): Set<String> = when (scope) {
    HealthConnectPermissionScope.CoreAppFeatures -> coreHealthConnectPermissions
    HealthConnectPermissionScope.WriteArchiveEntries -> healthConnectNutritionWritePermissions + setOf(
        HealthPermission.getWritePermission(WeightRecord::class),
    )
    HealthConnectPermissionScope.ReadNutrition -> healthConnectNutritionReadPermissions
    HealthConnectPermissionScope.ReadGoalProfile -> healthConnectGoalProfileReadPermissions
    HealthConnectPermissionScope.WriteGoalProfile -> healthConnectGoalProfileWritePermissions
    HealthConnectPermissionScope.ExportReadableData -> allReadPermissions
    HealthConnectPermissionScope.MutateNutritionRecords -> healthConnectNutritionWritePermissions + healthConnectNutritionReadPermissions
}

internal val checkInReadPermissions: Set<String> = checkInRecordTypes
    .map { HealthPermission.getReadPermission(it) }
    .toSet() + HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
