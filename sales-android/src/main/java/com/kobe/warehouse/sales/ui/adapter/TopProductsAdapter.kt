package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.sales.data.model.TopProduct
import com.kobe.warehouse.sales.databinding.ItemTopProductBinding

/**
 * Top Products Adapter
 * RecyclerView adapter for displaying top selling products in dashboard
 */
class TopProductsAdapter : ListAdapter<TopProduct, TopProductsAdapter.TopProductViewHolder>(TopProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopProductViewHolder {
        val binding = ItemTopProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TopProductViewHolder(
        private val binding: ItemTopProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(topProduct: TopProduct) {
            binding.tvRank.text = topProduct.rank.toString()
            binding.tvProductName.text = topProduct.productName
            binding.tvQuantitySold.text = topProduct.getFormattedQuantitySold()
            binding.tvTotalSales.text = topProduct.getFormattedTotalSales()
        }
    }

    private class TopProductDiffCallback : DiffUtil.ItemCallback<TopProduct>() {
        override fun areItemsTheSame(oldItem: TopProduct, newItem: TopProduct): Boolean {
            return oldItem.productId == newItem.productId
        }

        override fun areContentsTheSame(oldItem: TopProduct, newItem: TopProduct): Boolean {
            return oldItem == newItem
        }
    }
}
