package com.quanthunter.pro.data

import com.quanthunter.pro.engine.MarketType
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Public Binance Spot candles adapter.
 * Supports CRYPTO only; it does not require an API key for public market data.
 * Call from a background thread. Never pass credentials to this provider.
 */
class BinanceSpotProvider(
    private val connectTimeoutMillis: Int = 8_000,
    private val readTimeoutMillis: Int = 10_000
) : MarketDataProvider {
    override val providerId: String = "Binance Public Spot API"

    override fun fetchCandles(request: CandleRequest): DataEnvelope<List<Candle>> {
        if (request.market != MarketType.CRYPTO) {
            return failure("This provider supports CRYPTO markets only")
        }
        val symbol = request.symbol.trim().uppercase()
        if (!symbol.matches(Regex("[A-Z0-9]{5,20}"))) {
            return failure("Invalid crypto symbol. Use a pair such as BTCUSDT")
        }
        val interval = when (request.interval) {
            CandleInterval.M1 -> "1m"
            CandleInterval.M5 -> "5m"
            CandleInterval.M15 -> "15m"
            CandleInterval.H1 -> "1h"
            CandleInterval.H4 -> "4h"
            CandleInterval.D1 -> "1d"
            CandleInterval.W1 -> "1w"
        }
        var connection: HttpURLConnection? = null
        return try {
            val endpoint = "https://api.binance.com/api/v3/klines" +
                "?symbol=$symbol&interval=$interval&limit=${request.limit}"
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMillis
                readTimeout = readTimeoutMillis
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "QuantHunterProV2/2.0")
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                failure("Provider HTTP $status: ${body.take(180)}")
            } else {
                val rows = JSONArray(body)
                val candles = ArrayList<Candle>(rows.length())
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONArray(i) ?: continue
                    if (row.length() < 6) continue
                    candles += Candle(
                        timestampEpochMillis = row.getLong(0),
                        open = row.getString(1).toDouble(),
                        high = row.getString(2).toDouble(),
                        low = row.getString(3).toDouble(),
                        close = row.getString(4).toDouble(),
                        volume = row.getString(5).toDouble()
                    )
                }
                if (candles.isEmpty()) failure("Provider returned no candle data")
                else DataEnvelope(candles, providerId, System.currentTimeMillis(), true)
            }
        } catch (e: Exception) {
            failure(e.message?.take(180) ?: "Network or parsing error")
        } finally {
            connection?.disconnect()
        }
    }

    private fun failure(message: String) =
        DataEnvelope<List<Candle>>(null, providerId, System.currentTimeMillis(), false, message)
}
