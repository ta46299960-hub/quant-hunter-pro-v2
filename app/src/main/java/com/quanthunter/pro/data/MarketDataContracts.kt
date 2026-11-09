package com.quanthunter.pro.data

import com.quanthunter.pro.engine.MarketType

enum class CandleInterval(val durationMillis: Long) {
    M1(60_000L), M5(300_000L), M15(900_000L), H1(3_600_000L),
    H4(14_400_000L), D1(86_400_000L), W1(604_800_000L)
}

data class Candle(
    val timestampEpochMillis: Long, val open: Double, val high: Double,
    val low: Double, val close: Double, val volume: Double
) {
    fun hasValidValues(): Boolean =
        timestampEpochMillis > 0L &&
            listOf(open, high, low, close, volume).all { it.isFinite() } &&
            open > 0.0 && high > 0.0 && low > 0.0 && close > 0.0 &&
            volume >= 0.0 && high >= low &&
            high >= maxOf(open, close) && low <= minOf(open, close)
}

data class CandleRequest(
    val symbol: String, val market: MarketType, val interval: CandleInterval,
    val limit: Int = 200
) {
    init {
        require(symbol.isNotBlank()) { "Symbol is required" }
        require(limit in 2..2_000) { "Limit must be between 2 and 2000" }
    }
}

data class DataEnvelope<T>(
    val data: T?, val source: String, val fetchedAtEpochMillis: Long,
    val isLive: Boolean, val error: String? = null
) {
    val isSuccessful: Boolean
        get() = data != null && source.isNotBlank() && fetchedAtEpochMillis > 0L && error == null
}

/** Provider adapters isolate vendor parsing from the analysis engine. Network work must not run on the UI thread. */
interface MarketDataProvider {
    val providerId: String
    fun fetchCandles(request: CandleRequest): DataEnvelope<List<Candle>>
}
