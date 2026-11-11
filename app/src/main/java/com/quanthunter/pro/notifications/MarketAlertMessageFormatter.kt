package com.quanthunter.pro.notifications

import java.util.Locale

data class MarketAlertMessage(val title: String, val body: String)

/** Keeps background market alerts consistent with the app's saved language preference. */
object MarketAlertMessageFormatter {
    fun format(symbol: String, changePercent: Double, english: Boolean): MarketAlertMessage {
        require(symbol.isNotBlank()) { "Symbol is required" }
        require(changePercent.isFinite()) { "Change must be finite" }
        val direction = when {
            changePercent > 0.0 -> if (english) "rose" else "ارتفع"
            changePercent < 0.0 -> if (english) "fell" else "انخفض"
            else -> if (english) "was unchanged" else "لم يتغير"
        }
        val title = if (english) "QUANT HUNTER PRO — Market alert" else "QUANT HUNTER PRO — تنبيه سوق"
        val body = if (english) {
            String.format(Locale.US, "%s %s in the last closed 1-hour candle: %+.2f%%", symbol, direction, changePercent)
        } else {
            String.format(Locale.US, "%s %s خلال آخر شمعة ساعة مغلقة: %+.2f%%", symbol, direction, changePercent)
        }
        return MarketAlertMessage(title, body)
    }
}
