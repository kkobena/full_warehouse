package com.kobe.warehouse.sales.ui.adapter

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.model.SaleLine

/**
 * Cart Adapter
 * RecyclerView adapter for displaying cart items
 */
class CartAdapter(
    private val onIncrementClick: (SaleLine) -> Unit,
    private val onDecrementClick: (SaleLine) -> Unit,
    private val onRemoveClick: (SaleLine) -> Unit,
    private val onQuantityChange: (SaleLine, Int) -> Unit = { _, _ -> },
    private val isViewMode: Boolean = false  // Read-only mode
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
            tvLibelle.text = saleLine.produitLibelle ?: ""
            tvCode.text = saleLine.code ?: ""
            tvUnitPrice.text = saleLine.getFormattedUnitPrice()
            tvQuantity.text = saleLine.quantitySold.toString()
            tvTotal.text = saleLine.getFormattedTotal()

            if (isViewMode) {
                // View-only mode: hide all controls
                btnIncrement.visibility = View.GONE
                btnDecrement.visibility = View.GONE
                btnRemove.visibility = View.GONE
                tvStockWarning.visibility = View.GONE
            } else {
                // Edit mode: show controls
                btnIncrement.visibility = View.VISIBLE
                btnDecrement.visibility = View.VISIBLE
                btnRemove.visibility = View.VISIBLE

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

                // Make quantity editable
                tvQuantity.setOnClickListener {
                    showQuantityEditDialog(saleLine)
                }

                // Disable decrement if quantity is 1
                btnDecrement.isEnabled = saleLine.quantitySold > 1
                btnDecrement.alpha = if (saleLine.quantitySold > 1) 1.0f else 0.5f
            }
        }

        /**
         * Show dialog to edit quantity
         */
        private fun showQuantityEditDialog(saleLine: SaleLine) {
            val context = itemView.context
            val input = EditText(context)
            input.inputType = InputType.TYPE_CLASS_NUMBER
            input.setText(saleLine.quantitySold.toString())
            input.selectAll()

            MaterialAlertDialogBuilder(context)
                .setTitle("Modifier la quantité")
                .setMessage(saleLine.produitLibelle ?: "")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val newQuantity = input.text.toString().toIntOrNull()
                    if (newQuantity != null && newQuantity > 0) {
                        onQuantityChange(saleLine, newQuantity)
                    }
                }
                .setNegativeButton("Annuler", null)
                .show()
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
