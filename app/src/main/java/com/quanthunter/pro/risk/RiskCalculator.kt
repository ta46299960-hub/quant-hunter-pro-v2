package com.quanthunter.pro.risk

import com.quanthunter.pro.engine.Direction

data class RiskRequest(
    val accountEquity: Double,
    val riskPercent: Double,
    val entryPrice: Double,
    val stopPrice: Double,
    val direction: Direction,
    val maxExposurePercent: Double = 25.0,
    /** Monetary value of one price-unit move for one quantity unit. */
    val unitValueMultiplier: Double = 1.0
)

data class RiskResult(
    val riskBudget: Double,
    val stopDistance: Double,
    val quantity: Double,
    val notionalExposure: Double,
    val estimatedStopLoss: Double,
    val effectiveRiskPercent: Double,
    val exposureCapApplied: Boolean
)

class RiskCalculator {
    fun calculate(request: RiskRequest): RiskResult {
        require(request.accountEquity.isFinite() && request.accountEquity > 0.0)
        require(request.riskPercent.isFinite() && request.riskPercent in 0.1..5.0) {
            "Risk per trade must be between 0.1% and 5%"
        }
        require(request.entryPrice.isFinite() && request.entryPrice > 0.0)
        require(request.stopPrice.isFinite() && request.stopPrice > 0.0)
        require(request.unitValueMultiplier.isFinite() && request.unitValueMultiplier > 0.0)
        require(request.maxExposurePercent.isFinite() && request.maxExposurePercent in 1.0..100.0)
        require(request.direction != Direction.NEUTRAL) { "A directional trade is required" }
        require(
            (request.direction == Direction.BULLISH && request.stopPrice < request.entryPrice) ||
                (request.direction == Direction.BEARISH && request.stopPrice > request.entryPrice)
        ) { "Stop price is on the wrong side of entry" }

        val distance = kotlin.math.abs(request.entryPrice - request.stopPrice)
        val budget = request.accountEquity * request.riskPercent / 100.0
        val rawQuantity = budget / (distance * request.unitValueMultiplier)
        val maxNotional = request.accountEquity * request.maxExposurePercent / 100.0
        val maxQuantity = maxNotional / (request.entryPrice * request.unitValueMultiplier)
        val quantity = minOf(rawQuantity, maxQuantity)
        val notional = quantity * request.entryPrice * request.unitValueMultiplier
        val stopLoss = quantity * distance * request.unitValueMultiplier

        return RiskResult(
            riskBudget = budget,
            stopDistance = distance,
            quantity = quantity,
            notionalExposure = notional,
            estimatedStopLoss = stopLoss,
            effectiveRiskPercent = stopLoss / request.accountEquity * 100.0,
            exposureCapApplied = quantity < rawQuantity
        )
    }
}
