package com.kobe.warehouse.reports.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.Recette
import com.kobe.warehouse.reports.databinding.ItemRecetteBinding

/**
 * Adapter for displaying recettes (receipts by payment mode) in the Activity Report.
 */
class RecetteAdapter : ListAdapter<Recette, RecetteAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecetteBinding.inflate(
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
        private val binding: ItemRecetteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recette: Recette) {
            binding.tvLibelle.text = recette.libelle
            binding.tvMontant.text = recette.getFormattedMontant()
            binding.tvPercent.text = recette.getFormattedPercent()

            // Set color indicator
            try {
                binding.colorIndicator.setBackgroundColor(Color.parseColor(recette.color))
            } catch (e: Exception) {
                binding.colorIndicator.setBackgroundColor(Color.GRAY)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Recette>() {
        override fun areItemsTheSame(oldItem: Recette, newItem: Recette): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: Recette, newItem: Recette): Boolean {
            return oldItem == newItem
        }
    }
}
