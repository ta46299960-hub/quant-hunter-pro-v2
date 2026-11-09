package com.quanthunter.pro.engine

import com.quanthunter.pro.data.Candle
import com.quanthunter.pro.data.CandleInterval
import com.quanthunter.pro.data.DataQualityValidator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class CryptoOpportunityAssessment(
    val symbol: String,
    val lastPrice: Double,
    val direction: Direction,
    val score: Int,
    val confidence: Int,
    val timeframeAgreement: Int,
    val timeframeCount: Int,
    val reasons: List<String>
)

/**
 * Conservative, explainable candle-based research assessment.
 * Requires independently fresh 15m, 1h and 4h data; it is not a trading signal guarantee.
 */
class CryptoOpportunityAnalyzer(
    private val qualityValidator: DataQualityValidator = DataQualityValidator(),
    private val multiTimeframeEngine: MultiTimeframeEngine = MultiTimeframeEngine(),
    private val scoringEngine: HybridScoringEngine = HybridScoringEngine()
) {
    fun assess(
        symbol: String,
        candlesByTimeframe: Map<CandleInterval, List<Candle>>,
        nowMillis: Long
    ): CryptoOpportunityAssessment? {
        require(symbol.isNotBlank())
        val required = listOf(CandleInterval.M15, CandleInterval.H1, CandleInterval.H4)
        val validated = required.map { interval ->
            val candles = candlesByTimeframe[interval] ?: return null
            val report = qualityValidator.validate(candles, interval, nowMillis)
            if (!report.isUsable || report.acceptedCandles.size < 32) return null
            interval to report.acceptedCandles
        }.toMap()

        val features = validated.map { (interval, candles) -> interval to featureScore(candles) }.toMap()
        val timeframeSignals = listOf(
            TimeframeSignal("15m", features.getValue(CandleInterval.M15).composite, 1.0),
            TimeframeSignal("1h", features.getValue(CandleInterval.H1).composite, 1.5),
            TimeframeSignal("4h", features.getValue(CandleInterval.H4).composite, 2.0)
        )
        val mtf = multiTimeframeEngine.assess(timeframeSignals)
        val short = features.getValue(CandleInterval.M15)
        val medium = features.getValue(CandleInterval.H1)
        val long = features.getValue(CandleInterval.H4)
        val trend = weighted(short.trend, medium.trend, long.trend)
        val momentum = weighted(short.momentum, medium.momentum, long.momentum)
        val liquidity = weighted(short.liquidity, medium.liquidity, long.liquidity)
        val volatility = weighted(short.volatilityQuality, medium.volatilityQuality, long.volatilityQuality)
        val mtfSigned = when (mtf.direction) {
            Direction.BULLISH -> mtf.agreementPercent
            Direction.BEARISH -> -mtf.agreementPercent
            Direction.NEUTRAL -> 0
        }
        val base = scoringEngine.assess(MarketType.CRYPTO, trend, momentum, liquidity, volatility, mtfSigned)
        val aligned = base.direction != Direction.NEUTRAL &&
            base.direction == mtf.direction && mtf.agreementPercent >= 60
        val finalDirection = if (aligned) base.direction else Direction.NEUTRAL
        val reasons = buildList {
            addAll(base.reasons)
            addAll(mtf.reasons)
            if (!aligned) add("Insufficient cross-timeframe agreement: no directional setup issued")
            if (aligned) add("All three required timeframes passed freshness and integrity checks")
            add("Volume and volatility are heuristic candle-derived proxies, not order-book liquidity")
        }
        return CryptoOpportunityAssessment(
            symbol = symbol.uppercase(),
            lastPrice = validated.getValue(CandleInterval.H1).last().close,
            direction = finalDirection,
            score = if (aligned) base.score else 0,
            confidence = if (aligned) min(base.confidence, mtf.confidence) else 0,
            timeframeAgreement = mtf.agreementPercent,
            timeframeCount = mtf.participatingTimeframes,
            reasons = reasons
        )
    }

    private data class Features(
        val trend: Int, val momentum: Int, val liquidity: Int,
        val volatilityQuality: Int, val composite: Int
    )

    private fun featureScore(candles: List<Candle>): Features {
        val closes = candles.map { it.close }
        val latest = closes.last()
        val fast = ema(closes.takeLast(9))
        val slow = ema(closes.takeLast(21))
        val trend = (((fast / slow) - 1.0) * 5_000.0).toInt().coerceIn(-100, 100)
        val reference = closes[closes.lastIndex - 5]
        val momentum = (((latest / reference) - 1.0) * 4_000.0).toInt().coerceIn(-100, 100)

        val recentVolume = candles.takeLast(5).map { it.volume }.average()
        val baselineVolume = candles.dropLast(5).takeLast(20).map { it.volume }.average()
        val volumeRatio = if (baselineVolume > 0.0) recentVolume / baselineVolume else 1.0
        val directionSign = when {
            trend > 0 -> 1
            trend < 0 -> -1
            else -> 0
        }
        val liquidity = ((volumeRatio - 1.0) * 50.0 * directionSign).toInt().coerceIn(-100, 100)

        val ranges = candles.takeLast(14).map { (it.high - it.low) / it.close * 100.0 }
        val averageRangePct = ranges.average()
        val quality = when {
            averageRangePct <= 0.0 -> 0
            averageRangePct < 0.15 -> 35
            averageRangePct <= 2.5 -> 85
            averageRangePct <= 5.0 -> 60
            else -> 25
        }
        val volatilityQuality = quality * directionSign
        val composite = (trend * 0.55 + momentum * 0.35 + liquidity * 0.10)
            .toInt().coerceIn(-100, 100)
        return Features(trend, momentum, liquidity, volatilityQuality, composite)
    }

    private fun ema(values: List<Double>): Double {
        require(values.isNotEmpty())
        val alpha = 2.0 / (values.size + 1.0)
        return values.drop(1).fold(values.first()) { current, value ->
            alpha * value + (1.0 - alpha) * current
        }
    }

    private fun weighted(a: Int, b: Int, c: Int): Int =
        (a * 1.0 + b * 1.5 + c * 2.0).div(4.5).toInt().coerceIn(-100, 100)
}
