package com.quanthunter.pro.backtest

import com.quanthunter.pro.engine.Direction
import org.junit.Assert.*
import org.junit.Test

class BacktestEngineTest {
    private val engine = BacktestEngine()

    private fun bars(signals: List<Direction>) = signals.mapIndexed { index, signal ->
        BacktestBar(index * 60_000L + 1L, 100.0 + index, 100.5 + index, signal)
    }

    @Test fun chargesFeesSpreadAndSlippageAndReportsOutOfSampleTrades() {
        val result = engine.run(BacktestRequest(
            bars = bars(listOf(Direction.BULLISH, Direction.BULLISH, Direction.NEUTRAL, Direction.BEARISH, Direction.NEUTRAL)),
            initialCapital = 10_000.0,
            feePercentPerSide = 0.1,
            spreadPercentRoundTrip = 0.02,
            slippagePercentPerSide = 0.05,
            outOfSampleStartIndex = 2
        ))
        assertEquals(2, result.tradeCount)
        assertTrue(result.totalFees > 0.0)
        assertEquals(2, result.outOfSampleTradeCount)
        assertEquals(100.0, result.winRatePercent, 100.0)
        assertTrue(result.finalCapital.isFinite())
    }

    @Test fun noSignalProducesNoTradesAndNoFees() {
        val result = engine.run(BacktestRequest(
            bars = bars(List(4) { Direction.NEUTRAL }),
            initialCapital = 1_000.0,
            outOfSampleStartIndex = 2
        ))
        assertEquals(0, result.tradeCount)
        assertEquals(0.0, result.totalFees, 0.0)
        assertEquals(0.0, result.netReturnPercent, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonChronologicalBars() {
        engine.run(BacktestRequest(
            bars = listOf(
                BacktestBar(2L, 100.0, 101.0, Direction.BULLISH),
                BacktestBar(1L, 101.0, 102.0, Direction.NEUTRAL),
                BacktestBar(3L, 102.0, 103.0, Direction.NEUTRAL)
            ),
            initialCapital = 1_000.0,
            outOfSampleStartIndex = 1
        ))
    }
}
