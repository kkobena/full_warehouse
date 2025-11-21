package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.sales.data.model.SaleLine
import com.kobe.warehouse.sales.databinding.ItemSaleLineDetailBinding
import java.text.NumberFormat
import java.util.Locale

class SaleLineDetailAdapter : RecyclerView.Adapter<SaleLineDetailAdapter.ViewHolder>() {

    private val items = mutableListOf<SaleLine>()

    fun submitList(newItems: List<SaleLine>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
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
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemSaleLineDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)

        fun bind(saleLine: SaleLine) {
            binding.tvProductName.text = saleLine.produitLibelle
            binding.tvTotalPrice.text = formatAmount(saleLine.salesAmount)
            binding.tvQuantityPrice.text = buildString {
                append(saleLine.quantitySold)
                append(" x ")
                append(formatAmount(saleLine.regularUnitPrice))
            }
        }

        private fun formatAmount(amount: Int): String {
            return "${numberFormat.format(amount)} FCFA"
        }
    }
}
