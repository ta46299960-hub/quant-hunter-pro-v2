package com.quanthunter.pro.engine

enum class MarketType { CRYPTO, FOREX, STOCKS, INDICES, COMMODITIES }

enum class DataStatus { LIVE, DELAYED, DEMO, STALE, UNAVAILABLE }

data class MarketQuote(
    val symbol: String,
    val market: MarketType,
    val price: Double?,
    val changePercent: Double?,
    val volume: Double?,
    val timestampEpochMillis: Long?,
    val status: DataStatus,
    val source: String
) {
    fun isUsable(nowMillis: Long, maxAgeMillis: Long = 120_000L): Boolean {
        val time = timestampEpochMillis ?: return false
        return price != null && price.isFinite() && price > 0.0 &&
            nowMillis >= time && nowMillis - time <= maxAgeMillis &&
            status != DataStatus.UNAVAILABLE && status != DataStatus.STALE
    }
}

data class SignalAssessment(
    val score: Int,
    val confidence: Int,
    val direction: Direction,
    val reasons: List<String>
)

enum class Direction { BULLISH, BEARISH, NEUTRAL }
