package com.choo.moviefinder.presentation.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.choo.moviefinder.presentation.home.BoxOfficePeriod

/**
 * 위젯 헤더의 일별/주간 배지 탭 콜백.
 *
 * 이 글랜스 인스턴스([glanceId])에 저장된 기간만 토글한다 — 홈 화면에 위젯을 여러 개 배치해도
 * 서로 다른 기간을 독립적으로 유지한다. 새 기간의 실제 데이터는 여기서 직접 받아오지 않고
 * [BoxOfficeWidgetWorker]에 위임한다(네트워크는 Worker가, 렌더링은 Glance가 담당하는 기존 원칙과 동일).
 */
class BoxOfficePeriodToggleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val currentPeriod = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId).readPeriod()
        val nextPeriod = when (currentPeriod) {
            BoxOfficePeriod.DAILY -> BoxOfficePeriod.WEEKLY
            BoxOfficePeriod.WEEKLY -> BoxOfficePeriod.DAILY
        }
        writePeriodAndRecompose(context, glanceId, nextPeriod)
        BoxOfficeWidgetWorker.enqueueOneTimeRefresh(context)
    }
}
