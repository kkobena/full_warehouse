package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.ProductProfitability
import com.kobe.warehouse.reports.databinding.ItemProfitabilityBinding
import com.kobe.warehouse.reports.utils.NumberFormatUtils

class ProfitabilityAdapter : ListAdapter<ProductProfitability, ProfitabilityAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProfitabilityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemProfitabilityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductProfitability) {
            binding.apply {
                tvProductName.text = item.libelle
                tvCodeCip.text = item.codeCip ?: "-"

                chipBcg.text = item.getBcgCategoryLabel()
                chipBcg.chipBackgroundColor = android.content.res.ColorStateList.valueOf(item.getBcgColor())

                tvRevenue.text = NumberFormatUtils.formatCurrency(item.caTotal.toLong())
                tvCost.text = NumberFormatUtils.formatCurrency(item.coutAchatTotal.toLong())
                tvMargin.text = NumberFormatUtils.formatCurrency(item.margeBrute.toLong())
                tvMarginPercent.text = item.getMarginFormatted()
                tvMarginPercent.setTextColor(item.getMarginColor())

                tvSalesCount.text = "${item.nbVentes} ventes"
                tvQuantitySold.text = "${item.qteVendue} unités"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ProductProfitability>() {
        override fun areItemsTheSame(oldItem: ProductProfitability, newItem: ProductProfitability) = oldItem.produitId == newItem.produitId
        override fun areContentsTheSame(oldItem: ProductProfitability, newItem: ProductProfitability) = oldItem == newItem
    }
}
