package mir.oslav.adrevenue.data.network

import androidx.annotation.Keep
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import mir.oslav.adrevenue.core.AdMobApiConstants
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.AdMobNetworkReport
import mir.oslav.adrevenue.data.model.AdMobReportRow

@Singleton
class AdMobApiClient @Inject constructor(
    private val json: Json,
) {
    private val httpClient: HttpClient = createDefaultHttpClient(json)

    suspend fun listAccounts(accessToken: String): List<AdMobAccount> {
        val url = "${AdMobApiConstants.ApiBaseUrl}/accounts"
        val response = httpClient.get(url) {
            bearerAuth(accessToken)
        }.readOrThrow<ListAccountsResponse>(url)

        return (response.account ?: response.accounts).orEmpty()
            .map { account ->
                AdMobAccount(
                    name = account.name,
                    publisherId = account.publisherId,
                    reportingTimeZone = account.reportingTimeZone,
                    currencyCode = account.currencyCode,
                )
            }
    }

    suspend fun generateNetworkReport(
        accessToken: String,
        accountName: String,
    ): AdMobNetworkReport = generateNetworkReport(
        accessToken = accessToken,
        accountName = accountName,
        request = NetworkReportRequest.lastThirtyDays(),
    )

    suspend fun generateNetworkReport(
        accessToken: String,
        accountName: String,
        startDateEpochMillis: Long,
        endDateEpochMillis: Long,
    ): AdMobNetworkReport {
        require(startDateEpochMillis <= endDateEpochMillis) {
            "The report start date must not be after its end date."
        }
        return generateNetworkReport(
            accessToken = accessToken,
            accountName = accountName,
            request = NetworkReportRequest.forDateRange(
                startDateEpochMillis = startDateEpochMillis,
                endDateEpochMillis = endDateEpochMillis,
            ),
        )
    }

    private suspend fun generateNetworkReport(
        accessToken: String,
        accountName: String,
        request: NetworkReportRequest,
    ): AdMobNetworkReport {
        val url = "${AdMobApiConstants.ApiBaseUrl}/$accountName/networkReport:generate"
        val response = httpClient.post(urlString = url) {
            bearerAuth(token = accessToken)
            contentType(type = ContentType.Application.Json)
            setBody(body = request)
        }.readOrThrow<JsonArray>(url = url)

        return parseNetworkReport(response)
    }

    private suspend inline fun <reified T> HttpResponse.readOrThrow(url: String): T {
        if (status.value !in 200..299) {
            val body = bodyAsText()
            val message = extractErrorMessage(body) ?: "AdMob API request failed."
            throw AdMobApiException(
                responseCode = status.value,
                url = url,
                responseBody = body,
                message = "AdMob API HTTP ${status.value}: $message",
            )
        }

        return body()
    }

    private fun parseNetworkReport(response: JsonArray): AdMobNetworkReport {
        var currencyCode = ""
        var startDate = ""
        var endDate = ""
        var matchingRowCount = 0
        val rows = mutableListOf<AdMobReportRow>()

        response.forEach { entryElement ->
            val entry = entryElement.asObjectOrNull() ?: return@forEach

            entry["header"].asObjectOrNull()?.let { header ->
                header["localizationSettings"].asObjectOrNull()?.let { localization ->
                    currencyCode = localization.stringValue("currencyCode")
                }
                header["dateRange"].asObjectOrNull()?.let { dateRange ->
                    startDate = dateRange["startDate"].asObjectOrNull()?.toReadableDate().orEmpty()
                    endDate = dateRange["endDate"].asObjectOrNull()?.toReadableDate().orEmpty()
                }
            }

            entry["row"].asObjectOrNull()?.let { row ->
                val dimensionValues =
                    row["dimensionValues"].asObjectOrNull() ?: JsonObject(emptyMap())
                val metricValues = row["metricValues"].asObjectOrNull() ?: JsonObject(emptyMap())
                val reportDate = dimensionValues["DATE"].asObjectOrNull()
                    ?.stringValue("value")
                    .orEmpty()
                    .toReportDate()
                rows += AdMobReportRow(
                    dateLabel = reportDate.label,
                    estimatedEarningsMicros = metricValues["ESTIMATED_EARNINGS"].asObjectOrNull()
                        ?.asLongValue(),
                    impressions = metricValues["IMPRESSIONS"].asObjectOrNull()?.asLongValue(),
                    clicks = metricValues["CLICKS"].asObjectOrNull()?.asLongValue(),
                    matchRate = metricValues["MATCH_RATE"].asObjectOrNull()?.asDoubleValue(),
                    impressionRpmMicros = metricValues["IMPRESSION_RPM"].asObjectOrNull()
                        ?.asLongValue(),
                    dateEpochMillis = reportDate.epochMillis,
                )
            }

            entry["footer"].asObjectOrNull()?.let { footer ->
                matchingRowCount = footer.intValue("matchingRowCount")
            }
        }

        return AdMobNetworkReport(
            currencyCode = currencyCode,
            dateRangeLabel = listOf(startDate, endDate)
                .filter { it.isNotBlank() }
                .joinToString(" - "),
            matchingRowCount = matchingRowCount,
            rows = rows.sortedByDescending { it.dateEpochMillis ?: Long.MIN_VALUE },
        )
    }

    private fun JsonObject.toReadableDate(): String {
        val calendar =
            Calendar.getInstance(TimeZone.getTimeZone(AdMobApiConstants.ReportTimeZone)).apply {
                set(Calendar.YEAR, intValue("year"))
                set(Calendar.MONTH, intValue("month") - 1)
                set(Calendar.DAY_OF_MONTH, intValue("day"))
            }
        return SimpleDateFormat("MMM d, yyyy", Locale.US).format(calendar.time)
    }

    private fun String.toReportDate(): ParsedReportDate {
        if (length != 8) return ParsedReportDate(label = this, epochMillis = null)
        return runCatching {
            val reportTimeZone = TimeZone.getTimeZone(AdMobApiConstants.ReportTimeZone)
            val input = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = reportTimeZone
            }
            val output = SimpleDateFormat("MMM d", Locale.US).apply {
                timeZone = reportTimeZone
            }
            val date = input.parse(this)!!
            ParsedReportDate(
                label = output.format(date),
                epochMillis = date.time,
            )
        }.getOrDefault(ParsedReportDate(label = this, epochMillis = null))
    }

    private fun JsonObject.asLongValue(): Long? =
        stringValue(name = "integerValue").toLongOrNull()
            ?: stringValue(name = "microsValue").toLongOrNull()
            ?: stringValue(name = "decimalValue").toDoubleOrNull()?.toLong()
            ?: stringValue(name = "doubleValue").toDoubleOrNull()?.toLong()

    private fun JsonObject.asDoubleValue(): Double? =
        stringValue(name = "decimalValue").toDoubleOrNull()
            ?: stringValue(name = "doubleValue").toDoubleOrNull()
            ?: stringValue(name = "integerValue").toDoubleOrNull()
            ?: stringValue(name = "microsValue").toDoubleOrNull()
                ?.div(other = 1_000_000.0)

    private fun extractErrorMessage(body: String): String? =
        runCatching {
            json.parseToJsonElement(string = body)
                .asObjectOrNull()
                ?.get(key = "error")
                .asObjectOrNull()
                ?.stringValue(name = "message")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

    private fun JsonObject.stringValue(name: String): String =
        (get(name) as? JsonPrimitive)?.contentOrNull.orEmpty()

    private fun JsonObject.intValue(name: String): Int =
        stringValue(name).toIntOrNull()
            ?: (get(name) as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0

    private fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

    private companion object {
        fun createDefaultHttpClient(json: Json): HttpClient = HttpClient(engineFactory = Android) {
            expectSuccess = false
            install(plugin = ContentNegotiation) {
                json(json = json)
            }
            install(plugin = HttpTimeout) {
                connectTimeoutMillis = AdMobApiConstants.ConnectTimeoutMillis
                requestTimeoutMillis = AdMobApiConstants.ReadTimeoutMillis
                socketTimeoutMillis = AdMobApiConstants.ReadTimeoutMillis
            }
            engine {
                connectTimeout = AdMobApiConstants.ConnectTimeoutMillis.toInt()
                socketTimeout = AdMobApiConstants.ReadTimeoutMillis.toInt()
            }
        }
    }
}

private data class ParsedReportDate(
    val label: String,
    val epochMillis: Long?,
)

@Keep
@Serializable
private data class ListAccountsResponse(
    val account: List<AdMobAccountDto>? = null,
    val accounts: List<AdMobAccountDto>? = null,
)

@Keep
@Serializable
private data class AdMobAccountDto(
    val name: String,
    val publisherId: String = "",
    val reportingTimeZone: String = "",
    val currencyCode: String = "",
)

@Keep
@Serializable
private data class NetworkReportRequest(
    val reportSpec: NetworkReportSpec,
) {
    companion object {
        fun lastThirtyDays(): NetworkReportRequest {
            val timeZone = TimeZone.getTimeZone(AdMobApiConstants.ReportTimeZone)
            val end = Calendar.getInstance(timeZone)
            val start = end.clone() as Calendar
            start.add(Calendar.DAY_OF_MONTH, -29)

            return forCalendars(start = start, end = end)
        }

        fun forDateRange(
            startDateEpochMillis: Long,
            endDateEpochMillis: Long,
        ): NetworkReportRequest {
            val timeZone = TimeZone.getTimeZone(AdMobApiConstants.ReportTimeZone)
            val start = Calendar.getInstance(timeZone).apply {
                timeInMillis = startDateEpochMillis
            }
            val end = Calendar.getInstance(timeZone).apply {
                timeInMillis = endDateEpochMillis
            }
            return forCalendars(start = start, end = end)
        }

        private fun forCalendars(start: Calendar, end: Calendar): NetworkReportRequest {
            return NetworkReportRequest(
                reportSpec = NetworkReportSpec(
                    dateRange = NetworkReportDateRange(
                        startDate = start.toAdMobDate(),
                        endDate = end.toAdMobDate(),
                    ),
                    timeZone = AdMobApiConstants.ReportTimeZone,
                    dimensions = AdMobApiConstants.NetworkReportDimensions,
                    metrics = AdMobApiConstants.NetworkReportMetrics,
                ),
            )
        }

        private fun Calendar.toAdMobDate(): NetworkReportDate =
            NetworkReportDate(
                year = get(Calendar.YEAR),
                month = get(Calendar.MONTH) + 1,
                day = get(Calendar.DAY_OF_MONTH),
            )
    }
}

@Keep
@Serializable
private data class NetworkReportSpec(
    val dateRange: NetworkReportDateRange,
    val timeZone: String,
    val dimensions: List<String>,
    val metrics: List<String>,
)

@Keep
@Serializable
private data class NetworkReportDateRange(
    val startDate: NetworkReportDate,
    val endDate: NetworkReportDate,
)

@Keep
@Serializable
private data class NetworkReportDate(
    val year: Int,
    val month: Int,
    val day: Int,
)
