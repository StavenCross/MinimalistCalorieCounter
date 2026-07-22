package com.makstuff.minimalistcaloriecounter.classes

import android.content.Intent
import android.net.Uri
import com.makstuff.minimalistcaloriecounter.ui.navigation.AppRoutes
import java.net.URLDecoder
import java.time.DateTimeException

internal data class MealImportLaunchRequest(
    val import: MealImportRequest,
    val requiresConfirmation: Boolean = MealImportContract.DEFAULT_CONFIRMATION_REQUIRED,
) {
    val targetRoute: String = AppRoutes.HEALTH_CONNECT_NUTRITION
    val openDrawer: Boolean = true

    companion object {
        fun fromIntent(intent: Intent?): MealImportLaunchRequest? {
            return when (intent?.action) {
                Intent.ACTION_VIEW -> fromUri(intent.data)
                Intent.ACTION_SEND -> fromSharedText(intent.sharedText())
                else -> null
            }
        }

        fun fromUri(uri: Uri?): MealImportLaunchRequest? {
            if (uri == null || !uri.isMealImportUri()) return null
            val payload = uri.getQueryParameter("payload") ?: error("Meal import payload is required.")
            return MealImportLaunchRequest(MealImportContract.fromBase64UrlPayload(payload))
        }

        fun fromUriString(uri: String): MealImportLaunchRequest? = fromUri(Uri.parse(uri))

        fun fromDeepLinkText(uri: String): MealImportLaunchRequest? {
            if (!uri.startsWith("foodlog://import") && !uri.startsWith(APP_LINK_PREFIX)) return null
            val query = uri.substringAfter('?', missingDelimiterValue = "")
            val payload = query.split('&')
                .mapNotNull { parameter ->
                    val key = parameter.substringBefore('=')
                    val value = parameter.substringAfter('=', missingDelimiterValue = "")
                    if (key == "payload") URLDecoder.decode(value, Charsets.UTF_8.name()) else null
                }
                .firstOrNull()
                ?: error("Meal import payload is required.")
            return MealImportLaunchRequest(MealImportContract.fromBase64UrlPayload(payload))
        }

        fun fromSharedText(text: String?): MealImportLaunchRequest? {
            val value = text?.trim().orEmpty()
            if (value.isBlank()) return null
            if (value.startsWith("foodlog://") || value.startsWith(APP_LINK_PREFIX)) return fromDeepLinkText(value)
            if (value.startsWith("{")) return MealImportLaunchRequest(MealImportContract.fromJson(value))
            return null
        }

        private fun Intent.sharedText(): String? {
            getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() }?.let {
                return it
            }
            getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let { return it }
            clipData?.let { data ->
                for (index in 0 until data.itemCount) {
                    data.getItemAt(index).text?.toString()?.takeIf { it.isNotBlank() }?.let {
                        return it
                    }
                }
            }
            extras?.keySet()?.forEach { key ->
                val value = extras?.get(key)
                if (value is CharSequence && value.isNotBlank()) return value.toString()
            }
            return null
        }

        private fun Uri.isMealImportUri(): Boolean {
            val isFoodLogScheme = scheme == "foodlog" && host == "import"
            val isVerifiedAppLink = scheme == "https" && host == APP_LINK_HOST && path == APP_LINK_PATH
            return isFoodLogScheme || isVerifiedAppLink
        }

        private const val APP_LINK_HOST = "nutrition.dioem.cloud"
        private const val APP_LINK_PATH = "/import"
        private const val APP_LINK_PREFIX = "https://$APP_LINK_HOST$APP_LINK_PATH"
    }
}

internal data class MealImportLaunchResolution(
    val request: MealImportLaunchRequest?,
    val errorMessage: String?,
)

/**
 * Converts untrusted external import data into a launch request without letting parse failures
 * escape into an Activity lifecycle callback. Only expected input/contract failures are handled;
 * programming and platform errors still surface normally.
 */
internal fun resolveMealImportLaunch(
    parse: () -> MealImportLaunchRequest?,
): MealImportLaunchResolution {
    return try {
        MealImportLaunchResolution(request = parse(), errorMessage = null)
    } catch (error: IllegalArgumentException) {
        MealImportLaunchResolution(request = null, errorMessage = error.message)
    } catch (error: IllegalStateException) {
        MealImportLaunchResolution(request = null, errorMessage = error.message)
    } catch (error: DateTimeException) {
        MealImportLaunchResolution(request = null, errorMessage = error.message)
    }
}
