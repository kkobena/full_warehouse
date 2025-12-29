package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.TiersPayantCreancesSummary
import com.kobe.warehouse.reports.databinding.ItemTiersPayantSummaryBinding
import com.kobe.warehouse.reports.utils.NumberFormatUtils

/**
 * Adapter for displaying Tiers Payant créances summaries.
 */
class TiersPayantSummaryAdapter(
    private val onItemClick: (TiersPayantCreancesSummary) -> Unit
) : ListAdapter<TiersPayantCreancesSummary, TiersPayantSummaryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTiersPayantSummaryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTiersPayantSummaryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: TiersPayantCreancesSummary) {
            binding.apply {
                // Groupe name
                tvGroupeName.text = item.groupeTiersPayantLibelle

                // Totals
                tvTotalFactures.text = "${item.totalFactures} facture(s)"
                tvMontantRestant.text = NumberFormatUtils.formatCurrency(item.montantRestant)

                // Progress bar
                val progress = item.getPaymentProgress()
                progressPayment.progress = progress.toInt()
                tvProgressPercent.text = String.format("%.0f%%", progress)

                // Age category counts
                tvCountLess30.text = item.countLessThan30.toString()
                tvCount3060.text = item.countBetween30And60.toString()
                tvCount6090.text = item.countBetween60And90.toString()
                tvCountMore90.text = item.countMoreThan90.toString()
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<TiersPayantCreancesSummary>() {
        override fun areItemsTheSame(
            oldItem: TiersPayantCreancesSummary,
            newItem: TiersPayantCreancesSummary
        ): Boolean {
            return oldItem.groupeTiersPayantId == newItem.groupeTiersPayantId
        }

        override fun areContentsTheSame(
            oldItem: TiersPayantCreancesSummary,
            newItem: TiersPayantCreancesSummary
        ): Boolean {
            return oldItem == newItem
        }
    }
}
