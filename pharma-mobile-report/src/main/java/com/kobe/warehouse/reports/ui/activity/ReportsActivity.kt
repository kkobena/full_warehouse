package com.kobe.warehouse.reports.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.databinding.ActivityReportsBinding

/**
 * Reports activity - displays list of available reports.
 * Extends BaseActivity for session management and logout functionality.
 */
class ReportsActivity : BaseActivity() {

    private lateinit var binding: ActivityReportsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupReportCards()
        setupBottomNavigation()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_refresh -> {
                    // No refresh needed on this screen
                    true
                }
                R.id.action_settings -> {
                    navigateToSettings()
                    true
                }
                R.id.action_logout -> {
                    performLogout()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupReportCards() {
        // Phase 3 Reports
        binding.cardPharmacistDashboard.setOnClickListener {
            navigateToPharmacistDashboard()
        }

        binding.cardCashSummary.setOnClickListener {
            navigateToCashSummary()
        }

        binding.cardActivityReport.setOnClickListener {
            navigateToActivityReport()
        }

        binding.cardCashBalance.setOnClickListener {
            navigateToCashBalance()
        }

        binding.cardTvaReport.setOnClickListener {
            navigateToTvaReport()
        }

        binding.cardPerformance.setOnClickListener {
            navigateToPerformance()
        }

        // Phase 4 Statistical Reports
        binding.cardTiersPayant.setOnClickListener {
            navigateToTiersPayantCreances()
        }

        binding.cardSupplierPerformance.setOnClickListener {
            navigateToSupplierPerformance()
        }

        binding.cardStockValuation.setOnClickListener {
            navigateToStockValuation()
        }

        binding.cardProfitability.setOnClickListener {
            navigateToProfitability()
        }

        binding.cardStockRotation.setOnClickListener {
            navigateToStockRotation()
        }

        binding.cardAbcPareto.setOnClickListener {
            navigateToAbcPareto()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    navigateToDashboard()
                    true
                }
                R.id.nav_alerts -> {
                    navigateToAlerts()
                    true
                }
                R.id.nav_reports -> true // Already on reports
                R.id.nav_todos -> {
                    navigateToTodos()
                    true
                }
                R.id.nav_search -> {
                    navigateToProductSearch()
                    true
                }
                else -> false
            }
        }

        // Set reports as selected in bottom nav
        binding.bottomNav.selectedItemId = R.id.nav_reports
    }

    // =========================================================================
    // NAVIGATION
    // =========================================================================

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToAlerts() {
        val intent = Intent(this, AlertsActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToTodos() {
        val intent = Intent(this, TodosActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToPerformance() {
        val intent = Intent(this, PerformanceActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToProductSearch() {
        val intent = Intent(this, ProductSearchActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToPharmacistDashboard() {
        val intent = Intent(this, PharmacistDashboardActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToCashSummary() {
        val intent = Intent(this, CashSummaryActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToActivityReport() {
        val intent = Intent(this, ActivityReportActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToCashBalance() {
        val intent = Intent(this, CashBalanceActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToTvaReport() {
        val intent = Intent(this, TvaReportActivity::class.java)
        startActivity(intent)
    }

    // Phase 4 Statistical Reports Navigation

    private fun navigateToTiersPayantCreances() {
        val intent = Intent(this, TiersPayantCreancesActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToSupplierPerformance() {
        val intent = Intent(this, SupplierPerformanceActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToStockValuation() {
        val intent = Intent(this, StockValuationActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToProfitability() {
        val intent = Intent(this, ProfitabilityActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToStockRotation() {
        val intent = Intent(this, StockRotationActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToAbcPareto() {
        val intent = Intent(this, AbcParetoActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToSettings() {
        val intent = Intent(this, ServerConfigActivity::class.java)
        startActivity(intent)
    }
}
