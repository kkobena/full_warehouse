package com.kobe.warehouse.sales.ui.activity

import android.content.Intent
import android.os.Bundle
import com.kobe.warehouse.sales.databinding.ActivitySaleMenuSelectionBinding

/**
 * SaleMenuSelectionActivity
 * Main landing screen after login
 * Allows user to choose between:
 * - Simple Sales (Comptant only)
 * - Full Menu (Comptant, Assurance, Carnet, Prévente)
 */
class SaleMenuSelectionActivity : BaseActivity() {

    private lateinit var binding: ActivitySaleMenuSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySaleMenuSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
    }

    /**
     * Setup toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            title = "Menu Principal"
        }
    }

    /**
     * Setup click listeners
     */
    private fun setupListeners() {
        // Simple sales - Comptant only
        binding.cardSimpleSales.setOnClickListener {
            openSimpleSales()
        }

        // Full menu - All sale types
        binding.cardFullMenu.setOnClickListener {
            openFullMenu()
        }
    }

    /**
     * Open simple sales (Comptant only via ComptantSaleActivity)
     */
    private fun openSimpleSales() {
        val intent = Intent(this, ComptantSaleActivity::class.java)
        startActivity(intent)
    }

    /**
     * Open full menu (All sale types: Comptant, Assurance, Carnet, Prévente)
     */
    private fun openFullMenu() {
        val intent = Intent(this, FullSaleHomeActivity::class.java)
        startActivity(intent)
    }
}
