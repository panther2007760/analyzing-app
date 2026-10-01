package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.db.PriceAlertDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class AlertNotificationManager(
    private val context: Context,
    private val priceAlertDao: PriceAlertDao,
    private val scope: CoroutineScope
) {
    private val tag = "AivoraAlertManager"
    private val channelId = "aivora_price_alerts"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Aivora Price Alerts & Signals"
            val descriptionText = "Notifications when target prices or critical technical levels are breached"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun checkAlertsForPrice(pairSymbol: String, currentPrice: Double) {
        if (currentPrice <= 0.0) return

        scope.launch(Dispatchers.IO) {
            try {
                // Collect one-shot from DAO
                priceAlertDao.getActiveAlerts().collect { activeList ->
                    for (alert in activeList) {
                        if (alert.pairSymbol.equals(pairSymbol, ignoreCase = true) ||
                            alert.pairSymbol.contains(pairSymbol, ignoreCase = true)
                        ) {
                            val triggered = if (alert.isAbove) {
                                currentPrice >= alert.targetPrice
                            } else {
                                currentPrice <= alert.targetPrice
                            }

                            if (triggered) {
                                triggerNotification(
                                    id = alert.id.toInt(),
                                    title = "Target Price Alert: ${alert.pairSymbol}",
                                    message = "Price ${if (alert.isAbove) "surpassed" else "dropped below"} target of $${alert.targetPrice}! Current: $${String.format(Locale.US, "%.2f", currentPrice)}"
                                )
                                priceAlertDao.markAlertTriggered(alert.id, System.currentTimeMillis())
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to check alerts: ${e.message}")
            }
        }
    }

    fun sendTestNotification(): Boolean {
        if (!hasNotificationPermission()) {
            return false
        }
        triggerNotification(
            id = 9999,
            title = "Aivora System Notification Test",
            message = "Price alerts & signal notifications are active and ready."
        )
        return true
    }

    private fun triggerNotification(id: Int, title: String, message: String) {
        if (!hasNotificationPermission()) {
            Log.w(tag, "Cannot trigger notification: POST_NOTIFICATIONS permission not granted")
            return
        }

        try {
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(id, builder.build())
        } catch (e: SecurityException) {
            Log.e(tag, "SecurityException sending notification", e)
        } catch (e: Exception) {
            Log.e(tag, "Error sending notification", e)
        }
    }
}
