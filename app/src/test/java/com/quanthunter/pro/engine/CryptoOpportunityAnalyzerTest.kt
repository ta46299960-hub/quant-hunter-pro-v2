package com.quanthunter.pro.engine

import com.quanthunter.pro.data.Candle
import com.quanthunter.pro.data.CandleInterval
import org.junit.Assert.*
import org.junit.Test

class CryptoOpportunityAnalyzerTest {
    private val analyzer = CryptoOpportunityAnalyzer()

    @Test fun insufficientTimeframesNeverProduceAssessment() {
        val candles = risingCandles(100.0, 40, 60_000L)
        val result = analyzer.assess("BTCUSDT", mapOf(CandleInterval.H1 to candles), candles.last().timestampEpochMillis + 1)
        assertNull(result)
    }

    @Test fun thirtyTwoRawCandlesAreInsufficientWhenOneIsStillForming() {
        val now = 2_000_000_000_000L
        val data = mapOf(
            CandleInterval.M15 to risingCandles(100.0, 32, CandleInterval.M15.durationMillis, now),
            CandleInterval.H1 to risingCandles(100.0, 32, CandleInterval.H1.durationMillis, now),
            CandleInterval.H4 to risingCandles(100.0, 32, CandleInterval.H4.durationMillis, now)
        )
        assertNull(analyzer.assess("BTCUSDT", data, now + 1))
    }

    @Test fun validAlignedTimeframesProduceExplainableAssessment() {
        val now = 2_000_000_000_000L
        val data = mapOf(
            CandleInterval.M15 to risingCandles(100.0, 64, CandleInterval.M15.durationMillis, now),
            CandleInterval.H1 to risingCandles(100.0, 64, CandleInterval.H1.durationMillis, now),
            CandleInterval.H4 to risingCandles(100.0, 64, CandleInterval.H4.durationMillis, now)
        )
        val result = analyzer.assess("BTCUSDT", data, now + 1)
        assertNotNull(result)
        assertEquals("BTCUSDT", result!!.symbol)
        assertEquals(3, result.timeframeCount)
        assertTrue(result.reasons.isNotEmpty())
        assertTrue(result.confidence in 0..85)
    }

    private fun risingCandles(
        start: Double, count: Int, interval: Long, now: Long = 2_000_000_000_000L
    ): List<Candle> = (0 until count).map { i ->
        val close = start + i * 0.8
        Candle(
            timestampEpochMillis = now - (count - 1 - i) * interval,
            open = close - 0.3, high = close + 0.5, low = close - 0.5,
            close = close, volume = 100.0 + i
        )
    }
}
