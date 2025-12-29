package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.StockValuation
import com.kobe.warehouse.reports.databinding.ItemLoadingFooterBinding
import com.kobe.warehouse.reports.databinding.ItemStockValuationBinding
import com.kobe.warehouse.reports.utils.NumberFormatUtils

class StockValuationAdapter : ListAdapter<StockValuation, RecyclerView.ViewHolder>(DiffCallback()) {

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
            val binding = ItemStockValuationBinding.inflate(
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

    class ItemViewHolder(private val binding: ItemStockValuationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StockValuation) {
            binding.apply {
                tvProductName.text = item.libelle
                tvCodeCip.text = item.codeCip ?: "-"
                tvCategory.text = item.categorie ?: "-"
                tvQuantity.text = "${item.stockQuantity} unités"
                tvPurchaseValue.text = NumberFormatUtils.formatCurrency(item.totalPurchaseValue)
                tvSalesValue.text = NumberFormatUtils.formatCurrency(item.totalSalesValue)
                tvMargin.text = NumberFormatUtils.formatCurrency(item.potentialMargin)
                tvMarginPercent.text = item.getMarginPercentageFormatted()
                tvMarginPercent.setTextColor(item.getMarginColor())
            }
        }
    }

    class LoadingViewHolder(binding: ItemLoadingFooterBinding) : RecyclerView.ViewHolder(binding.root)

    private class DiffCallback : DiffUtil.ItemCallback<StockValuation>() {
        override fun areItemsTheSame(oldItem: StockValuation, newItem: StockValuation) = oldItem.produitId == newItem.produitId
        override fun areContentsTheSame(oldItem: StockValuation, newItem: StockValuation) = oldItem == newItem
    }
}
