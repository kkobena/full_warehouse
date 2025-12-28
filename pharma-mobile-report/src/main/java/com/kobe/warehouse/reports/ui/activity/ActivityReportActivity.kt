package com.kobe.warehouse.reports.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.ActivityReport
import com.kobe.warehouse.reports.databinding.ActivityActivityReportBinding
import com.kobe.warehouse.reports.ui.adapter.AchatFournisseurAdapter
import com.kobe.warehouse.reports.ui.adapter.AchatTpAdapter
import com.kobe.warehouse.reports.ui.adapter.MouvementAdapter
import com.kobe.warehouse.reports.ui.adapter.RecetteAdapter
import com.kobe.warehouse.reports.ui.adapter.ReglementTpAdapter
import com.kobe.warehouse.reports.ui.viewmodel.ActivityReportViewModel
import com.kobe.warehouse.reports.ui.viewmodel.ActivityReportViewModelFactory
import java.time.Instant
import java.time.ZoneId

/**
 * Activity Report Activity - displays Rapport d'Activité report.
 * Extends BaseActivity for session management and logout functionality.
 */
class ActivityReportActivity : BaseActivity() {

    private lateinit var binding: ActivityActivityReportBinding
    private lateinit var viewModel: ActivityReportViewModel
    private lateinit var recetteAdapter: RecetteAdapter
    private lateinit var mouvementAdapter: MouvementAdapter
    private lateinit var achatFournisseurAdapter: AchatFournisseurAdapter
    private lateinit var reglementTpAdapter: ReglementTpAdapter
    private lateinit var achatTpAdapter: AchatTpAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupPeriodSelector()
        setupRecyclerViews()
        setupExpandableCards()
        setupListeners()
        observeViewModel()

