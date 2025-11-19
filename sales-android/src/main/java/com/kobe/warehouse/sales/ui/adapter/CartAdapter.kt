package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.model.SaleLine

/**
 * Cart Adapter
 * RecyclerView adapter for displaying cart items
 */
class CartAdapter(
    private val onIncrementClick: (SaleLine) -> Unit,
    private val onDecrementClick: (SaleLine) -> Unit,
    private val onRemoveClick: (SaleLine) -> Unit
) : ListAdapter<SaleLine, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val saleLine = getItem(position)
        holder.bind(saleLine)
    }

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLibelle: TextView = itemView.findViewById(R.id.tvLibelle)
        private val tvCode: TextView = itemView.findViewById(R.id.tvCode)
        private val tvUnitPrice: TextView = itemView.findViewById(R.id.tvUnitPrice)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        private val btnIncrement: MaterialButton = itemView.findViewById(R.id.btnIncrement)
        private val btnDecrement: MaterialButton = itemView.findViewById(R.id.btnDecrement)
        private val btnRemove: MaterialButton = itemView.findViewById(R.id.btnRemove)
        private val tvStockWarning: TextView = itemView.findViewById(R.id.tvStockWarning)

        fun bind(saleLine: SaleLine) {
            tvLibelle.text = saleLine.produitLibelle
            tvCode.text = saleLine.code
            tvUnitPrice.text = saleLine.getFormattedUnitPrice()
            tvQuantity.text = saleLine.quantitySold.toString()
            tvTotal.text = saleLine.getFormattedTotal()

            // Show warning if insufficient stock
            if (saleLine.hasInsufficientStock()) {
                tvStockWarning.visibility = View.VISIBLE
                tvStockWarning.text = "Stock insuffisant (${saleLine.qtyStock} disponible)"
            } else {
                tvStockWarning.visibility = View.GONE
            }

            // Click listeners
            btnIncrement.setOnClickListener { onIncrementClick(saleLine) }
            btnDecrement.setOnClickListener { onDecrementClick(saleLine) }
            btnRemove.setOnClickListener { onRemoveClick(saleLine) }

            // Disable decrement if quantity is 1
            btnDecrement.isEnabled = saleLine.quantitySold > 1
            btnDecrement.alpha = if (saleLine.quantitySold > 1) 1.0f else 0.5f
        }
    }

    class CartDiffCallback : DiffUtil.ItemCallback<SaleLine>() {
        override fun areItemsTheSame(oldItem: SaleLine, newItem: SaleLine): Boolean {
            return oldItem.produitId == newItem.produitId
        }

        override fun areContentsTheSame(oldItem: SaleLine, newItem: SaleLine): Boolean {
            return oldItem == newItem
        }
    }

    /**
     * Get total amount of all items
     */
    fun getTotalAmount(): Int {
        return currentList.sumOf { it.salesAmount }
    }

    /**
     * Get total item count
     */
    fun getTotalItemCount(): Int {
        return currentList.size
    }

    /**
     * Get total quantity
     */
    fun getTotalQuantity(): Int {
        return currentList.sumOf { it.quantitySold }
    }
}
