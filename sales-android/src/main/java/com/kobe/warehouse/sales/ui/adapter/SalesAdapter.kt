package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SalesStatut

/**
 * Sales Adapter
 * RecyclerView adapter for displaying sales list
 *
 * Features:
 * - Click to open sale
 * - Optional delete callback
 */
class SalesAdapter(
    private val onSaleClick: (Sale) -> Unit,
    private val onSaleDelete: ((Sale) -> Unit)? = null
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
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvSaleType: TextView = itemView.findViewById(R.id.tvSaleType)

        fun bind(sale: Sale) {
            tvTransactionNumber.text = sale.numberTransaction
            tvUpdatedDate.text = sale.getFormattedUpdatedDate()
            if (sale.customer != null) {
                tvCustomerName.text = sale.customer.getDisplayName()
                tvCustomerName.visibility = View.VISIBLE
            } else {
                tvCustomerName.visibility = View.GONE
            }
            tvTotalAmount.text = sale.getFormattedSalesAmount()

            // Display sale type badge
            bindSaleTypeBadge(sale.natureVente)

            // Display status badge
            bindStatusBadge(sale.statut)

            // Click to view sale details
            cardView.setOnClickListener { onSaleClick(sale) }

            // Long click to delete (if callback provided)
            if (onSaleDelete != null) {
                cardView.setOnLongClickListener {
                    onSaleDelete.invoke(sale)
                    true
                }
            }
        }

        private fun bindSaleTypeBadge(natureVente: String) {
            val context = itemView.context
            tvSaleType.setBackgroundResource(R.drawable.bg_badge_sale_type)
            when (natureVente) {
                "ASSURANCE" -> {
                    tvSaleType.text = "Assurance"
                    tvSaleType.background.setTint(ContextCompat.getColor(context, R.color.badge_assurance_bg))
                    tvSaleType.setTextColor(ContextCompat.getColor(context, R.color.badge_assurance_text))
                }
                "CARNET" -> {
                    tvSaleType.text = "Carnet"
                    tvSaleType.background.setTint(ContextCompat.getColor(context, R.color.badge_carnet_bg))
                    tvSaleType.setTextColor(ContextCompat.getColor(context, R.color.badge_carnet_text))
                }
                else -> {
                    tvSaleType.text = "Comptant"
                    tvSaleType.background.setTint(ContextCompat.getColor(context, R.color.badge_comptant_bg))
                    tvSaleType.setTextColor(ContextCompat.getColor(context, R.color.badge_comptant_text))
                }
            }
        }

        private fun bindStatusBadge(statut: SalesStatut) {
            when (statut) {
                SalesStatut.PENDING -> {
                    tvStatus.visibility = View.VISIBLE
                    tvStatus.text = itemView.context.getString(R.string.statut_pending)
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_success)
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.success))
                }
                SalesStatut.PROCESSING -> {
                    tvStatus.visibility = View.VISIBLE
                    tvStatus.text = itemView.context.getString(R.string.statut_processing)
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_warning)
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.warning))
                }
                else -> {
                    tvStatus.visibility = View.GONE
                }
            }
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
