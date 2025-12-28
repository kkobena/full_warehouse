package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.GroupeFournisseurAchat
import com.kobe.warehouse.reports.databinding.ItemAchatFournisseurBinding

/**
 * Adapter for displaying supplier purchases in the Activity Report.
 */
class AchatFournisseurAdapter : ListAdapter<GroupeFournisseurAchat, AchatFournisseurAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAchatFournisseurBinding.inflate(
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
        private val binding: ItemAchatFournisseurBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(achat: GroupeFournisseurAchat) {
            binding.tvLibelle.text = achat.libelle
            binding.tvMontantTtc.text = achat.getFormattedMontantTtc()
            binding.tvMontantHt.text = "HT: ${achat.montantHt}"
            binding.tvPercent.text = "${achat.getFormattedPercent()} du total"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<GroupeFournisseurAchat>() {
        override fun areItemsTheSame(oldItem: GroupeFournisseurAchat, newItem: GroupeFournisseurAchat): Boolean {
            return oldItem.libelle == newItem.libelle
        }

        override fun areContentsTheSame(oldItem: GroupeFournisseurAchat, newItem: GroupeFournisseurAchat): Boolean {
            return oldItem == newItem
        }
    }
}
