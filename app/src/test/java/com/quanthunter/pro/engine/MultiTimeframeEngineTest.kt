package com.quanthunter.pro.engine

import org.junit.Assert.*
import org.junit.Test

class MultiTimeframeEngineTest {
    private val engine = MultiTimeframeEngine()

    @Test fun alignedTimeframesProduceDirectionalAssessment() {
        val result = engine.assess(listOf(
            TimeframeSignal("15m", 70, 1.0),
            TimeframeSignal("1h", 65, 1.5),
            TimeframeSignal("4h", 80, 2.0)
        ))
        assertEquals(Direction.BULLISH, result.direction)
        assertTrue(result.agreementPercent >= 99)
        assertTrue(result.confidence in 0..90)
    }

    @Test fun opposingTimeframesReduceAgreement() {
        val result = engine.assess(listOf(TimeframeSignal("15m", 90, 1.0), TimeframeSignal("4h", -80, 1.0)))
        assertTrue(result.agreementPercent < 60)
        assertTrue(result.reasons.any { it.contains("conflict") })
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateTimeframesAreRejected() {
        engine.assess(listOf(TimeframeSignal("1h", 20, 1.0), TimeframeSignal("1h", 30, 1.0)))
    }
}
