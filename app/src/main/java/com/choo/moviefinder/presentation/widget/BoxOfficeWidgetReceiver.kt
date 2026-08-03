package com.choo.moviefinder.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 박스오피스 위젯의 AppWidget 진입점.
 *
 * 모든 콜백에서 `super`를 반드시 먼저 호출한다 — Glance의 렌더링/세션 파이프라인이 여기에 붙어 있다.
 * 또한 여기서는 blocking I/O를 하지 않는다(데이터 갱신은 전부 [BoxOfficeWidgetWorker]에 위임).
 */
class BoxOfficeWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget
        get() = BoxOfficeWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        BoxOfficeWidgetWorker.enqueuePeriodic(context)
        // 첫 배치 직후에는 보여줄 데이터가 없으므로 즉시 1회 갱신을 예약한다.
        BoxOfficeWidgetWorker.enqueueOneTimeRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        BoxOfficeWidgetWorker.cancelAll(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // KEEP 정책이라 이미 예약돼 있으면 주기가 리셋되지 않는다 (앱 재설치/복원 시 재예약 목적).
        BoxOfficeWidgetWorker.enqueuePeriodic(context)
    }
}
