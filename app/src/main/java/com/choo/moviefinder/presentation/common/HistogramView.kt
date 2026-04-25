package com.choo.moviefinder.presentation.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.choo.moviefinder.domain.model.RatingBucket
import java.util.Locale

class HistogramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val buckets = mutableListOf<RatingBucket>()

    // onDraw 할당 방지: setData()에서 미리 계산
    private var countMap: Map<Float, RatingBucket> = emptyMap()
    private val ratingLabels: MutableMap<Float, String> = mutableMapOf()
    private var cachedMaxCount: Int = 1

    // onDraw 할당 방지: onSizeChanged()에서 미리 계산
    private var cachedLabelWidth: Float = 0f
    private var cachedValueAreaWidth: Float = 0f
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val barStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
    private val barRect = RectF()
    private val barCornerRadius = 6f

    private var barColor: Int
    private var textColor: Int

    init {
        val tv = TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)
        textColor = if (tv.resourceId != 0) context.getColor(tv.resourceId) else tv.data
        labelPaint.color = textColor
        valuePaint.color = textColor

        context.theme.resolveAttribute(com.google.android.material.R.attr.colorSecondary, tv, true)
        barColor = if (tv.resourceId != 0) context.getColor(tv.resourceId) else 0xFF01B4E4.toInt()
        barPaint.color = barColor
        barPaint.alpha = 217
        barStrokePaint.color = barColor

        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    // 평점 분포 데이터를 설정하고 히스토그램을 다시 그린다
    fun setData(items: List<RatingBucket>) {
        buckets.clear()
        buckets.addAll(items)
        // onDraw에서 매 프레임 생성되던 객체들을 미리 계산
        countMap = items.associateBy { it.rating }
        cachedMaxCount = items.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
        ratingLabels.clear()
        ALL_RATINGS.forEach { rating -> ratingLabels[rating] = String.format(Locale.US, "%.1f★", rating) }
        contentDescription = buckets.joinToString(", ") { bucket ->
            String.format(Locale.US, "%.1f★: %d", bucket.rating, bucket.count)
        }
        invalidate()
    }

    // 뷰 크기 변경 시 텍스트 크기를 재계산한다
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val rowHeight = computeRowHeight(h)
        labelPaint.textSize = rowHeight * 0.45f
        valuePaint.textSize = rowHeight * 0.40f
        cachedLabelWidth = labelPaint.measureText("5.0★") * 1.2f
        cachedValueAreaWidth = valuePaint.measureText("999") * 1.5f
    }

    private fun computeRowHeight(h: Int): Float {
        val totalRows = ALL_RATINGS.size
        val availableHeight = (h - paddingTop - paddingBottom).toFloat()
        return availableHeight / totalRows
    }

    // 히스토그램 바와 레이블을 그린다
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val maxCount = cachedMaxCount
        if (buckets.isEmpty()) return

        val totalRows = ALL_RATINGS.size
        val availableHeight = (height - paddingTop - paddingBottom).toFloat()
        val rowHeight = availableHeight / totalRows

        val labelWidth = cachedLabelWidth
        val valueAreaWidth = cachedValueAreaWidth
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        val barAreaLeft = left + labelWidth + LABEL_BAR_GAP
        val barAreaRight = right - valueAreaWidth - LABEL_BAR_GAP
        val barAreaWidth = (barAreaRight - barAreaLeft).coerceAtLeast(1f)

        ALL_RATINGS.forEachIndexed { index, rating ->
            val bucket = countMap[rating]
            val count = bucket?.count ?: 0
            val rowTop = paddingTop + index * rowHeight
            val rowCenterY = rowTop + rowHeight / 2f

            // rating label
            canvas.drawText(
                ratingLabels[rating] ?: "",
                left + labelWidth,
                rowCenterY + labelPaint.textSize * 0.35f,
                labelPaint
            )

            // bar
            val barWidth = if (maxCount > 0) (count.toFloat() / maxCount) * barAreaWidth else 0f
            val barTop = rowTop + rowHeight * 0.15f
            val barBottom = rowTop + rowHeight * 0.85f
            barRect.set(barAreaLeft, barTop, barAreaLeft + barWidth, barBottom)
            if (barWidth > 0f) {
                canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, barPaint)
                canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, barStrokePaint)
            }

            // count label
            if (count > 0) {
                canvas.drawText(
                    count.toString(),
                    barAreaLeft + barWidth + LABEL_BAR_GAP,
                    rowCenterY + valuePaint.textSize * 0.35f,
                    valuePaint
                )
            }
        }
    }

    companion object {
        private val ALL_RATINGS = listOf(
            5.0f, 4.5f, 4.0f, 3.5f, 3.0f, 2.5f, 2.0f, 1.5f, 1.0f, 0.5f
        )
        private const val LABEL_BAR_GAP = 8f
    }
}
