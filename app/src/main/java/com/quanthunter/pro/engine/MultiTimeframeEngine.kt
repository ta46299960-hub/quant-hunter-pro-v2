package com.quanthunter.pro.engine

data class TimeframeSignal(val timeframe: String, val score: Int, val weight: Double) {
    init {
        require(timeframe.isNotBlank())
        require(score in -100..100)
        require(weight.isFinite() && weight > 0.0)
    }
}

data class MultiTimeframeAssessment(
    val compositeScore: Int, val direction: Direction, val agreementPercent: Int,
    val confidence: Int, val participatingTimeframes: Int, val reasons: List<String>
)

/** Confidence describes evidence alignment only; it is not a probability of profit. */
class MultiTimeframeEngine {
    fun assess(signals: List<TimeframeSignal>): MultiTimeframeAssessment {
        require(signals.isNotEmpty()) { "At least one timeframe signal is required" }
        require(signals.map { it.timeframe }.distinct().size == signals.size) {
            "Timeframe names must be unique"
        }
        val totalWeight = signals.sumOf { it.weight }
        val weightedScore = signals.sumOf { it.score * it.weight } / totalWeight
        val score = weightedScore.toInt().coerceIn(-100, 100)
        val direction = when {
            score >= 25 -> Direction.BULLISH
            score <= -25 -> Direction.BEARISH
            else -> Direction.NEUTRAL
        }
        val directional = signals.filter { it.score != 0 }
        val alignedWeight = when (direction) {
            Direction.BULLISH -> directional.filter { it.score > 0 }.sumOf { it.weight }
            Direction.BEARISH -> directional.filter { it.score < 0 }.sumOf { it.weight }
            Direction.NEUTRAL -> 0.0
        }
        val directionalWeight = directional.sumOf { it.weight }
        val agreement = if (directionalWeight == 0.0 || direction == Direction.NEUTRAL) 0
            else (alignedWeight / directionalWeight * 100.0).toInt()
        val confidence = if (direction == Direction.NEUTRAL) 0
            else (agreement * 0.7 + kotlin.math.abs(score) * 0.3).toInt().coerceIn(0, 90)
        val reasons = buildList {
            add("Combined ${signals.size} timeframe(s) using supplied weights")
            add("Weighted score: $score")
            add("Directional alignment: $agreement%")
            if (direction == Direction.NEUTRAL) add("No clear multi-timeframe direction")
            if (agreement < 60 && direction != Direction.NEUTRAL) add("Timeframes conflict: treat signal cautiously")
            add("Confidence is evidence alignment, not probability of profit")
        }
        return MultiTimeframeAssessment(score, direction, agreement, confidence, signals.size, reasons)
    }
}
