package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.FournisseurAchat
import com.kobe.warehouse.reports.databinding.ItemSupplierPurchaseBinding

/**
 * Adapter for displaying supplier purchases in the Pharmacist Dashboard.
 */
class SupplierPurchaseAdapter(
    private val onItemClick: ((FournisseurAchat) -> Unit)? = null
) : ListAdapter<FournisseurAchat, SupplierPurchaseAdapter.SupplierViewHolder>(SupplierDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierViewHolder {
        val binding = ItemSupplierPurchaseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SupplierViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class SupplierViewHolder(
        private val binding: ItemSupplierPurchaseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(supplier: FournisseurAchat, rank: Int) {
            binding.apply {
                // Rank
                tvRank.text = rank.toString()

                // Set rank background color based on position
                val bgColor = when (rank) {
                    1 -> R.color.rank_gold
                    2 -> R.color.rank_silver
                    3 -> R.color.rank_bronze
                    else -> R.color.primary
                }
                tvRank.background.setTint(ContextCompat.getColor(root.context, bgColor))

                // Supplier name
                tvSupplierName.text = supplier.libelle

                // Amount
                tvAmount.text = supplier.getFormattedMontantNet()

                // Percentage
                tvPercent.text = supplier.getFormattedPercent()

                // Progress bar
                progressPercent.progress = supplier.percentTotal.toInt().coerceIn(0, 100)

                // Click listener
                root.setOnClickListener {
                    onItemClick?.invoke(supplier)
                }
            }
        }
    }

    class SupplierDiffCallback : DiffUtil.ItemCallback<FournisseurAchat>() {
        override fun areItemsTheSame(oldItem: FournisseurAchat, newItem: FournisseurAchat): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FournisseurAchat, newItem: FournisseurAchat): Boolean {
            return oldItem == newItem
        }
    }
}
