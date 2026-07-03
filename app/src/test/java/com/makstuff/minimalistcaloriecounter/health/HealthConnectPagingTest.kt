package com.makstuff.minimalistcaloriecounter.health

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectPagingTest {
    @Test
    fun readAllHealthConnectPagesReadsUntilBlankToken() = runTest {
        val requestedTokens = mutableListOf<String?>()

        val records = readAllHealthConnectPages { token ->
            requestedTokens += token
            when (token) {
                null -> HealthConnectPage(listOf("one", "two"), "next")
                "next" -> HealthConnectPage(listOf("three"), "")
                else -> error("Unexpected token $token")
            }
        }

        assertEquals(listOf(null, "next"), requestedTokens)
        assertEquals(listOf("one", "two", "three"), records)
    }
}
