package com.makstuff.minimalistcaloriecounter.automation

import com.makstuff.minimalistcaloriecounter.AppViewModel
import com.makstuff.minimalistcaloriecounter.health.HealthConnectCleanupMode
import com.makstuff.minimalistcaloriecounter.health.HealthConnectExportMode
import org.json.JSONObject
import java.time.LocalDate

// Keep debug-only Health Connect actions outside AutomationBootstrap so route dispatch stays readable and under the project line cap.
internal fun clearQuickImportOutbox(context: android.content.Context, viewModel: AppViewModel, body: JSONObject): JSONObject {
    viewModel.quickImportClearOutbox(
        context = context,
        outboxId = body.optString("id").ifBlank { null },
        attentionOnly = body.optBoolean("attentionOnly", true),
    )
    return JSONObject().put("cleared", true)
}

internal fun AppViewModel.applyHealthCleanupBody(body: JSONObject) {
    if (body.has("startDate")) updateHealthConnectNutritionCleanupStartDate(LocalDate.parse(body.requireString("startDate")))
    if (body.has("endDate")) updateHealthConnectNutritionCleanupEndDate(LocalDate.parse(body.requireString("endDate")))
    if (body.has("mode")) updateHealthConnectNutritionCleanupMode(HealthConnectCleanupMode.valueOf(body.requireString("mode")))
}

internal fun AppViewModel.applyHealthExportBody(body: JSONObject) {
    if (body.has("startDate")) updateHealthConnectExportStartDate(LocalDate.parse(body.requireString("startDate")))
    if (body.has("endDate")) updateHealthConnectExportEndDate(LocalDate.parse(body.requireString("endDate")))
    if (body.has("mode")) updateHealthConnectExportMode(HealthConnectExportMode.valueOf(body.requireString("mode")))
    if (body.has("redacted")) updateHealthConnectExportRedacted(body.optBoolean("redacted"))
}

internal fun healthOptionsJson(viewModel: AppViewModel): JSONObject {
    val state = viewModel.uiState.value
    val preview = state.healthConnectNutritionCleanupPreview
    return JSONObject()
        .put("exportMode", state.healthConnectExportMode.name)
        .put("exportRedacted", state.healthConnectExportRedacted)
        .put("cleanupMode", state.healthConnectNutritionCleanupMode.name)
        .put("cleanupPreview", preview?.let {
            JSONObject()
                .put("total", it.total)
                .put("historicalImports", it.historicalImports)
                .put("addMeal", it.addMeal)
                .put("legacyDailyTotals", it.legacyDailyTotals)
        })
}

internal fun previewHealthDeleteRange(viewModel: AppViewModel, body: JSONObject): JSONObject {
    viewModel.applyHealthCleanupBody(body)
    viewModel.previewHealthConnectNutritionRange()
    return JSONObject().put("started", true).put("healthConnect", healthOptionsJson(viewModel))
}

internal fun deleteHealthRange(viewModel: AppViewModel, body: JSONObject): JSONObject {
    viewModel.applyHealthCleanupBody(body)
    viewModel.removeHealthConnectNutritionRange()
    return JSONObject().put("started", true).put("healthConnect", healthOptionsJson(viewModel))
}

internal fun updateHealthExportOptions(viewModel: AppViewModel, body: JSONObject): JSONObject {
    viewModel.applyHealthExportBody(body)
    return JSONObject().put("updated", true).put("healthConnect", healthOptionsJson(viewModel))
}

internal fun exportHealthRange(viewModel: AppViewModel, body: JSONObject): JSONObject {
    viewModel.applyHealthExportBody(body)
    viewModel.exportHealthConnectRange()
    return JSONObject().put("started", true).put("healthConnect", healthOptionsJson(viewModel))
}
