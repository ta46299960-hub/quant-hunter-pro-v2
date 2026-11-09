package com.quanthunter.pro.paper

import com.quanthunter.pro.engine.Direction

data class PaperTradeRequest(
    val direction: Direction,
    val quantity: Double,
    val entryPrice: Double,
    val exitPrice: Double,
    val feePercent: Double = 0.1,
    val slippagePercent: Double = 0.05
)

data class PaperTradeResult(
    val entryFillPrice: Double,
    val exitFillPrice: Double,
    val grossPnl: Double,
    val fees: Double,
    val netPnl: Double,
    val returnOnEntryPercent: Double
)

/** Deterministic paper-only execution estimate. Never connects to a broker. */
class PaperTradeCalculator {
    fun calculate(request: PaperTradeRequest): PaperTradeResult {
        require(request.direction == Direction.BULLISH || request.direction == Direction.BEARISH) {
            "Choose a bullish or bearish paper position"
        }
        require(request.quantity.isFinite() && request.quantity > 0.0)
        require(request.entryPrice.isFinite() && request.entryPrice > 0.0)
        require(request.exitPrice.isFinite() && request.exitPrice > 0.0)
        require(request.feePercent.isFinite() && request.feePercent in 0.0..5.0)
        require(request.slippagePercent.isFinite() && request.slippagePercent in 0.0..5.0)

        val slip = request.slippagePercent / 100.0
        val long = request.direction == Direction.BULLISH
        val entryFill = request.entryPrice * if (long) (1.0 + slip) else (1.0 - slip)
        val exitFill = request.exitPrice * if (long) (1.0 - slip) else (1.0 + slip)
        val gross = if (long) (exitFill - entryFill) * request.quantity
            else (entryFill - exitFill) * request.quantity
        val fees = (entryFill + exitFill) * request.quantity * request.feePercent / 100.0
        val net = gross - fees
        val notional = entryFill * request.quantity
        return PaperTradeResult(
            entryFillPrice = entryFill,
            exitFillPrice = exitFill,
            grossPnl = gross,
            fees = fees,
            netPnl = net,
            returnOnEntryPercent = net / notional * 100.0
        )
    }
}
