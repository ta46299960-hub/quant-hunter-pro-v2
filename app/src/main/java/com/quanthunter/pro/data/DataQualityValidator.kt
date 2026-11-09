package com.quanthunter.pro.data

data class DataQualityReport(
    val acceptedCandles: List<Candle>, val rejectedCount: Int, val duplicateCount: Int,
    val isFresh: Boolean, val qualityScore: Int, val warnings: List<String>
) {
    val isUsable: Boolean
        get() = acceptedCandles.size >= 2 && isFresh && qualityScore >= 60
}

/** Conservative validation: malformed, duplicate and future-dated candles are excluded. */
class DataQualityValidator {
    fun validate(
        candles: List<Candle>, interval: CandleInterval, nowMillis: Long,
        maxAgeIntervals: Int = 2
    ): DataQualityReport {
        require(nowMillis > 0L) { "nowMillis must be positive" }
        require(maxAgeIntervals >= 1) { "maxAgeIntervals must be at least 1" }

        val sorted = candles.sortedBy { it.timestampEpochMillis }
        val accepted = mutableListOf<Candle>()
        val seen = mutableSetOf<Long>()
        var duplicates = 0
        var rejected = 0

        for (candle in sorted) {
            if (!seen.add(candle.timestampEpochMillis)) {
                duplicates++
                continue
            }
            if (!candle.hasValidValues() || candle.timestampEpochMillis > nowMillis) {
                rejected++
                continue
            }
            accepted += candle
        }

        val latest = accepted.lastOrNull()?.timestampEpochMillis
        val fresh = latest != null && nowMillis - latest <= interval.durationMillis * maxAgeIntervals
        val warnings = buildList {
            if (rejected > 0) add("$rejected malformed or future-dated candle(s) rejected")
            if (duplicates > 0) add("$duplicates duplicate candle(s) ignored")
            if (!fresh) add("Latest candle is missing or stale")
            if (accepted.size < 2) add("At least two valid candles are required")
        }
        val total = candles.size.coerceAtLeast(1)
        val integrity = ((accepted.size.toDouble() / total) * 100.0).toInt()
        val score = (integrity - if (fresh) 0 else 40).coerceIn(0, 100)
        return DataQualityReport(accepted, rejected, duplicates, fresh, score, warnings)
    }
}
