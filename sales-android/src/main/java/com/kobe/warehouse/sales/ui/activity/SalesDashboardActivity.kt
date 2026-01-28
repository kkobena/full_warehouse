package com.kobe.warehouse.sales.ui.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.model.DashboardData
import com.kobe.warehouse.sales.data.model.Period
import com.kobe.warehouse.sales.data.model.TopProduct
import com.kobe.warehouse.sales.databinding.ActivitySalesDashboardBinding
import com.kobe.warehouse.sales.ui.adapter.TopProductsAdapter

/**
 * Sales Dashboard Activity
 * Displays sales statistics and KPIs for the current user/seller
 *
 * Features:
 * - Total sales (CA) for selected period
 * - Number of sales transactions
 * - Average basket value
 * - Objective progress
 * - Top selling products list
 * - Period filter (Today, Week, Month)
 * - Refresh button
 *
 * TODO: Integrate with ViewModel and API
 * TODO: Add chart library for visual data representation (e.g., MPAndroidChart)
 * TODO: Add pull-to-refresh
 * TODO: Add error handling
 * TODO: Add empty state
 */
class SalesDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalesDashboardBinding
    private lateinit var topProductsAdapter: TopProductsAdapter
    private var selectedPeriod: Period = Period.TODAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadDashboardData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        topProductsAdapter = TopProductsAdapter()
        binding.rvTopProducts.apply {
            layoutManager = LinearLayoutManager(this@SalesDashboardActivity)
            adapter = topProductsAdapter
        }
    }

    private fun setupListeners() {
        // Period selection
        binding.chipGroupPeriod.setOnCheckedChangeListener { _, checkedId ->
            selectedPeriod = when (checkedId) {
                R.id.chip_today -> Period.TODAY
                R.id.chip_week -> Period.WEEK
                R.id.chip_month -> Period.MONTH
                else -> Period.TODAY
            }
            loadDashboardData()
        }

        // Refresh button
        binding.btnRefresh.setOnClickListener {
            loadDashboardData()
        }
    }

    private fun loadDashboardData() {
        // TODO: Integrate with ViewModel
        // TODO: Call API endpoint: GET /api/mobile/dashboard?period={selectedPeriod}
        //
        // For now, show mock data for demonstration
        showLoading(true)

        // Simulate API call with delay
        binding.root.postDelayed({
            showLoading(false)
            displayDashboardData(getMockDashboardData())
        }, 1000)
    }

    private fun displayDashboardData(data: DashboardData) {
        // Display KPIs
        binding.tvTotalSales.text = data.getFormattedTotalSales()
        binding.tvSalesCount.text = data.salesCount.toString()
        binding.tvAverageBasket.text = data.getFormattedAverageBasket()
        binding.tvObjectiveProgress.text = data.getFormattedObjectiveProgress()

        // Update objective progress bar
        binding.progressObjective.progress = data.getObjectiveProgressPercentage()

        // Display top products
        if (data.topProducts.isEmpty()) {
            binding.rvTopProducts.visibility = View.GONE
            binding.tvNoProducts.visibility = View.VISIBLE
        } else {
            binding.rvTopProducts.visibility = View.VISIBLE
            binding.tvNoProducts.visibility = View.GONE
            topProductsAdapter.submitList(data.topProducts)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRefresh.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                loadDashboardData()
            }
            .show()
    }

    /**
     * Mock data for demonstration
     * TODO: Remove when integrating with real API
     */
    private fun getMockDashboardData(): DashboardData {
        val mockTopProducts = listOf(
            TopProduct(
                productId = 1,
                productName = "Paracétamol 500mg",
                productCode = "PARA500",
                quantitySold = 150,
                totalSales = 75000,
                rank = 1
            ),
            TopProduct(
                productId = 2,
                productName = "Ibuprofène 400mg",
                productCode = "IBU400",
                quantitySold = 120,
                totalSales = 60000,
                rank = 2
            ),
            TopProduct(
                productId = 3,
                productName = "Amoxicilline 500mg",
                productCode = "AMOX500",
                quantitySold = 95,
                totalSales = 47500,
                rank = 3
            ),
            TopProduct(
                productId = 4,
                productName = "Aspirine 100mg",
                productCode = "ASP100",
                quantitySold = 80,
                totalSales = 40000,
                rank = 4
            ),
            TopProduct(
                productId = 5,
                productName = "Vitamine C 1000mg",
                productCode = "VITC1000",
                quantitySold = 75,
                totalSales = 37500,
                rank = 5
            )
        )

        return DashboardData(
            totalSales = 500000,
            salesCount = 45,
            averageBasket = 11111,
            objective = 750000,
            objectiveProgress = 0.67,
            topProducts = mockTopProducts,
            period = selectedPeriod
        )
    }
}
