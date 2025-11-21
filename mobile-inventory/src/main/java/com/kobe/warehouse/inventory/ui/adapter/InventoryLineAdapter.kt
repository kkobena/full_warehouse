package com.kobe.warehouse.inventory.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import com.kobe.warehouse.inventory.databinding.ItemInventoryLineBinding

class InventoryLineAdapter(
    private val onLineClick: (StoreInventoryLine) -> Unit
) : ListAdapter<StoreInventoryLine, InventoryLineAdapter.InventoryLineViewHolder>(InventoryLineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryLineViewHolder {
        val binding = ItemInventoryLineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InventoryLineViewHolder(binding, onLineClick)
    }

    override fun onBindViewHolder(holder: InventoryLineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class InventoryLineViewHolder(
        private val binding: ItemInventoryLineBinding,
        private val onLineClick: (StoreInventoryLine) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(line: StoreInventoryLine) {
            binding.apply {
                tvProductName.text = line.produitLibelle
                tvProductCode.text = "Code: ${line.produitCip ?: "N/A"}"

                // Quantity info
                tvQuantityInit.text = "Init: ${line.quantityInit}"
                tvQuantityOnHand.text = "Compté: ${line.quantityOnHand}"

                // Calculate gap
                val gap = line.gap ?: (line.quantityOnHand - line.quantityInit)
                tvGap.text = "Écart: $gap"

                // Set gap color
                when {
                    gap > 0 -> tvGap.setTextColor(root.context.getColor(android.R.color.holo_green_dark))
                    gap < 0 -> tvGap.setTextColor(root.context.getColor(android.R.color.holo_red_dark))
                    else -> tvGap.setTextColor(root.context.getColor(android.R.color.darker_gray))
                }

                root.setOnClickListener {
                    onLineClick(line)
                }
            }
        }
    }

    class InventoryLineDiffCallback : DiffUtil.ItemCallback<StoreInventoryLine>() {
        override fun areItemsTheSame(oldItem: StoreInventoryLine, newItem: StoreInventoryLine): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StoreInventoryLine, newItem: StoreInventoryLine): Boolean {
            return oldItem == newItem
        }
    }
}
