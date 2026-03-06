package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.sales.data.model.Remise
import com.kobe.warehouse.sales.databinding.ItemRemiseBinding

/**
 * Adapter for remise (discount) selection in BottomSheet
 */
class RemiseAdapter(
    private val saleType: String,
    private val onRemiseClick: (Remise) -> Unit
) : ListAdapter<Remise, RemiseAdapter.RemiseViewHolder>(RemiseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemiseViewHolder {
        val binding = ItemRemiseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RemiseViewHolder(binding, saleType, onRemiseClick)
    }

    override fun onBindViewHolder(holder: RemiseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RemiseViewHolder(
        private val binding: ItemRemiseBinding,
        private val saleType: String,
        private val onRemiseClick: (Remise) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(remise: Remise) {
            binding.apply {
                tvRemiseName.text = remise.valeur
                tvRemiseType.text = remise.typeLibelle ?: ""

                val rate = remise.getDiscountRate(saleType)
                if (rate != null) {
                    tvRemiseRate.text = "$rate%"
                    tvRemiseRate.visibility = android.view.View.VISIBLE
                } else {
                    tvRemiseRate.visibility = android.view.View.GONE
                }

                root.setOnClickListener {
                    onRemiseClick(remise)
                }
            }
        }
    }

    private class RemiseDiffCallback : DiffUtil.ItemCallback<Remise>() {
        override fun areItemsTheSame(oldItem: Remise, newItem: Remise): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Remise, newItem: Remise): Boolean {
            return oldItem == newItem
        }
    }
}
