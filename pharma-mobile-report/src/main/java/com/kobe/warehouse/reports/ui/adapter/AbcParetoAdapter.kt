package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.AbcPareto
import com.kobe.warehouse.reports.databinding.ItemAbcParetoBinding
import com.kobe.warehouse.reports.databinding.ItemLoadingFooterBinding
import com.kobe.warehouse.reports.utils.NumberFormatUtils

class AbcParetoAdapter : ListAdapter<AbcPareto, RecyclerView.ViewHolder>(DiffCallback()) {

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
            val binding = ItemAbcParetoBinding.inflate(
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

    class ItemViewHolder(private val binding: ItemAbcParetoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AbcPareto) {
            binding.apply {
                tvRank.text = item.rang.toString()
                tvRank.background.setTint(item.getParetoColor())

                tvProductName.text = item.libelle
                tvCodeCip.text = item.codeCip ?: "-"

                chipParetoClass.text = item.getParetoShortLabel()
                chipParetoClass.chipBackgroundColor = android.content.res.ColorStateList.valueOf(item.getParetoColor())

                tvRevenue.text = NumberFormatUtils.formatCurrency(item.caTotal.toLong())
                tvContribution.text = item.getContributionFormatted()
                tvCumulative.text = item.getCumulativeFormatted()

                progressCumulative.progress = (item.caCumulePct?.toFloat() ?: 0f).toInt()

                tvQuantitySold.text = "${item.qteVendue} unités"
                tvSalesCount.text = "${item.nbVentes} ventes"
            }
        }
    }

    class LoadingViewHolder(binding: ItemLoadingFooterBinding) : RecyclerView.ViewHolder(binding.root)

    private class DiffCallback : DiffUtil.ItemCallback<AbcPareto>() {
        override fun areItemsTheSame(oldItem: AbcPareto, newItem: AbcPareto) = oldItem.produitId == newItem.produitId
        override fun areContentsTheSame(oldItem: AbcPareto, newItem: AbcPareto) = oldItem == newItem
    }
}
