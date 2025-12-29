package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.StockRotation
import com.kobe.warehouse.reports.databinding.ItemLoadingFooterBinding
import com.kobe.warehouse.reports.databinding.ItemStockRotationBinding
import com.kobe.warehouse.reports.utils.NumberFormatUtils

class StockRotationAdapter : ListAdapter<StockRotation, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    private var isLoadingMore = false

    fun setLoadingMore(loading: Boolean) {
        if (isLoadingMore != loading) {
            isLoadingMore = loading
            if (loading) {
                notifyItemInserted(itemCount)
            } else {
                notifyItemRemoved(itemCount)
            }
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (isLoadingMore) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoadingMore && position == itemCount - 1) {
            VIEW_TYPE_LOADING
        } else {
            VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_LOADING) {
            val binding = ItemLoadingFooterBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            LoadingViewHolder(binding)
        } else {
            val binding = ItemStockRotationBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder && position < super.getItemCount()) {
            holder.bind(getItem(position))
        }
    }

    class ItemViewHolder(private val binding: ItemStockRotationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StockRotation) {
            binding.apply {
                tvProductName.text = item.libelle
                tvCodeCip.text = item.codeCip ?: "-"

                chipAbcClass.text = item.getAbcShortLabel()
                chipAbcClass.chipBackgroundColor = android.content.res.ColorStateList.valueOf(item.getAbcColor())

                tvRotationRate.text = item.getRotationRateFormatted()
                tvDaysInStock.text = "${item.avgDaysInStock ?: 0} jours"
                tvDaysInStock.setTextColor(item.getDaysInStockColor())

                tvStockQuantity.text = "${item.stockQuantity} unités"
                tvStockValue.text = NumberFormatUtils.formatCurrency(item.stockValue)
                tvCa12Months.text = NumberFormatUtils.formatCurrency(item.caLast12Months.toLong())
                tvSales30Days.text = "${item.qtySoldLast30Days} vendus (30j)"
            }
        }
    }

    class LoadingViewHolder(binding: ItemLoadingFooterBinding) : RecyclerView.ViewHolder(binding.root)

    private class DiffCallback : DiffUtil.ItemCallback<StockRotation>() {
        override fun areItemsTheSame(oldItem: StockRotation, newItem: StockRotation) = oldItem.produitId == newItem.produitId
        override fun areContentsTheSame(oldItem: StockRotation, newItem: StockRotation) = oldItem == newItem
    }
}
