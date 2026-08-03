package com.choo.moviefinder.presentation.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/**
 * 위젯 새로고침 버튼 콜백.
 *
 * 여기서 직접 네트워크를 타지 않는다 — 제약/재시도/중복 실행 제어를 WorkManager에 맡기기 위해
 * 1회성 작업만 예약하고 즉시 반환한다.
 */
class BoxOfficeRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        BoxOfficeWidgetWorker.enqueueOneTimeRefresh(context)
    }
}
