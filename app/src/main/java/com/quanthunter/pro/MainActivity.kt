package com.quanthunter.pro

import android.app.Activity
import android.app.ActivityManager
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.EditText
import android.text.InputType
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.quanthunter.pro.data.BinanceSpotProvider
import com.quanthunter.pro.data.CandleInterval
import com.quanthunter.pro.data.CandleRequest
import com.quanthunter.pro.data.DataQualityValidator
import com.quanthunter.pro.engine.MarketType
import com.quanthunter.pro.engine.CryptoOpportunityAnalyzer
import com.quanthunter.pro.engine.Direction
import com.quanthunter.pro.backtest.BacktestBar
import com.quanthunter.pro.backtest.BacktestEngine
import com.quanthunter.pro.backtest.BacktestRequest
import com.quanthunter.pro.paper.PaperTradeCalculator
import com.quanthunter.pro.paper.PaperTradeRequest
import com.quanthunter.pro.risk.RiskCalculator
import com.quanthunter.pro.risk.RiskRequest
import java.util.Locale

class MainActivity : Activity() {
    private val gold = Color.rgb(245, 190, 65)
    private val neon = Color.rgb(140, 255, 65)
    private var lightTheme = false
    private var english = false
    private var selectedDepth = "AUTO"
    private lateinit var content: LinearLayout
    private var scannerStatusView: TextView? = null
    private var activeSection = sections.first()
    private var scanRequestId = 0
    private var paperDirection = Direction.BULLISH
    private var riskDirection = Direction.BULLISH

    private val screenBg: Int get() = if (lightTheme) Color.rgb(239, 242, 246) else Color.rgb(13, 16, 20)
    private val panelBg: Int get() = if (lightTheme) Color.WHITE else Color.rgb(25, 30, 36)
    private val primaryText: Int get() = if (lightTheme) Color.rgb(28, 33, 40) else Color.LTGRAY
    private val mutedText: Int get() = if (lightTheme) Color.rgb(90, 98, 108) else Color.rgb(170, 178, 188)

    private val preferences by lazy { getSharedPreferences("qh_preferences", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lightTheme = preferences.getBoolean("light_theme", false)
        english = preferences.getBoolean("english_language", false)
        selectedDepth = preferences.getString("scan_depth", "AUTO") ?: "AUTO"
        if (selectedDepth !in listOf("32", "64", "128", "256", "AUTO")) selectedDepth = "AUTO"
        showSection(activeSection)
    }

    private fun savePreferences() {
        preferences.edit()
            .putBoolean("light_theme", lightTheme)
            .putBoolean("english_language", english)
            .putString("scan_depth", selectedDepth)
            .apply()
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(16, 10, 16, 10)
        }

    private fun rounded(color: Int, stroke: Int = Color.TRANSPARENT): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = 18f
            if (stroke != Color.TRANSPARENT) setStroke(2, stroke)
        }

    private fun chip(value: String, selected: Boolean, action: () -> Unit): TextView =
        label(value, 12f, if (selected) neon else gold, true).apply {
            gravity = Gravity.CENTER
            background = rounded(if (selected) Color.rgb(48, 54, 43) else panelBg, if (selected) neon else Color.rgb(70, 76, 84))
            setPadding(14, 12, 14, 12)
            setOnClickListener { action() }
        }

    private fun showSection(section: String) {
        scanRequestId++ // Invalidate responses from the previous screen.
        activeSection = section
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(screenBg)
            setPadding(14, 16, 14, 10)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val branding = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        branding.addView(label("QUANT HUNTER PRO", 20f, gold, true))
        branding.addView(label("V2  •  HYBRID QUANT SYSTEM", 10f, neon, true))
        header.addView(branding, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(chip(if (lightTheme) "☾" else "☀", false) {
            lightTheme = !lightTheme
            savePreferences()
            showSection(activeSection)
        })
        header.addView(chip(if (english) "AR" else "EN", false) {
            english = !english
            savePreferences()
            showSection(activeSection)
        })
        root.addView(header)

        val navScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
        }
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        sections.forEach { key ->
            val item = chip(sectionTitle(key), key == section) { showSection(key) }
            val params = LinearLayout.LayoutParams(-2, -2)
            params.setMargins(0, 10, 8, 10)
            nav.addView(item, params)
        }
        navScroll.addView(nav)
        root.addView(navScroll, LinearLayout.LayoutParams(-1, -2))

        val bodyScroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(2, 8, 2, 24)
        }
        bodyScroll.addView(content)
        root.addView(bodyScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        renderContent(section)
        if (section == "ماسح الأسواق") loadCryptoSnapshot()
        if (section == "صائد الفرص") loadCryptoOpportunity()\n        if (section == "غرفة المراقبة") loadWatchlist()
    }