        viewModel.loadActivityReport()
    }

    private fun setupViewModel() {
        val repository = PharmaReportApplication.getRepository()
        val factory = ActivityReportViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ActivityReportViewModel::class.java]
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
                R.id.chipToday -> viewModel.setPeriod(ActivityReportViewModel.Period.TODAY)
                R.id.chipYesterday -> viewModel.setPeriod(ActivityReportViewModel.Period.YESTERDAY)
                R.id.chipThisWeek -> viewModel.setPeriod(ActivityReportViewModel.Period.THIS_WEEK)
                R.id.chipThisMonth -> viewModel.setPeriod(ActivityReportViewModel.Period.THIS_MONTH)
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
        // Recettes adapter
        recetteAdapter = RecetteAdapter()
        binding.rvRecettes.apply {
            layoutManager = LinearLayoutManager(this@ActivityReportActivity)
            adapter = recetteAdapter
            isNestedScrollingEnabled = false
        }

        // Mouvements adapter
        mouvementAdapter = MouvementAdapter()
        binding.rvMouvements.apply {
            layoutManager = LinearLayoutManager(this@ActivityReportActivity)
            adapter = mouvementAdapter
            isNestedScrollingEnabled = false
        }

        // Achats fournisseurs adapter
        achatFournisseurAdapter = AchatFournisseurAdapter()
        binding.rvAchats.apply {
            layoutManager = LinearLayoutManager(this@ActivityReportActivity)
            adapter = achatFournisseurAdapter
            isNestedScrollingEnabled = false
        }

        // Reglements tiers payants adapter
        reglementTpAdapter = ReglementTpAdapter()
        binding.rvReglements.apply {
            layoutManager = LinearLayoutManager(this@ActivityReportActivity)
            adapter = reglementTpAdapter
            isNestedScrollingEnabled = false
        }

        // Achats tiers payants adapter
        achatTpAdapter = AchatTpAdapter()
        binding.rvAchatsTp.apply {
            layoutManager = LinearLayoutManager(this@ActivityReportActivity)
            adapter = achatTpAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupExpandableCards() {
        // Chiffre d'affaires
        binding.headerChiffreAffaire.setOnClickListener {
            viewModel.toggleChiffreAffaire()
        }

        // Recettes
        binding.headerRecettes.setOnClickListener {
            viewModel.toggleRecettes()
        }

        // Mouvements
        binding.headerMouvements.setOnClickListener {
            viewModel.toggleMouvements()
        }

        // Achats
        binding.headerAchats.setOnClickListener {
            viewModel.toggleAchats()
        }

        // Tiers Payants
        binding.headerTiersPayants.setOnClickListener {
            viewModel.toggleTiersPayants()
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshActivityReport()
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

        viewModel.activityReport.observe(this) { report ->
            report?.let {
                showContent()
                updateUI(it)
            }
        }

        viewModel.isEmpty.observe(this) { isEmpty ->
            binding.emptyState.isVisible = isEmpty
        }

        // Expansion states
        viewModel.chiffreAffaireExpanded.observe(this) { expanded ->
            binding.contentChiffreAffaire.isVisible = expanded
            binding.iconCaExpand.setImageResource(
                if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }

        viewModel.recettesExpanded.observe(this) { expanded ->
            binding.rvRecettes.isVisible = expanded
            binding.iconRecettesExpand.setImageResource(
                if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }

        viewModel.mouvementsExpanded.observe(this) { expanded ->
            binding.contentMouvements.isVisible = expanded
            binding.iconMouvementsExpand.setImageResource(
                if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }

        viewModel.achatsExpanded.observe(this) { expanded ->
            binding.rvAchats.isVisible = expanded
            binding.iconAchatsExpand.setImageResource(
                if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
        }

        viewModel.tiersPayantsExpanded.observe(this) { expanded ->
            binding.contentTiersPayants.isVisible = expanded
            binding.iconTiersPayantsExpand.setImageResource(
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

    private fun updateUI(report: ActivityReport) {
        // Period label
        binding.tvPeriodLabel.text = report.periodLabel

        // Chiffre d'affaires
        val ca = report.chiffreAffaire
        binding.tvCaTotalHeader.text = ca.getFormattedMontantTtc()

        // Set CA detail rows
        setRowData(binding.rowMontantTtc.root, getString(R.string.montant_ttc), ca.getFormattedMontantTtc())
        setRowData(binding.rowMontantHt.root, getString(R.string.montant_ht), ca.getFormattedMontantHt())
        setRowData(binding.rowMontantTva.root, getString(R.string.montant_tva), ca.getFormattedMontantTva())
        setRowData(binding.rowRemise.root, getString(R.string.remise), ca.getFormattedMontantRemise())
        setRowData(binding.rowMontantNet.root, getString(R.string.montant_net), ca.getFormattedMontantNet())
        setRowData(binding.rowMarge.root, getString(R.string.marge), "${ca.getFormattedMarge()} (${ca.getFormattedMargePercent()})")

        // Recettes
        binding.tvRecettesTotalHeader.text = report.getFormattedTotalRecettes()
        recetteAdapter.submitList(report.recettes)

        // Mouvements
        binding.tvTotalEntrees.text = report.getFormattedTotalEntrees()
        binding.tvTotalSorties.text = report.getFormattedTotalSorties()
        mouvementAdapter.submitList(report.mouvementsCaisse)

        // Achats fournisseurs
        binding.tvAchatsTotalHeader.text = report.getFormattedTotalAchats()
        achatFournisseurAdapter.submitList(report.achatsFournisseurs)

        // Tiers payants
        val tp = report.tiersPayants
        binding.tvTotalFacture.text = tp.getFormattedTotalFacture()
        binding.tvTotalRegle.text = tp.getFormattedTotalRegle()
        binding.tvTotalRestant.text = tp.getFormattedTotalRestant()
        reglementTpAdapter.submitList(tp.reglements)

        binding.tvTotalBons.text = tp.totalBons.toString()
        binding.tvTotalMontantAchats.text = tp.getFormattedTotalMontantAchats()
        binding.tvTotalClients.text = tp.totalClients.toString()
        achatTpAdapter.submitList(tp.achats)
    }

    private fun setRowData(rowView: View, label: String, value: String) {
        rowView.findViewById<TextView>(R.id.tvLabel).text = label
        rowView.findViewById<TextView>(R.id.tvValue).text = value
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.refreshActivityReport()
            }
            .show()
    }

    companion object {
        private const val VIEW_SKELETON = 0
        private const val VIEW_CONTENT = 1
    }
}
