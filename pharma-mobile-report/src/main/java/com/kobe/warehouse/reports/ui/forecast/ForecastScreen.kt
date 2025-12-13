package com.kobe.warehouse.reports.ui.forecast

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.databinding.FragmentForecastBinding
import com.kobe.warehouse.reports.data.model.ForecastData
import kotlinx.coroutines.launch

/**
 * Forecast Screen - ML-based sales predictions
 * Shows 7-day forecast with recommendations
 */
class ForecastScreen : Fragment() {

    private var _binding: FragmentForecastBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForecastViewModel by viewModels()
    private lateinit var recommendationsAdapter: RecommendationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForecastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        loadForecast()
    }

    private fun setupUI() {
        // Setup recommendations RecyclerView
        recommendationsAdapter = RecommendationsAdapter()
        binding.recyclerRecommendations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recommendationsAdapter
        }

        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadForecast(forceRefresh = true)
        }

        // Setup chart
        setupChart()
    }

    private fun setupChart() {
        binding.chartForecast.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            legend.isEnabled = true

            // X-axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelRotationAngle = -45f
            }

            // Y-axis (left)
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                axisMinimum = 0f
            }

            // Y-axis (right) - disable
            axisRight.isEnabled = false
        }
    }

    private fun setupObservers() {
        // Observe forecast data
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.forecastState.collect { state ->
                when (state) {
                    is ForecastViewModel.ForecastState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.layoutContent.visibility = View.GONE
                        binding.layoutError.visibility = View.GONE
                    }
                    is ForecastViewModel.ForecastState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.layoutContent.visibility = View.VISIBLE
                        binding.layoutError.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        displayForecast(state.data)
                    }
                    is ForecastViewModel.ForecastState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.layoutContent.visibility = View.GONE
                        binding.layoutError.visibility = View.VISIBLE
                        binding.swipeRefresh.isRefreshing = false
                        binding.textError.text = state.message
                    }
                }
            }
        }
    }

    private fun loadForecast(forceRefresh: Boolean = false) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadForecast(forceRefresh)
        }
    }

    private fun displayForecast(forecast: ForecastData) {
        // Display summary stats
        val totalPredicted = forecast.getTotalPredictedSales()
        val avgDaily = forecast.getAveragePredictedDaily()
        val growthRate = forecast.getGrowthRate()

        binding.textTotalPredicted.text = formatAmount(totalPredicted)
        binding.textAvgDaily.text = formatAmount(avgDaily)
        binding.textGrowthRate.text = String.format("%.1f%%", growthRate)

        // Set growth rate color
        binding.textGrowthRate.setTextColor(
            if (growthRate >= 0) Color.GREEN else Color.RED
        )

        // Confidence
        val confidencePercent = (forecast.confidence * 100).toInt()
        binding.textConfidence.text = "$confidencePercent%"
        binding.progressConfidence.progress = confidencePercent

        // Update chart
        updateChart(forecast)

        // Update recommendations
        recommendationsAdapter.submitList(forecast.recommendations)

        // Show message if needed
        if (forecast.recommendations.isEmpty()) {
            Snackbar.make(
                binding.root,
                "Aucune recommandation pour le moment",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateChart(forecast: ForecastData) {
        val allDates = mutableListOf<String>()
        val historicalEntries = mutableListOf<Entry>()
        val predictedEntries = mutableListOf<Entry>()

        // Historical data (last 7 days)
        val recentHistory = forecast.historical.takeLast(7)
        recentHistory.forEachIndexed { index, dailySales ->
            allDates.add(formatDateShort(dailySales.date))
            historicalEntries.add(Entry(index.toFloat(), dailySales.amount.toFloat()))
        }

        // Predicted data (next 7 days)
        val offset = recentHistory.size
        forecast.predicted.forEachIndexed { index, dailySales ->
            allDates.add(dailySales.label ?: formatDateShort(dailySales.date))
            predictedEntries.add(Entry((offset + index).toFloat(), dailySales.amount.toFloat()))
        }

        // Create datasets
        val historicalDataSet = LineDataSet(historicalEntries, "Historique").apply {
            color = Color.GRAY
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(Color.GRAY)
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val predictedDataSet = LineDataSet(predictedEntries, "Prévisions").apply {
            color = Color.BLUE
            lineWidth = 2.5f
            setDrawCircles(true)
            setCircleColor(Color.BLUE)
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            enableDashedLine(10f, 5f, 0f) // Dashed line for predictions
        }

        // Combine and set data
        val lineData = LineData(historicalDataSet, predictedDataSet)
        binding.chartForecast.apply {
            data = lineData
            xAxis.valueFormatter = IndexAxisValueFormatter(allDates)
            xAxis.labelCount = allDates.size
            animateX(500)
            invalidate()
        }
    }

    private fun formatAmount(amount: Double): String {
        return String.format("%,.0f FCFA", amount)
    }

    private fun formatDateShort(isoDate: String): String {
        return try {
            val parts = isoDate.split("-")
            "${parts[2]}/${parts[1]}"
        } catch (e: Exception) {
            isoDate
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
