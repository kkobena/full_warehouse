package com.kobe.warehouse.reports.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.CashBalance
import com.kobe.warehouse.reports.data.model.PaymentModeBreakdown
import com.kobe.warehouse.reports.databinding.ActivityCashBalanceBinding
import com.kobe.warehouse.reports.ui.adapter.CashMovementAdapter
import com.kobe.warehouse.reports.ui.adapter.CategoryBalanceAdapter
import com.kobe.warehouse.reports.ui.adapter.PaymentBreakdownAdapter
import com.kobe.warehouse.reports.ui.viewmodel.CashBalanceViewModel
import com.kobe.warehouse.reports.ui.viewmodel.CashBalanceViewModelFactory
import com.kobe.warehouse.reports.utils.NumberFormatUtils
import java.time.Instant
import java.time.ZoneId

/**
 * Cash Balance Activity - displays Balance Caisse report.
 * Extends BaseActivity for session management and logout functionality.
 */
class CashBalanceActivity : BaseActivity() {

    private lateinit var binding: ActivityCashBalanceBinding
    private lateinit var viewModel: CashBalanceViewModel
    private lateinit var paymentBreakdownAdapter: PaymentBreakdownAdapter
    private lateinit var categoryBalanceAdapter: CategoryBalanceAdapter
    private lateinit var cashMovementAdapter: CashMovementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCashBalanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupPeriodSelector()
        setupRecyclerViews()
        setupPieChart()
        setupExpandableCards()
        setupListeners()
        observeViewModel()

