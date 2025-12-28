package com.kobe.warehouse.reports.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kobe.warehouse.reports.BuildConfig
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.ui.activity.AlertsActivity
import com.kobe.warehouse.reports.ui.activity.DashboardActivity
import com.kobe.warehouse.reports.ui.activity.ProductDetailActivity
import com.kobe.warehouse.reports.ui.activity.TodosActivity
import com.kobe.warehouse.reports.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.leolin.shortcutbadger.ShortcutBadger
import androidx.core.content.edit

/**
 * Firebase Cloud Messaging service for handling push notifications.
 * Can be enabled/disabled via BuildConfig.FIREBASE_ENABLED
 */
class PharmaFirebaseMessagingService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val TAG = "PharmaFCM"
        private const val CHANNEL_NAME_ALERTS = "Alertes"
        private const val CHANNEL_NAME_DAILY = "Resume quotidien"
        private const val PREFS_NAME = "badge_prefs"
        private const val KEY_BADGE_COUNT = "badge_count"

        // Notification types from backend
        const val TYPE_STOCK_RUPTURE = "STOCK_RUPTURE"
        const val TYPE_STOCK_LOW = "STOCK_LOW"
        const val TYPE_EXPIRY = "EXPIRY"
        const val TYPE_CASH_DISCREPANCY = "CASH_DISCREPANCY"
        const val TYPE_INVOICE_OVERDUE = "INVOICE_OVERDUE"
        const val TYPE_DAILY_DIGEST = "DAILY_DIGEST"
        const val TYPE_TARGET_REACHED = "TARGET_REACHED"
        const val TYPE_HIGH_VALUE_SALE = "HIGH_VALUE_SALE"

        // Data keys
        const val KEY_TYPE = "type"
        const val KEY_PRODUCT_ID = "productId"
        const val KEY_LOT_ID = "lotId"
        const val KEY_INVOICE_ID = "invoiceId"
        const val KEY_SALE_ID = "saleId"
        const val KEY_DATE = "date"
        const val KEY_ACTION = "action"

        // Make channels publicly accessible
        const val CHANNEL_ID_ALERTS = "pharma_alerts"
        const val CHANNEL_ID_DAILY = "pharma_daily"

        /**
         * Reset badge count to zero.
         * Call this when user opens the app or views notifications.
         */
        fun resetBadgeCount(context: Context) {
            try {
                ShortcutBadger.removeCount(context)
                saveBadgeCount(context, 0)
                Log.d(TAG, "Badge count reset to 0")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset badge count", e)
            }
        }

        /**
         * Get current badge count.
         */
        fun getBadgeCount(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_BADGE_COUNT, 0)
        }

        /**
         * Save badge count to SharedPreferences.
         */
        private fun saveBadgeCount(context: Context, count: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit { putInt(KEY_BADGE_COUNT, count) }
        }

        /**
         * Increment badge count.
         */
        fun incrementBadgeCount(context: Context): Int {
            val newCount = getBadgeCount(context) + 1
            saveBadgeCount(context, newCount)
            applyBadgeCount(context, newCount)
            return newCount
        }

        /**
         * Apply badge count using ShortcutBadger.
         */
        private fun applyBadgeCount(context: Context, count: Int) {
            try {
                if (count > 0) {
                    ShortcutBadger.applyCount(context, count)
                } else {
                    ShortcutBadger.removeCount(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "ShortcutBadger error", e)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Check if Firebase is enabled
        if (!BuildConfig.FIREBASE_ENABLED) {
            Log.d(TAG, "Firebase is disabled. Ignoring FCM token.")
            return
        }

        // Save token locally
        TokenManager.getInstance(applicationContext).saveFcmToken(token)

        // Register token with backend
        scope.launch {
            try {
                val repository = PharmaReportApplication.getRepository()
                repository.registerFcmToken(token)
                Log.d(TAG, "FCM token registered successfully")
            } catch (e: Exception) {
                // Token will be registered on next login
                Log.e(TAG, "Failed to register FCM token", e)
                e.printStackTrace()
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if Firebase is enabled
        if (!BuildConfig.FIREBASE_ENABLED) {
            Log.d(TAG, "Firebase is disabled. Ignoring push notification.")
            return
        }

        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "Pharma-Smart",
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }

        // Handle data payload (when app is in foreground)
        if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data[KEY_TYPE] ?: return
        val groupManager = NotificationGroupManager.getInstance(applicationContext)

        val (title, body) = when (type) {
            TYPE_STOCK_RUPTURE, TYPE_STOCK_LOW -> {
                Pair("Alerte Stock", data["message"] ?: "Verifiez le stock")
            }
            TYPE_EXPIRY -> {
                Pair("Peremption proche", data["message"] ?: "Produits a verifier")
            }
            TYPE_CASH_DISCREPANCY -> {
                Pair("Ecart de caisse", data["message"] ?: "Verifiez la caisse")
            }
            TYPE_INVOICE_OVERDUE -> {
                Pair("Facture impayee", data["message"] ?: "Facture a relancer")
            }
            TYPE_DAILY_DIGEST -> {
                Pair("Resume quotidien", data["message"] ?: "Votre resume du jour")
            }
            TYPE_TARGET_REACHED -> {
                Pair("Objectif atteint!", data["message"] ?: "Bravo!")
            }
            TYPE_HIGH_VALUE_SALE -> {
                Pair("Grosse vente!", data["message"] ?: "Nouvelle vente importante")
            }
            else -> return
        }

        // Use NotificationGroupManager for intelligent grouping
        groupManager.addNotification(title, body, type, data)

        // Update badge count
        updateBadgeCount(groupManager.getTotalBadgeCount())
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        channelId: String = CHANNEL_ID_ALERTS
    ) {
        createNotificationChannels()

        val intent = createIntentFromData(data)
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createIntentFromData(data: Map<String, String>): Intent {
        val type = data[KEY_TYPE]
        val action = data[KEY_ACTION]

        return when {
            // Navigate to product detail (stock alerts, expiry)
            action == "VIEW_PRODUCT" || type in listOf(TYPE_STOCK_RUPTURE, TYPE_STOCK_LOW, TYPE_EXPIRY) -> {
                val productId = data[KEY_PRODUCT_ID]?.toLongOrNull()
                if (productId != null) {
                    Intent(this, ProductDetailActivity::class.java).apply {
                        putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productId)
                        data[KEY_LOT_ID]?.toLongOrNull()?.let { lotId ->
                            putExtra("lot_id", lotId)
                        }
                    }
                } else {
                    Intent(this, AlertsActivity::class.java).apply {
                        putExtra(AlertsActivity.EXTRA_FILTER_TYPE, type)
                    }
                }
            }

            // Navigate to alerts with filter (cash discrepancy, invoice overdue)
            type in listOf(TYPE_CASH_DISCREPANCY, TYPE_INVOICE_OVERDUE) -> {
                Intent(this, AlertsActivity::class.java).apply {
                    putExtra(AlertsActivity.EXTRA_FILTER_TYPE, type)
                    data[KEY_INVOICE_ID]?.toLongOrNull()?.let { invoiceId ->
                        putExtra("invoice_id", invoiceId)
                    }
                }
            }

            // Navigate to todos (create order action)
            action == "CREATE_ORDER" || action == "REORDER" -> {
                Intent(this, TodosActivity::class.java).apply {
                    putExtra("action", action)
                    data[KEY_PRODUCT_ID]?.toLongOrNull()?.let { productId ->
                        putExtra("product_id", productId)
                    }
                }
            }

            // Navigate to performance for daily digest
            type == TYPE_DAILY_DIGEST -> {
                Intent(this, com.kobe.warehouse.reports.ui.activity.PerformanceActivity::class.java).apply {
                    data[KEY_DATE]?.let { date ->
                        putExtra("date", date)
                    }
                }
            }

            // Navigate to dashboard for target reached or high value sale
            type in listOf(TYPE_TARGET_REACHED, TYPE_HIGH_VALUE_SALE) -> {
                Intent(this, DashboardActivity::class.java).apply {
                    putExtra("highlight_ca", true)
                    data[KEY_SALE_ID]?.toLongOrNull()?.let { saleId ->
                        putExtra("sale_id", saleId)
                    }
                }
            }

            // Call client action
            action == "CALL_CLIENT" -> {
                val phone = data["phone"]
                if (phone != null) {
                    Intent(Intent.ACTION_DIAL).apply {
                        this.data = android.net.Uri.parse("tel:$phone")
                    }
                } else {
                    Intent(this, TodosActivity::class.java)
                }
            }

            // Default: navigate to dashboard
            else -> {
                Intent(this, DashboardActivity::class.java)
            }
        }.apply {
            // Add common flags for activity launch
            if (this.component != null) {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            // Add source marker for analytics
            putExtra("from_notification", true)
            type?.let { putExtra("notification_type", it) }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Alerts channel (high priority)
            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                CHANNEL_NAME_ALERTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes critiques (ruptures, peremptions, ecarts)"
                enableVibration(true)
            }

            // Daily channel (default priority)
            val dailyChannel = NotificationChannel(
                CHANNEL_ID_DAILY,
                CHANNEL_NAME_DAILY,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Resume quotidien et objectifs"
            }

            notificationManager.createNotificationChannel(alertsChannel)
            notificationManager.createNotificationChannel(dailyChannel)
        }
    }

    /**
     * Update app badge count using ShortcutBadger.
     */
    private fun updateBadgeCount(count: Int) {
        try {
            saveBadgeCount(applicationContext, count)
            applyBadgeCount(applicationContext, count)
            Log.d(TAG, "Badge count updated to: $count")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update badge count", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
