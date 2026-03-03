package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.sales.data.model.SaleLine
import com.kobe.warehouse.sales.databinding.ItemSaleLineDetailBinding
import java.text.NumberFormat
import java.util.Locale

class SaleLineDetailAdapter : ListAdapter<SaleLine, SaleLineDetailAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SaleLine>() {
            override fun areItemsTheSame(oldItem: SaleLine, newItem: SaleLine): Boolean {
                return oldItem.produitId == newItem.produitId
            }

            override fun areContentsTheSame(oldItem: SaleLine, newItem: SaleLine): Boolean {
                return oldItem.quantityRequested == newItem.quantityRequested
                        && oldItem.regularUnitPrice == newItem.regularUnitPrice
                        && oldItem.salesAmount == newItem.salesAmount
                        && oldItem.produitLibelle == newItem.produitLibelle
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSaleLineDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemSaleLineDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)

        fun bind(saleLine: SaleLine) {
            binding.tvProductName.text = saleLine.produitLibelle
            binding.tvTotalPrice.text = formatAmount(saleLine.salesAmount)
            binding.tvQuantityPrice.text = buildString {
                append(saleLine.quantityRequested)
                append(" x ")
                append(formatAmount(saleLine.regularUnitPrice))
            }
        }

        private fun formatAmount(amount: Int): String {
            return "${numberFormat.format(amount)} FCFA"
        }
    }
}