        viewModel.loadCashBalance()
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = CashBalanceViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CashBalanceViewModel::class.java]
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupPeriodSelector() {
        binding.chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener

            when (checkedIds.first()) {
                R.id.chipToday -> viewModel.setPeriod(CashBalanceViewModel.Period.TODAY)
                R.id.chipYesterday -> viewModel.setPeriod(CashBalanceViewModel.Period.YESTERDAY)
                R.id.chipThisWeek -> viewModel.setPeriod(CashBalanceViewModel.Period.THIS_WEEK)
                R.id.chipThisMonth -> viewModel.setPeriod(CashBalanceViewModel.Period.THIS_MONTH)
                R.id.chipCustom -> showDateRangePicker()
            }
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.select_period))
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val fromDate = Instant.ofEpochMilli(selection.first)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val toDate = Instant.ofEpochMilli(selection.second)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            viewModel.setDateRange(fromDate, toDate)
        }

        picker.addOnNegativeButtonClickListener {
            binding.chipToday.isChecked = true
        }

        picker.show(supportFragmentManager, "date_range_picker")
    }

    private fun setupRecyclerViews() {
        // Payment breakdown adapter
        paymentBreakdownAdapter = PaymentBreakdownAdapter()
        binding.rvPaymentBreakdown.apply {
            layoutManager = LinearLayoutManager(this@CashBalanceActivity)
            adapter = paymentBreakdownAdapter
            isNestedScrollingEnabled = false
        }

        // Category balance adapter
        categoryBalanceAdapter = CategoryBalanceAdapter()
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@CashBalanceActivity)
            adapter = categoryBalanceAdapter
            isNestedScrollingEnabled = false
        }

        // Cash movement adapter
        cashMovementAdapter = CashMovementAdapter()
        binding.rvMouvements.apply {
            layoutManager = LinearLayoutManager(this@CashBalanceActivity)
            adapter = cashMovementAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupPieChart() {
        binding.pieChartPayments.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = getString(R.string.paiements)
            setCenterTextSize(14f)
            setEntryLabelTextSize(10f)
            setEntryLabelColor(Color.DKGRAY)
            legend.isEnabled = false
            setNoDataText(getString(R.string.no_data))
        }
    }

    private fun setupExpandableCards() {
        // Totaux
        binding.headerTotaux.setOnClickListener {
            viewModel.toggleTotaux()
        }

        // Payment Breakdown
        binding.headerPaymentBreakdown.setOnClickListener {
            viewModel.togglePaymentBreakdown()
        }

        // Categories
        binding.headerCategories.setOnClickListener {
            viewModel.toggleCategories()
        }

        // Mouvements
        binding.headerMouvements.setOnClickListener {
            viewModel.toggleMouvements()
        }

        // Marge
        binding.headerMarge.setOnClickListener {
            viewModel.toggleMarge()
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshCashBalance()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showSkeleton()
            }
            binding.loadingOverlay.isVisible = false
        }

        viewModel.isRefreshing.observe(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showContent()
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.cashBalance.observe(this) { balance ->
            balance?.let {
                showContent()
                updateUI(it)
            }
        }

        viewModel.isEmpty.observe(this) { isEmpty ->
            binding.emptyState.isVisible = isEmpty
        }

        // Expansion states
        viewModel.totauxExpanded.observe(this) { expanded ->
            binding.contentTotaux.isVisible = expanded
            binding.iconTotauxExpand.setImageResource(
                if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }

        viewModel.paymentBreakdownExpanded.observe(this) { expanded ->
            binding.contentPaymentBreakdown.isVisible = expanded
            binding.iconPaymentBreakdownExpand.setImageResource(
                if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }

        viewModel.categoriesExpanded.observe(this) { expanded ->
            binding.rvCategories.isVisible = expanded
            binding.iconCategoriesExpand.setImageResource(
                if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }

        viewModel.mouvementsExpanded.observe(this) { expanded ->
            binding.contentMouvements.isVisible = expanded
            binding.iconMouvementsExpand.setImageResource(
                if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }

        viewModel.margeExpanded.observe(this) { expanded ->
            binding.contentMarge.isVisible = expanded
            binding.iconMargeExpand.setImageResource(
                if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }
    }

    private fun showSkeleton() {
        binding.viewFlipper.displayedChild = VIEW_SKELETON
    }

    private fun showContent() {
        binding.viewFlipper.displayedChild = VIEW_CONTENT
    }

    private fun updateUI(balance: CashBalance) {
        // Period label
        binding.tvPeriodLabel.text = balance.periodLabel

        // Totaux header
        binding.tvTotauxHeader.text = balance.getFormattedMontantTtc()

        // Transaction count
        binding.tvTransactionsCount.text = balance.transactionsCount.toString()

        // Totaux detail rows
        setRowData(binding.rowMontantTtc.root, getString(R.string.montant_ttc), balance.getFormattedMontantTtc())
        setRowData(binding.rowMontantHt.root, getString(R.string.montant_ht), balance.getFormattedMontantHt())
        setRowData(binding.rowMontantTva.root, getString(R.string.montant_tva), balance.getFormattedMontantTva())
        setRowData(binding.rowRemise.root, getString(R.string.remise), balance.getFormattedMontantRemise())
        setRowData(binding.rowMontantNet.root, getString(R.string.montant_net), balance.getFormattedMontantNet())
        setRowData(binding.rowPanierMoyen.root, getString(R.string.panier_moyen), balance.getFormattedPanierMoyen())

        // Payment breakdown
        updatePieChart(balance.paymentBreakdown)
        paymentBreakdownAdapter.submitList(balance.paymentBreakdown)

        // Categories
        categoryBalanceAdapter.submitList(balance.categoryBalances)

        // Mouvements
        val totalEntrees = balance.cashMovements.filter { it.isEntree() }.sumOf { it.montant }
        val totalSorties = balance.cashMovements.filter { it.isSortie() }.sumOf { kotlin.math.abs(it.montant) }
        binding.tvTotalEntrees.text = NumberFormatUtils.formatCurrency(totalEntrees)
        binding.tvTotalSorties.text = NumberFormatUtils.formatCurrency(totalSorties)
        cashMovementAdapter.submitList(balance.cashMovements)

        // Marge
        setRowData(binding.rowMontantAchats.root, getString(R.string.montant_achats), balance.getFormattedMontantAchats())
        setRowData(binding.rowMontantMarge.root, getString(R.string.marge), balance.getFormattedMontantMarge())
        setRowData(binding.rowRatioVenteAchat.root, getString(R.string.ratio_vente_achat), balance.getFormattedRatioVenteAchat())
        setRowData(binding.rowRatioAchatVente.root, getString(R.string.ratio_achat_vente), balance.getFormattedRatioAchatVente())
    }

    private fun updatePieChart(breakdown: List<PaymentModeBreakdown>) {
        if (breakdown.isEmpty()) {
            binding.pieChartPayments.setNoDataText(getString(R.string.no_data))
            binding.pieChartPayments.invalidate()
            return
        }

        val entries = breakdown.map { PieEntry(it.percent.toFloat(), it.libelle) }
        val colors = breakdown.mapNotNull {
            try {
                Color.parseColor(it.color)
            } catch (e: Exception) {
                Color.GRAY
            }
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 11f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(binding.pieChartPayments)
            sliceSpace = 2f
        }

        binding.pieChartPayments.apply {
            data = PieData(dataSet)
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    private fun setRowData(rowView: View, label: String, value: String) {
        rowView.findViewById<TextView>(R.id.tvLabel).text = label
        rowView.findViewById<TextView>(R.id.tvValue).text = value
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.refreshCashBalance()
            }
            .show()
    }

    companion object {
        private const val VIEW_SKELETON = 0
        private const val VIEW_CONTENT = 1
    }
}
