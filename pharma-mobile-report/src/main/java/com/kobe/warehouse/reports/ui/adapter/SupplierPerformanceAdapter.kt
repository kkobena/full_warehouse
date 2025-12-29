package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.SupplierPerformance
import com.kobe.warehouse.reports.databinding.ItemSupplierPerformanceBinding
import com.kobe.warehouse.reports.utils.NumberFormatUtils

/**
 * Adapter for displaying Supplier Performance items.
 */
class SupplierPerformanceAdapter : ListAdapter<SupplierPerformance, SupplierPerformanceAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSupplierPerformanceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    class ViewHolder(
        private val binding: ItemSupplierPerformanceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupplierPerformance, rank: Int) {
            binding.apply {
                // Rank
                tvRank.text = rank.toString()
                tvRank.background.setTint(item.getScoreColor())

                // Supplier name
                tvSupplierName.text = item.fournisseurName

                // Performance score
                val score = item.performanceScore?.toFloat() ?: 0f
                tvScore.text = String.format("%.0f", score)
                progressScore.progress = score.toInt()
                progressScore.setIndicatorColor(item.getScoreColor())

                // Performance label
                chipPerformance.text = getPerformanceLabel(item.getPerformanceLevel())
                chipPerformance.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    item.getScoreColor()
                )

                // Delivery time
                tvDeliveryTime.text = "${item.avgDeliveryDays ?: 0} jours"

                // Conformity rate
                val conformity = item.conformityRatePct?.toFloat() ?: 0f
                tvConformityRate.text = String.format("%.1f%%", conformity)

                // Purchase volumes
                tvPurchases30d.text = NumberFormatUtils.formatCurrency(item.purchaseAmountLast30Days)
                tvPurchases12m.text = NumberFormatUtils.formatCurrency(item.purchaseAmountLast12Months)

                // Orders count
                tvOrdersCount.text = "${item.nbOrdersLast12Months}"
            }
        }

        private fun getPerformanceLabel(level: SupplierPerformance.PerformanceLevel): String {
            return when (level) {
                SupplierPerformance.PerformanceLevel.EXCELLENT -> "Excellent"
                SupplierPerformance.PerformanceLevel.GOOD -> "Bon"
                SupplierPerformance.PerformanceLevel.AVERAGE -> "Moyen"
                SupplierPerformance.PerformanceLevel.POOR -> "Faible"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SupplierPerformance>() {
        override fun areItemsTheSame(
            oldItem: SupplierPerformance,
            newItem: SupplierPerformance
        ): Boolean {
            return oldItem.fournisseurId == newItem.fournisseurId
        }

        override fun areContentsTheSame(
            oldItem: SupplierPerformance,
            newItem: SupplierPerformance
        ): Boolean {
            return oldItem == newItem
        }
    }
}
