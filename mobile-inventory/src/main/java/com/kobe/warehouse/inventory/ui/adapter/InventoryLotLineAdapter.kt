package com.kobe.warehouse.inventory.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.inventory.R
import com.kobe.warehouse.inventory.data.model.InventoryLotLine
import com.kobe.warehouse.inventory.databinding.ItemInventoryLotLineBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Vue à plat « un lot = une ligne », mode de saisie quand la gestion des lots est
 * active. Même ergonomie que la liste par produit : saisie dans la ligne, touche
 * « Suivant » qui enchaîne, et le lot compté figure sur la ligne elle-même.
 */
class InventoryLotLineAdapter(
    private val onQuantityEntered: (InventoryLotLine, Int) -> Unit,
    private val onAddLot: (InventoryLotLine) -> Unit,
    private val onAdvance: (fromPosition: Int) -> Unit
) : ListAdapter<InventoryLotLine, InventoryLotLineAdapter.LotLineViewHolder>(DiffCallback()) {

    companion object {
        private const val PAYLOAD_SHOW_STOCK = "PAYLOAD_SHOW_STOCK"
    }

    /** Mode aveugle : stock théorique et écart masqués sans le privilège dédié */
    var showStock: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SHOW_STOCK)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LotLineViewHolder {
        val binding = ItemInventoryLotLineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LotLineViewHolder(binding, onQuantityEntered, onAddLot, onAdvance)
    }

    override fun onBindViewHolder(holder: LotLineViewHolder, position: Int) {
        holder.bind(getItem(position), showStock)
    }

    override fun onBindViewHolder(
        holder: LotLineViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads.all { it == PAYLOAD_SHOW_STOCK }) {
            holder.bindStockVisibility(getItem(position), showStock)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onViewRecycled(holder: LotLineViewHolder) {
        holder.releaseEditor()
        super.onViewRecycled(holder)
    }

    class LotLineViewHolder(
        private val binding: ItemInventoryLotLineBinding,
        private val onQuantityEntered: (InventoryLotLine, Int) -> Unit,
        private val onAddLot: (InventoryLotLine) -> Unit,
        private val onAdvance: (fromPosition: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var boundLine: InventoryLotLine? = null

        /** Voir InventoryLineAdapter : évite le double envoi à la perte de focus */
        private var committedQuantity: Int? = null

        init {
            binding.etQuantity.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                    commit()
                    onAdvance(bindingAdapterPosition)
                    true
                } else {
                    false
                }
            }
            binding.etQuantity.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) commit()
            }
            binding.btnAddLot.setOnClickListener {
                boundLine?.let(onAddLot)
            }
            binding.root.setOnClickListener {
                binding.etQuantity.requestFocus()
            }
        }

        fun bind(line: InventoryLotLine, showStock: Boolean) {
            boundLine = line
            committedQuantity = line.quantityOnHand
            binding.apply {
                tvProductName.text = line.produitLibelle
                tvProductCode.text = "Code: ${line.produitCip ?: "N/A"}"
                tvLot.text = lotLabel(line)
                setQuantityText(line.quantityOnHand?.toString().orEmpty())
                bindStockVisibility(line, showStock)
            }
        }

        /** « Lot X — exp. 31/12/2030 », la péremption étant omise si absente */
        private fun lotLabel(line: InventoryLotLine): String {
            val context = binding.root.context
            val numLot = line.numLot?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.lot_unnamed)
            val expiry = line.expiryDate?.let { formatExpiry(it) }
            return if (expiry != null) {
                context.getString(R.string.lot_with_expiry, numLot, expiry)
            } else {
                context.getString(R.string.lot_only, numLot)
            }
        }

        fun bindStockVisibility(line: InventoryLotLine, showStock: Boolean) {
            binding.apply {
                if (showStock) {
                    tvQuantityInit.visibility = View.VISIBLE
                    tvQuantityInit.text = "Init: ${line.quantityInit ?: 0}"
                } else {
                    tvQuantityInit.visibility = View.INVISIBLE
                }

                if (showStock && line.isCounted()) {
                    tvGap.visibility = View.VISIBLE
                    val gap = line.gap ?: line.calculateGap()
                    tvGap.text = "Écart: $gap"
                    when {
                        gap > 0 -> tvGap.setTextColor(root.context.getColor(android.R.color.holo_green_dark))
                        gap < 0 -> tvGap.setTextColor(root.context.getColor(android.R.color.holo_red_dark))
                        else -> tvGap.setTextColor(root.context.getColor(android.R.color.darker_gray))
                    }
                } else {
                    tvGap.visibility = View.INVISIBLE
                }
            }
        }

        fun releaseEditor() {
            binding.etQuantity.clearFocus()
            boundLine = null
        }

        private fun setQuantityText(value: String) {
            if (binding.etQuantity.text.toString() == value) return
            binding.etQuantity.setText(value)
        }

        private fun commit() {
            val line = boundLine ?: return
            val typed = binding.etQuantity.text.toString().trim()
            if (typed.isEmpty()) return
            val quantity = typed.toIntOrNull()
            if (quantity == null || quantity < 0) {
                setQuantityText(line.quantityOnHand?.toString().orEmpty())
                return
            }
            if (quantity == committedQuantity) return
            committedQuantity = quantity
            onQuantityEntered(line, quantity)
        }

        /** ISO yyyy-MM-dd → dd/MM/yyyy ; renvoie null si non parsable */
        private fun formatExpiry(iso: String): String? = try {
            LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } catch (e: DateTimeParseException) {
            null
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<InventoryLotLine>() {
        override fun areItemsTheSame(oldItem: InventoryLotLine, newItem: InventoryLotLine): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: InventoryLotLine, newItem: InventoryLotLine): Boolean =
            oldItem == newItem
    }
}
