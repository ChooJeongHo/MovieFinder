package com.choo.moviefinder.presentation.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.choo.moviefinder.R
import java.util.Locale

class CircularRatingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var rating: Double = 0.0
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcRect = RectF()

    init {
        backgroundPaint.color = ContextCompat.getColor(context, R.color.rating_background)
        backgroundPaint.style = Paint.Style.FILL

        trackPaint.color = ContextCompat.getColor(context, R.color.rating_track)
        trackPaint.style = Paint.Style.STROKE
        trackPaint.strokeCap = Paint.Cap.ROUND

        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeCap = Paint.Cap.ROUND

        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textAlign = Paint.Align.CENTER
    }

    fun setRating(value: Double) {
        rating = value
        updateProgressColor()
        invalidate()
    }

    private fun updateProgressColor() {
        val colorRes = when {
            rating >= 7.0 -> R.color.rating_green
            rating >= 4.0 -> R.color.rating_yellow
            else -> R.color.rating_red
        }
        progressPaint.color = ContextCompat.getColor(context, colorRes)
    }

    // 그리기 순서: 배경 원 → 트랙(회색 원호) → 진행률 원호 → 중앙 텍스트
    // 모든 수치는 뷰 크기에 비례하여 계산되므로 다양한 크기에서 동일한 비율 유지
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy)

        canvas.drawCircle(cx, cy, radius, backgroundPaint)

        // strokeWidth, arcRadius 모두 radius 기준 비례값으로 계산
        val strokeWidth = radius * 0.15f
        trackPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth

        // 원호가 배경 원 안쪽에 위치하도록 stroke 절반 + 여백(10%)만큼 안쪽으로 이동
        val arcRadius = radius - strokeWidth / 2 - radius * 0.1f
        arcRect.set(
            cx - arcRadius, cy - arcRadius,
            cx + arcRadius, cy + arcRadius
        )

        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)

        // rating(0~10)을 0~360도로 변환, -90f 시작은 12시 방향
        val sweepAngle = (rating / 10.0 * 360.0).toFloat()
        canvas.drawArc(arcRect, -90f, sweepAngle, false, progressPaint)

        // Locale.US 고정으로 소수점 표기 일관성 보장 (한국 로케일은 동일하지만 명시적으로)
        textPaint.textSize = radius * 0.55f
        val text = String.format(Locale.US, "%.1f", rating)
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, cx, textY, textPaint)
    }
}
