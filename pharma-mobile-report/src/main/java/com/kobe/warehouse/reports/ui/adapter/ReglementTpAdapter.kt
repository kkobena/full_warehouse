package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.ReglementTiersPayant
import com.kobe.warehouse.reports.databinding.ItemReglementTpBinding

/**
 * Adapter for displaying third-party payer payments in the Activity Report.
 */
class ReglementTpAdapter : ListAdapter<ReglementTiersPayant, ReglementTpAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReglementTpBinding.inflate(
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
        private val binding: ItemReglementTpBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(reglement: ReglementTiersPayant) {
            binding.tvLibelle.text = reglement.libelle
            binding.tvCategorie.text = reglement.categorie

            if (!reglement.numFacture.isNullOrBlank()) {
                binding.tvNumFacture.text = "N° ${reglement.numFacture}"
                binding.tvNumFacture.isVisible = true
            } else {
                binding.tvNumFacture.isVisible = false
            }

            binding.tvMontantFacture.text = reglement.getFormattedMontantFacture()
            binding.tvMontantRegle.text = reglement.getFormattedMontantReglement()
            binding.tvMontantRestant.text = reglement.getFormattedMontantRestant()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ReglementTiersPayant>() {
        override fun areItemsTheSame(oldItem: ReglementTiersPayant, newItem: ReglementTiersPayant): Boolean {
            return oldItem.libelle == newItem.libelle && oldItem.numFacture == newItem.numFacture
        }

        override fun areContentsTheSame(oldItem: ReglementTiersPayant, newItem: ReglementTiersPayant): Boolean {
            return oldItem == newItem
        }
    }
}
