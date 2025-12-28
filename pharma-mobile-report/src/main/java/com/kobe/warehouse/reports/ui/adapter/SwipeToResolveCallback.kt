package com.kobe.warehouse.reports.ui.adapter

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.R

/**
 * ItemTouchHelper callback for swipe actions on alerts.
 * - Swipe right: Resolve alert (green background)
 * - Swipe left: Dismiss alert (red background)
 */
class SwipeToResolveCallback(
    private val onSwipeRight: (position: Int) -> Unit,
    private val onSwipeLeft: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private var resolveIcon: Drawable? = null
    private var dismissIcon: Drawable? = null
    private val resolvePaint = Paint().apply { color = 0xFF4CAF50.toInt() } // Green
    private val dismissPaint = Paint().apply { color = 0xFFF44336.toInt() } // Red
    private val cornerRadius = 16f

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            when (direction) {
                ItemTouchHelper.RIGHT -> onSwipeRight(position)
                ItemTouchHelper.LEFT -> onSwipeLeft(position)
            }
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val context = itemView.context

        // Initialize icons if needed
        if (resolveIcon == null) {
            resolveIcon = ContextCompat.getDrawable(context, R.drawable.ic_check_white)
        }
        if (dismissIcon == null) {
            dismissIcon = ContextCompat.getDrawable(context, R.drawable.ic_close_white)
        }

        val iconMargin = (itemView.height - (resolveIcon?.intrinsicHeight ?: 0)) / 2
        val iconTop = itemView.top + iconMargin
        val iconBottom = iconTop + (resolveIcon?.intrinsicHeight ?: 0)

        when {
            // Swipe right - Resolve (green)
            dX > 0 -> {
                val rect = RectF(
                    itemView.left.toFloat(),
                    itemView.top.toFloat(),
                    itemView.left + dX,
                    itemView.bottom.toFloat()
                )
                c.drawRoundRect(rect, cornerRadius, cornerRadius, resolvePaint)

                // Draw icon
                resolveIcon?.let { icon ->
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = iconLeft + icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    icon.draw(c)
                }
            }
            // Swipe left - Dismiss (red)
            dX < 0 -> {
                val rect = RectF(
                    itemView.right + dX,
                    itemView.top.toFloat(),
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat()
                )
                c.drawRoundRect(rect, cornerRadius, cornerRadius, dismissPaint)

                // Draw icon
                dismissIcon?.let { icon ->
                    val iconRight = itemView.right - iconMargin
                    val iconLeft = iconRight - icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    icon.draw(c)
                }
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.4f

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float = defaultValue * 0.5f
}
