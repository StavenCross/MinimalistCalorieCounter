package com.makstuff.minimalistcaloriecounter.automation

import org.json.JSONObject
import java.io.BufferedReader

internal data class Request(val method: String, val path: String, val body: String)

internal fun readRequest(reader: BufferedReader): Request {
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

internal fun ok(body: JSONObject): String = jsonResponse(200, JSONObject().put("ok", true).put("data", body))

internal fun jsonResponse(statusCode: Int, body: JSONObject): String {
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
