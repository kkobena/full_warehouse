package com.kobe.warehouse.reports.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.Dashboard
import com.kobe.warehouse.reports.data.repository.ReportRepository
import com.kobe.warehouse.reports.ui.activity.DashboardActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Dashboard widget provider - shows CA and alerts on home screen.
 */
class DashboardWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val ACTION_REFRESH = "com.kobe.warehouse.reports.ACTION_WIDGET_REFRESH"
        private const val ACTION_OPEN_APP = "com.kobe.warehouse.reports.ACTION_OPEN_APP"

        /**
         * Request widget update from outside.
         */
        fun updateWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DashboardWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            val intent = Intent(context, DashboardWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REFRESH -> {
                // Trigger widget update
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, DashboardWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Create remote views
        val views = RemoteViews(context.packageName, R.layout.widget_dashboard)

        // Show loading state
        views.setViewVisibility(R.id.widget_loading, View.VISIBLE)
        views.setViewVisibility(R.id.widget_content, View.GONE)
        views.setViewVisibility(R.id.widget_error, View.GONE)

        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Load data asynchronously
        scope.launch {
            try {
                val repository = ReportRepository.getInstance(context)
                val result = repository.getDashboard()
                if (result.isFailure) {
                    throw result.exceptionOrNull() ?: Exception("Unknown error")
                }
                val dashboard = result.getOrThrow()

                // Update widget with data
                launch(Dispatchers.Main) {
                    updateWidgetWithData(context, appWidgetManager, appWidgetId, dashboard)
                }
            } catch (e: Exception) {
                // Show error state
                launch(Dispatchers.Main) {
                    showErrorState(context, appWidgetManager, appWidgetId)
                }
            }
        }
    }

    private fun updateWidgetWithData(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        dashboard: Dashboard
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_dashboard)

        // Hide loading, show content
        views.setViewVisibility(R.id.widget_loading, View.GONE)
        views.setViewVisibility(R.id.widget_content, View.VISIBLE)
        views.setViewVisibility(R.id.widget_error, View.GONE)

        // Format CA amount
        val formatter = NumberFormat.getNumberInstance(Locale.FRANCE)
        val caFormatted = formatter.format(dashboard.dailyCA)
        views.setTextViewText(R.id.tv_widget_ca, "$caFormatted FCFA")

        // Variation
        val variationText = String.format("%+.1f%%", dashboard.variationPercent)
        views.setTextViewText(R.id.tv_widget_variation, variationText)

        // Variation color and icon
        if (dashboard.isVariationPositive()) {
            views.setTextColor(R.id.tv_widget_variation, context.getColor(R.color.success))
            views.setImageViewResource(R.id.iv_widget_variation_icon, R.drawable.ic_trending_up)
        } else {
            views.setTextColor(R.id.tv_widget_variation, context.getColor(R.color.error))
            views.setImageViewResource(R.id.iv_widget_variation_icon, R.drawable.ic_trending_down)
        }

        // Target progress
        val progress = ((dashboard.dailyCA.toFloat() / dashboard.dailyTarget.toFloat()) * 100).toInt()
        views.setProgressBar(R.id.progress_widget_target, 100, progress.coerceIn(0, 100), false)
        views.setTextViewText(R.id.tv_widget_target_progress, "$progress%")

        // Alerts count
        val alertCount = dashboard.alerts.size
        if (alertCount > 0) {
            views.setViewVisibility(R.id.ll_widget_alerts, View.VISIBLE)
            views.setTextViewText(R.id.tv_widget_alerts_count, alertCount.toString())
        } else {
            views.setViewVisibility(R.id.ll_widget_alerts, View.GONE)
        }

        // Click to open app
        val openAppIntent = Intent(context, DashboardActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)

        // Refresh button
        val refreshIntent = Intent(context, DashboardWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            refreshIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.btn_widget_refresh, refreshPendingIntent)

        // Update widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun showErrorState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_dashboard)

        // Show error state
        views.setViewVisibility(R.id.widget_loading, View.GONE)
        views.setViewVisibility(R.id.widget_content, View.GONE)
        views.setViewVisibility(R.id.widget_error, View.VISIBLE)

        // Retry button
        val retryIntent = Intent(context, DashboardWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val retryPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            retryIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.btn_widget_retry, retryPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onEnabled(context: Context) {
        // Widget first added
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        // Last widget removed
        super.onDisabled(context)
        job.cancel()
    }
}
