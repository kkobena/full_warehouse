package com.kobe.warehouse.inventory.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.inventory.R
import com.kobe.warehouse.inventory.data.model.StoreInventory
import com.kobe.warehouse.inventory.databinding.ItemInventoryBinding
import java.text.SimpleDateFormat
import java.util.*

class InventoryAdapter(
    private val onInventoryClick: (StoreInventory) -> Unit
) : ListAdapter<StoreInventory, InventoryAdapter.InventoryViewHolder>(InventoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val binding = ItemInventoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InventoryViewHolder(binding, onInventoryClick)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class InventoryViewHolder(
        private val binding: ItemInventoryBinding,
        private val onInventoryClick: (StoreInventory) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(inventory: StoreInventory) {
            binding.apply {
                tvInventoryName.text = inventory.name ?: "Sans nom"
                tvInventoryCategory.text = root.context.getString(
                    R.string.inventory_category,
                    inventory.inventoryCategory.name
                )

                // Format date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                tvLastUpdated.text = root.context.getString(
                    R.string.last_updated,
                    dateFormat.format(inventory.updatedAt)
                )

                // Set status color
                val statusColor = when (inventory.inventoryStatus) {
                    com.kobe.warehouse.inventory.data.model.InventoryStatut.OPEN ->
                        root.context.getColor(R.color.status_open)
                    com.kobe.warehouse.inventory.data.model.InventoryStatut.CLOSED ->
                        root.context.getColor(R.color.status_closed)
                }
                tvInventoryStatus.setTextColor(statusColor)
                tvInventoryStatus.text = inventory.inventoryStatus.name

                root.setOnClickListener {
                    onInventoryClick(inventory)
                }
            }
        }
    }

    class InventoryDiffCallback : DiffUtil.ItemCallback<StoreInventory>() {
        override fun areItemsTheSame(oldItem: StoreInventory, newItem: StoreInventory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StoreInventory, newItem: StoreInventory): Boolean {
            return oldItem == newItem
        }
    }
}
