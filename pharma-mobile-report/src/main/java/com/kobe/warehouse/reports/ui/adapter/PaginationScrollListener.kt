package com.kobe.warehouse.reports.ui.adapter

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView scroll listener for infinite pagination.
 * Triggers loading more items when scrolling near the end of the list.
 */
abstract class PaginationScrollListener(
    private val layoutManager: LinearLayoutManager
) : RecyclerView.OnScrollListener() {

    companion object {
        /**
         * Number of items remaining before triggering load more.
         */
        private const val VISIBLE_THRESHOLD = 3
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        // Only trigger when scrolling down
        if (dy <= 0) return

        val totalItemCount = layoutManager.itemCount
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

        // Check if we need to load more
        if (!isLoading() && !isLastPage()) {
            if ((lastVisibleItemPosition + VISIBLE_THRESHOLD) >= totalItemCount && totalItemCount > 0) {
                loadMoreItems()
            }
        }
    }

    /**
     * Load more items from the data source.
     */
    protected abstract fun loadMoreItems()

    /**
     * Check if currently loading.
     */
    abstract fun isLoading(): Boolean

    /**
     * Check if this is the last page.
     */
    abstract fun isLastPage(): Boolean
}
