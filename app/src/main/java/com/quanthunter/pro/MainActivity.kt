package com.quanthunter.pro

import android.app.Activity
import android.app.ActivityManager
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.quanthunter.pro.data.BinanceSpotProvider
import com.quanthunter.pro.data.CandleInterval
import com.quanthunter.pro.data.CandleRequest
import com.quanthunter.pro.data.DataQualityValidator
import com.quanthunter.pro.engine.MarketType
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

    private val screenBg: Int get() = if (lightTheme) Color.rgb(239, 242, 246) else Color.rgb(13, 16, 20)
    private val panelBg: Int get() = if (lightTheme) Color.WHITE else Color.rgb(25, 30, 36)
    private val primaryText: Int get() = if (lightTheme) Color.rgb(28, 33, 40) else Color.LTGRAY
    private val mutedText: Int get() = if (lightTheme) Color.rgb(90, 98, 108) else Color.rgb(170, 178, 188)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showSection(activeSection)
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
            showSection(activeSection)
        })
        header.addView(chip(if (english) "AR" else "EN", false) {
            english = !english
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
                    val item = chip(caption, selectedDepth == depth) { selectedDepth = depth; showSection(section) }
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
                metricRow(listOf("TREND" to (if (ar) "اتجاه" else "Trend"), "MOM" to (if (ar) "زخم" else "Momentum"), "MTF" to (if (ar) "أطر" else "Frames")), neon)
                card(if (ar) "منهج ترتيب الفرص" else "Opportunity ranking",
                    if (ar) "يجمع الاتجاه والزخم والسيولة والحجم والتذبذب وتوافق الأطر الزمنية، مع فحص الاختراقات الوهمية." else "Combines trend, momentum, liquidity, volume, volatility and timeframe agreement, including false-breakout checks.", neon)
                card(if (ar) "تفسير النتيجة" else "Score interpretation",
                    if (ar) "درجة الثقة تعبر عن توافق الأدلة، وليست احتمالاً للربح." else "Confidence measures evidence agreement, not probability of profit.")
            }
            "غرفة المراقبة" -> {
                card(if (ar) "قائمة المتابعة" else "Watchlist",
                    if (ar) "ستظهر الأصول المضافة مع السعر ووقت آخر تحديث ومصدر البيانات بعد الاتصال." else "Tracked assets will show price, last update time and data source after connectivity.", accent)
                card(if (ar) "مركز التنبيهات" else "Alert center",
                    if (ar) "تنبيهات الاتجاه والحجم والتذبذب، مع بيان حالة كل تنبيه." else "Trend, volume and volatility alerts with explicit alert status.")
            }
            "التداول التجريبي" -> {
                metricRow(listOf("PAPER" to (if (ar) "محاكاة" else "Mode"), ("0.00" to (if (ar) "رصيد تجريبي" else "Demo equity")), ("0" to (if (ar) "صفقات" else "Trades"))), accent)
                card(if (ar) "لوحة تنفيذ افتراضي" else "Virtual execution",
                    if (ar) "ستتم محاكاة الدخول والخروج والرسوم والانزلاق دون اتصال بوسيط." else "Entry, exit, fees and slippage will be simulated without broker execution.", accent)
                card(if (ar) "حماية المستخدم" else "Execution safety",
                    if (ar) "هذا القسم للتداول التجريبي فقط ولا يرسل أوامر حقيقية." else "Paper trading only. No real orders are sent.", neon)
            }
            "مختبر التحليل" -> {
                card(if (ar) "مختبر الاختبار التاريخي" else "Backtesting lab",
                    if (ar) "يدعم التصميم احتساب الرسوم والسبريد والانزلاق وفصل بيانات التدريب عن الاختبار." else "Designed to include fees, spread, slippage and separate training from test data.", accent)
                card(if (ar) "مقاييس التقييم" else "Evaluation metrics",
                    if (ar) "العائد، التراجع الأقصى، عدد الصفقات، متوسط الربح والخسارة، وثبات الأداء خارج العينة." else "Return, maximum drawdown, trade count, average win/loss and out-of-sample stability.")
            }
            "إدارة المخاطر" -> {
                metricRow(listOf("RISK" to (if (ar) "مخاطرة" else "Risk"), "STOP" to (if (ar) "وقف" else "Stop"), "CAP" to (if (ar) "تعرض" else "Exposure")), accent)
                card(if (ar) "حاسبة حجم المركز" else "Position sizing",
                    if (ar) "تحسب الحجم من رأس المال ونسبة المخاطرة وسعر الدخول والوقف، مع حد أقصى للتعرض." else "Calculates size from equity, risk fraction, entry and stop, with a maximum exposure cap.", accent)
                card(if (ar) "فحوص ما قبل الصفقة" else "Pre-trade checks",
                    if (ar) "رفض الاتجاه المحايد أو الوقف غير الصحيح أو تجاوز حدود المخاطر." else "Rejects neutral direction, invalid stops and risk-limit breaches.", neon)
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
