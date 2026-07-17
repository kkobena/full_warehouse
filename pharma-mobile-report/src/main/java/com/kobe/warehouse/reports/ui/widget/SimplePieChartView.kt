package com.kobe.warehouse.reports.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class SimplePieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Slice(val label: String, val value: Float, val color: Int)

    private val slices = mutableListOf<Slice>()
    private val piePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 20f
    }
    private val sliceTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private var legendEnabled = true
    private var sliceLabelsEnabled = true

    fun setSlices(items: List<Slice>) {
        slices.clear()
        slices.addAll(items.filter { it.value > 0f })
        invalidate()
    }

    fun setLegendEnabled(enabled: Boolean) {
        legendEnabled = enabled
        invalidate()
    }

    fun setSliceLabelsEnabled(enabled: Boolean) {
        sliceLabelsEnabled = enabled
        invalidate()
    }

    fun clearChart() {
        slices.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (slices.isEmpty()) return

        val topPadding = 12f
        val sidePadding = 16f
        val legendTopSpacing = 10f
        val legendDotRadius = 8f
        val legendColumns = if (slices.size > 3) 2 else 1
        val legendRows = (slices.size + legendColumns - 1) / legendColumns
        val legendRowHeight = 30f
        val legendAreaHeight = if (legendEnabled) (legendRows * legendRowHeight) + 12f else 0f
        val pieAreaHeight = if (legendEnabled) {
            max(80f, height - legendAreaHeight - legendTopSpacing)
        } else {
            max(80f, height.toFloat())
        }
        val legendTop = pieAreaHeight + legendTopSpacing
        val legendLeft = sidePadding
        val pieSize = min(width.toFloat() - sidePadding * 2f, pieAreaHeight - topPadding * 2f)
        val radius = pieSize / 2f
        val cx = width / 2f
        val cy = topPadding + (pieAreaHeight - topPadding * 2f) / 2f
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        val total = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
        var startAngle = -90f

        slices.forEach { slice ->
            val sweep = (slice.value / total) * 360f
            piePaint.color = slice.color
            canvas.drawArc(oval, startAngle, sweep, true, piePaint)

            // Draw percentage labels on sufficiently large slices
            if (sliceLabelsEnabled && sweep >= 22f) {
                val midAngleDeg = startAngle + sweep / 2f
                val midAngleRad = Math.toRadians(midAngleDeg.toDouble())
                val labelRadius = radius * 0.74f
                val tx = cx + (labelRadius * cos(midAngleRad)).toFloat()
                val ty = cy + (labelRadius * sin(midAngleRad)).toFloat() + 8f
                val percent = (slice.value / total) * 100f
                canvas.drawText(String.format("%.0f%%", percent), tx, ty, sliceTextPaint)
            }
            startAngle += sweep
        }

        canvas.drawCircle(cx, cy, radius * 0.52f, centerPaint)

        if (!legendEnabled) {
            return
        }

        val legendColumnWidth = (width - sidePadding * 2f) / legendColumns.toFloat()
        slices.forEachIndexed { index, slice ->
            val column = index % legendColumns
            val row = index / legendColumns
            val rowY = legendTop + (row * legendRowHeight) + legendRowHeight / 2f
            val colX = legendLeft + (column * legendColumnWidth)
            piePaint.color = slice.color
            canvas.drawCircle(colX + legendDotRadius, rowY, legendDotRadius, piePaint)
            val percent = (slice.value / total) * 100f
            val shortLabel = if (slice.label.length > 14) slice.label.take(14) + "…" else slice.label
            val label = "$shortLabel ${String.format("%.1f%%", percent)}"
            canvas.drawText(label, colX + 28f, rowY + 7f, legendTextPaint)
        }
    }
}
