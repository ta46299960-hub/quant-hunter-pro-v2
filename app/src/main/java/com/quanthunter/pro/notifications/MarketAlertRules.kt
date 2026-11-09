package com.quanthunter.pro.notifications

import com.quanthunter.pro.data.Candle
import com.quanthunter.pro.data.CandleInterval
import kotlin.math.abs

data class MarketAlertSignal(
    val symbol: String,
    val candleTimestampEpochMillis: Long,
    val changePercent: Double
)

/** Generates alerts only from two fully closed hourly candles. */
object MarketAlertRules {
    fun evaluate(
        symbol: String,
        candles: List<Candle>,
        nowMillis: Long,
        thresholdPercent: Double = 2.0
    ): MarketAlertSignal? {
        require(nowMillis > 0L)
        require(thresholdPercent.isFinite() && thresholdPercent > 0.0)
        val closed = candles.asSequence()
            .filter { it.timestampEpochMillis + CandleInterval.H1.durationMillis <= nowMillis }
            .filter { it.hasValidValues() }
            .distinctBy { it.timestampEpochMillis }
            .sortedBy { it.timestampEpochMillis }
            .toList()
        if (closed.size < 2) return null
        val previous = closed[closed.lastIndex - 1]
        val latest = closed.last()
        if (previous.close <= 0.0) return null
        val change = (latest.close / previous.close - 1.0) * 100.0
        if (!change.isFinite() || abs(change) < thresholdPercent) return null
        return MarketAlertSignal(symbol, latest.timestampEpochMillis, change)
    }
}
