package com.kobe.warehouse.inventory.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.inventory.R
import com.kobe.warehouse.inventory.utils.NetworkMonitor
import com.kobe.warehouse.inventory.utils.SessionManager
import com.kobe.warehouse.inventory.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * Activité de base des écrans authentifiés.
 *
 * Aligné sur `sales-android/ui/activity/BaseActivity` : écoute des événements de
 * session, redirection vers la connexion, menu de déconnexion.
 *
 * **Différence assumée** : une perte de connexion n'entraîne pas de redirection vers
 * l'écran de connexion. Le comptage se poursuit hors ligne (cf. `SessionManager`).
 */
abstract class BaseActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    /** À surcharger pour masquer le menu de déconnexion sur un écran donné */
    protected open val showLogoutMenu: Boolean = true

    /**
     * Installe la barre d'outils et active la flèche de retour.
     *
     * Le clic est déjà traité par [onOptionsItemSelected] via `android.R.id.home`, qui
     * délègue à `onBackPressedDispatcher` — même comportement que le geste de retour
     * système.
     *
     * @param showBack `false` pour un écran racine (accueil), qui n'a nulle part où revenir
     */
    protected fun setupToolbar(toolbar: MaterialToolbar, showBack: Boolean = true) {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(showBack)
            setDisplayShowHomeEnabled(showBack)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager.getInstance(this)

        lifecycleScope.launch {
            sessionManager.sessionEvents.collect { event ->
                when (event) {
                    SessionManager.SessionEvent.SESSION_EXPIRED ->
                        handleSessionExpired(getString(R.string.session_expired))

                    SessionManager.SessionEvent.UNAUTHORIZED ->
                        handleSessionExpired(getString(R.string.session_unauthorized))

                    // Hors ligne : on informe, on ne déconnecte pas. Le message n'a
                    // d'intérêt que si l'appareil se croit connecté — serveur arrêté,
                    // mauvaise adresse, Wi-Fi sans accès. Quand il se sait hors réseau
                    // (ou l'ignore encore), le bandeau permanent suffit.
                    SessionManager.SessionEvent.CONNECTION_LOST ->
                        if (NetworkMonitor.isOnline.value == true) {
                            Toast.makeText(
                                this@BaseActivity,
                                getString(R.string.session_connection_lost),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
        }
    }

    private fun handleSessionExpired(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        redirectToLogin()
    }

    private fun redirectToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (showLogoutMenu) {
            menuInflater.inflate(R.menu.menu_base, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }

            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm)
            .setPositiveButton(R.string.logout) { _, _ -> performLogout() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    protected fun performLogout() {
        TokenManager(this).clearTokens()
        redirectToLogin()
    }
}
