package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.LotInfo
import com.kobe.warehouse.reports.databinding.ItemLotBinding

/**
 * Adapter for displaying lots in a RecyclerView.
 */
class LotAdapter : ListAdapter<LotInfo, LotAdapter.ViewHolder>(LotDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLotBinding.inflate(
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
        private val binding: ItemLotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(lot: LotInfo) {
            val context = binding.root.context

            // Lot number
            binding.tvLotNumber.text = lot.lotNumber?.let { "Lot: $it" } ?: "Lot non spécifié"

            // Expiry date
            binding.tvExpiryDate.text = lot.getExpiryText()

            // Quantity
            binding.tvLotQuantity.text = lot.quantity.toString()

            // Expiry status indicator color
            val statusColor = when (lot.expiryStatus) {
                "EXPIRED" -> R.color.error
                "CRITICAL" -> R.color.error
                "WARNING" -> R.color.warning
                else -> R.color.success
            }
            binding.viewExpiryStatus.backgroundTintList =
                ContextCompat.getColorStateList(context, statusColor)
        }
    }

    private class LotDiffCallback : DiffUtil.ItemCallback<LotInfo>() {
        override fun areItemsTheSame(oldItem: LotInfo, newItem: LotInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LotInfo, newItem: LotInfo): Boolean {
            return oldItem == newItem
        }
    }
}
