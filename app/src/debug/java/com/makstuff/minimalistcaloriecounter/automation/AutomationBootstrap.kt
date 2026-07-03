package com.makstuff.minimalistcaloriecounter.automation

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.makstuff.minimalistcaloriecounter.AppViewModel
import com.makstuff.minimalistcaloriecounter.classes.ActivityLevel
import com.makstuff.minimalistcaloriecounter.classes.GoalFieldKey
import com.makstuff.minimalistcaloriecounter.classes.GoalMacro
import com.makstuff.minimalistcaloriecounter.classes.GoalSex
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.WeeklyWeightLossTarget
import com.makstuff.minimalistcaloriecounter.ui.settings.SettingsSheet
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object AutomationBootstrap {
    private const val TAG = "MCCAutomation"
    private const val PORT = 8765
    private val started = AtomicBoolean(false)

    @JvmStatic
    fun start(context: Context, viewModel: AppViewModel) {
        if (!started.compareAndSet(false, true)) return
        AutomationServer(context.applicationContext, viewModel, PORT).start()
    }

    private class AutomationServer(
        private val context: Context,
        private val viewModel: AppViewModel,
        private val port: Int,
    ) {
        private val mainHandler = Handler(Looper.getMainLooper())
        private val executor = Executors.newCachedThreadPool()

        fun start() {
            Thread {
                runCatching {
                    ServerSocket(port, 8, InetAddress.getByName("127.0.0.1")).use { server ->
                        Log.i(TAG, "Debug automation bridge listening on 127.0.0.1:$port")
                        while (!Thread.currentThread().isInterrupted) {
                            val socket = server.accept()
                            executor.execute { handle(socket) }
                        }
                    }
                }.onFailure {
                    started.set(false)
                    Log.e(TAG, "Debug automation bridge stopped: ${it.message}", it)
                }
            }.apply {
                name = "mcc-automation-server"
                isDaemon = true
                start()
            }
        }

        private fun handle(socket: Socket) {
            socket.use {
                val reader = BufferedReader(InputStreamReader(it.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(it.getOutputStream()))
                val response = runCatching { dispatch(readRequest(reader)) }
                    .getOrElse { error ->
                        jsonResponse(
                            statusCode = 500,
                            body = JSONObject()
                                .put("ok", false)
                                .put("error", error.message ?: "Automation request failed"),
                        )
                    }
                writer.write(response)
                writer.flush()
            }
        }

        private fun dispatch(request: Request): String {
            val body = request.body.takeIf { it.isNotBlank() }?.let { JSONObject(it) } ?: JSONObject()
            return when (request.method to request.path) {
                "GET" to "/health" -> ok(JSONObject().put("status", "ok").put("port", port))
                "GET" to "/state" -> ok(stateJson())
                "POST" to "/navigate" -> ok(runOnMain {
                    val route = automationRouteFor(body.requireString("screen"))
                    viewModel.requestNavigation(route)
                    JSONObject().put("route", route)
                })
                "POST" to "/settings/open" -> ok(runOnMain {
                    val sheet = SettingsSheet.fromKey(body.optString("sheet").ifBlank { null })
                    viewModel.updateActiveSettingsSheet(sheet)
                    JSONObject().put("sheet", sheet?.key)
                })
                "POST" to "/quick-import/preview" -> ok(runOnMain {
                    applyQuickImportBody(body)
                    quickImportJson()
                })
                "POST" to "/quick-import/commit" -> ok(runOnMain {
                    applyQuickImportBody(body)
                    viewModel.quickImportCommit(context)
                    JSONObject()
                        .put("started", true)
                        .put("quickImport", quickImportJson())
                })
                "POST" to "/meals/select-date" -> ok(runOnMain {
                    val date = LocalDate.parse(body.requireString("date"))
                    viewModel.updateHealthConnectViewerDate(date)
                    JSONObject().put("date", date.toString())
                })
                "POST" to "/health-connect/read-day" -> ok(runOnMain {
                    val date = LocalDate.parse(body.requireString("date"))
                    viewModel.updateHealthConnectViewerDate(date)
                    JSONObject()
                        .put("date", date.toString())
                        .put("started", true)
                })
                "POST" to "/health-connect/delete-range" -> ok(runOnMain {
                    val startDate = LocalDate.parse(body.requireString("startDate"))
                    val endDate = LocalDate.parse(body.requireString("endDate"))
                    viewModel.updateHealthConnectNutritionCleanupStartDate(startDate)
                    viewModel.updateHealthConnectNutritionCleanupEndDate(endDate)
                    viewModel.removeHealthConnectNutritionRange()
                    JSONObject()
                        .put("started", true)
                        .put("startDate", startDate.toString())
                        .put("endDate", endDate.toString())
                })
                "POST" to "/health-connect/export-range" -> ok(runOnMain {
                    val startDate = LocalDate.parse(body.requireString("startDate"))
                    val endDate = LocalDate.parse(body.requireString("endDate"))
                    viewModel.updateHealthConnectExportStartDate(startDate)
                    viewModel.updateHealthConnectExportEndDate(endDate)
                    viewModel.exportHealthConnectRange()
                    JSONObject()
                        .put("started", true)
                        .put("startDate", startDate.toString())
                        .put("endDate", endDate.toString())
                })
                "GET" to "/goals/state" -> ok(runOnMain { goalsJson(viewModel.uiState.value.goals) })
                "POST" to "/goals/settings" -> ok(runOnMain {
                    viewModel.updateGoalsSettingsVisible(body.optBoolean("visible", true))
                    goalsJson(viewModel.uiState.value.goals)
                })
                "POST" to "/goals/set-profile" -> ok(runOnMain {
                    applyGoalsProfileBody(body)
                    goalsJson(viewModel.uiState.value.goals)
                })
                "POST" to "/goals/set-measurement" -> ok(runOnMain {
                    viewModel.updateGoalMeasurement(
                        field = GoalFieldKey.valueOf(body.requireString("field")),
                        value = body.optDoubleOrNull("value"),
                    )
                    goalsJson(viewModel.uiState.value.goals)
                })
                "POST" to "/goals/toggle-measurement-lock" -> ok(runOnMain {
                    viewModel.toggleGoalMeasurementLock(GoalFieldKey.valueOf(body.requireString("field")))
                    goalsJson(viewModel.uiState.value.goals)
                })
                "POST" to "/goals/set-macro" -> ok(runOnMain {
                    viewModel.updateGoalMacro(
                        macro = GoalMacro.valueOf(body.requireString("macro")),
                        value = body.optDoubleOrNull("value"),
                    )
                    goalsJson(viewModel.uiState.value.goals)
                })
                "POST" to "/goals/toggle-macro-lock" -> ok(runOnMain {
                    viewModel.toggleGoalMacroLock(GoalMacro.valueOf(body.requireString("macro")))
                    goalsJson(viewModel.uiState.value.goals)
                })
                "POST" to "/goals/refresh-health-connect" -> ok(runOnMain {
                    viewModel.refreshGoalsFromHealthConnect()
                    JSONObject().put("started", true).put("goals", goalsJson(viewModel.uiState.value.goals))
                })
                "POST" to "/goals/recalculate" -> ok(runOnMain {
                    viewModel.recalculateGoalRecommendation()
                    goalsJson(viewModel.uiState.value.goals)
                })
                "POST" to "/goals/apply-recommendation" -> ok(runOnMain {
                    viewModel.applyGoalRecommendation()
                    goalsJson(viewModel.uiState.value.goals)
                })
                "POST" to "/reset-debug-state" -> ok(runOnMain {
                    viewModel.resetQuickImport()
                    viewModel.updateActiveSettingsSheet(null)
                    viewModel.updateQuickImportSettingsVisible(false)
                    JSONObject().put("reset", true)
                })
                else -> jsonResponse(
                    statusCode = 404,
                    body = JSONObject().put("ok", false).put("error", "Unknown endpoint ${request.method} ${request.path}"),
                )
            }
        }

        private fun applyQuickImportBody(body: JSONObject) {
            if (body.has("text")) viewModel.updateQuickImportText(body.optString("text"))
            if (body.has("dateTime")) viewModel.updateQuickImportDateTime(LocalDateTime.parse(body.requireString("dateTime")))
            if (body.has("mealType")) viewModel.updateQuickImportMealTypeOverride(QuickImportMealType.valueOf(body.requireString("mealType")))
            if (body.has("snackOverride")) viewModel.updateQuickImportSnackOverride(body.optBoolean("snackOverride"))
            if (body.has("addDatabase")) viewModel.updateQuickImportAddFoodsToDatabase(body.optBoolean("addDatabase"))
            if (body.has("addDay")) viewModel.updateQuickImportAddFoodsToDay(body.optBoolean("addDay"))
            if (body.has("writeHealthConnect")) viewModel.updateQuickImportWriteHealthConnect(body.optBoolean("writeHealthConnect"))
        }

        private fun stateJson(): JSONObject = runOnMain {
            val state = viewModel.uiState.value
            JSONObject()
                .put("loading", state.loading)
                .put("topBarTitle", state.topBarTitle)
                .put("navigation", state.navigationBarHighlight.name)
                .put("automationRouteRequest", state.automationRouteRequest)
                .put("activeSettingsSheet", state.activeSettingsSheet?.key)
                .put("quickImportSettingsVisible", state.quickImportSettingsVisible)
                .put("healthConnectPermissionsGranted", state.healthConnectPermissionsGranted)
                .put("healthConnectExportPermissionsGranted", state.healthConnectExportPermissionsGranted)
                .put("healthConnectAnyPermissionsGranted", state.healthConnectAnyPermissionsGranted)
                .put("healthConnectSyncEnabled", state.healthConnectSyncEnabled)
                .put("healthConnectViewerDate", state.healthConnectViewerDate.toString())
                .put("healthConnectViewerLoading", state.healthConnectViewerLoading)
                .put("healthConnectViewerMessage", state.healthConnectViewerMessage)
                .put("healthConnectMeals", JSONArray(state.healthConnectViewerMeals.map { it.toJson() }))
                .put("goals", goalsJson(state.goals))
                .put("quickImport", quickImportJson())
                .put("historicalMealImportMessage", state.historicalMealImportMessage)
                .put("historicalMealImportInProgress", state.historicalMealImportInProgress)
                .put("healthConnectExportStartDate", state.healthConnectExportStartDate.toString())
                .put("healthConnectExportEndDate", state.healthConnectExportEndDate.toString())
                .put("healthConnectExportInProgress", state.healthConnectExportInProgress)
                .put("healthConnectExportMessage", state.healthConnectExportMessage)
        }

        private fun quickImportJson(): JSONObject {
            val state = viewModel.uiState.value
            return JSONObject()
                .put("textLength", state.inputQuickImportText.length)
                .put("dateTime", state.inputQuickImportDateTime.toString())
                .put("mealType", state.quickImportMealType.label)
                .put("snackOverride", state.quickImportSnackOverride)
                .put("addDatabase", state.quickImportAddFoodsToDatabase)
                .put("addDay", state.quickImportAddFoodsToDay)
                .put("writeHealthConnect", state.quickImportWriteHealthConnect)
                .put("inProgress", state.quickImportInProgress)
                .put("error", state.quickImportError)
                .put("successMessage", state.quickImportSuccessMessage)
                .put("successToken", state.quickImportSuccessToken)
                .put("meal", state.quickImportMeal?.toJson())
                .put("result", state.quickImportResult?.toJson())
        }

        private fun <T> runOnMain(block: () -> T): T {
            if (Looper.myLooper() == Looper.getMainLooper()) return block()
            var value: T? = null
            var failure: Throwable? = null
            val latch = CountDownLatch(1)
            mainHandler.post {
                try {
                    value = block()
                } catch (t: Throwable) {
                    failure = t
                } finally {
                    latch.countDown()
                }
            }
            if (!latch.await(10, TimeUnit.SECONDS)) error("Timed out waiting for main thread")
            failure?.let { throw it }
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        private fun applyGoalsProfileBody(body: JSONObject) {
            if (body.has("birthday")) viewModel.updateGoalBirthday(LocalDate.parse(body.requireString("birthday")))
            if (body.has("sex")) viewModel.updateGoalSex(GoalSex.valueOf(body.requireString("sex")))
            if (body.has("activityLevel")) viewModel.updateGoalActivityLevel(ActivityLevel.valueOf(body.requireString("activityLevel")))
            if (body.has("weightLossTarget")) viewModel.updateGoalWeightLossTarget(WeeklyWeightLossTarget.valueOf(body.requireString("weightLossTarget")))
            if (body.has("heightCm")) viewModel.updateGoalMeasurement(GoalFieldKey.HeightCm, body.optDoubleOrNull("heightCm"))
            if (body.has("weightKg")) viewModel.updateGoalMeasurement(GoalFieldKey.WeightKg, body.optDoubleOrNull("weightKg"))
            if (body.has("bodyFatPercent")) viewModel.updateGoalMeasurement(GoalFieldKey.BodyFatPercent, body.optDoubleOrNull("bodyFatPercent"))
            if (body.has("leanMassKg")) viewModel.updateGoalMeasurement(GoalFieldKey.LeanMassKg, body.optDoubleOrNull("leanMassKg"))
            body.optJSONObject("targets")?.let { targets ->
                if (targets.has("calories")) viewModel.updateGoalMacro(GoalMacro.Calories, targets.optDoubleOrNull("calories"))
                if (targets.has("protein")) viewModel.updateGoalMacro(GoalMacro.Protein, targets.optDoubleOrNull("protein"))
                if (targets.has("carbs")) viewModel.updateGoalMacro(GoalMacro.Carbs, targets.optDoubleOrNull("carbs"))
                if (targets.has("fat")) viewModel.updateGoalMacro(GoalMacro.Fat, targets.optDoubleOrNull("fat"))
                if (targets.has("fiber")) viewModel.updateGoalMacro(GoalMacro.Fiber, targets.optDoubleOrNull("fiber"))
            }
        }
    }
}
