package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.TiersPayantInvoice
import com.kobe.warehouse.reports.databinding.ItemTiersPayantInvoiceBinding
import com.kobe.warehouse.reports.utils.NumberFormatUtils

/**
 * Adapter for displaying Tiers Payant invoices.
 */
class TiersPayantInvoiceAdapter : ListAdapter<TiersPayantInvoice, TiersPayantInvoiceAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTiersPayantInvoiceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemTiersPayantInvoiceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TiersPayantInvoice) {
            binding.apply {
                // Invoice number and date
                tvNumeroFacture.text = item.numeroFacture
                tvDateFacture.text = item.dateFacture

                // Tiers payant info
                tvTiersPayant.text = item.tiersPayantLibelle
                tvGroupe.text = item.groupeTiersPayantLibelle

                // Amounts
                tvMontantFacture.text = NumberFormatUtils.formatCurrency(item.montantFacture)
                tvMontantRestant.text = NumberFormatUtils.formatCurrency(item.montantRestant)

                // Age category chip
                chipAgeCategory.text = item.getAgeCategoryLabel()
                chipAgeCategory.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    item.getAgeCategoryColor()
                )

                // Days since invoice
                tvDaysSince.text = "${item.daysSinceInvoice} jour(s)"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<TiersPayantInvoice>() {
        override fun areItemsTheSame(
            oldItem: TiersPayantInvoice,
            newItem: TiersPayantInvoice
        ): Boolean {
            return oldItem.factureId == newItem.factureId
        }

        override fun areContentsTheSame(
            oldItem: TiersPayantInvoice,
            newItem: TiersPayantInvoice
        ): Boolean {
            return oldItem == newItem
        }
    }
}
