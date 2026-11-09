package com.quanthunter.pro.engine

import org.junit.Assert.*
import org.junit.Test

class HybridScoringEngineTest {
    private val engine = HybridScoringEngine()

    @Test fun alignedPositiveInputsProduceBullishAssessment() {
        val result = engine.assess(MarketType.CRYPTO, 80, 70, 60, 40, 75)
        assertEquals(Direction.BULLISH, result.direction)
        assertTrue(result.score > 0)
        assertTrue(result.confidence in 35..85)
    }

    @Test fun mixedInputsCanRemainNeutral() {
        val result = engine.assess(MarketType.FOREX, 10, -10, 0, 5, -5)
        assertEquals(Direction.NEUTRAL, result.direction)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeInput() {
        engine.assess(MarketType.STOCKS, 101, 0, 0, 0, 0)
    }

    @Test fun staleQuotesAreNotUsable() {
        val quote = MarketQuote("BTCUSDT", MarketType.CRYPTO, 65000.0, 1.0, 1000.0,
            1_000L, DataStatus.LIVE, "test")
        assertFalse(quote.isUsable(nowMillis = 500_000L))
    }
}
