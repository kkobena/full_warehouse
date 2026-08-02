package com.kobe.warehouse.inventory.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kobe.warehouse.inventory.utils.TokenManager

/**
 * Écran de démarrage : oriente vers l'accueil ou la connexion selon l'état de session.
 *
 * Aligné sur `sales-android/ui/activity/SplashActivity` — **sans temporisation
 * artificielle** : l'API SplashScreen affiche déjà l'écran pendant l'initialisation de
 * l'application, une attente supplémentaire ne fait que retarder l'opérateur.
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val tokenManager = TokenManager(this)
        val destination = if (tokenManager.getAccessToken() != null) {
            InventoryListActivity::class.java
        } else {
            LoginActivity::class.java
        }

        startActivity(Intent(this, destination))
        finish()
    }
}
