package com.makstuff.minimalistcaloriecounter.classes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class MealImportLaunchRequestTest {
    @Test
    fun parsesFoodlogDeepLink() {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(SAMPLE_JSON.toByteArray())
        val request = MealImportLaunchRequest.fromDeepLinkText("foodlog://import?payload=$payload")

        assertNotNull(request)
        assertEquals(QuickImportMealType.Lunch, request!!.import.mealType)
        assertEquals(true, request.requiresConfirmation)
    }

    @Test
    fun parsesHttpsAppLink() {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(SAMPLE_JSON.toByteArray())
        val request = MealImportLaunchRequest.fromDeepLinkText(
            "https://nutrition.dioem.cloud/import?payload=$payload"
        )

        assertNotNull(request)
        assertEquals(QuickImportMealType.Lunch, request!!.import.mealType)
    }

    @Test
    fun parsesSharedHttpsAppLinkText() {
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(SAMPLE_JSON.toByteArray())
        val request = MealImportLaunchRequest.fromSharedText(
            "https://nutrition.dioem.cloud/import?payload=$payload"
        )

        assertNotNull(request)
        assertEquals("chatgpt", request!!.import.source)
    }

    @Test
    fun parsesSharedJsonText() {
        val request = MealImportLaunchRequest.fromSharedText(SAMPLE_JSON)

        assertNotNull(request)
        assertEquals("chatgpt", request!!.import.source)
    }

    @Test
    fun ignoresOtherSharedText() {
        assertNull(MealImportLaunchRequest.fromSharedText("Japanese curry; Calories 425."))
    }

    @Test
    fun invalidExternalPayloadResolvesToErrorWithoutThrowing() {
        val resolution = resolveMealImportLaunch {
            MealImportLaunchRequest.fromDeepLinkText(
                "https://nutrition.dioem.cloud/import?payload=not-valid-base64"
            )
        }

        assertNull(resolution.request)
        assertTrue(resolution.errorMessage?.isNotBlank() == true)
    }

    @Test
    fun invalidExternalDateAndTimeResolveToErrorsWithoutThrowing() {
        val invalidFields = listOf(
            "date" to "not-a-date",
            "time" to "25:00",
        )

        invalidFields.forEach { (field, value) ->
            val json = """{"action":"log_meal","$field":"$value","items":[{"name":"Food","calories":1}]}"""
            val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
            val resolution = resolveMealImportLaunch {
                MealImportLaunchRequest.fromDeepLinkText(
                    "https://nutrition.dioem.cloud/import?payload=$payload"
                )
            }

            assertNull(resolution.request)
            assertTrue(resolution.errorMessage?.isNotBlank() == true)
        }
    }

    private companion object {
        const val SAMPLE_JSON = """
            {
              "source": "chatgpt",
              "action": "log_meal",
              "date": "2026-07-08",
              "meal": "lunch",
              "items": [{"name": "Japanese curry", "amount": "400g", "calories": 425}]
            }
        """
    }
}
