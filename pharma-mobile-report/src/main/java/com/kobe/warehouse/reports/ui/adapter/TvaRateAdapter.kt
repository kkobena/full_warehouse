package com.kobe.warehouse.reports.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.TvaChartData
import com.kobe.warehouse.reports.data.model.TvaRateBreakdown
import com.kobe.warehouse.reports.databinding.ItemTvaRateBinding

/**
 * Adapter for TVA rate breakdown items.
 */
class TvaRateAdapter(
    private val chartData: List<TvaChartData> = emptyList(),
    private val totalTtc: Long = 0
) : ListAdapter<TvaRateBreakdown, TvaRateAdapter.ViewHolder>(DiffCallback()) {

    private var chartDataMap: Map<Int, TvaChartData> = emptyMap()
    private var currentTotalTtc: Long = 0

    fun updateChartData(data: List<TvaChartData>, total: Long) {
        chartDataMap = data.associateBy { it.label.removeSuffix("%").toIntOrNull() ?: 0 }
        currentTotalTtc = total
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTvaRateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val chartItem = chartDataMap[item.codeTva]
        holder.bind(item, chartItem, currentTotalTtc)
    }

    class ViewHolder(
        private val binding: ItemTvaRateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TvaRateBreakdown, chartItem: TvaChartData?, totalTtc: Long) {
            binding.tvRateName.text = "TVA ${item.rateName}"
            binding.tvMontantHt.text = item.getFormattedMontantHt()
            binding.tvMontantTva.text = item.getFormattedMontantTva()
            binding.tvMontantTtc.text = item.getFormattedMontantTtc()

            // Percent from chart data or calculate
            val percent = chartItem?.percent ?: item.getPercent(totalTtc)
            binding.tvPercent.text = String.format("%.1f%%", percent)

            // Date visibility
            binding.tvDate.isVisible = item.date != null
            item.date?.let { binding.tvDate.text = it }

            // Color indicator
            try {
                val color = chartItem?.color?.let { Color.parseColor(it) }
                    ?: getDefaultColor(item.codeTva)
                val drawable = binding.colorIndicator.background as? GradientDrawable
                drawable?.setColor(color)
            } catch (e: Exception) {
                // Default color if parsing fails
            }
        }

        private fun getDefaultColor(codeTva: Int): Int {
            return when (codeTva) {
                0 -> Color.parseColor("#4CAF50")   // Green
                5 -> Color.parseColor("#2196F3")   // Blue
                10 -> Color.parseColor("#FF9800")  // Orange
                18 -> Color.parseColor("#9C27B0")  // Purple
                20 -> Color.parseColor("#F44336")  // Red
                else -> Color.parseColor("#607D8B") // Blue Grey
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TvaRateBreakdown>() {
        override fun areItemsTheSame(oldItem: TvaRateBreakdown, newItem: TvaRateBreakdown): Boolean {
            return oldItem.codeTva == newItem.codeTva && oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: TvaRateBreakdown, newItem: TvaRateBreakdown): Boolean {
            return oldItem == newItem
        }
    }
}
