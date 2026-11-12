package com.quanthunter.pro.data

data class DataQualityReport(
    val acceptedCandles: List<Candle>,
    val rejectedCount: Int,
    val duplicateCount: Int,
    val formingCount: Int,
    val gapCount: Int,
    val isFresh: Boolean,
    val qualityScore: Int,
    val warnings: List<String>
) {
    val isUsable: Boolean
        get() = acceptedCandles.size >= 2 && isFresh && qualityScore >= 60 && gapCount == 0
}

/** Conservative validation: only fully closed, well-formed, unique candles are accepted. */
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
        var forming = 0
        val latestClosedOpenTime = nowMillis - interval.durationMillis

        for (candle in sorted) {
            if (!candle.hasValidValues() || candle.timestampEpochMillis > nowMillis) {
                rejected++
                continue
            }
            // Exchange candle timestamps identify the interval open, not its close.
            // Exclude the currently forming interval to prevent unstable live signals.
            if (candle.timestampEpochMillis > latestClosedOpenTime) {
                forming++
                continue
            }
            if (!seen.add(candle.timestampEpochMillis)) {
                duplicates++
                continue
            }
            accepted += candle
        }

        var gaps = 0
        for (index in 1 until accepted.size) {
            val delta = accepted[index].timestampEpochMillis - accepted[index - 1].timestampEpochMillis
            if (delta != interval.durationMillis) {
                val expectedSteps = delta / interval.durationMillis +
                    if (delta % interval.durationMillis == 0L) 0L else 1L
                gaps += (expectedSteps - 1L).coerceAtLeast(1L).toInt()
            }
        }

        val latest = accepted.lastOrNull()?.timestampEpochMillis
        val fresh = latest != null && nowMillis - latest <= interval.durationMillis * maxAgeIntervals
        val warnings = buildList {
            if (rejected > 0) add("$rejected malformed or future-dated candle(s) rejected")
            if (duplicates > 0) add("$duplicates duplicate candle(s) ignored")
            if (forming > 0) add("$forming still-open candle(s) excluded")
            if (gaps > 0) add("$gaps missing or irregular candle interval(s) detected")
            if (!fresh) add("Latest fully closed candle is missing or stale")
            if (accepted.size < 2) add("At least two valid candles are required")
        }
        val total = candles.size.coerceAtLeast(1)
        val integrity = ((accepted.size.toDouble() / total) * 100.0).toInt()
        val freshnessPenalty = if (fresh) 0 else 40
        val gapPenalty = (gaps * 10).coerceAtMost(40)
        val score = (integrity - freshnessPenalty - gapPenalty).coerceIn(0, 100)
        return DataQualityReport(
            acceptedCandles = accepted,
            rejectedCount = rejected,
            duplicateCount = duplicates,
            formingCount = forming,
            gapCount = gaps,
            isFresh = fresh,
            qualityScore = score,
            warnings = warnings
        )
    }
}
