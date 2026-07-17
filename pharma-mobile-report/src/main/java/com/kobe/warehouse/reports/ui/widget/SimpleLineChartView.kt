package com.kobe.warehouse.reports.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class SimpleLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Point(val label: String, val value: Float)

    private val points = mutableListOf<Point>()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1976D2")
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1976D2")
        style = Paint.Style.FILL
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 2f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    fun setPoints(items: List<Point>) {
        points.clear()
        points.addAll(items)
        invalidate()
    }

    fun clearChart() {
        points.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val leftPad = 40f
        val rightPad = 32f
        val topPad = 24f
        val bottomPad = 52f
        val chartWidth = width - leftPad - rightPad
        val chartHeight = height - topPad - bottomPad
        if (chartWidth <= 0f || chartHeight <= 0f) return

        val maxValue = max(1f, points.maxOf { it.value })
        val baselineY = topPad + chartHeight
        canvas.drawLine(leftPad, baselineY, leftPad + chartWidth, baselineY, axisPaint)

        val stepX = if (points.size == 1) 0f else chartWidth / (points.size - 1)
        val path = Path()

        points.forEachIndexed { index, point ->
            val x = leftPad + index * stepX
            val y = baselineY - (point.value / maxValue) * (chartHeight - 8f)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, linePaint)

        points.forEachIndexed { index, point ->
            val x = leftPad + index * stepX
            val y = baselineY - (point.value / maxValue) * (chartHeight - 8f)
            canvas.drawCircle(x, y, 6f, pointPaint)
            canvas.drawText(point.label, x, baselineY + 30f, labelPaint)
        }
    }
}

