package com.makstuff.minimalistcaloriecounter.health

internal const val HEALTH_CONNECT_PAGE_SIZE = 500

internal data class HealthConnectPage<T>(
    val records: List<T>,
    val nextPageToken: String?,
)

internal suspend fun <T> readAllHealthConnectPages(
    fetchPage: suspend (pageToken: String?) -> HealthConnectPage<T>,
): List<T> {
    val records = mutableListOf<T>()
    var pageToken: String? = null
    do {
        val page = fetchPage(pageToken)
        records += page.records
        pageToken = page.nextPageToken
    } while (!pageToken.isNullOrBlank())
    return records
}
