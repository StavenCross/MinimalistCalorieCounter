package com.makstuff.minimalistcaloriecounter.health

import com.makstuff.minimalistcaloriecounter.classes.HealthConnectGoalSnapshot

sealed class HealthConnectGoalProfileReadResult {
    data class Success(val snapshot: HealthConnectGoalSnapshot) : HealthConnectGoalProfileReadResult()
    data object HealthConnectUnavailable : HealthConnectGoalProfileReadResult()
    data object PermissionsMissing : HealthConnectGoalProfileReadResult()
    data class Failed(val message: String) : HealthConnectGoalProfileReadResult()
}

sealed class HealthConnectGoalProfileWriteResult {
    data object Success : HealthConnectGoalProfileWriteResult()
    data object HealthConnectUnavailable : HealthConnectGoalProfileWriteResult()
    data object PermissionsMissing : HealthConnectGoalProfileWriteResult()
    data class Failed(val message: String) : HealthConnectGoalProfileWriteResult()
}

sealed class HealthConnectExportResult {
    data class Success(val displayPath: String, val records: Int) : HealthConnectExportResult()
    data object HealthConnectUnavailable : HealthConnectExportResult()
    data object PermissionsMissing : HealthConnectExportResult()
    data class Failed(val message: String) : HealthConnectExportResult()
}
