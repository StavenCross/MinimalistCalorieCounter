package com.makstuff.minimalistcaloriecounter.automation

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.makstuff.minimalistcaloriecounter.AppViewModel
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMeal
import com.makstuff.minimalistcaloriecounter.classes.QuickImportNutrients
import com.makstuff.minimalistcaloriecounter.classes.QuickImportResult
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import com.makstuff.minimalistcaloriecounter.health.HealthConnectNutritionMeal
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

        private fun readRequest(reader: BufferedReader): Request {
            val requestLine = reader.readLine() ?: error("Missing request line")
            val parts = requestLine.split(" ")
            require(parts.size >= 2) { "Malformed request line" }
            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val separator = line.indexOf(':')
                if (separator > 0 && line.substring(0, separator).equals("content-length", true)) {
                    contentLength = line.substring(separator + 1).trim().toIntOrNull() ?: 0
                }
            }
            val body = if (contentLength > 0) {
                CharArray(contentLength).also { reader.read(it) }.concatToString()
            } else {
                ""
            }
            return Request(method = parts[0], path = parts[1].substringBefore("?"), body = body)
        }

        private fun dispatch(request: Request): String {
            val body = request.body.takeIf { it.isNotBlank() }?.let { JSONObject(it) } ?: JSONObject()
            return when (request.method to request.path) {
                "GET" to "/health" -> ok(JSONObject().put("status", "ok").put("port", port))
                "GET" to "/state" -> ok(stateJson())
                "POST" to "/navigate" -> ok(runOnMain {
                    val route = routeFor(body.requireString("screen"))
                    viewModel.requestNavigation(route)
                    JSONObject().put("route", route)
                })
                "POST" to "/settings/open" -> ok(runOnMain {
                    val sheet = body.optString("sheet").ifBlank { null }
                    viewModel.updateActiveSettingsSheet(sheet)
                    JSONObject().put("sheet", sheet)
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
            if (body.has("snackOverride")) viewModel.updateQuickImportSnackOverride(body.optBoolean("snackOverride"))
            if (body.has("addDatabase")) viewModel.updateQuickImportAddFoodsToDatabase(body.optBoolean("addDatabase"))
            if (body.has("addDay")) viewModel.updateQuickImportAddFoodsToDay(body.optBoolean("addDay"))
            if (body.has("writeHealthConnect")) viewModel.updateQuickImportWriteHealthConnect(body.optBoolean("writeHealthConnect"))
        }

        private fun routeFor(screen: String): String {
            return when (screen.lowercase().replace("-", "_")) {
                "quick_add", "quick_import" -> "quick_import"
                "meals", "health_connect", "health_connect_nutrition" -> "health_connect_nutrition"
                "settings", "options" -> "settings_home"
                "database" -> "database_home"
                "day" -> "day_home"
                else -> error("Unknown screen '$screen'")
            }
        }

        private fun stateJson(): JSONObject = runOnMain {
            val state = viewModel.uiState.value
            JSONObject()
                .put("loading", state.loading)
                .put("topBarTitle", state.topBarTitle)
                .put("navigation", state.navigationBarHighlight.name)
                .put("automationRouteRequest", state.automationRouteRequest)
                .put("activeSettingsSheet", state.activeSettingsSheet)
                .put("quickImportSettingsVisible", state.quickImportSettingsVisible)
                .put("healthConnectPermissionsGranted", state.healthConnectPermissionsGranted)
                .put("healthConnectAnyPermissionsGranted", state.healthConnectAnyPermissionsGranted)
                .put("healthConnectSyncEnabled", state.healthConnectSyncEnabled)
                .put("healthConnectViewerDate", state.healthConnectViewerDate.toString())
                .put("healthConnectViewerLoading", state.healthConnectViewerLoading)
                .put("healthConnectViewerMessage", state.healthConnectViewerMessage)
                .put("healthConnectMeals", JSONArray(state.healthConnectViewerMeals.map { it.toJson() }))
                .put("quickImport", quickImportJson())
                .put("historicalMealImportMessage", state.historicalMealImportMessage)
                .put("historicalMealImportInProgress", state.historicalMealImportInProgress)
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

        private fun ok(body: JSONObject): String = jsonResponse(200, JSONObject().put("ok", true).put("data", body))

        private fun jsonResponse(statusCode: Int, body: JSONObject): String {
            val statusText = when (statusCode) {
                200 -> "OK"
                404 -> "Not Found"
                else -> "Internal Server Error"
            }
            val payload = body.toString()
            return buildString {
                append("HTTP/1.1 $statusCode $statusText\r\n")
                append("Content-Type: application/json; charset=utf-8\r\n")
                append("Content-Length: ${payload.toByteArray(Charsets.UTF_8).size}\r\n")
                append("Connection: close\r\n")
                append("\r\n")
                append(payload)
            }
        }
    }

    private data class Request(val method: String, val path: String, val body: String)
}

private fun JSONObject.requireString(name: String): String {
    require(has(name) && !isNull(name)) { "Missing '$name'" }
    return getString(name)
}

private fun QuickImportMeal.toJson(): JSONObject = JSONObject()
    .put("foods", JSONArray(foods.map { food ->
        JSONObject()
            .put("amountText", food.amountText)
            .put("name", food.name)
            .put("grams", food.grams)
            .put("nutrients", food.nutrients.toJson())
    }))
    .put("totals", totals.toJson())

private fun QuickImportNutrients.toJson(): JSONObject = JSONObject()
    .put("energy", energy)
    .put("carbohydrate", carbohydrate)
    .put("appCarbohydrate", appCarbohydrate)
    .put("sugar", sugar)
    .put("protein", protein)
    .put("fat", fat)
    .put("saturatedFat", saturatedFat)
    .put("fiber", fiber)

private fun QuickImportResult.toJson(): JSONObject = JSONObject()
    .put("databaseEntriesAdded", databaseEntriesAdded)
    .put("dayFoodsAdded", dayFoodsAdded)
    .put("healthWriteResult", healthWriteResult.toJson())

private fun QuickImportHealthWriteResult?.toJson(): JSONObject? {
    return when (this) {
        null -> null
        QuickImportHealthWriteResult.Success -> JSONObject().put("status", "success")
        QuickImportHealthWriteResult.HealthConnectUnavailable -> JSONObject().put("status", "health_connect_unavailable")
        QuickImportHealthWriteResult.PermissionsMissing -> JSONObject().put("status", "permissions_missing")
        is QuickImportHealthWriteResult.Failed -> JSONObject().put("status", "failed").put("message", message)
    }
}

private fun HealthConnectNutritionMeal.toJson(): JSONObject = JSONObject()
    .put("recordId", recordId)
    .put("clientRecordId", clientRecordId)
    .put("startTime", startTime.toString())
    .put("endTime", endTime.toString())
    .put("name", name)
    .put("energy", energy)
    .put("energyFromFat", energyFromFat)
    .put("totalCarbohydrate", totalCarbohydrate)
    .put("sugar", sugar)
    .put("protein", protein)
    .put("totalFat", totalFat)
    .put("saturatedFat", saturatedFat)
    .put("dietaryFiber", dietaryFiber)
    .put("mealType", mealType)
