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

    @Test fun validCandleSurvivesMalformedDuplicateTimestamp() {
        val malformed = Candle(now - 60_000, 9.0, 8.0, 7.0, 9.0, 1.0)
        val valid = candle(now - 60_000)
        val result = validator.validate(
            listOf(malformed, valid, valid, candle(now - 120_000)),
            CandleInterval.M1, now
        )
        assertEquals(1, result.rejectedCount)
        assertEquals(1, result.duplicateCount)
        assertTrue(result.acceptedCandles.any { it.timestampEpochMillis == now - 60_000 })
    }

    @Test fun excludesCurrentFormingCandleUntilItsIntervalCloses() {
        val previous = candle(now - 90_000)
        val forming = candle(now - 30_000)
        val beforeClose = validator.validate(listOf(previous, forming), CandleInterval.M1, now)
        assertEquals(1, beforeClose.formingCount)
        assertEquals(1, beforeClose.acceptedCandles.size)
        assertFalse(beforeClose.isUsable)
        assertTrue(beforeClose.warnings.any { it.contains("still-open") })

        val atClose = validator.validate(listOf(previous, forming), CandleInterval.M1, now + 30_000)
        assertEquals(0, atClose.formingCount)
        assertEquals(2, atClose.acceptedCandles.size)
        assertTrue(atClose.isUsable)
    }

    @Test fun rejectsGapsInOtherwiseFreshCandleSeries() {
        val result = validator.validate(
            listOf(candle(now - 180_000), candle(now - 60_000)),
            CandleInterval.M1,
            now
        )
        assertEquals(1, result.gapCount)
        assertTrue(result.warnings.any { it.contains("missing or irregular") })
        assertFalse(result.isUsable)
    }

    @Test fun acceptsContiguousCandlesAfterDuplicateRemoval() {
        val first = candle(now - 120_000)
        val second = candle(now - 60_000)
        val result = validator.validate(
            listOf(first, second, second),
            CandleInterval.M1,
            now
        )
        assertEquals(1, result.duplicateCount)
        assertEquals(0, result.gapCount)
        assertTrue(result.isUsable)
    }

    @Test fun marksOldDataAsStale() {
        val result = validator.validate(listOf(candle(now - 600_000), candle(now - 540_000)), CandleInterval.M1, now)
        assertFalse(result.isFresh)
        assertFalse(result.isUsable)
    }
}
