package com.quanthunter.pro.paper

import com.quanthunter.pro.engine.Direction
import org.junit.Assert.*
import org.junit.Test

class PaperTradeCalculatorTest {
    private val calculator = PaperTradeCalculator()

    @Test fun profitableLongIncludesExecutionCosts() {
        val result = calculator.calculate(PaperTradeRequest(Direction.BULLISH, 1.0, 100.0, 110.0, 0.1, 0.05))
        assertTrue(result.grossPnl > 0.0)
        assertTrue(result.fees > 0.0)
        assertTrue(result.netPnl < result.grossPnl)
    }

    @Test fun profitableShortUsesAdverseSlippageOnBothSides() {
        val result = calculator.calculate(PaperTradeRequest(Direction.BEARISH, 2.0, 100.0, 90.0, 0.1, 0.05))
        assertTrue(result.entryFillPrice < 100.0)
        assertTrue(result.exitFillPrice > 90.0)
        assertTrue(result.netPnl > 0.0)
    }

    @Test fun costsCanTurnSmallGrossProfitIntoNetLoss() {
        val result = calculator.calculate(PaperTradeRequest(Direction.BULLISH, 1.0, 100.0, 100.05, 0.1, 0.05))
        assertTrue(result.netPnl < result.grossPnl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun neutralDirectionIsRejected() {
        calculator.calculate(PaperTradeRequest(Direction.NEUTRAL, 1.0, 100.0, 110.0))
    }
}
