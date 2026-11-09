package com.quanthunter.pro

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val gold = Color.rgb(245, 190, 65)
    private val neon = Color.rgb(140, 255, 65)
    private val background = Color.rgb(13, 16, 20)
    private val panel = Color.rgb(25, 30, 36)
    private lateinit var content: LinearLayout
    private var activeSection = "مركز القيادة"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showSection(activeSection)
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(18, 12, 18, 12)
        }

    private fun showSection(section: String) {
        activeSection = section
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
            setPadding(16, 18, 16, 12)
        }
        root.addView(label("QUANT HUNTER  PRO  V2", 21f, gold, true))
        root.addView(label("HYBRID QUANT SYSTEM  •  الإصدار 2.0", 11f, neon))
        val navScroll = ScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        sections.forEach { name ->
            nav.addView(Button(this).apply {
                text = name
                textSize = 10f
                setTextColor(if (name == section) neon else gold)
                setOnClickListener { showSection(name) }
            })
        }
        navScroll.addView(nav)
        root.addView(navScroll)
        val bodyScroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 16, 4, 24)
        }
        bodyScroll.addView(content)
        root.addView(bodyScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        renderContent(section)
    }

    private fun card(title: String, detail: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(panel)
            setPadding(8, 8, 8, 8)
        }
        box.addView(label(title, 16f, gold, true))
        box.addView(label(detail, 13f, Color.LTGRAY))
        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 8, 0, 8)
        content.addView(box, params)
    }

    private fun renderContent(section: String) {
        content.addView(label(section, 23f, neon, true))
        when (section) {
            "مركز القيادة" -> {
                card("حالة النظام", "واجهة V2 تعمل — بيانات السوق الحية لم يتم ربطها بعد.")
                card("الأسواق المعتمدة", "Crypto • Forex • Stocks • Indices • Commodities")
                card("منهج التحليل", "Hybrid: نظرة سوقية عامة + قواعد مستقلة لكل سوق.")
                card("تنبيه الجودة", "لن نعرض بيانات تجريبية على أنها أسعار حقيقية أو مباشرة.")
            }
            "ماسح الأسواق" -> card("ماسح الأسواق", "سيتم عرض الأدوات بعد ربط مزودي البيانات والتحقق من حداثتها.")
            "صائد الفرص" -> card("Quant Hunter", "الإشارات ستتطلب توافق الاتجاه والزخم والسيولة وإدارة المخاطر.")
            "غرفة المراقبة" -> card("Watch Room", "قوائم متابعة وتنبيهات قابلة للتخصيص.")
            "التداول التجريبي" -> card("Paper Trading", "محاكاة فقط — لا يتم إرسال أوامر تداول حقيقية.")
            "مختبر التحليل" -> card("Analysis Lab", "تحليل متعدد الأطر الزمنية واختبار الاستراتيجيات.")
            "إدارة المخاطر" -> card("Risk Manager", "حساب حجم الصفقة والوقف والأهداف وفق حدود المخاطر.")
            else -> card("الإعدادات والتنبيهات", "اللغة • المظهر • مصادر البيانات • التنبيهات • التشخيص")
        }
    }

    companion object {
        private val sections = listOf(
            "مركز القيادة", "ماسح الأسواق", "صائد الفرص", "غرفة المراقبة",
            "التداول التجريبي", "مختبر التحليل", "إدارة المخاطر", "الإعدادات"
        )
    }
}
