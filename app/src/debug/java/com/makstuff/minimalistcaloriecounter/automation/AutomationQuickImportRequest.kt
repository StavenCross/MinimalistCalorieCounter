package com.makstuff.minimalistcaloriecounter.automation

import org.json.JSONObject

/**
 * Guards debug Add Meal requests against removed destination controls.
 *
 * Visible Add Meal destinations are fixed now: local backup/day backup plus Health Connect. The
 * debug bridge rejects old per-request destination flags so automation cannot silently believe a
 * Health Connect write was disabled when the app would use its fixed commit path.
 */
internal fun guardedQuickImportRequest(body: JSONObject, block: () -> String): String {
    val removedFields = listOf("addDatabase", "addDay", "writeHealthConnect").filter { body.has(it) }
    if (removedFields.isEmpty()) return block()
    return jsonResponse(
        statusCode = 400,
        body = JSONObject()
            .put("ok", false)
            .put(
                "error",
                "Add Meal destination flags are no longer supported: ${removedFields.joinToString()}. " +
                    "The app now uses fixed local backup and Health Connect destinations.",
            ),
    )
}
