package com.kobe.warehouse.sales.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.utils.SessionManager
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * Base Activity
 * All authenticated activities should extend this class
 * Handles session expiration, redirects to login, and provides logout functionality
 */
abstract class BaseActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    /**
     * Override this to disable logout menu in specific activities
     * Default: true (logout menu is shown)
     */
    protected open val showLogoutMenu: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager.getInstance(this)

        // Collect session events
        lifecycleScope.launch {
            sessionManager.sessionEvents.collect { event ->
                when (event) {
                    SessionManager.SessionEvent.SESSION_EXPIRED -> {
                        handleSessionExpired("Votre session a expiré")
                    }
                    SessionManager.SessionEvent.UNAUTHORIZED -> {
                        handleSessionExpired("Accès non autorisé. Veuillez vous reconnecter.")
                    }
                    SessionManager.SessionEvent.CONNECTION_LOST -> {
                        handleConnectionLost("Connexion au serveur perdue")
                    }
                }
            }
        }
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
