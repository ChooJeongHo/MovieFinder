@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.choo.moviefinder.presentation.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.choo.moviefinder.domain.model.DailyWatchCount
import java.text.DateFormatSymbols
import java.util.Locale
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class CalendarHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Cell(val date: LocalDate, val count: Int)

    private val cells = mutableListOf<Cell>()
    private val monthLabels = mutableListOf<Pair<Int, String>>() // column index to label

    // onDraw 할당 방지: setData()에서 미리 계산
    private var cachedShortWeekdays: Array<String> = emptyArray()
    private var dowLabels: List<Pair<Int, String>> = emptyList()

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val level1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val level2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
    private val dayLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT }
    private val cellRect = RectF()
    private val cellCornerRadius = 3f

    private var textColor: Int
    private var colorPrimary: Int

    init {
        val tv = TypedValue()
        context.theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)
        textColor = if (tv.resourceId != 0) context.getColor(tv.resourceId) else tv.data
        labelPaint.color = textColor
        dayLabelPaint.color = textColor

        context.theme.resolveAttribute(com.google.android.material.R.attr.colorSecondary, tv, true)
        colorPrimary = if (tv.resourceId != 0) context.getColor(tv.resourceId) else 0xFF01B4E4.toInt()

        // empty cell: textColor with low alpha
        emptyPaint.color = textColor
        emptyPaint.alpha = 30

        // level 1 (count == 1): primary color, medium alpha
        level1Paint.color = colorPrimary
        level1Paint.alpha = 120

        // level 2+ (count >= 2): primary color, full alpha
        level2Paint.color = colorPrimary
        level2Paint.alpha = 220

        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    // 일별 시청 데이터를 설정하고 히트맵을 다시 그린다 (최근 3개월)
    fun setData(items: List<DailyWatchCount>) {
        cells.clear()
        monthLabels.clear()
        cachedShortWeekdays = DateFormatSymbols(Locale.getDefault()).shortWeekdays
        dowLabels = listOf(1 to cachedShortWeekdays[2], 3 to cachedShortWeekdays[4], 5 to cachedShortWeekdays[6])

        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        val startDate = today.minus(DatePeriod(months = MONTHS_TO_SHOW))

        val countMap = items.associateBy { it.date }

        // find the Sunday on or before startDate
        val startDow = startDate.dayOfWeek.ordinal // Mon=0..Sun=6
        val sundayOffset = (startDow + 1) % 7 // days back to reach Sunday
        var current = startDate.minus(DatePeriod(days = sundayOffset))

        var col = 0
        var lastMonth = -1
        while (current <= today) {
            if (current.monthNumber != lastMonth && current >= startDate) {
                monthLabels.add(col to DateFormatSymbols(Locale.getDefault()).shortMonths[current.monthNumber - 1])
                lastMonth = current.monthNumber
            }
            val count = countMap[current.toString()]?.count ?: 0
            if (current >= startDate) {
                cells.add(Cell(date = current, count = count))
            } else {
                cells.add(Cell(date = current, count = -1)) // padding, not shown
            }
            current = current.plus(DatePeriod(days = 1))
            if (current.dayOfWeek.ordinal == 0) col++ // new week starts Monday
        }

        val watchedDays = items.count { it.count > 0 }
        contentDescription = context.getString(
            com.choo.moviefinder.R.string.stats_calendar_cd,
            watchedDays
        )
        invalidate()
    }

    // 뷰 크기 변경 시 텍스트 크기를 재계산한다
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val cellSize = computeCellSize(w, h)
        labelPaint.textSize = cellSize * 0.9f
        dayLabelPaint.textSize = cellSize * 0.75f
    }

    private fun computeCellSize(w: Int, h: Int): Float {
        val availW = (w - paddingLeft - paddingRight).toFloat()
        val availH = (h - paddingTop - paddingBottom).toFloat()
        // 7 rows + 1 row header, plus gaps
        val cellFromH = availH / (DAYS_IN_WEEK + 1.5f)
        val maxCols = (MONTHS_TO_SHOW * 4.5f).toInt().coerceAtLeast(1)
        val cellFromW = availW / (maxCols + 2f)
        return minOf(cellFromH, cellFromW)
    }

    // 히트맵 그리드와 레이블을 그린다
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cells.isEmpty()) return

        val w = (width - paddingLeft - paddingRight).toFloat()
        val h = (height - paddingTop - paddingBottom).toFloat()
        val cellSize = minOf(
            h / (DAYS_IN_WEEK + 1.5f),
            w / ((cells.size / DAYS_IN_WEEK + 1).coerceAtLeast(1).toFloat() + 2f)
        )
        val gap = cellSize * 0.2f
        val dayLabelWidth = dayLabelPaint.measureText("Sun") + gap * 2

        val gridLeft = paddingLeft + dayLabelWidth
        val gridTop = paddingTop + cellSize * 1.5f // space for month labels

        // draw month labels
        monthLabels.forEach { (col, name) ->
            val x = gridLeft + col * (cellSize + gap)
            canvas.drawText(name, x, paddingTop + labelPaint.textSize, labelPaint)
        }

        // draw day-of-week labels (Mon, Wed, Fri) — locale-aware
        // DateFormatSymbols.shortWeekdays is 1-indexed: 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat
        dowLabels.forEach { (row, label) ->
            val y = gridTop + row * (cellSize + gap) + cellSize * 0.75f
            canvas.drawText(label, paddingLeft + dayLabelWidth - gap, y, dayLabelPaint)
        }

        // draw cells
        cells.forEachIndexed { index, cell ->
            if (cell.count < 0) return@forEachIndexed // padding cell before start date
            val col = index / DAYS_IN_WEEK
            val row = index % DAYS_IN_WEEK
            val cx = gridLeft + col * (cellSize + gap)
            val cy = gridTop + row * (cellSize + gap)
            cellRect.set(cx, cy, cx + cellSize, cy + cellSize)
            val paint = when {
                cell.count == 0 -> emptyPaint
                cell.count == 1 -> level1Paint
                else -> level2Paint
            }
            canvas.drawRoundRect(cellRect, cellCornerRadius, cellCornerRadius, paint)
        }
    }

    companion object {
        private const val DAYS_IN_WEEK = 7
        private const val MONTHS_TO_SHOW = 3
    }
}
