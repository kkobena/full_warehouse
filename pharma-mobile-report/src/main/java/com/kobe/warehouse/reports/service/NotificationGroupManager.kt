package com.kobe.warehouse.reports.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.ui.activity.AlertsActivity
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages notification grouping and batching to prevent notification spam.
 * Groups similar notifications together (e.g., multiple stock alerts).
 */
class NotificationGroupManager private constructor(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Store pending notifications by type
    private val pendingNotifications = ConcurrentHashMap<String, MutableList<NotificationData>>()

    // Last notification time by type (to implement batching delay)
    private val lastNotificationTime = ConcurrentHashMap<String, Long>()

    companion object {
        private const val BATCHING_DELAY_MS = 5000L // 5 seconds
        private const val MAX_NOTIFICATIONS_PER_GROUP = 5

        // Notification groups
        const val GROUP_STOCK_ALERTS = "stock_alerts"
        const val GROUP_EXPIRY_ALERTS = "expiry_alerts"
        const val GROUP_CASH_ALERTS = "cash_alerts"
        const val GROUP_INVOICE_ALERTS = "invoice_alerts"
        const val GROUP_DAILY_UPDATES = "daily_updates"

        @Volatile
        private var instance: NotificationGroupManager? = null

        fun getInstance(context: Context): NotificationGroupManager {
            return instance ?: synchronized(this) {
                instance ?: NotificationGroupManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    data class NotificationData(
        val id: Int,
        val title: String,
        val body: String,
        val type: String,
        val data: Map<String, String>,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Add notification to group. Will batch if multiple notifications of same type arrive quickly.
     */
    fun addNotification(
        title: String,
        body: String,
        type: String,
        data: Map<String, String>
    ) {
        val groupKey = getGroupKey(type)
        val notificationId = System.currentTimeMillis().toInt()

        val notificationData = NotificationData(
            id = notificationId,
            title = title,
            body = body,
            type = type,
            data = data
        )

        // Add to pending notifications
        pendingNotifications.getOrPut(groupKey) { mutableListOf() }.add(notificationData)

        // Check if we should batch or show immediately
        val lastTime = lastNotificationTime[groupKey] ?: 0L
        val timeSinceLastNotification = System.currentTimeMillis() - lastTime

        if (timeSinceLastNotification > BATCHING_DELAY_MS ||
            pendingNotifications[groupKey]!!.size >= MAX_NOTIFICATIONS_PER_GROUP) {
            // Show batched notifications
            showGroupedNotifications(groupKey)
        } else {
            // Schedule delayed show (would need WorkManager or Handler for production)
            // For now, show after short delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                showGroupedNotifications(groupKey)
            }, BATCHING_DELAY_MS)
        }
    }

    /**
     * Show grouped notifications for a specific group.
     */
    private fun showGroupedNotifications(groupKey: String) {
        val notifications = pendingNotifications[groupKey] ?: return
        if (notifications.isEmpty()) return

        lastNotificationTime[groupKey] = System.currentTimeMillis()

        if (notifications.size == 1) {
            // Show single notification
            val notification = notifications.first()
            showSingleNotification(notification, groupKey)
        } else {
            // Show grouped notification with summary
            showBatchedNotification(groupKey, notifications)
        }

        // Clear pending notifications for this group
        pendingNotifications[groupKey]?.clear()
    }

    /**
     * Show a single notification.
     */
    private fun showSingleNotification(notification: NotificationData, groupKey: String) {
        val channelId = getChannelIdForGroup(groupKey)
        val intent = createIntentForNotification(notification)

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(groupKey)

        // Add action buttons based on notification type
        addActionButtons(builder, notification)

        notificationManager.notify(notification.id, builder.build())
    }

    /**
     * Show batched notification with summary.
     */
    private fun showBatchedNotification(groupKey: String, notifications: List<NotificationData>) {
        val channelId = getChannelIdForGroup(groupKey)
        val summaryText = getSummaryText(groupKey, notifications.size)
        val groupTitle = getGroupTitle(groupKey)

        // Create summary notification
        val summaryIntent = Intent(context, AlertsActivity::class.java).apply {
            putExtra("filter_group", groupKey)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val summaryPendingIntent = PendingIntent.getActivity(
            context,
            groupKey.hashCode(),
            summaryIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val summaryNotification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(groupTitle)
            .setContentText(summaryText)
            .setStyle(NotificationCompat.InboxStyle()
                .setBigContentTitle(groupTitle)
                .setSummaryText(summaryText)
                .also { style ->
                    notifications.take(5).forEach { notification ->
                        style.addLine("${notification.title}: ${notification.body}")
                    }
                }
            )
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(summaryPendingIntent)
            .setAutoCancel(true)
            .setNumber(notifications.size)
            .build()

        notificationManager.notify(groupKey.hashCode(), summaryNotification)

        // Show individual notifications (collapsed in notification drawer)
        notifications.forEach { notification ->
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notification.title)
                .setContentText(notification.body)
                .setGroup(groupKey)
                .setAutoCancel(true)

            notificationManager.notify(notification.id, builder.build())
        }
    }

    /**
     * Add action buttons to notification based on type.
     */
    private fun addActionButtons(builder: NotificationCompat.Builder, notification: NotificationData) {
        when (notification.type) {
            PharmaFirebaseMessagingService.TYPE_STOCK_RUPTURE,
            PharmaFirebaseMessagingService.TYPE_STOCK_LOW -> {
                // Add "Commander" action
                val orderIntent = Intent(context, AlertsActivity::class.java).apply {
                    action = "ACTION_ORDER_PRODUCT"
                    putExtra("product_id", notification.data["productId"])
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val orderPendingIntent = PendingIntent.getActivity(
                    context,
                    notification.id + 1,
                    orderIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(
                    R.drawable.ic_shopping_cart,
                    "Commander",
                    orderPendingIntent
                )
            }

            PharmaFirebaseMessagingService.TYPE_EXPIRY -> {
                // Add "Demarquer" action
                val discountIntent = Intent(context, AlertsActivity::class.java).apply {
                    action = "ACTION_CREATE_DISCOUNT"
                    putExtra("product_id", notification.data["productId"])
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val discountPendingIntent = PendingIntent.getActivity(
                    context,
                    notification.id + 1,
                    discountIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(
                    R.drawable.ic_discount,
                    "Demarquer",
                    discountPendingIntent
                )
            }

            PharmaFirebaseMessagingService.TYPE_INVOICE_OVERDUE -> {
                // Add "Appeler" action
                val callIntent = Intent(context, AlertsActivity::class.java).apply {
                    action = "ACTION_CALL_CLIENT"
                    putExtra("invoice_id", notification.data["invoiceId"])
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val callPendingIntent = PendingIntent.getActivity(
                    context,
                    notification.id + 1,
                    callIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(
                    R.drawable.ic_phone,
                    "Appeler",
                    callPendingIntent
                )
            }
        }
    }

    private fun createIntentForNotification(notification: NotificationData): Intent {
        return Intent(context, AlertsActivity::class.java).apply {
            putExtra("notification_type", notification.type)
            putExtra("notification_data", HashMap(notification.data))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    private fun getGroupKey(type: String): String {
        return when (type) {
            PharmaFirebaseMessagingService.TYPE_STOCK_RUPTURE,
            PharmaFirebaseMessagingService.TYPE_STOCK_LOW -> GROUP_STOCK_ALERTS

            PharmaFirebaseMessagingService.TYPE_EXPIRY -> GROUP_EXPIRY_ALERTS

            PharmaFirebaseMessagingService.TYPE_CASH_DISCREPANCY -> GROUP_CASH_ALERTS

            PharmaFirebaseMessagingService.TYPE_INVOICE_OVERDUE -> GROUP_INVOICE_ALERTS

            PharmaFirebaseMessagingService.TYPE_DAILY_DIGEST,
            PharmaFirebaseMessagingService.TYPE_TARGET_REACHED,
            PharmaFirebaseMessagingService.TYPE_HIGH_VALUE_SALE -> GROUP_DAILY_UPDATES

            else -> "other"
        }
    }

    private fun getChannelIdForGroup(groupKey: String): String {
        return when (groupKey) {
            GROUP_STOCK_ALERTS, GROUP_EXPIRY_ALERTS,
            GROUP_CASH_ALERTS, GROUP_INVOICE_ALERTS -> PharmaFirebaseMessagingService.CHANNEL_ID_ALERTS

            GROUP_DAILY_UPDATES -> PharmaFirebaseMessagingService.CHANNEL_ID_DAILY

            else -> PharmaFirebaseMessagingService.CHANNEL_ID_ALERTS
        }
    }

    private fun getGroupTitle(groupKey: String): String {
        return when (groupKey) {
            GROUP_STOCK_ALERTS -> "Alertes Stock"
            GROUP_EXPIRY_ALERTS -> "Alertes Peremption"
            GROUP_CASH_ALERTS -> "Alertes Caisse"
            GROUP_INVOICE_ALERTS -> "Alertes Factures"
            GROUP_DAILY_UPDATES -> "Mises a jour"
            else -> "Notifications"
        }
    }

    private fun getSummaryText(groupKey: String, count: Int): String {
        return when (groupKey) {
            GROUP_STOCK_ALERTS -> "$count alertes de stock"
            GROUP_EXPIRY_ALERTS -> "$count produits proches de la peremption"
            GROUP_CASH_ALERTS -> "$count alertes de caisse"
            GROUP_INVOICE_ALERTS -> "$count factures a relancer"
            GROUP_DAILY_UPDATES -> "$count notifications"
            else -> "$count notifications"
        }
    }

    /**
     * Clear all pending notifications for a group.
     */
    fun clearPendingNotifications(groupKey: String) {
        pendingNotifications[groupKey]?.clear()
    }

    /**
     * Get pending notification count for a group.
     */
    fun getPendingCount(groupKey: String): Int {
        return pendingNotifications[groupKey]?.size ?: 0
    }

    /**
     * Get total badge count (all pending notifications).
     */
    fun getTotalBadgeCount(): Int {
        return pendingNotifications.values.sumOf { it.size }
    }
}
