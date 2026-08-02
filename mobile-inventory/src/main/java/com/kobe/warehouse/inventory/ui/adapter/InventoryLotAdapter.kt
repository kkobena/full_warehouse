package com.kobe.warehouse.inventory.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.inventory.R
import com.kobe.warehouse.inventory.data.model.InventoryLot
import com.kobe.warehouse.inventory.databinding.ItemInventoryLotBinding

class InventoryLotAdapter(
    private val onLotClick: (InventoryLot) -> Unit,
    private val onLotDelete: (InventoryLot) -> Unit
) : ListAdapter<InventoryLot, InventoryLotAdapter.InventoryLotViewHolder>(InventoryLotDiffCallback()) {

    companion object {
        private const val PAYLOAD_SHOW_STOCK = "PAYLOAD_SHOW_STOCK"
    }

    /**
     * Mode aveugle : stock théorique et écart masqués sans le privilège dédié.
     * Le basculement ne rebinde que ces deux champs (voir `InventoryLineAdapter`).
     */
    var showStock: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SHOW_STOCK)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryLotViewHolder {
        val binding = ItemInventoryLotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InventoryLotViewHolder(binding, onLotClick, onLotDelete)
    }

    override fun onBindViewHolder(holder: InventoryLotViewHolder, position: Int) {
        holder.bind(getItem(position), showStock)
    }

    override fun onBindViewHolder(
        holder: InventoryLotViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads.all { it == PAYLOAD_SHOW_STOCK }) {
            holder.bindStockVisibility(getItem(position), showStock)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class InventoryLotViewHolder(
        private val binding: ItemInventoryLotBinding,
        private val onLotClick: (InventoryLot) -> Unit,
        private val onLotDelete: (InventoryLot) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(lot: InventoryLot, showStock: Boolean) {
            binding.apply {
                tvLotNumber.text = lot.numLot ?: "Lot ${lot.id ?: ""}"
                tvLotExpiry.text = root.context.getString(
                    R.string.lot_expiry_display,
                    lot.expiryDate ?: "-"
                )

                tvLotQuantityOnHand.text = "Compté: ${lot.quantityOnHand ?: "-"}"

                bindStockVisibility(lot, showStock)

                root.setOnClickListener { onLotClick(lot) }
                btnDeleteLot.setOnClickListener { onLotDelete(lot) }
            }
        }

        /** Fragment dépendant du mode aveugle, rebindé seul via la charge utile */
        fun bindStockVisibility(lot: InventoryLot, showStock: Boolean) {
            binding.apply {
                if (showStock) {
                    tvLotQuantityInit.visibility = android.view.View.VISIBLE
                    tvLotGap.visibility = android.view.View.VISIBLE
                    tvLotQuantityInit.text = "Init: ${lot.quantityInit ?: 0}"

                    val gap = lot.gap ?: lot.calculateGap()
                    tvLotGap.text = "Écart: $gap"
                    when {
                        gap > 0 -> tvLotGap.setTextColor(root.context.getColor(android.R.color.holo_green_dark))
                        gap < 0 -> tvLotGap.setTextColor(root.context.getColor(android.R.color.holo_red_dark))
                        else -> tvLotGap.setTextColor(root.context.getColor(android.R.color.darker_gray))
                    }
                } else {
                    tvLotQuantityInit.visibility = android.view.View.INVISIBLE
                    tvLotGap.visibility = android.view.View.INVISIBLE
                }
            }
        }
    }

    class InventoryLotDiffCallback : DiffUtil.ItemCallback<InventoryLot>() {
        override fun areItemsTheSame(oldItem: InventoryLot, newItem: InventoryLot): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: InventoryLot, newItem: InventoryLot): Boolean {
            return oldItem == newItem
        }
    }
}
