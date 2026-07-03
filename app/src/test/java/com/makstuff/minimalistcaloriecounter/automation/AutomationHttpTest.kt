package com.makstuff.minimalistcaloriecounter.automation

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AutomationHttpTest {
    @Test
    fun readRequestRejectsOversizedBodies() {
        val request = (
            "POST /quick-import/preview HTTP/1.1\r\n" +
                "Content-Length: ${MAX_AUTOMATION_BODY_BYTES + 1}\r\n" +
                "\r\n"
            )

        assertThrows(IllegalArgumentException::class.java) {
            readRequest(request.byteInputStream())
        }
    }

    @Test
    fun readRequestKeepsPathWithoutQueryString() {
        val body = """{"text":"100g arroz"}"""
        val request = """
            POST /quick-import/preview?ignored=true HTTP/1.1
            Content-Length: ${body.length}

            $body
        """.trimIndent().replace("\n", "\r\n")

        val parsed = readRequest(request.byteInputStream())

        assertEquals("POST", parsed.method)
        assertEquals("/quick-import/preview", parsed.path)
        assertEquals(body, parsed.body)
    }

    @Test
    fun readRequestUsesUtf8ByteContentLength() {
        val body = """{"text":"100g arroz, piña"}"""
        val request = (
            "POST /quick-import/preview HTTP/1.1\r\n" +
                "Content-Length: ${body.toByteArray(Charsets.UTF_8).size}\r\n" +
                "\r\n" +
                body
            ).toByteArray(Charsets.UTF_8)

        val parsed = readRequest(ByteArrayInputStream(request))

        assertEquals(body, parsed.body)
    }
}
