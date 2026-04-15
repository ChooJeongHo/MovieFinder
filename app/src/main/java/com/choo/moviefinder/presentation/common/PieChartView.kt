package com.choo.moviefinder.presentation.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Slice(val label: String, val value: Float, val color: Int, val legendText: String)

    private val slices = mutableListOf<Slice>()
    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val arcRect = RectF()

    private val defaultColors = intArrayOf(
        0xFF01B4E4.toInt(),
        0xFF90CEA1.toInt(),
        0xFFD2D531.toInt(),
        0xFFDB2360.toInt(),
        0xFF9B59B6.toInt(),
        0xFFE67E22.toInt(),
        0xFF1ABC9C.toInt(),
        0xFF3498DB.toInt()
    )

    private var textColor: Int

    init {
        val tv = TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)
        textColor = if (tv.resourceId != 0) {
            context.getColor(tv.resourceId)
        } else {
            tv.data
        }
        textPaint.color = textColor
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    // 장르 데이터를 설정하고 파이차트를 다시 그린다
    fun setData(items: List<Pair<String, Int>>) {
        slices.clear()
        val total = items.sumOf { it.second }.toFloat()
        if (total == 0f) {
            invalidate()
            return
        }
        items.forEachIndexed { index, (label, value) ->
            val percent = (value / total * 100).toInt()
            slices.add(Slice(label, value / total, defaultColors[index % defaultColors.size], "$label $percent%"))
        }
        contentDescription = items.joinToString(", ") { "${it.first} ${(it.second / total * 100).toInt()}%" }
        invalidate()
    }

    // 뷰 크기 변경 시 파이 영역과 텍스트 크기를 재계산한다
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val padding = paddingLeft + paddingRight
        val availableWidth = w - padding
        val pieDiameter = minOf((availableWidth * 0.45f), h.toFloat() - paddingTop - paddingBottom)
        val pieRadius = pieDiameter / 2f
        val pieCx = paddingLeft + pieRadius + (availableWidth * 0.025f)
        val pieCy = paddingTop + (h - paddingTop - paddingBottom) / 2f
        arcRect.set(pieCx - pieRadius, pieCy - pieRadius, pieCx + pieRadius, pieCy + pieRadius)
        textPaint.textSize = pieDiameter / 10f
    }

    // 파이 세그먼트와 범례를 그린다
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (slices.isEmpty()) return

        var startAngle = -90f
        for (slice in slices) {
            val sweep = slice.value * 360f
            slicePaint.color = slice.color
            canvas.drawArc(arcRect, startAngle, sweep, true, slicePaint)
            startAngle += sweep
        }

        val legendX = arcRect.right + (width - arcRect.right) * 0.15f
        val lineHeight = textPaint.textSize * 1.8f
        val totalLegendHeight = slices.size * lineHeight
        var legendY = arcRect.centerY() - totalLegendHeight / 2f + textPaint.textSize

        val dotRadius = textPaint.textSize * 0.3f

        for (slice in slices) {
            dotPaint.color = slice.color
            canvas.drawCircle(legendX, legendY - dotRadius, dotRadius, dotPaint)

            canvas.drawText(slice.legendText, legendX + dotRadius * 3, legendY, textPaint)
            legendY += lineHeight
        }
    }
}
