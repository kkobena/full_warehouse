package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.model.Sale

/**
 * Sales Adapter
 * RecyclerView adapter for displaying ongoing sales list
 */
class SalesAdapter(
    private val onSaleClick: (Sale) -> Unit,
    private val onEditClick: (Sale) -> Unit,
    private val onDeleteClick: (Sale) -> Unit
) : ListAdapter<Sale, SalesAdapter.SaleViewHolder>(SaleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sale, parent, false)
        return SaleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        val sale = getItem(position)
        holder.bind(sale)
    }

    inner class SaleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.saleCard)
        private val tvTransactionNumber: TextView = itemView.findViewById(R.id.tvTransactionNumber)
        private val tvUpdatedDate: TextView = itemView.findViewById(R.id.tvUpdatedDate)
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvItemCount: TextView = itemView.findViewById(R.id.tvItemCount)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvNatureType: TextView = itemView.findViewById(R.id.tvNatureType)
        private val btnEdit: View = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: View = itemView.findViewById(R.id.btnDelete)

        fun bind(sale: Sale) {
            tvTransactionNumber.text = sale.numberTransaction
            tvUpdatedDate.text = sale.getFormattedUpdatedDate()
            tvCustomerName.text = sale.getCustomerName()
            tvItemCount.text = "${sale.getItemCount()} article(s)"
            tvTotalAmount.text = sale.getFormattedSalesAmount()
            tvNatureType.text = sale.getNatureLabel()

            // Click listeners
            cardView.setOnClickListener { onSaleClick(sale) }
            btnEdit.setOnClickListener { onEditClick(sale) }
            btnDelete.setOnClickListener { onDeleteClick(sale) }
        }
    }

    class SaleDiffCallback : DiffUtil.ItemCallback<Sale>() {
        override fun areItemsTheSame(oldItem: Sale, newItem: Sale): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Sale, newItem: Sale): Boolean {
            return oldItem == newItem
        }
    }
}
