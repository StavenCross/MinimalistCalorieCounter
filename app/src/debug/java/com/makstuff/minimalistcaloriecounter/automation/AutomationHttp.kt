package com.makstuff.minimalistcaloriecounter.automation

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal const val MAX_AUTOMATION_BODY_BYTES = 256 * 1024
private const val MAX_AUTOMATION_HEADER_BYTES = 16 * 1024

internal data class Request(val method: String, val path: String, val body: String)

internal fun readRequest(input: InputStream): Request {
    val headerText = readHeaderText(input)
    val headerLines = headerText.split("\r\n")
    val requestLine = headerLines.firstOrNull().takeUnless { it.isNullOrBlank() } ?: error("Missing request line")
    val parts = requestLine.split(" ")
    require(parts.size >= 2) { "Malformed request line" }
    val contentLength = headerLines.drop(1).firstNotNullOfOrNull { line ->
        val separator = line.indexOf(':')
        if (separator > 0 && line.substring(0, separator).equals("content-length", true)) {
            line.substring(separator + 1).trim().toIntOrNull()
        } else {
            null
        }
    } ?: 0
    require(contentLength <= MAX_AUTOMATION_BODY_BYTES) { "Automation request body too large." }
    val body = readBody(input, contentLength).toString(Charsets.UTF_8)
    return Request(method = parts[0], path = parts[1].substringBefore("?"), body = body)
}

private fun readHeaderText(input: InputStream): String {
    val bytes = ByteArrayOutputStream()
    var previous3 = -1
    var previous2 = -1
    var previous1 = -1
    while (true) {
        val next = input.read()
        if (next == -1) error("Incomplete request headers")
        bytes.write(next)
        require(bytes.size() <= MAX_AUTOMATION_HEADER_BYTES) { "Automation request headers too large." }
        if (previous3 == '\r'.code && previous2 == '\n'.code && previous1 == '\r'.code && next == '\n'.code) {
            return bytes.toByteArray().toString(Charsets.UTF_8).removeSuffix("\r\n\r\n")
        }
        previous3 = previous2
        previous2 = previous1
        previous1 = next
    }
}

private fun readBody(input: InputStream, contentLength: Int): ByteArray {
    if (contentLength <= 0) return ByteArray(0)
    val body = ByteArray(contentLength)
    var offset = 0
    while (offset < contentLength) {
        val read = input.read(body, offset, contentLength - offset)
        if (read == -1) error("Incomplete request body")
        offset += read
    }
    return body
}

internal fun ok(body: JSONObject): String = jsonResponse(200, JSONObject().put("ok", true).put("data", body))

internal fun jsonResponse(statusCode: Int, body: JSONObject): String {
    val statusText = when (statusCode) {
        200 -> "OK"
        400 -> "Bad Request"
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
