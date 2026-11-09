package com.quanthunter.pro.risk

import com.quanthunter.pro.engine.Direction
import org.junit.Assert.*
import org.junit.Test

class RiskCalculatorTest {
    private val calculator = RiskCalculator()

    @Test fun calculatesQuantityFromRiskBudgetAndStopDistance() {
        val result = calculator.calculate(RiskRequest(
            accountEquity = 10_000.0, riskPercent = 1.0,
            entryPrice = 100.0, stopPrice = 90.0, direction = Direction.BULLISH,
            maxExposurePercent = 100.0
        ))
        assertEquals(100.0, result.riskBudget, 0.0001)
        assertEquals(10.0, result.quantity, 0.0001)
        assertEquals(100.0, result.estimatedStopLoss, 0.0001)
        assertFalse(result.exposureCapApplied)
    }

    @Test fun exposureLimitReducesPositionSize() {
        val result = calculator.calculate(RiskRequest(
            accountEquity = 10_000.0, riskPercent = 2.0,
            entryPrice = 100.0, stopPrice = 99.0, direction = Direction.BULLISH,
            maxExposurePercent = 10.0
        ))
        assertTrue(result.exposureCapApplied)
        assertEquals(1_000.0, result.notionalExposure, 0.0001)
        assertTrue(result.effectiveRiskPercent < 2.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsStopOnWrongSide() {
        calculator.calculate(RiskRequest(
            10_000.0, 1.0, 100.0, 105.0, Direction.BULLISH
        ))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNeutralDirection() {
        calculator.calculate(RiskRequest(
            10_000.0, 1.0, 100.0, 95.0, Direction.NEUTRAL
        ))
    }
}
