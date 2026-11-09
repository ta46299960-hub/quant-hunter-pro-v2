package com.quanthunter.pro

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val gold = Color.rgb(245, 190, 65)
    private val neon = Color.rgb(140, 255, 65)
    private var lightTheme = false
    private var english = false
    private lateinit var content: LinearLayout
    private var activeSection = sections.first()

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

        val bodyScroll = ScrollView(this).apply { fillViewport = true }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(2, 8, 2, 24)
        }
        bodyScroll.addView(content)
        root.addView(bodyScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        renderContent(section)
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

    private fun renderContent(section: String) {
        content.addView(label(sectionTitle(section), 23f, neon, true))
        val ar = !english
        when (section) {
            "مركز القيادة" -> {
                card(if (ar) "حالة النظام" else "System status",
                    if (ar) "الواجهة تعمل. لم يتم ربط مزودي بيانات حقيقية بعد." else "Interface ready. Live market providers are not connected yet.", neon)
                card(if (ar) "الأسواق المدعومة" else "Supported markets", "CRYPTO  •  FOREX  •  STOCKS  •  INDICES  •  COMMODITIES")
                card(if (ar) "منهج التحليل" else "Analysis method",
                    if (ar) "Hybrid: سياق عام للسوق مع قواعد وأوزان مستقلة لكل سوق." else "Hybrid: broad market context with independent rules and weights per market.")
                card(if (ar) "بوابة جودة البيانات" else "Data quality gate",
                    if (ar) "البيانات القديمة أو غير الصالحة لا تُستخدم لإصدار إشارة موثوقة." else "Stale or invalid data is blocked from trusted signal generation.", neon)
            }
            "ماسح الأسواق" -> card(if (ar) "ماسح الأسواق" else "Market Scanner",
                if (ar) "جاهز لاستقبال الأسعار والشموع بعد ربط مزود بيانات والتحقق من المصدر والوقت." else "Ready for candles and quotes once a provider is connected and source freshness is verified.")
            "صائد الفرص" -> card(if (ar) "محرك صائد الفرص" else "Opportunity engine",
                if (ar) "سيجمع الاتجاه والزخم والسيولة والتذبذب وتوافق الأطر الزمنية. الثقة ليست احتمال ربح." else "Combines trend, momentum, liquidity, volatility and timeframe alignment. Confidence is not win probability.", neon)
            "غرفة المراقبة" -> card(if (ar) "قوائم المتابعة" else "Watchlists",
                if (ar) "ستتم إضافة قوائم مخصصة وتنبيهات عند اكتمال اتصال البيانات." else "Custom watchlists and alerts will be enabled after data connectivity.")
            "التداول التجريبي" -> card(if (ar) "Paper Trading" else "Paper Trading",
                if (ar) "محاكاة فقط. لا يتم إرسال أوامر تداول حقيقية أو ربط حساب وسيط." else "Simulation only. No live orders or broker account execution.")
            "مختبر التحليل" -> card(if (ar) "مختبر التحليل" else "Analysis Lab",
                if (ar) "اختبار تاريخي مع الرسوم والسبريد والانزلاق والتحقق خارج العينة." else "Historical testing with fees, spread, slippage and out-of-sample validation.")
            "إدارة المخاطر" -> card(if (ar) "إدارة المخاطر" else "Risk Manager",
                if (ar) "سيحسب حجم المركز والوقف والتعرض قبل السماح بأي محاكاة." else "Position sizing, stop distance and exposure checks before simulation.", neon)
            else -> {
                card(if (ar) "الإعدادات" else "Settings",
                    if (ar) "المظهر واللغة وإعدادات التنبيه ومعلومات مصدر البيانات." else "Theme, language, alert preferences and data source information.")
                card(if (ar) "الخصوصية" else "Privacy",
                    if (ar) "لا تضع مفاتيح API أو كلمات المرور داخل التطبيق أو المستودع." else "Never place API keys or passwords in the app or repository.")
            }
        }
        card(if (ar) "تنبيه" else "Notice",
            if (ar) "هذا الإصدار قيد التطوير. لا توجد توصيات مالية أو ضمانات ربح." else "This build is under development. No financial advice or profit guarantees.", mutedText)
    }

    companion object {
        private val sections = listOf(
            "مركز القيادة", "ماسح الأسواق", "صائد الفرص", "غرفة المراقبة",
            "التداول التجريبي", "مختبر التحليل", "إدارة المخاطر", "الإعدادات"
        )
    }
}
