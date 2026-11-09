package com.quanthunter.pro.engine

/**
 * Deterministic, explainable baseline scoring.
 * This is a research heuristic, not a probability of profit or a validated trading edge.
 */
class HybridScoringEngine {
    fun assess(
        market: MarketType,
        trend: Int,
        momentum: Int,
        liquidity: Int,
        volatilityQuality: Int,
        timeframeAgreement: Int
    ): SignalAssessment {
        val inputs = listOf(trend, momentum, liquidity, volatilityQuality, timeframeAgreement)
        require(inputs.all { it in -100..100 }) { "Scores must be between -100 and 100" }

        val weights = weightsFor(market)
        val weighted = inputs.zip(weights).sumOf { (value, weight) -> value * weight }
        val score = weighted.coerceIn(-100.0, 100.0).toInt()
        val direction = when {
            score >= 25 -> Direction.BULLISH
            score <= -25 -> Direction.BEARISH
            else -> Direction.NEUTRAL
        }
        val agreement = inputs.count { it != 0 && (it > 0) == (score > 0) }
        val confidence = (35 + agreement * 10 + (kotlin.math.abs(score) / 10)).coerceIn(0, 85)
        val reasons = buildList {
            add("Market profile: $market")
            add("Weighted composite score: $score")
            add("Timeframe agreement input: $timeframeAgreement")
            if (kotlin.math.abs(score) < 25) add("No directional edge: remain neutral")
            add("Heuristic confidence is not a win probability")
        }
        return SignalAssessment(score, confidence, direction, reasons)
    }

    private fun weightsFor(market: MarketType): List<Double> = when (market) {
        MarketType.CRYPTO -> listOf(.25, .25, .20, .10, .20)
        MarketType.FOREX -> listOf(.30, .20, .15, .10, .25)
        MarketType.STOCKS -> listOf(.30, .20, .20, .10, .20)
        MarketType.INDICES -> listOf(.30, .20, .15, .10, .25)
        MarketType.COMMODITIES -> listOf(.30, .20, .15, .15, .20)
    }
}
