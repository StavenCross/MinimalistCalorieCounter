package com.makstuff.minimalistcaloriecounter.health

internal enum class HealthConnectReplaceStep {
    InsertReplacement,
    DeleteOriginal,
}

internal fun nutritionReplaceSteps(recordIds: List<String>, replacementCount: Int): List<HealthConnectReplaceStep> {
    return buildList {
        if (replacementCount > 0) add(HealthConnectReplaceStep.InsertReplacement)
        if (recordIds.isNotEmpty()) add(HealthConnectReplaceStep.DeleteOriginal)
    }
}
