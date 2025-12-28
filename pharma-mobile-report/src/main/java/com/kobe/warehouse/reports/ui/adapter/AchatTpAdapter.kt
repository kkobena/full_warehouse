package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.AchatTiersPayant
import com.kobe.warehouse.reports.databinding.ItemAchatTpBinding

/**
 * Adapter for displaying third-party payer purchases in the Activity Report.
 */
class AchatTpAdapter : ListAdapter<AchatTiersPayant, AchatTpAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAchatTpBinding.inflate(
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
        private val binding: ItemAchatTpBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(achat: AchatTiersPayant) {
            binding.tvLibelle.text = achat.libelle
            binding.tvCategorie.text = achat.categorie
            binding.tvBonsCount.text = achat.bonsCount.toString()
            binding.tvMontant.text = achat.getFormattedMontant()
            binding.tvClientCount.text = achat.clientCount.toString()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AchatTiersPayant>() {
        override fun areItemsTheSame(oldItem: AchatTiersPayant, newItem: AchatTiersPayant): Boolean {
            return oldItem.libelle == newItem.libelle
        }

        override fun areContentsTheSame(oldItem: AchatTiersPayant, newItem: AchatTiersPayant): Boolean {
            return oldItem == newItem
        }
    }
}
