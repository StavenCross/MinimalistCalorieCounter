package com.makstuff.minimalistcaloriecounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AddMealDrawerRequestTest {
    @Test
    fun matchingRequestIsConsumedExactlyOnce() {
        val consumed = consumeAddMealDrawerRequest(currentToken = 7L, prepared = true, handledToken = 7L)

        assertEquals(0L, consumed.token)
        assertFalse(consumed.prepared)
    }

    @Test
    fun staleAcknowledgementDoesNotConsumeNewerRequest() {
        val current = AddMealDrawerRequestStatus(token = 8L, prepared = true)
        val result = consumeAddMealDrawerRequest(
            currentToken = current.token,
            prepared = current.prepared,
            handledToken = 7L,
        )

        assertEquals(current, result)
    }
}
