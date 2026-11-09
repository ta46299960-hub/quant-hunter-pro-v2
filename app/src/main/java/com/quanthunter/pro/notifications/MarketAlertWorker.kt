package com.quanthunter.pro.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quanthunter.pro.R
import com.quanthunter.pro.data.BinanceSpotProvider
import com.quanthunter.pro.data.CandleInterval
import com.quanthunter.pro.data.CandleRequest
import com.quanthunter.pro.data.DataQualityValidator
import com.quanthunter.pro.engine.MarketType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class MarketAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences("qh_preferences", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("background_alerts_enabled", false)) return@withContext Result.success()

        try {
            val symbols = (prefs.getStringSet("watchlist_symbols", setOf("BTCUSDT", "ETHUSDT", "BNBUSDT"))
                ?: setOf("BTCUSDT")).toList().take(8)
            val provider = BinanceSpotProvider()
            for (symbol in symbols) {
                val response = provider.fetchCandles(
                    CandleRequest(symbol, MarketType.CRYPTO, CandleInterval.H1, 5)
                )
                val candles = response.data ?: continue
                if (!response.isSuccessful || !response.isLive || candles.size < 2) continue
                val quality = DataQualityValidator().validate(candles, CandleInterval.H1, System.currentTimeMillis())
                if (!quality.isUsable || quality.acceptedCandles.size < 2) continue

                val now = System.currentTimeMillis()
                val signal = MarketAlertRules.evaluate(symbol, quality.acceptedCandles, now, 2.0) ?: continue
                val key = "alert_last_closed_candle_$symbol"
                val lastNotifiedCandle = prefs.getLong(key, 0L)
                if (signal.candleTimestampEpochMillis > lastNotifiedCandle &&
                    postAlert(symbol, signal.changePercent)
                ) {
                    prefs.edit().putLong(key, signal.candleTimestampEpochMillis).apply()
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun postAlert(symbol: String, change: Double): Boolean {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "qh_market_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Market movement alerts", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false

        val direction = if (change >= 0) "ارتفع" else "انخفض"
        val body = String.format(Locale.US, "%s %s خلال آخر شمعة ساعة: %+.2f%%", symbol, direction, change)
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("QUANT HUNTER PRO — تنبيه سوق")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(symbol.hashCode(), notification)
        return true
    }
}
