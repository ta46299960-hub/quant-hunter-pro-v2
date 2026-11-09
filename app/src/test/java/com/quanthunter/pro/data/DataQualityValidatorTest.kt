package com.quanthunter.pro.data

import org.junit.Assert.*
import org.junit.Test

class DataQualityValidatorTest {
    private val validator = DataQualityValidator()
    private val now = 10_000_000L
    private fun candle(time: Long, close: Double = 10.0) =
        Candle(time, 9.0, maxOf(11.0, close), 8.0, close, 100.0)

    @Test fun acceptsFreshValidCandles() {
        val result = validator.validate(listOf(candle(now - 120_000), candle(now - 60_000)), CandleInterval.M1, now)
        assertTrue(result.isUsable)
        assertEquals(2, result.acceptedCandles.size)
    }

    @Test fun rejectsMalformedAndFutureCandles() {
        val malformed = Candle(now - 60_000, 9.0, 8.0, 7.0, 9.0, 1.0)
        val result = validator.validate(listOf(malformed, candle(now + 60_000)), CandleInterval.M1, now)
        assertEquals(2, result.rejectedCount)
        assertFalse(result.isUsable)
    }

    @Test fun ignoresDuplicateTimestamps() {
        val item = candle(now - 60_000)
        val result = validator.validate(listOf(item, item), CandleInterval.M1, now)
        assertEquals(1, result.duplicateCount)
        assertEquals(1, result.acceptedCandles.size)
    }

    @Test fun marksOldDataAsStale() {
        val result = validator.validate(listOf(candle(now - 600_000), candle(now - 540_000)), CandleInterval.M1, now)
        assertFalse(result.isFresh)
        assertFalse(result.isUsable)
    }
}