    private fun sectionTitle(key: String): String {
        if (!english) return key
        return when (key) {
            "مركز القيادة" -> "Dashboard"
            "ماسح الأسواق" -> "Market Scanner"
            "صائد الفرص" -> "Quant Hunter"
            "غرفة المراقبة" -> "Watch Room"
            "التداول التجريبي" -> "Paper Trading"
            "مختبر التحليل" -> "Analysis Lab"
            "إدارة المخاطر" -> "Risk Manager"
            else -> "Settings"
        }
    }

    private fun card(title: String, detail: String, accent: Int = gold) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panelBg)
            setPadding(8, 8, 8, 8)
        }
        box.addView(label(title, 16f, accent, true))
        box.addView(label(detail, 13f, primaryText))
        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 7, 0, 7)
        content.addView(box, params)
    }

    private fun hero(icon: String, title: String, subtitle: String, accent: Int) {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(if (lightTheme) Color.rgb(250, 248, 240) else Color.rgb(30, 32, 35), accent)
            setPadding(12, 14, 12, 14)
        }
        panel.addView(label(icon, 28f, accent, true))
        panel.addView(label(title, 21f, accent, true))
        panel.addView(label(subtitle, 12f, primaryText))
        val p = LinearLayout.LayoutParams(-1, -2)
        p.setMargins(0, 6, 0, 12)
        content.addView(panel, p)
    }

    private fun statusCard(title: String, detail: String, accent: Int) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panelBg, accent)
            setPadding(8, 8, 8, 8)
        }
        box.addView(label(title, 16f, accent, true))
        scannerStatusView = label(detail, 13f, primaryText)
        box.addView(scannerStatusView)
        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 7, 0, 7)
        content.addView(box, params)
    }

    /** Select a conservative scan depth from available device memory and CPU capacity. */
    private fun resolvedDepth(): Int {
        if (selectedDepth != "AUTO") return selectedDepth.toIntOrNull() ?: 64
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memoryMb = manager.memoryClass
        val cores = Runtime.getRuntime().availableProcessors()
        return when {
            memoryMb >= 512 && cores >= 8 -> 256
            memoryMb >= 256 && cores >= 4 -> 128
            memoryMb >= 192 && cores >= 2 -> 64
            else -> 32
        }
    }

    /** Analyze BTC/USDT only after three independently fetched and validated timeframes. */
    private fun loadCryptoOpportunity() {
        val ar = !english
        val requestId = ++scanRequestId
        val depth = resolvedDepth()
        scannerStatusView?.text = if (ar) "جارٍ جمع بيانات 15m و1h و4h..." else "Fetching 15m, 1h and 4h market data..."
        Thread({
            val message = try {
                val provider = BinanceSpotProvider()
                val candles = linkedMapOf<CandleInterval, List<com.quanthunter.pro.data.Candle>>()
                val intervals = listOf(CandleInterval.M15, CandleInterval.H1, CandleInterval.H4)
                var failure: String? = null
                for (interval in intervals) {
                    val envelope = provider.fetchCandles(CandleRequest("BTCUSDT", MarketType.CRYPTO, interval, depth))
                    if (!envelope.isSuccessful || !envelope.isLive || envelope.data == null) {
                        failure = "${interval.name}: ${envelope.error ?: "Live provider data unavailable"}"
                        break
                    }
                    candles[interval] = envelope.data
                }
                if (failure != null) {
                    (if (ar) "تعذر إصدار تقييم: " else "Assessment unavailable: ") + failure
                } else {
                    val assessment = CryptoOpportunityAnalyzer().assess("BTCUSDT", candles, System.currentTimeMillis())
                    if (assessment == null) {
                        if (ar) "الأدلة غير كافية: نحتاج 32 شمعة صالحة وحديثة على كل إطار 15m و1h و4h."
                        else "Insufficient evidence: at least 32 valid, fresh candles are required on 15m, 1h and 4h."
                    } else {
                        val direction = when (assessment.direction) {
                            com.quanthunter.pro.engine.Direction.BULLISH -> if (ar) "صاعد" else "BULLISH"
                            com.quanthunter.pro.engine.Direction.BEARISH -> if (ar) "هابط" else "BEARISH"
                            com.quanthunter.pro.engine.Direction.NEUTRAL -> if (ar) "محايد — لا توجد فرصة مؤكدة" else "NEUTRAL — no qualified setup"
                        }
                        val price = String.format(Locale.US, "%.2f", assessment.lastPrice)
                        val details = assessment.reasons.takeLast(3).joinToString(" • ")
                        if (ar) "BTC/USDT: $price USDT | الاتجاه: $direction | الدرجة: ${assessment.score} | توافق الأطر: ${assessment.timeframeAgreement}% | الثقة المنهجية: ${assessment.confidence}/85. $details"
                        else "BTC/USDT: $price USDT | Direction: $direction | Score: ${assessment.score} | Timeframe agreement: ${assessment.timeframeAgreement}% | Evidence confidence: ${assessment.confidence}/85. $details"
                    }
                }
            } catch (e: Exception) {
                (if (ar) "فشل التحليل: " else "Analysis failed: ") + (e.message ?: "Unexpected error")
            }
            runOnUiThread {
                if (requestId == scanRequestId && activeSection == "صائد الفرص" && !isFinishing) scannerStatusView?.text = message
            }
        }, "QH-Opportunity-Analysis").apply { isDaemon = true; start() }
    }

    /** Fetch public BTC/USDT candles off the UI thread and ignore stale screen responses. */
    private fun loadCryptoSnapshot() {
        val ar = !english
        val requestId = ++scanRequestId
        val depth = resolvedDepth()
        scannerStatusView?.text = if (ar) "جارٍ الاتصال بمصدر Binance العام..." else "Connecting to public Binance data..."
        Thread({
            val message = try {
                val envelope = BinanceSpotProvider().fetchCandles(
                    CandleRequest(symbol = "BTCUSDT", market = MarketType.CRYPTO, interval = CandleInterval.H1, limit = depth)
                )
                val report = envelope.data?.let {
                    DataQualityValidator().validate(it, CandleInterval.H1, System.currentTimeMillis())
                }
                when {
                    !envelope.isSuccessful || !envelope.isLive ->
                        (if (ar) "تعذر جلب بيانات حية: " else "Live data request failed: ") + (envelope.error ?: "Provider did not confirm live data")
                    report == null || !report.isUsable ->
                        (if (ar) "البيانات مرفوضة بسبب الجودة أو الحداثة. " else "Data rejected by freshness/quality gate. ") + (report?.warnings?.joinToString() ?: "")
                    else -> {
                        val last = report.acceptedCandles.last()
                        val price = String.format(Locale.US, "%.2f", last.close)
                        val candleTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(java.util.Date(last.timestampEpochMillis))
                        val fetchedTime = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(java.util.Date(envelope.fetchedAtEpochMillis))
                        if (ar) "BTC/USDT: $price USDT | جودة: ${report.qualityScore}/100 | العمق: $depth | الشمعة: $candleTime | الجلب: $fetchedTime | المصدر: ${envelope.source}"
                        else "BTC/USDT: $price USDT | Quality: ${report.qualityScore}/100 | Depth: $depth | Candle: $candleTime | Fetched: $fetchedTime | Source: ${envelope.source}"
                    }
                }
            } catch (e: Exception) {
                (if (ar) "حدث خطأ أثناء فحص البيانات: " else "Market data check failed: ") + (e.message ?: "Unexpected error")
            }
            runOnUiThread {
                if (requestId == scanRequestId && activeSection == "ماسح الأسواق" && !isFinishing) {
                    scannerStatusView?.text = message
                }
            }
        }, "QH-Market-Scanner").apply { isDaemon = true; start() }
    }



    /** Fetch the locally saved crypto watchlist from the public Spot provider. */
    private fun loadWatchlist() {
        val ar = !english
        val requestId = ++scanRequestId
        val symbols = (preferences.getStringSet("watchlist_symbols", setOf("BTCUSDT", "ETHUSDT", "BNBUSDT"))
            ?: setOf("BTCUSDT")).toList().sorted().take(8)
        scannerStatusView?.text = if (ar) "جارٍ تحديث قائمة المتابعة..." else "Refreshing watchlist..."
        Thread({
            val lines = mutableListOf<String>()
            try {
                val provider = BinanceSpotProvider()
                for (symbol in symbols) {
                    val envelope = provider.fetchCandles(
                        CandleRequest(symbol, MarketType.CRYPTO, CandleInterval.H1, 32)
                    )
                    val candles = envelope.data
                    if (!envelope.isSuccessful || !envelope.isLive || candles == null) {
                        lines += "$symbol: " + (envelope.error ?: if (ar) "لا توجد بيانات حية" else "No live data")
                        continue
                    }
                    val report = DataQualityValidator().validate(candles, CandleInterval.H1, System.currentTimeMillis())
                    if (!report.isUsable || report.acceptedCandles.size < 25) {
                        lines += "$symbol: " + if (ar) "بيانات مرفوضة — جودة/حداثة غير كافية" else "Rejected — insufficient quality/freshness"
                        continue
                    }
                    val valid = report.acceptedCandles
                    val last = valid.last()
                    val reference = valid[maxOf(0, valid.lastIndex - 24)]
                    val change = (last.close / reference.close - 1.0) * 100.0
                    lines += String.format(Locale.US, "%s  %.6f USDT  |  24h* %+.2f%%  |  Q%d/100",
                        symbol, last.close, change, report.qualityScore)
                }
            } catch (e: Exception) {
                lines += (if (ar) "تعذر تحديث القائمة: " else "Watchlist refresh failed: ") + (e.message ?: "Unexpected error")
            }
            val resultText = lines.joinToString("\n")
            runOnUiThread {
                if (requestId == scanRequestId && activeSection == "غرفة المراقبة" && !isFinishing) {
                    scannerStatusView?.text = resultText
                }
            }
        }, "QH-Watchlist-Refresh").apply { isDaemon = true; start() }
    }

    /** Run a transparent EMA-cross research backtest on validated public BTC/USDT candles. */
    private fun loadHistoricalBacktest() {
        val ar = !english
        val requestId = ++scanRequestId
        val depth = maxOf(resolvedDepth(), 64)
        scannerStatusView?.text = if (ar) "جارٍ جلب شموع BTC/USDT التاريخية وفحص جودتها..." else "Fetching and validating historical BTC/USDT candles..."
        Thread({
            val message = try {
                val envelope = BinanceSpotProvider().fetchCandles(
                    CandleRequest("BTCUSDT", MarketType.CRYPTO, CandleInterval.H1, depth)
                )
                val raw = envelope.data
                if (!envelope.isSuccessful || !envelope.isLive || raw == null) {
                    (if (ar) "تعذر الاختبار: " else "Backtest unavailable: ") + (envelope.error ?: "Live historical data unavailable")
                } else {
                    val report = DataQualityValidator().validate(raw, CandleInterval.H1, System.currentTimeMillis())
                    val candles = report.acceptedCandles
                    if (!report.isUsable || candles.size < 40) {
                        if (ar) "تم رفض البيانات: الجودة أو الحداثة غير كافية لإجراء اختبار موثوق."
                        else "Data rejected: insufficient freshness or quality for a meaningful test."
                    } else {
                        val bars = candles.mapIndexed { index, candle ->
                            val priorCloses = candles.take(index).map { it.close }
                            val signal = if (priorCloses.size < 21) Direction.NEUTRAL else {
                                val fast = emaValue(priorCloses.takeLast(9))
                                val slow = emaValue(priorCloses.takeLast(21))
                                when {
                                    fast > slow * 1.0005 -> Direction.BULLISH
                                    fast < slow * 0.9995 -> Direction.BEARISH
                                    else -> Direction.NEUTRAL
                                }
                            }
                            BacktestBar(candle.timestampEpochMillis, candle.open, candle.close, signal)
                        }
                        val split = (bars.size * 0.7).toInt().coerceIn(1, bars.lastIndex)
                        val result = BacktestEngine().run(
                            BacktestRequest(
                                bars = bars,
                                initialCapital = 10_000.0,
                                positionFraction = 0.25,
                                feePercentPerSide = 0.1,
                                spreadPercentRoundTrip = 0.02,
                                slippagePercentPerSide = 0.05,
                                outOfSampleStartIndex = split
                            )
                        )
                        if (ar) String.format(
                            Locale.US,
                            "BTC/USDT • إطار 1h • شموع صالحة: %d • صفقات: %d • صافي العائد: %.2f%% • التراجع الأقصى: %.2f%% • نسبة الصفقات الرابحة: %.1f%% • رسوم: %.2f • اختبار خارج العينة: %d صفقة / صافي %.2f. نموذج EMA تجريبي، وليس توقعاً للمستقبل.",
                            candles.size, result.tradeCount, result.netReturnPercent, result.maxDrawdownPercent,
                            result.winRatePercent, result.totalFees, result.outOfSampleTradeCount, result.outOfSampleNetPnl
                        ) else String.format(
                            Locale.US,
                            "BTC/USDT • 1h • Valid candles: %d • Trades: %d • Net return: %.2f%% • Max drawdown: %.2f%% • Win rate: %.1f%% • Fees: %.2f • Out-of-sample: %d trades / net %.2f. Experimental EMA model; not a forecast.",
                            candles.size, result.tradeCount, result.netReturnPercent, result.maxDrawdownPercent,
                            result.winRatePercent, result.totalFees, result.outOfSampleTradeCount, result.outOfSampleNetPnl
                        )
                    }
                }
            } catch (e: Exception) {
                (if (ar) "فشل الاختبار التاريخي: " else "Backtest failed: ") + (e.message ?: "Unexpected error")
            }
            runOnUiThread {
                if (requestId == scanRequestId && activeSection == "مختبر التحليل" && !isFinishing) scannerStatusView?.text = message
            }
        }, "QH-Historical-Backtest").apply { isDaemon = true; start() }
    }

    private fun emaValue(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val alpha = 2.0 / (values.size + 1.0)
        return values.drop(1).fold(values.first()) { current, value ->
            alpha * value + (1.0 - alpha) * current
        }
    }

    override fun onDestroy() {
        scanRequestId++
        super.onDestroy()
    }
    private fun metricRow(items: List<Pair<String, String>>, accent: Int) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        items.forEach { item ->
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = rounded(panelBg, accent)
                setPadding(3, 10, 3, 10)
                addView(label(item.first, 17f, accent, true))
                addView(label(item.second, 10f, mutedText))
            }
            val p = LinearLayout.LayoutParams(0, -2, 1f)
            p.setMargins(3, 3, 3, 8)
            row.addView(tile, p)
        }
        content.addView(row)
    }

    private fun renderContent(section: String) {
        val ar = !english
        val accent = when (section) {
            "ماسح الأسواق", "صائد الفرص" -> neon
            "غرفة المراقبة" -> Color.rgb(80, 190, 255)
            "التداول التجريبي" -> Color.rgb(190, 145, 255)
            "مختبر التحليل" -> Color.rgb(255, 155, 75)
            "إدارة المخاطر" -> Color.rgb(255, 100, 115)
            "الإعدادات" -> Color.rgb(170, 180, 195)
            else -> gold
        }
        val subtitle = when (section) {
            "مركز القيادة" -> if (ar) "نظرة موحدة على حالة المنظومة والأسواق" else "Unified view of system and market health"
            "ماسح الأسواق" -> if (ar) "اكتشاف منظم للفرص عبر مستويات فحص متعددة" else "Structured opportunity discovery at multiple scan depths"
            "صائد الفرص" -> if (ar) "ترتيب الفرص مع تفسير أسباب التقييم" else "Ranked opportunities with explainable scoring"
            "غرفة المراقبة" -> if (ar) "متابعة الأصول والتنبيهات في مساحة واحدة" else "Track assets and alerts in one workspace"
            "التداول التجريبي" -> if (ar) "تدريب ومحاكاة دون إرسال أوامر حقيقية" else "Practice and simulation without live orders"
            "مختبر التحليل" -> if (ar) "اختبارات تاريخية وتحليل متعدد الأطر" else "Historical testing and multi-timeframe research"
            "إدارة المخاطر" -> if (ar) "ضبط التعرض وحجم المركز قبل المحاكاة" else "Control exposure and position size before simulation"
            else -> if (ar) "تخصيص التجربة واللغة والتنبيهات" else "Customize appearance, language and alerts"
        }
        val icon = when (section) {
            "مركز القيادة" -> "◆"
            "ماسح الأسواق" -> "⌕"
            "صائد الفرص" -> "✦"
            "غرفة المراقبة" -> "◉"
            "التداول التجريبي" -> "⇄"
            "مختبر التحليل" -> "⌁"
            "إدارة المخاطر" -> "⬡"
            else -> "⚙"
        }
        hero(icon, sectionTitle(section), subtitle, accent)

        when (section) {
            "مركز القيادة" -> {
                metricRow(listOf("05" to (if (ar) "فئات أسواق" else "Markets"), "08" to (if (ar) "أقسام" else "Modules"), "HYBRID" to (if (ar) "منهج" else "Model")), gold)
                card(if (ar) "حالة المنظومة" else "System health",
                    if (ar) "مصدر Binance العام متصل للعملات الرقمية عند فتح الماسح. بقية الأسواق لم تُربط بعد." else "Public Binance data is queried for crypto in the scanner. Other markets are not connected yet.", neon)
                card(if (ar) "بوابة جودة البيانات" else "Data quality gate",
                    if (ar) "يتم رفض البيانات القديمة أو المشوهة قبل استخدامها في التقييم." else "Stale or malformed data is rejected before scoring.", gold)
                card(if (ar) "تغطية الأسواق" else "Market coverage", "CRYPTO  •  FOREX  •  STOCKS  •  INDICES  •  COMMODITIES")
            }
            "ماسح الأسواق" -> {
                card(if (ar) "مستويات عمق الفحص" else "Scan depth", if (ar) "اختر نطاق التحليل. العمق الأكبر قد يحتاج موارد ووقتاً أكثر." else "Choose the analysis window. Higher depth may need more time and device resources.", accent)
                val depthRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                listOf("32", "64", "128", "256", "AUTO").forEach { depth ->
                    val caption = if (depth == "AUTO" && ar) "تلقائي" else depth
                    val item = chip(caption, selectedDepth == depth) { selectedDepth = depth; savePreferences(); showSection(section) }
                    val p = LinearLayout.LayoutParams(0, -2, 1f); p.setMargins(2, 4, 2, 4); depthRow.addView(item, p)
                }
                content.addView(depthRow)
                val desc = when (selectedDepth) {
                    "32" -> if (ar) "سريع — نافذة فحص صغيرة." else "Quick — compact scan window."
                    "64" -> if (ar) "متوازن — توازن بين السرعة والتغطية." else "Balanced — speed and coverage."
                    "128" -> if (ar) "متقدم — نطاق تحليل أوسع." else "Advanced — broader analysis window."
                    "256" -> if (ar) "عميق — أكبر نطاق حسابي متاح." else "Deep — largest available computation window."
                    else -> if (ar) "تلقائي ذكي — يحدد العمق وفق موارد الجهاز وجودة البيانات." else "Smart auto — selects depth from device capacity and data quality."
                }
                card((if (ar) "الاختيار الحالي: " else "Current selection: ") + selectedDepth, desc, neon)
                statusCard(if (ar) "بيانات BTC/USDT من مصدر Binance العام" else "BTC/USDT from public Binance",
                    if (ar) "يتم فحص المصدر والحداثة وجودة الشموع قبل عرض السعر." else "Source, freshness and candle quality are checked before display.", mutedText)
                content.addView(chip(if (ar) "تحديث البيانات الآن" else "Refresh market data", false) { loadCryptoSnapshot() })
            }
            "صائد الفرص" -> {
                metricRow(listOf("15m" to (if (ar) "قصير" else "Short"), "1h" to (if (ar) "متوسط" else "Medium"), "4h" to (if (ar) "اتجاه عام" else "Higher trend")), neon)
                card(if (ar) "تحليل BTC/USDT متعدد الأطر" else "BTC/USDT multi-timeframe analysis",
                    if (ar) "يطلب بيانات مستقلة من Binance على 15 دقيقة وساعة و4 ساعات. لا يصدر اتجاهاً عند نقص البيانات أو تعارض الأطر." else "Fetches independent Binance candles for 15m, 1h and 4h. No directional setup is issued when data is missing or timeframes conflict.", neon)
                statusCard(if (ar) "نتيجة صائد الفرص" else "Opportunity assessment",
                    if (ar) "اضغط تحليل الآن لجمع البيانات وفحص حداثتها وجودتها." else "Tap Analyze now to fetch data and validate freshness and quality.", mutedText)
                content.addView(chip(if (ar) "تحليل الآن" else "Analyze now", false) { loadCryptoOpportunity() })
                card(if (ar) "تفسير النتيجة" else "Score interpretation",
                    if (ar) "درجة الثقة تعبر عن توافق الأدلة، وليست احتمالاً للربح. التحليل بحثي وليس توصية مالية." else "Confidence measures evidence alignment, not probability of profit. Research only, not financial advice.")
            }
            "غرفة المراقبة" -> {
                card(if (ar) "قائمة متابعة العملات الرقمية" else "Crypto watchlist",
                    if (ar) "قائمة محلية محفوظة على الجهاز. أدخل رمز Binance مثل ETHUSDT. الحد الأقصى 8 أصول." else "Locally saved list. Enter a Binance symbol such as ETHUSDT. Maximum 8 assets.", accent)
                val watchSymbols = (preferences.getStringSet("watchlist_symbols", setOf("BTCUSDT", "ETHUSDT", "BNBUSDT"))
                    ?: setOf("BTCUSDT")).toList().sorted()
                val symbolField = EditText(this).apply {
                    hint = if (ar) "رمز العملة (مثال ETHUSDT)" else "Crypto symbol (e.g. ETHUSDT)"
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                    setTextColor(primaryText)
                    setHintTextColor(mutedText)
                    textSize = 14f
                    setSingleLine(true)
                    setPadding(16, 8, 16, 8)
                    background = rounded(panelBg, Color.rgb(80, 86, 94))
                }
                content.addView(symbolField, LinearLayout.LayoutParams(-1, -2))
                content.addView(chip(if (ar) "إضافة إلى القائمة" else "Add to watchlist", false) {
                    val symbol = symbolField.text.toString().trim().uppercase(Locale.US)
                    val current = preferences.getStringSet("watchlist_symbols", setOf("BTCUSDT", "ETHUSDT", "BNBUSDT"))
                        ?.toMutableSet() ?: mutableSetOf("BTCUSDT")
                    if (!symbol.matches(Regex("[A-Z0-9]{3,20}")) || (symbol !in current && current.size >= 8)) {
                        scannerStatusView?.text = if (ar) "رمز غير صالح أو وصلت القائمة إلى 8 أصول." else "Invalid symbol or the 8-asset limit has been reached."
                    } else {
                        current.add(symbol)
                        preferences.edit().putStringSet("watchlist_symbols", current).apply()
                        showSection(section)
                    }
                })
                statusCard(if (ar) "الأسعار وحالة البيانات" else "Prices and data status",
                    if (ar) "اضغط تحديث لجلب بيانات Spot والتحقق من حداثتها وجودتها." else "Refresh to fetch Spot data and validate freshness and quality.", mutedText)
                content.addView(chip(if (ar) "تحديث قائمة المتابعة" else "Refresh watchlist", false) { loadWatchlist() })
                card(if (ar) "التغير التقريبي" else "Approximate change",
                    if (ar) "التغير محسوب من آخر 25 شمعة ساعة المتاحة (24 فترة تقريباً)، وليس تغير يوم تقويمي مضموناً." else "Change is measured across up to 25 hourly candles (about 24 intervals), not a guaranteed calendar-day return.")
            }
            "التداول التجريبي" -> {
                metricRow(listOf("PAPER" to (if (ar) "محاكاة" else "Mode"), ("0.00" to (if (ar) "رصيد تجريبي" else "Demo equity")), ("0" to (if (ar) "صفقات" else "Trades"))), accent)
                card(if (ar) "حاسبة الصفقة التجريبية" else "Paper trade calculator",
                    if (ar) "أدخل الكمية وسعر الدخول والخروج. تُخصم رسوم التنفيذ والانزلاق التقديري من النتيجة." else "Enter quantity, entry and exit prices. Estimated fees and slippage are deducted.", accent)
                val sideRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                sideRow.addView(chip(if (ar) "شراء / صعود" else "LONG", paperDirection == Direction.BULLISH) { paperDirection = Direction.BULLISH; showSection(section) })
                sideRow.addView(chip(if (ar) "بيع / هبوط" else "SHORT", paperDirection == Direction.BEARISH) { paperDirection = Direction.BEARISH; showSection(section) })
                content.addView(sideRow)
                fun numberField(hintText: String): EditText = EditText(this).apply {
                    hint = hintText
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    setTextColor(primaryText)
                    setHintTextColor(mutedText)
                    textSize = 14f
                    setSingleLine(true)
                    setPadding(16, 8, 16, 8)
                    background = rounded(panelBg, Color.rgb(80, 86, 94))
                    val p = LinearLayout.LayoutParams(-1, -2)
                    p.setMargins(0, 5, 0, 5)
                    content.addView(this, p)
                }
                val quantityField = numberField(if (ar) "الكمية (مثال: 0.01)" else "Quantity (e.g. 0.01)")
                val entryField = numberField(if (ar) "سعر الدخول" else "Entry price")
                val exitField = numberField(if (ar) "سعر الخروج" else "Exit price")
                statusCard(if (ar) "نتيجة المحاكاة" else "Simulation result",
                    if (ar) "أدخل القيم ثم احسب النتيجة." else "Enter values and calculate.", mutedText)
                content.addView(chip(if (ar) "احسب الصفقة" else "Calculate trade", false) {
                    val result = try {
                        PaperTradeCalculator().calculate(
                            PaperTradeRequest(
                                direction = paperDirection,
                                quantity = quantityField.text.toString().toDouble(),
                                entryPrice = entryField.text.toString().toDouble(),
                                exitPrice = exitField.text.toString().toDouble(),
                                feePercent = 0.1,
                                slippagePercent = 0.05
                            )
                        )
                    } catch (e: Exception) {
                        scannerStatusView?.text = if (ar) "تحقق من إدخال كمية وأسعار صحيحة وموجبة." else "Enter valid positive quantity and prices."
                        null
                    }
                    if (result != null) {
                        scannerStatusView?.text = String.format(
                            Locale.US,
                            if (ar) "سعر الدخول المنفذ: %.4f | الخروج المنفذ: %.4f | الربح قبل التكاليف: %.4f | الرسوم: %.4f | النتيجة الصافية: %.4f (%.3f%%)"
                            else "Entry fill: %.4f | Exit fill: %.4f | Gross PnL: %.4f | Fees: %.4f | Net PnL: %.4f (%.3f%%)",
                            result.entryFillPrice, result.exitFillPrice, result.grossPnl,
                            result.fees, result.netPnl, result.returnOnEntryPercent
                        )
                    }
                })
                card(if (ar) "افتراضات المحاكاة" else "Simulation assumptions",
                    if (ar) "رسوم تقديرية 0.10% لكل جانب وانزلاق 0.05% لكل تنفيذ. لا يتصل هذا القسم بوسيط." else "Assumed 0.10% fee per side and 0.05% slippage per execution. No broker connection.", neon)
            }
            "مختبر التحليل" -> {
                card(if (ar) "اختبار تاريخي مباشر لبيانات BTC/USDT" else "Historical backtest on BTC/USDT data",
                    if (ar) "يجلب شموع 1h من Binance، ويرفض البيانات غير الصالحة، ثم يختبر تقاطع EMA مع رسوم وسبريد وانزلاق. تقسيم زمني 70% / 30%." else "Fetches Binance 1h candles, rejects invalid data, then tests an EMA crossover with fees, spread and slippage. Chronological 70% / 30% split.", accent)
                statusCard(if (ar) "نتيجة الاختبار" else "Backtest result",
                    if (ar) "اضغط تشغيل الاختبار لجلب البيانات وحساب النتائج." else "Run the test to fetch data and calculate results.", mutedText)
                content.addView(chip(if (ar) "تشغيل الاختبار التاريخي" else "Run historical backtest", false) { loadHistoricalBacktest() })
                card(if (ar) "تنبيه منهجي" else "Research note",
                    if (ar) "النموذج تقاطع متوسطات بسيط، والاختبار التاريخي لا يضمن أداءً مستقبلياً. نتيجة خارج العينة تُعرض منفصلة." else "This is a simple moving-average crossover; historical results do not guarantee future performance. Out-of-sample results are reported separately.")
            }
            "إدارة المخاطر" -> {
                metricRow(listOf("RISK" to (if (ar) "مخاطرة" else "Risk"), "STOP" to (if (ar) "وقف" else "Stop"), "CAP" to (if (ar) "تعرض" else "Exposure")), accent)
                card(if (ar) "حاسبة حجم المركز" else "Position sizing calculator",
                    if (ar) "أدخل رأس المال ونسبة المخاطرة وسعر الدخول والوقف. يتم حساب الحجم وحد التعرض قبل أي محاكاة." else "Enter equity, risk percentage, entry and stop. Position size and exposure cap are calculated before simulation.", accent)
                val sideRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                sideRow.addView(chip(if (ar) "شراء / صعود" else "LONG", riskDirection == Direction.BULLISH) { riskDirection = Direction.BULLISH; showSection(section) })
                sideRow.addView(chip(if (ar) "بيع / هبوط" else "SHORT", riskDirection == Direction.BEARISH) { riskDirection = Direction.BEARISH; showSection(section) })
                content.addView(sideRow)
                fun numericRiskField(hintText: String): EditText = EditText(this).apply {
                    hint = hintText
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    setTextColor(primaryText)
                    setHintTextColor(mutedText)
                    textSize = 14f
                    setSingleLine(true)
                    setPadding(16, 8, 16, 8)
                    background = rounded(panelBg, Color.rgb(80, 86, 94))
                    val p = LinearLayout.LayoutParams(-1, -2)
                    p.setMargins(0, 5, 0, 5)
                    content.addView(this, p)
                }
                val equityField = numericRiskField(if (ar) "رأس المال (مثال: 1000)" else "Account equity (e.g. 1000)")
                val riskPercentField = numericRiskField(if (ar) "نسبة المخاطرة % (0.1 إلى 5)" else "Risk percent (0.1 to 5)")
                val entryField = numericRiskField(if (ar) "سعر الدخول" else "Entry price")
                val stopField = numericRiskField(if (ar) "سعر وقف الخسارة" else "Stop price")
                statusCard(if (ar) "نتيجة إدارة المخاطر" else "Risk calculation",
                    if (ar) "أدخل القيم واحسب حجم المركز." else "Enter values and calculate position size.", mutedText)
                content.addView(chip(if (ar) "احسب حجم المركز" else "Calculate position size", false) {
                    val result = try {
                        RiskCalculator().calculate(
                            RiskRequest(
                                accountEquity = equityField.text.toString().toDouble(),
                                riskPercent = riskPercentField.text.toString().toDouble(),
                                entryPrice = entryField.text.toString().toDouble(),
                                stopPrice = stopField.text.toString().toDouble(),
                                direction = riskDirection
                            )
                        )
                    } catch (e: Exception) {
                        scannerStatusView?.text = if (ar) "قيم غير صالحة: تحقق من رأس المال والوقف ونسبة المخاطرة واتجاه الصفقة." else "Invalid inputs: check equity, stop, risk percent and trade direction."
                        null
                    }
                    if (result != null) {
                        scannerStatusView?.text = String.format(
                            Locale.US,
                            if (ar) "الكمية: %.6f | قيمة المركز: %.2f | الخسارة المقدرة عند الوقف: %.2f | المخاطرة الفعلية: %.2f%% | تم تطبيق حد التعرض: %s"
                            else "Quantity: %.6f | Notional: %.2f | Estimated stop loss: %.2f | Effective risk: %.2f%% | Exposure cap applied: %s",
                            result.quantity, result.notionalExposure, result.estimatedStopLoss,
                            result.effectiveRiskPercent, result.exposureCapApplied.toString()
                        )
                    }
                })
                card(if (ar) "حدود الحماية" else "Safety limits",
                    if (ar) "نسبة المخاطرة 0.1% إلى 5% وحد التعرض الافتراضي 25%. النتيجة تقديرية ولا تشمل فجوات الأسعار أو تغير السيولة." else "Risk is limited to 0.1%–5% and default exposure cap is 25%. Estimates do not cover price gaps or changing liquidity.", neon)
            }
            else -> {
                card(if (ar) "التخصيص" else "Preferences",
                    if (ar) "المظهر الليلي والنهاري واللغة العربية والإنجليزية." else "Dark/light appearance and Arabic/English language.")
                card(if (ar) "مصادر البيانات والخصوصية" else "Data & privacy",
                    if (ar) "اعرض المصدر والطابع الزمني وحالة الاتصال. لا تخزن مفاتيح API داخل التطبيق." else "Show provider, timestamp and connection status. Never store API keys in the app.")
                card(if (ar) "التنبيهات" else "Notifications",
                    if (ar) "تفضيلات تنبيهات الأسعار وجودة البيانات وحالة النظام." else "Configure price, data-quality and system alerts.")
            }
        }
        card(if (ar) "ملاحظة تطوير" else "Development notice",
            if (ar) "بيانات العملات الرقمية فقط عبر Binance العام في الماسح. الفوركس والأسهم والمؤشرات والسلع غير متصلة حالياً. لا توجد توصيات مالية أو ضمانات ربح." else "Crypto data only via public Binance in the scanner. Forex, stocks, indices and commodities are not connected yet. No financial advice or profit guarantees.", mutedText)
    }

    companion object {
        private val sections = listOf(
            "مركز القيادة", "ماسح الأسواق", "صائد الفرص", "غرفة المراقبة",
            "التداول التجريبي", "مختبر التحليل", "إدارة المخاطر", "الإعدادات"
        )
    }
}
