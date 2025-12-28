package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.CashierRecap
import com.kobe.warehouse.reports.data.model.SummaryItem
import com.kobe.warehouse.reports.databinding.ItemCashierRecapBinding
import com.kobe.warehouse.reports.databinding.ItemSummaryLineBinding

/**
 * Adapter for displaying cashier recaps in the Cash Summary screen.
 */
class CashierRecapAdapter : ListAdapter<CashierRecap, CashierRecapAdapter.CashierRecapViewHolder>(
    CashierRecapDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CashierRecapViewHolder {
        val binding = ItemCashierRecapBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CashierRecapViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CashierRecapViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class CashierRecapViewHolder(
        private val binding: ItemCashierRecapBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val detailsAdapter = SummaryItemAdapter()
        private val summaryAdapter = SummaryItemAdapter()

        init {
            binding.rvDetails.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = detailsAdapter
                isNestedScrollingEnabled = false
            }

            binding.rvSummary.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = summaryAdapter
                isNestedScrollingEnabled = false
            }
        }

        fun bind(recap: CashierRecap, position: Int) {
            // User initials in circle badge
            binding.tvUserInitials.text = recap.userInitials

            // User name
            binding.tvUserName.text = recap.userName

            // Total amount
            binding.tvTotal.text = recap.getFormattedTotal()

            // Set initials background color based on position
            val colors = listOf(
                R.color.primary,
                R.color.secondary,
                R.color.info,
                R.color.success,
                R.color.warning
            )
            val colorRes = colors[position % colors.size]
            binding.tvUserInitials.background.setTint(
                ContextCompat.getColor(binding.root.context, colorRes)
            )

            // Details
            detailsAdapter.submitList(recap.details)

            // Summary (mobile totals etc.)
            if (recap.summary.isNotEmpty()) {
                binding.rvSummary.visibility = android.view.View.VISIBLE
                summaryAdapter.submitList(recap.summary)
            } else {
                binding.rvSummary.visibility = android.view.View.GONE
            }
        }
    }

    class CashierRecapDiffCallback : DiffUtil.ItemCallback<CashierRecap>() {
        override fun areItemsTheSame(oldItem: CashierRecap, newItem: CashierRecap): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: CashierRecap, newItem: CashierRecap): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * Adapter for displaying summary line items.
 */
class SummaryItemAdapter : ListAdapter<SummaryItem, SummaryItemAdapter.SummaryItemViewHolder>(
    SummaryItemDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryItemViewHolder {
        val binding = ItemSummaryLineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SummaryItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SummaryItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SummaryItemViewHolder(
        private val binding: ItemSummaryLineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SummaryItem) {
            binding.tvLibelle.text = item.libelle
            binding.tvValue.text = item.getFormattedValue()

            // Highlight totals and special items
            val isTotal = item.libelle.contains("Total", ignoreCase = true)
            val isMobile = item.libelle.contains("Mobile", ignoreCase = true)

            if (isTotal || isMobile) {
                binding.tvLibelle.setTypeface(null, android.graphics.Typeface.BOLD)
                binding.tvValue.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                binding.tvLibelle.setTypeface(null, android.graphics.Typeface.NORMAL)
                binding.tvValue.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }

    class SummaryItemDiffCallback : DiffUtil.ItemCallback<SummaryItem>() {
        override fun areItemsTheSame(oldItem: SummaryItem, newItem: SummaryItem): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: SummaryItem, newItem: SummaryItem): Boolean {
            return oldItem == newItem
        }
    }
}
