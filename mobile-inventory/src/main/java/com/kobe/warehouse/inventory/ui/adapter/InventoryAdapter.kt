package com.kobe.warehouse.inventory.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.inventory.R
import com.kobe.warehouse.inventory.data.model.InventoryStatut
import com.kobe.warehouse.inventory.data.model.StoreInventory
import com.kobe.warehouse.inventory.databinding.ItemInventoryBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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
                tvInventoryName.text = inventory.getDisplayName()
                tvInventoryCategory.text = root.context.getString(
                    R.string.inventory_category,
                    inventory.getCategoryDisplay()
                )

                // Format date (backend sends ISO LocalDateTime strings)
                val formattedDate = inventory.updatedAt?.let {
                    try {
                        LocalDateTime.parse(it)
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    } catch (_: DateTimeParseException) {
                        it
                    }
                } ?: ""
                tvLastUpdated.text = root.context.getString(
                    R.string.last_updated,
                    formattedDate
                )

                // Set status color
                val statusColor = when (inventory.statut) {
                    InventoryStatut.CREATE, InventoryStatut.PROCESSING ->
                        root.context.getColor(R.color.status_open)
                    InventoryStatut.CLOSED ->
                        root.context.getColor(R.color.status_closed)
                }
                tvInventoryStatus.setTextColor(statusColor)
                tvInventoryStatus.text = inventory.statut.displayLabel()

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
