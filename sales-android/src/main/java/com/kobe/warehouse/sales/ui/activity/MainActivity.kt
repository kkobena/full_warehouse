package com.kobe.warehouse.sales.ui.activity

import android.content.Intent
import android.os.Bundle
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.utils.TokenManager

/**
 * Main Activity
 * Dashboard with navigation to sales features
 * Extends BaseActivity for automatic session management
 */
class MainActivity : BaseActivity() {

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(this)

        // Create simple dashboard layout programmatically for now
        // TODO: Create proper layout XML file
        setContentView(R.layout.activity_main)

        setupUI()
    }

    private fun setupUI() {
        // Navigate to Sales Home
        findViewById<MaterialCardView>(R.id.cardSales)?.setOnClickListener {
            startActivity(Intent(this, SalesHomeActivity::class.java))
        }

        // Logout button
        findViewById<MaterialButton>(R.id.btnLogout)?.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        tokenManager.clearTokens()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
