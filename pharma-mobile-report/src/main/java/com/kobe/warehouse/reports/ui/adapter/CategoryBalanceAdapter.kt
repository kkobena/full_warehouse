package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.CategoryBalance
import com.kobe.warehouse.reports.databinding.ItemCategoryBalanceBinding

/**
 * Adapter for category balance items in Cash Balance.
 */
class CategoryBalanceAdapter : ListAdapter<CategoryBalance, CategoryBalanceAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryBalanceBinding.inflate(
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
        private val binding: ItemCategoryBalanceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryBalance) {
            binding.tvCategoryLabel.text = item.categoryLabel
            binding.tvCount.text = "${item.count} ventes"
            binding.tvMontantTtc.text = item.getFormattedMontantTtc()
            binding.tvMontantNet.text = item.getFormattedMontantNet()
            binding.tvMarge.text = item.getFormattedMontantMarge()
            binding.tvPanierMoyen.text = item.getFormattedPanierMoyen()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryBalance>() {
        override fun areItemsTheSame(oldItem: CategoryBalance, newItem: CategoryBalance): Boolean {
            return oldItem.categoryCode == newItem.categoryCode
        }

        override fun areContentsTheSame(oldItem: CategoryBalance, newItem: CategoryBalance): Boolean {
            return oldItem == newItem
        }
    }
}
