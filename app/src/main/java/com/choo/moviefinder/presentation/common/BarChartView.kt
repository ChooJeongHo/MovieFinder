package com.choo.moviefinder.presentation.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.choo.moviefinder.R

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Bar(val label: String, val value: Int, val valueText: String)

    private val bars = mutableListOf<Bar>()
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val barStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }
    private val barRect = RectF()
    private val barCornerRadius = 8f

    private var barColor: Int
    private var textColor: Int
    private val minBarHeightDp = 4f
    private var minBarHeightPx: Float

    init {
        val tv = TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)
        textColor = if (tv.resourceId != 0) context.getColor(tv.resourceId) else tv.data
        textPaint.color = textColor
        labelPaint.color = textColor

        context.theme.resolveAttribute(com.google.android.material.R.attr.colorSecondary, tv, true)
        barColor = if (tv.resourceId != 0) context.getColor(tv.resourceId) else 0xFF01B4E4.toInt()

        barPaint.color = barColor
        barPaint.alpha = 217
        barStrokePaint.color = barColor
        gridPaint.color = textColor
        gridPaint.alpha = 40

        minBarHeightPx = minBarHeightDp * context.resources.displayMetrics.density
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    // 월별 시청 데이터를 설정하고 바차트를 다시 그린다
    fun setData(items: List<Pair<String, Int>>) {
        bars.clear()
        val reversed = items.reversed()
        for ((yearMonth, count) in reversed) {
            val month = yearMonth.substringAfter("-").trimStart('0')
            val label = context.getString(R.string.chart_month_format, month)
            bars.add(Bar(label = label, value = count, valueText = count.toString()))
        }
        contentDescription = bars.joinToString(", ") {
            context.getString(R.string.chart_count_format, it.label, it.value)
        }
        invalidate()
    }

    // 뷰 크기 변경 시 텍스트 크기를 재계산한다
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val availableHeight = (h - paddingTop - paddingBottom).toFloat()
        textPaint.textSize = availableHeight * 0.065f
        labelPaint.textSize = availableHeight * 0.07f
    }

    // 바차트와 그리드 라인을 그린다
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars.isEmpty()) return

        val count = bars.size
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        val top = paddingTop.toFloat() + textPaint.textSize * 1.5f
        val bottom = (height - paddingBottom).toFloat() - labelPaint.textSize * 2f
        val chartHeight = bottom - top
        val maxValue = bars.maxOf { it.value }.coerceAtLeast(1)

        val totalWidth = right - left
        val gap = totalWidth * 0.08f / count.coerceAtLeast(1)
        val barWidth = (totalWidth - gap * (count + 1)) / count

        for (i in 1..2) {
            val y = bottom - (chartHeight * i / 3f)
            canvas.drawLine(left, y, right, y, gridPaint)
        }

        bars.forEachIndexed { index, bar ->
            val barLeft = left + gap + index * (barWidth + gap)
            val barRight = barLeft + barWidth
            val rawHeight = if (maxValue > 0) (bar.value.toFloat() / maxValue) * chartHeight else 0f
            val barHeight = if (bar.value > 0) rawHeight.coerceAtLeast(minBarHeightPx) else 0f
            val barTop = bottom - barHeight

            barRect.set(barLeft, barTop, barRight, bottom)
            canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, barPaint)
            canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, barStrokePaint)

            canvas.drawText(
                bar.valueText,
                (barLeft + barRight) / 2f,
                barTop - textPaint.textSize * 0.3f,
                textPaint
            )

            canvas.drawText(
                bar.label,
                (barLeft + barRight) / 2f,
                bottom + labelPaint.textSize * 1.3f,
                labelPaint
            )
        }
    }
}
