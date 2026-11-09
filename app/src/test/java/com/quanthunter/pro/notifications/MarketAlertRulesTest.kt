package com.quanthunter.pro.notifications

import com.quanthunter.pro.data.Candle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarketAlertRulesTest {
    private fun candle(hour: Long, close: Double) = Candle(
        timestampEpochMillis = hour * 3_600_000L,
        open = close, high = close, low = close, close = close, volume = 10.0
    )

    @Test fun ignoresCurrentUnclosedHourlyCandle() {
        val now = 2L * 3_600_000L + 30L * 60_000L
        val result = MarketAlertRules.evaluate("BTCUSDT", listOf(candle(1, 100.0), candle(2, 100.0), candle(3, 110.0)), now)
        assertNull(result)
    }

    @Test fun emitsSignalForClosedCandleAtThreshold() {
        val now = 3L * 3_600_000L
        val result = MarketAlertRules.evaluate("BTCUSDT", listOf(candle(1, 100.0), candle(2, 103.0)), now)
        assertEquals("BTCUSDT", result?.symbol)
        assertEquals(2L * 3_600_000L, result?.candleTimestampEpochMillis)
        assertEquals(3.0, result?.changePercent ?: 0.0, 0.0001)
    }

    @Test fun ignoresMovesBelowThreshold() {
        val now = 3L * 3_600_000L
        assertNull(MarketAlertRules.evaluate("BTCUSDT", listOf(candle(1, 100.0), candle(2, 101.0)), now))
    }

    @Test fun supportsDownwardSignals() {
        val now = 3L * 3_600_000L
        val result = MarketAlertRules.evaluate("BTCUSDT", listOf(candle(1, 100.0), candle(2, 97.0)), now)
        assertEquals(-3.0, result?.changePercent ?: 0.0, 0.0001)
    }
}
