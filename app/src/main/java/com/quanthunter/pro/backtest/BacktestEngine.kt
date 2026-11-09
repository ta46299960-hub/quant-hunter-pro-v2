package com.quanthunter.pro.backtest

import com.quanthunter.pro.engine.Direction
import kotlin.math.max

data class BacktestBar(
    val timestampEpochMillis: Long,
    val open: Double,
    val close: Double,
    /** Signal computed only from information available before this bar opens. */
    val signalAtOpen: Direction
)

data class BacktestRequest(
    val bars: List<BacktestBar>,
    val initialCapital: Double,
    val positionFraction: Double = 1.0,
    val feePercentPerSide: Double = 0.1,
    val spreadPercentRoundTrip: Double = 0.02,
    val slippagePercentPerSide: Double = 0.05,
    /** First index included in out-of-sample metrics. */
    val outOfSampleStartIndex: Int
)

data class BacktestTrade(
    val direction: Direction,
    val entryIndex: Int,
    val exitIndex: Int,
    val entryFill: Double,
    val exitFill: Double,
    val grossPnl: Double,
    val fees: Double,
    val netPnl: Double,
    val outOfSample: Boolean
)

data class BacktestResult(
    val initialCapital: Double,
    val finalCapital: Double,
    val netReturnPercent: Double,
    val maxDrawdownPercent: Double,
    val tradeCount: Int,
    val winningTrades: Int,
    val winRatePercent: Double,
    val totalFees: Double,
    val outOfSampleNetPnl: Double,
    val outOfSampleTradeCount: Int,
    val trades: List<BacktestTrade>
)

/**
 * Simple one-position research simulator. A signal on a bar is assumed to have
 * been calculated before that bar opens; fills occur at its open. Fees, half-spread
 * per side and slippage are applied adversely. It is not a broker or execution model.
 */
class BacktestEngine {
    fun run(request: BacktestRequest): BacktestResult {
        require(request.bars.size >= 3) { "At least three bars are required" }
        require(request.initialCapital.isFinite() && request.initialCapital > 0.0)
        require(request.positionFraction.isFinite() && request.positionFraction in 0.01..1.0)
        require(request.feePercentPerSide.isFinite() && request.feePercentPerSide in 0.0..5.0)
        require(request.spreadPercentRoundTrip.isFinite() && request.spreadPercentRoundTrip in 0.0..5.0)
        require(request.slippagePercentPerSide.isFinite() && request.slippagePercentPerSide in 0.0..5.0)
        require(request.outOfSampleStartIndex in 1 until request.bars.size)
        require(request.bars.all {
            it.timestampEpochMillis > 0L && it.open.isFinite() && it.close.isFinite() &&
                it.open > 0.0 && it.close > 0.0
        }) { "Bars contain invalid timestamps or prices" }
        require(request.bars.zipWithNext().all { (a, b) -> b.timestampEpochMillis > a.timestampEpochMillis }) {
            "Bars must be strictly chronological"
        }

        data class Position(
            val direction: Direction,
            val entryIndex: Int,
            val entryFill: Double,
            val quantity: Double,
            val entryFee: Double
        )

        val trades = mutableListOf<BacktestTrade>()
        var capital = request.initialCapital
        var peak = capital
        var maxDrawdown = 0.0
        var position: Position? = null
        val halfSpread = request.spreadPercentRoundTrip / 200.0
        val adverse = halfSpread + request.slippagePercentPerSide / 100.0

        fun closePosition(index: Int, rawPrice: Double) {
            val p = position ?: return
            val exitFill = rawPrice * if (p.direction == Direction.BULLISH) (1.0 - adverse) else (1.0 + adverse)
            val exitNotional = exitFill * p.quantity
            val exitFee = exitNotional * request.feePercentPerSide / 100.0
            val gross = if (p.direction == Direction.BULLISH)
                (exitFill - p.entryFill) * p.quantity
            else (p.entryFill - exitFill) * p.quantity
            val fees = p.entryFee + exitFee
            val net = gross - fees
            capital += net
            trades += BacktestTrade(
                p.direction, p.entryIndex, index, p.entryFill, exitFill, gross, fees, net,
                p.entryIndex >= request.outOfSampleStartIndex
            )
            position = null
        }

        request.bars.forEachIndexed { index, bar ->
            val desired = bar.signalAtOpen
            if (position != null && desired != position!!.direction) closePosition(index, bar.open)
            if (position == null && desired != Direction.NEUTRAL) {
                val entryFill = bar.open * if (desired == Direction.BULLISH) (1.0 + adverse) else (1.0 - adverse)
                val quantity = (capital * request.positionFraction) / entryFill
                val fee = entryFill * quantity * request.feePercentPerSide / 100.0
                position = Position(desired, index, entryFill, quantity, fee)
            }
            peak = max(peak, capital)
            if (peak > 0.0) maxDrawdown = max(maxDrawdown, (peak - capital) / peak * 100.0)
        }
        if (position != null) closePosition(request.bars.lastIndex, request.bars.last().close)

        val wins = trades.count { it.netPnl > 0.0 }
        val outTrades = trades.filter { it.outOfSample }
        val totalFees = trades.sumOf { it.fees }
        return BacktestResult(
            initialCapital = request.initialCapital,
            finalCapital = capital,
            netReturnPercent = (capital / request.initialCapital - 1.0) * 100.0,
            maxDrawdownPercent = maxDrawdown,
            tradeCount = trades.size,
            winningTrades = wins,
            winRatePercent = if (trades.isEmpty()) 0.0 else wins * 100.0 / trades.size,
            totalFees = totalFees,
            outOfSampleNetPnl = outTrades.sumOf { it.netPnl },
            outOfSampleTradeCount = outTrades.size,
            trades = trades
        )
    }
}
