package com.kobe.warehouse.reports.ui.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.utils.TokenManager

/**
 * Base activity for all authenticated screens.
 * Handles session management and common menu options.
 */
abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var tokenManager: TokenManager

    private val sessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_SESSION_EXPIRED,
                ACTION_UNAUTHORIZED,
                ACTION_CONNECTION_LOST -> {
                    handleSessionExpired()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = PharmaReportApplication.getTokenManager()

        // Check authentication
        if (!tokenManager.isAuthenticated()) {
            redirectToLogin()
            return
        }

        // Register session receiver
        registerSessionReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterSessionReceiver()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            R.id.action_settings -> {
                // Navigate to settings
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Register broadcast receiver for session events.
     */
    private fun registerSessionReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_SESSION_EXPIRED)
            addAction(ACTION_UNAUTHORIZED)
            addAction(ACTION_CONNECTION_LOST)
        }
        ContextCompat.registerReceiver(
            this,
            sessionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * Unregister broadcast receiver.
     */
    private fun unregisterSessionReceiver() {
        try {
            unregisterReceiver(sessionReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered
        }
    }

    /**
     * Handle session expiration.
     */
    private fun handleSessionExpired() {
        tokenManager.clearTokens()
        redirectToLogin()
    }

    /**
     * Redirect to login screen.
     */
    protected fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    /**
     * Show logout confirmation dialog.
     */
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm)
            .setPositiveButton(R.string.yes) { _, _ ->
                performLogout()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    /**
     * Perform logout.
     */
    protected fun performLogout() {
        tokenManager.clearTokens()
        redirectToLogin()
    }

    /**
     * Broadcast session expired event.
     */
    protected fun broadcastSessionExpired() {
        val intent = Intent(ACTION_SESSION_EXPIRED)
        sendBroadcast(intent)
    }

    companion object {
        const val ACTION_SESSION_EXPIRED = "com.kobe.warehouse.reports.SESSION_EXPIRED"
        const val ACTION_UNAUTHORIZED = "com.kobe.warehouse.reports.UNAUTHORIZED"
        const val ACTION_CONNECTION_LOST = "com.kobe.warehouse.reports.CONNECTION_LOST"
    }
}
