package com.kobe.warehouse.reports.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class SimpleBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class BarItem(val label: String, val value: Float, val color: Int)

    private val bars = mutableListOf<BarItem>()
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 2f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    fun setBars(items: List<BarItem>) {
        bars.clear()
        bars.addAll(items.filter { it.value > 0f })
        invalidate()
    }

    fun clearChart() {
        bars.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars.isEmpty()) return

        val leftPad = 32f
        val rightPad = 32f
        val topPad = 24f
        val bottomPad = 48f
        val chartWidth = width - leftPad - rightPad
        val chartHeight = height - topPad - bottomPad
        if (chartWidth <= 0f || chartHeight <= 0f) return

        val maxValue = max(1f, bars.maxOf { it.value })
        val slotWidth = chartWidth / bars.size
        val barWidth = slotWidth * 0.65f
        val baselineY = topPad + chartHeight
        canvas.drawLine(leftPad, baselineY, leftPad + chartWidth, baselineY, axisPaint)

        bars.forEachIndexed { index, item ->
            val xCenter = leftPad + (index * slotWidth) + slotWidth / 2f
            val barHeight = (item.value / maxValue) * (chartHeight - 8f)
            val left = xCenter - barWidth / 2f
            val top = baselineY - barHeight
            val right = xCenter + barWidth / 2f
            val bottom = baselineY

            barPaint.color = item.color
            canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, barPaint)

            canvas.drawText(item.label, xCenter, baselineY + 28f, labelPaint)
        }
    }
}

