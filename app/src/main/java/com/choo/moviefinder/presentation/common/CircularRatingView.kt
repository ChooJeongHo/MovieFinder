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
    private var ratingText: String = "0.0"
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

    // 평점 값 설정 및 접근성 설명 갱신 후 뷰 다시 그리기
    fun setRating(value: Double) {
        if (rating == value) return
        rating = value
        ratingText = String.format(Locale.US, "%.1f", value)
        contentDescription = context.getString(R.string.cd_rating, value)
        updateProgressColor()
        invalidate()
    }

    // 평점에 따라 진행률 색상 변경 (녹색/노란색/빨간색)
    private fun updateProgressColor() {
        val colorRes = when {
            rating >= 7.0 -> R.color.rating_green
            rating >= 4.0 -> R.color.rating_yellow
            else -> R.color.rating_red
        }
        progressPaint.color = ContextCompat.getColor(context, colorRes)
    }

    // 크기 변경 시 1회만 계산하여 onDraw() 부하 최소화
    private var cx = 0f
    private var cy = 0f
    private var radius = 0f

    // 뷰 크기 변경 시 중심좌표, 반지름, 선 굵기, 텍스트 크기 재계산
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = w / 2f
        cy = h / 2f
        radius = minOf(cx, cy)

        val strokeWidth = radius * 0.15f
        trackPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth
        textPaint.textSize = radius * 0.55f

        val arcRadius = radius - strokeWidth / 2 - radius * 0.1f
        arcRect.set(
            cx - arcRadius, cy - arcRadius,
            cx + arcRadius, cy + arcRadius
        )
    }

    // 그리기 순서: 배경 원 → 트랙(회색 원호) → 진행률 원호 → 중앙 텍스트
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawCircle(cx, cy, radius, backgroundPaint)
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)

        // rating(0~10)을 0~360도로 변환, -90f 시작은 12시 방향
        val sweepAngle = (rating / 10.0 * 360.0).toFloat()
        canvas.drawArc(arcRect, -90f, sweepAngle, false, progressPaint)

        // Locale.US 고정으로 소수점 표기 일관성 보장 (setRating()에서 미리 계산)
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(ratingText, cx, textY, textPaint)
    }
}
