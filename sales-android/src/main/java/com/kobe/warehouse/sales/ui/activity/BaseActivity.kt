package com.kobe.warehouse.sales.ui.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.utils.SessionManager
import com.kobe.warehouse.sales.utils.TokenManager

/**
 * Base Activity
 * All authenticated activities should extend this class
 * Handles session expiration, redirects to login, and provides logout functionality
 */
abstract class BaseActivity : AppCompatActivity() {

    private lateinit var sessionReceiver: BroadcastReceiver
    private lateinit var sessionManager: SessionManager

    /**
     * Override this to disable logout menu in specific activities
     * Default: true (logout menu is shown)
     */
    protected open val showLogoutMenu: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager.getInstance(this)

        // Register broadcast receiver for session events
        registerSessionReceiver()
    }

    /**
     * Register broadcast receiver to listen for session events
     */
    private fun registerSessionReceiver() {
        sessionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    SessionManager.ACTION_SESSION_EXPIRED -> {
                        handleSessionExpired("Votre session a expiré")
                    }
                    SessionManager.ACTION_UNAUTHORIZED -> {
                        handleSessionExpired("Accès non autorisé. Veuillez vous reconnecter.")
                    }
                    SessionManager.ACTION_CONNECTION_LOST -> {
                        handleConnectionLost("Connexion au serveur perdue")
                    }
                }
            }
        }

        // Register for session events
        val filter = IntentFilter().apply {
            addAction(SessionManager.ACTION_SESSION_EXPIRED)
            addAction(SessionManager.ACTION_UNAUTHORIZED)
            addAction(SessionManager.ACTION_CONNECTION_LOST)
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(sessionReceiver, filter)
    }

    /**
     * Handle session expired
     * Shows message and redirects to login
     */
    private fun handleSessionExpired(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        redirectToLogin()
    }

    /**
     * Handle connection lost
     * Shows error message and redirects to login
     * Backend service must be accessible for the app to function
     */
    private fun handleConnectionLost(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        redirectToLogin()
    }

    /**
     * Redirect to login activity
     */
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sessionReceiver)
    }

    /**
     * Inflate base menu with logout option
     * Subclasses can override to add more menu items
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (showLogoutMenu) {
            menuInflater.inflate(R.menu.menu_base, menu)
        }
        return true
    }

    /**
     * Handle base menu item selection
     * Subclasses should call super.onOptionsItemSelected() to handle logout
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            android.R.id.home -> {
                // Handle back button in toolbar
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Show logout confirmation dialog
     */
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Déconnexion")
            .setMessage("Voulez-vous vraiment vous déconnecter ?")
            .setPositiveButton("Déconnexion") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Perform logout
     * Clears tokens and redirects to login
     */
    protected fun performLogout() {
        val tokenManager = TokenManager(this)
        tokenManager.clearTokens()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
