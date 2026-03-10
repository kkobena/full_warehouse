package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.MargeDTO
import com.kobe.warehouse.reports.databinding.ItemProfitabilityBinding
import com.kobe.warehouse.reports.utils.NumberFormatUtils

class ProfitabilityAdapter : ListAdapter<MargeDTO, ProfitabilityAdapter.ViewHolder>(DiffCallback()) {

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
        fun bind(item: MargeDTO) {
            binding.apply {
                tvProductName.text  = item.libelle
                tvCodeCip.text      = item.codeCip ?: "-"
                tvRevenue.text      = NumberFormatUtils.formatCurrency(item.caTotal)
                tvCost.text         = NumberFormatUtils.formatCurrency(item.coutAchatTotal)
                tvMargin.text       = "Marge : ${NumberFormatUtils.formatCurrency(item.margeBrute)}"
                tvMarginPercent.text  = item.getMarginFormatted()
                tvMarginPercent.setTextColor(item.getMarginColor())
                tvSalesCount.text   = "${item.nbVentes} ventes"
                tvQuantitySold.text = "${item.qteVendue} unités"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MargeDTO>() {
        override fun areItemsTheSame(old: MargeDTO, new: MargeDTO) = old.produitId == new.produitId
        override fun areContentsTheSame(old: MargeDTO, new: MargeDTO) = old == new
    }
}
