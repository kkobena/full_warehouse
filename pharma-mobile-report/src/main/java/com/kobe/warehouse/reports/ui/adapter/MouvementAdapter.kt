package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.MouvementCaisse
import com.kobe.warehouse.reports.databinding.ItemMouvementBinding

/**
 * Adapter for displaying mouvements de caisse in the Activity Report.
 */
class MouvementAdapter : ListAdapter<MouvementCaisse, MouvementAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMouvementBinding.inflate(
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
        private val binding: ItemMouvementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mouvement: MouvementCaisse) {
            binding.tvLibelle.text = mouvement.libelle

            val context = binding.root.context
            if (mouvement.isEntree()) {
                binding.iconType.setImageResource(R.drawable.ic_arrow_upward)
                binding.iconType.setColorFilter(ContextCompat.getColor(context, R.color.success))
                binding.tvMontant.text = "+ ${mouvement.getFormattedMontant()}"
                binding.tvMontant.setTextColor(ContextCompat.getColor(context, R.color.success))
            } else {
                binding.iconType.setImageResource(R.drawable.ic_arrow_downward)
                binding.iconType.setColorFilter(ContextCompat.getColor(context, R.color.error))
                binding.tvMontant.text = "- ${mouvement.getFormattedMontant()}"
                binding.tvMontant.setTextColor(ContextCompat.getColor(context, R.color.error))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MouvementCaisse>() {
        override fun areItemsTheSame(oldItem: MouvementCaisse, newItem: MouvementCaisse): Boolean {
            return oldItem.libelle == newItem.libelle && oldItem.montant == newItem.montant
        }

        override fun areContentsTheSame(oldItem: MouvementCaisse, newItem: MouvementCaisse): Boolean {
            return oldItem == newItem
        }
    }
}
