package com.kobe.warehouse.sales.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kobe.warehouse.sales.utils.TokenManager

/**
 * Splash Activity
 * Shows splash screen and routes to Login or Main based on authentication status
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen API
        installSplashScreen()

        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(this)

        // Check if user is already logged in
        val hasToken = tokenManager.getAccessToken() != null

        val intent = if (hasToken) {
            // Go directly to SalesHomeActivity (skip welcome screen)
            Intent(this, SalesHomeActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}
