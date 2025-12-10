package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.TopProduct
import com.kobe.warehouse.reports.databinding.ItemTopProductBinding

/**
 * Adapter for displaying top products in a RecyclerView.
 */
class TopProductAdapter(
    private val onItemClick: (TopProduct) -> Unit
) : ListAdapter<TopProduct, TopProductAdapter.ViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTopProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(product: TopProduct) {
            binding.tvRank.text = product.rank.toString()
            binding.tvProductName.text = product.name
            binding.tvProductCode.text = product.codeCip?.let { "CIP: $it" } ?: ""
            binding.tvSalesAmount.text = product.getFormattedSalesAmount()
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<TopProduct>() {
        override fun areItemsTheSame(oldItem: TopProduct, newItem: TopProduct): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TopProduct, newItem: TopProduct): Boolean {
            return oldItem == newItem
        }
    }
}
