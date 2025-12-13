package com.kobe.warehouse.reports.ui.customreport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.databinding.FragmentCustomReportBuilderBinding
import com.kobe.warehouse.reports.data.model.ReportMetric
import com.kobe.warehouse.reports.data.model.ReportPeriod
import com.kobe.warehouse.reports.data.model.ReportTemplate
import kotlinx.coroutines.launch

/**
 * Custom Report Builder Screen
 * Allows users to create personalized reports by selecting metrics and period
 */
class CustomReportBuilderScreen : Fragment() {

    private var _binding: FragmentCustomReportBuilderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CustomReportBuilderViewModel by viewModels()

    private lateinit var metricsAdapter: MetricsSelectionAdapter
    private lateinit var templatesAdapter: ReportTemplatesAdapter

    private val selectedMetrics = mutableSetOf<ReportMetric>()
    private var selectedPeriod = ReportPeriod.WEEK

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomReportBuilderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        // Setup metrics selection adapter
        metricsAdapter = MetricsSelectionAdapter { metric, isSelected ->
            if (isSelected) {
                selectedMetrics.add(metric)
            } else {
                selectedMetrics.remove(metric)
            }
            updateGenerateButtonState()
        }

        binding.recyclerMetrics.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = metricsAdapter
        }

        // Load all metrics
        metricsAdapter.submitList(ReportMetric.entries)

        // Setup period chips
        binding.chipGroupPeriod.removeAllViews()
        ReportPeriod.entries.forEach { period ->
            val chip = Chip(requireContext()).apply {
                text = period.displayName
                isCheckable = true
                isChecked = period == selectedPeriod
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedPeriod = period
                    }
                }
            }
            binding.chipGroupPeriod.addView(chip)
        }

        // Setup templates adapter
        templatesAdapter = ReportTemplatesAdapter { template ->
            applyTemplate(template)
        }

        binding.recyclerTemplates.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = templatesAdapter
        }

        // Load built-in templates
        templatesAdapter.submitList(ReportTemplate.BUILT_IN_TEMPLATES)

        // Generate button
        binding.buttonGenerate.setOnClickListener {
            generateReport()
        }

        // Save button
        binding.buttonSave.setOnClickListener {
            saveReportTemplate()
        }

        updateGenerateButtonState()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reportState.collect { state ->
                when (state) {
                    is CustomReportBuilderViewModel.ReportState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.buttonGenerate.isEnabled = false
                    }
                    is CustomReportBuilderViewModel.ReportState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.buttonGenerate.isEnabled = true
                        // Navigate to report view
                        navigateToReportView(state.reportId)
                    }
                    is CustomReportBuilderViewModel.ReportState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.buttonGenerate.isEnabled = true
                        showError(state.message)
                    }
                    is CustomReportBuilderViewModel.ReportState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.savedReports.collect { reports ->
                // Update saved reports list if needed
            }
        }
    }

    private fun applyTemplate(template: ReportTemplate) {
        // Clear current selection
        selectedMetrics.clear()

        // Add template metrics
        selectedMetrics.addAll(template.metrics)

        // Set period
        selectedPeriod = template.period

        // Update UI
        metricsAdapter.setSelectedMetrics(selectedMetrics)

        // Update period chips
        binding.chipGroupPeriod.children.forEach { view ->
            if (view is Chip) {
                view.isChecked = view.text.toString() == selectedPeriod.displayName
            }
        }

        updateGenerateButtonState()

        Snackbar.make(
            binding.root,
            "Modèle \"${template.name}\" appliqué",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun generateReport() {
        if (selectedMetrics.isEmpty()) {
            showError("Veuillez sélectionner au moins un indicateur")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.generateReport(
                name = "Rapport personnalisé",
                metrics = selectedMetrics.toList(),
                period = selectedPeriod
            )
        }
    }

    private fun saveReportTemplate() {
        if (selectedMetrics.isEmpty()) {
            showError("Veuillez sélectionner au moins un indicateur")
            return
        }

        // Show dialog to enter report name
        val input = android.widget.EditText(requireContext())
        input.hint = "Nom du rapport"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enregistrer le rapport")
            .setMessage("Donnez un nom à votre rapport personnalisé")
            .setView(input)
            .setPositiveButton("Enregistrer") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.saveReport(
                            name = name,
                            metrics = selectedMetrics.toList(),
                            period = selectedPeriod
                        )

                        Snackbar.make(
                            binding.root,
                            "Rapport \"$name\" enregistré",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    showError("Veuillez entrer un nom")
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun updateGenerateButtonState() {
        binding.buttonGenerate.isEnabled = selectedMetrics.isNotEmpty()
        binding.buttonSave.isEnabled = selectedMetrics.isNotEmpty()

        // Update selected count
        binding.textSelectedCount.text = "${selectedMetrics.size} indicateur(s) sélectionné(s)"
    }

    private fun navigateToReportView(reportId: Long) {
        // Navigate to report view screen
        findNavController().navigate(
            R.id.action_customReportBuilder_to_customReportView,
            Bundle().apply {
                putLong("reportId", reportId)
            }
        )
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Extension function to iterate over Chip Group children
private val ViewGroup.children: Sequence<View>
    get() = sequence {
        for (i in 0 until childCount) {
            yield(getChildAt(i))
        }
    }
