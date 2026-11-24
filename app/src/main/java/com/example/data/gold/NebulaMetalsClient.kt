package com.example.data.gold

import com.example.BuildConfig
import com.example.data.local.GoldRateSnapshotEntity
import com.example.domain.gold.GoldRateSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

interface GoldMarketRateProvider {
    suspend fun fetchRates(city: String): Result<GoldRateSnapshotEntity>
}

data class MarketFetchEligibility(
    val isEligibleNow: Boolean,
    val istDateIso: String,
    val reason: String,
)

object GoldMarketSchedule {
    val IST_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")
    val MARKET_OPEN_TIME: LocalTime = LocalTime.of(10, 0)

    fun evaluateFetchWindow(nowIst: ZonedDateTime = ZonedDateTime.now(IST_ZONE)): MarketFetchEligibility {
        val dateIso = nowIst.toLocalDate().toString()
        val dow = nowIst.dayOfWeek
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return MarketFetchEligibility(
                isEligibleNow = false,
                istDateIso = dateIso,
                reason = "Markets are closed on weekends (${dow.name.lowercase().replaceFirstChar { it.uppercase() }}). Showing last cached rate.",
            )
        }
        val time = nowIst.toLocalTime()
        if (time.isBefore(MARKET_OPEN_TIME)) {
            return MarketFetchEligibility(
                isEligibleNow = false,
                istDateIso = dateIso,
                reason = "Daily rates publish after 10:00 AM IST. Showing last cached rate.",
            )
        }
        return MarketFetchEligibility(
            isEligibleNow = true,
            istDateIso = dateIso,
            reason = "Within daily NebulaMetals market window.",
        )
    }

    fun isRateStale(rateDateIso: String?, nowIst: ZonedDateTime = ZonedDateTime.now(IST_ZONE)): Boolean {
        if (rateDateIso.isNullOrBlank()) return true
        val latestExpectedDate = when (nowIst.dayOfWeek) {
            DayOfWeek.SATURDAY -> nowIst.toLocalDate().minusDays(1)
            DayOfWeek.SUNDAY -> nowIst.toLocalDate().minusDays(2)
            DayOfWeek.MONDAY -> if (nowIst.toLocalTime().isBefore(MARKET_OPEN_TIME)) {
                nowIst.toLocalDate().minusDays(3)
            } else nowIst.toLocalDate()
            else -> if (nowIst.toLocalTime().isBefore(MARKET_OPEN_TIME)) {
                nowIst.toLocalDate().minusDays(1)
            } else nowIst.toLocalDate()
        }
        val parsed = runCatching { java.time.LocalDate.parse(rateDateIso) }.getOrNull() ?: return true
        return parsed.isBefore(latestExpectedDate)
    }
}

class NebulaMetalsClient(
    private val apiKeyProvider: () -> String = { BuildConfig.NEBULA_API_KEY },
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
) : GoldMarketRateProvider {

    override suspend fun fetchRates(city: String): Result<GoldRateSnapshotEntity> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isEmpty() || apiKey == "MY_NEBULA_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("NebulaMetals API key is not configured in Secrets. Enter NEBULA_API_KEY or add a manual market rate."),
            )
        }

        val url = "https://workspace.nebulaapi.com/gold_price"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("city", city.ifBlank { "Chennai" })
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("Accept", "application/json")
            .get()
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("NebulaMetals HTTP ${response.code}: ${body.take(140).ifBlank { response.message }}")
                }
                parseNebulaMetalsResponse(body, fallbackCity = city)
            }
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val HUNDRED = BigDecimal(100)

        fun parseNebulaMetalsResponse(
            rawJson: String,
            fallbackCity: String = "Chennai",
            fetchedAtMillis: Long = System.currentTimeMillis(),
        ): GoldRateSnapshotEntity {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val dateIso = root.str("date")
                ?: ZonedDateTime.now(GoldMarketSchedule.IST_ZONE).toLocalDate().toString()
            val city = root.str("city") ?: fallbackCity
            val market = root.str("market") ?: ""
            val timestampIso = root.str("timestamp") ?: ""
            val prices = root["prices"]?.jsonObject ?: error("Missing prices object in response.")
            val meta = root["metadata"]?.jsonObject

            val gstIncluded = meta?.get("gst_included")?.jsonPrimitive?.booleanOrNull ?: true
            val sourcesUsed = meta?.get("sources_used")?.jsonPrimitive?.intOrNull ?: 0
            val confidence = meta?.str("confidence") ?: ""

            val g24 = parsePurityBlock(prices["gold24k"])
            val g22 = parsePurityBlock(prices["gold22k"])
            val g18 = parsePurityBlock(prices["gold18k"])
            val g14 = parsePurityBlock(prices["gold14k"])

            require(g24.finalPaise > 0L) { "24K gold price missing or invalid in API response." }

            return GoldRateSnapshotEntity(
                city = city,
                market = market,
                rateDateIso = dateIso,
                source = GoldRateSource.NEBULA_METALS.name,
                fetchedAtMillis = fetchedAtMillis,
                apiTimestampIso = timestampIso,
                gstIncluded = gstIncluded,
                sourcesUsed = sourcesUsed,
                confidence = confidence,
                rate24kBasePaise = g24.basePaise,
                rate24kGstPaise = g24.gstPaise,
                rate24kFinalPaise = g24.finalPaise,
                rate22kBasePaise = g22.basePaise,
                rate22kGstPaise = g22.gstPaise,
                rate22kFinalPaise = g22.finalPaise,
                rate18kBasePaise = g18.basePaise,
                rate18kGstPaise = g18.gstPaise,
                rate18kFinalPaise = g18.finalPaise,
                rate14kBasePaise = g14.basePaise,
                rate14kGstPaise = g14.gstPaise,
                rate14kFinalPaise = g14.finalPaise,
            )
        }

        private data class ParsedPurityRate(
            val basePaise: Long,
            val gstPaise: Long,
            val finalPaise: Long,
        )

        private fun parsePurityBlock(elem: JsonElement?): ParsedPurityRate {
            if (elem !is JsonObject) return ParsedPurityRate(0L, 0L, 0L)
            val base = elem.rupeesFieldToPaise("basePricePerGram")
            val gst = elem.rupeesFieldToPaise("gstAmountPerGram")
            val final = elem.rupeesFieldToPaise("finalPricePerGram")
                .takeIf { it > 0L }
                ?: elem.rupeesFieldToPaise("pricePerGram").takeIf { it > 0L }
                ?: (elem.rupeesFieldToPaise("priceFor10g") / 10L)
            val resolvedBase = if (base > 0L) base else (final - gst).coerceAtLeast(0L)
            return ParsedPurityRate(
                basePaise = resolvedBase,
                gstPaise = gst,
                finalPaise = final,
            )
        }

        private fun JsonObject.str(key: String): String? {
            return runCatching { this[key]?.jsonPrimitive?.content }.getOrNull()?.takeIf { it.isNotBlank() && it != "null" }
        }

        private fun JsonObject.rupeesFieldToPaise(key: String): Long {
            val raw = str(key) ?: return 0L
            return runCatching {
                BigDecimal(raw).multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).toLong()
            }.getOrDefault(0L)
        }
    }
}
