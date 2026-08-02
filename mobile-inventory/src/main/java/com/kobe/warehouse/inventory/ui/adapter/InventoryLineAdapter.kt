package com.kobe.warehouse.inventory.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.inventory.data.model.StoreInventoryLine
import com.kobe.warehouse.inventory.R
import com.kobe.warehouse.inventory.databinding.ItemInventoryLineBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class InventoryLineAdapter(
    private val onLineClick: (StoreInventoryLine) -> Unit
) : ListAdapter<StoreInventoryLine, InventoryLineAdapter.InventoryLineViewHolder>(InventoryLineDiffCallback()) {

    companion object {
        /** Seule la visibilité stock/écart change : voir [showStock] */
        private const val PAYLOAD_SHOW_STOCK = "PAYLOAD_SHOW_STOCK"
    }

    /**
     * Mode aveugle : sans le privilège pr-voir-stock-inventaire, le stock
     * théorique (Init) et l'écart sont masqués pendant le comptage.
     *
     * Le basculement ne modifie pas les données, seulement leur présentation : on
     * notifie un changement partiel porteur d'une charge utile, qui ne réattache aucune
     * vue et ne rebinde que les deux champs concernés. Un `notifyDataSetChanged()`
     * invaliderait toute la liste, perdrait la position de défilement et interdirait
     * les animations d'élément.
     */
    var showStock: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SHOW_STOCK)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryLineViewHolder {
        val binding = ItemInventoryLineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InventoryLineViewHolder(binding, onLineClick)
    }

    override fun onBindViewHolder(holder: InventoryLineViewHolder, position: Int) {
        holder.bind(getItem(position), showStock)
    }

    override fun onBindViewHolder(
        holder: InventoryLineViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        // Rebind complet dès qu'une charge utile inconnue est présente : on ne peut
        // pas savoir ce qu'elle recouvre
        if (payloads.isNotEmpty() && payloads.all { it == PAYLOAD_SHOW_STOCK }) {
            holder.bindStockVisibility(getItem(position), showStock)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class InventoryLineViewHolder(
        private val binding: ItemInventoryLineBinding,
        private val onLineClick: (StoreInventoryLine) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(line: StoreInventoryLine, showStock: Boolean) {
            binding.apply {
                tvProductName.text = line.produitLibelle
                tvProductCode.text = "Code: ${line.produitCip ?: "N/A"}"

                // Quantity info
                tvQuantityOnHand.text = "Compté: ${line.quantityOnHand ?: "-"}"

                bindStockVisibility(line, showStock)

                // Traçabilité : qui a compté cette ligne (et quand)
                if (line.countedBy.isNullOrBlank()) {
                    tvCountedBy.visibility = android.view.View.GONE
                } else {
                    tvCountedBy.visibility = android.view.View.VISIBLE
                    val date = line.updatedAt?.let { formatDate(it) }
                    tvCountedBy.text = if (date != null) {
                        root.context.getString(R.string.counted_by_at, line.countedBy, date)
                    } else {
                        root.context.getString(R.string.counted_by, line.countedBy)
                    }
                }

                root.setOnClickListener {
                    onLineClick(line)
                }
            }
        }

        /**
         * Stock théorique et écart : seul fragment de la ligne dépendant du mode
         * aveugle, donc seul rebindé lors d'un changement de privilège
         */
        fun bindStockVisibility(line: StoreInventoryLine, showStock: Boolean) {
            binding.apply {
                if (showStock) {
                    tvQuantityInit.visibility = android.view.View.VISIBLE
                    tvGap.visibility = android.view.View.VISIBLE
                    tvQuantityInit.text = "Init: ${line.quantityInit}"

                    val gap = line.gap ?: line.calculateGap()
                    tvGap.text = "Écart: $gap"
                    when {
                        gap > 0 -> tvGap.setTextColor(root.context.getColor(android.R.color.holo_green_dark))
                        gap < 0 -> tvGap.setTextColor(root.context.getColor(android.R.color.holo_red_dark))
                        else -> tvGap.setTextColor(root.context.getColor(android.R.color.darker_gray))
                    }
                } else {
                    tvQuantityInit.visibility = android.view.View.INVISIBLE
                    tvGap.visibility = android.view.View.INVISIBLE
                }
            }
        }

        /** ISO LocalDateTime → dd/MM HH:mm ; renvoie null si non parsable */
        private fun formatDate(iso: String): String? = try {
            LocalDateTime.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
        } catch (e: DateTimeParseException) {
            null
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
