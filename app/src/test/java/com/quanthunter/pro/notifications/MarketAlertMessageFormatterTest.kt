package com.quanthunter.pro.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class MarketAlertMessageFormatterTest {
    @Test fun formatsEnglishRisingAlert() {
        val message = MarketAlertMessageFormatter.format("BTCUSDT", 2.35, true)
        assertEquals("QUANT HUNTER PRO — Market alert", message.title)
        assertEquals("BTCUSDT rose in the last closed 1-hour candle: +2.35%", message.body)
    }

    @Test fun formatsArabicFallingAlert() {
        val message = MarketAlertMessageFormatter.format("ETHUSDT", -2.0, false)
        assertEquals("QUANT HUNTER PRO — تنبيه سوق", message.title)
        assertEquals("ETHUSDT انخفض خلال آخر شمعة ساعة مغلقة: -2.00%", message.body)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteChange() {
        MarketAlertMessageFormatter.format("BTCUSDT", Double.NaN, true)
    }
}
