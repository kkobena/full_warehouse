package com.kobe.warehouse.reports.ui.forecast

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.databinding.ItemRecommendationBinding
import com.kobe.warehouse.reports.data.model.ForecastRecommendation
import com.kobe.warehouse.reports.data.model.RecommendationImpact
import com.kobe.warehouse.reports.data.model.RecommendationType

/**
 * Adapter for displaying forecast recommendations
 */
class RecommendationsAdapter : ListAdapter<ForecastRecommendation, RecommendationsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecommendationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemRecommendationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recommendation: ForecastRecommendation) {
            binding.apply {
                // Title and description
                textTitle.text = recommendation.title
                textDescription.text = recommendation.description

                // Icon based on type
                imageIcon.setImageResource(getIconForType(recommendation.type))

                // Impact indicator
                val impactInfo = getImpactInfo(recommendation.impact)
                textImpact.text = impactInfo.first
                textImpact.setTextColor(impactInfo.second)

                // Expected change (if available)
                if (recommendation.expectedChange != null) {
                    val change = recommendation.expectedChange
                    val sign = if (change >= 0) "+" else ""
                    textExpectedChange.text = String.format("%s%.1f%%", sign, change)
                    textExpectedChange.visibility = android.view.View.VISIBLE

                    textExpectedChange.setTextColor(
                        if (change >= 0) Color.GREEN else Color.RED
                    )
                } else {
                    textExpectedChange.visibility = android.view.View.GONE
                }

                // Card background color based on impact
                cardView.setCardBackgroundColor(getBackgroundColorForImpact(recommendation.impact))
            }
        }

        private fun getIconForType(type: RecommendationType): Int {
            return when (type) {
                RecommendationType.INCREASE_STOCK -> R.drawable.ic_trending_up
                RecommendationType.DECREASE_STOCK -> R.drawable.ic_trending_down
                RecommendationType.PRICE_ADJUST -> R.drawable.ic_price_tag
                RecommendationType.PROMOTION -> R.drawable.ic_sale
                RecommendationType.REORDER_SOON -> R.drawable.ic_shopping_cart
                RecommendationType.HIGH_DEMAND -> R.drawable.ic_alert_circle
            }
        }

        private fun getImpactInfo(impact: RecommendationImpact): Pair<String, Int> {
            return when (impact) {
                RecommendationImpact.CRITICAL -> Pair("Critique", Color.parseColor("#D32F2F"))
                RecommendationImpact.HIGH -> Pair("Élevé", Color.parseColor("#F57C00"))
                RecommendationImpact.MEDIUM -> Pair("Moyen", Color.parseColor("#FBC02D"))
                RecommendationImpact.LOW -> Pair("Faible", Color.parseColor("#388E3C"))
            }
        }

        private fun getBackgroundColorForImpact(impact: RecommendationImpact): Int {
            return when (impact) {
                RecommendationImpact.CRITICAL -> Color.parseColor("#FFEBEE")
                RecommendationImpact.HIGH -> Color.parseColor("#FFF3E0")
                RecommendationImpact.MEDIUM -> Color.parseColor("#FFFDE7")
                RecommendationImpact.LOW -> Color.parseColor("#E8F5E9")
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ForecastRecommendation>() {
            override fun areItemsTheSame(
                oldItem: ForecastRecommendation,
                newItem: ForecastRecommendation
            ): Boolean {
                return oldItem.title == newItem.title &&
                        oldItem.type == newItem.type
            }

            override fun areContentsTheSame(
                oldItem: ForecastRecommendation,
                newItem: ForecastRecommendation
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
