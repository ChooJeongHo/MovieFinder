package com.choo.moviefinder.presentation.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * 오늘의 박스오피스 TOP 3를 보여주는 2x2 Glance 위젯.
 *
 * 이 클래스는 **네트워크/DB I/O를 절대 수행하지 않는다.** [provideGlance]는 재구성마다 호출될 수 있어
 * 여기서 fetch를 하면 KOFIC + TMDB 호출이 반복된다. 데이터 획득은 전적으로
 * [BoxOfficeWidgetWorker]가 담당하고, 이 클래스는 Glance state에 저장된 스냅샷을 읽어 그리기만 한다.
 */
class BoxOfficeWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences>
        get() = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode
        get() = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val preferences = currentState<Preferences>()
            BoxOfficeWidgetBody(
                snapshot = preferences.readSnapshot(),
                hasError = preferences[KEY_LAST_ERROR] == true
            )
        }
    }

    companion object {
        const val TOP_COUNT = 3

        val KEY_SNAPSHOT: Preferences.Key<String> = stringPreferencesKey("box_office_snapshot")
        val KEY_LAST_ERROR: Preferences.Key<Boolean> = booleanPreferencesKey("box_office_last_error")
    }
}

/**
 * Glance state에 저장된 JSON 스냅샷을 역직렬화한다.
 * 저장된 적이 없거나(최초 설치) 포맷이 깨졌으면 null — 호출부는 로딩/빈 상태로 처리한다.
 */
internal fun Preferences.readSnapshot(): BoxOfficeWidgetSnapshot? {
    val raw = this[BoxOfficeWidget.KEY_SNAPSHOT] ?: return null
    return try {
        boxOfficeWidgetJson.decodeFromString<BoxOfficeWidgetSnapshot>(raw)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "위젯: 박스오피스 스냅샷 역직렬화 실패")
        null
    }
}

/**
 * 스냅샷과 에러 플래그를 Glance state에 기록한다.
 * [snapshot]이 null이면 **기존 스냅샷을 지우지 않는다** — 갱신 실패 시에도 마지막 성공 데이터를 계속 보여주기 위함.
 */
internal suspend fun writeWidgetState(
    context: Context,
    glanceId: GlanceId,
    snapshot: BoxOfficeWidgetSnapshot?,
    hasError: Boolean
) {
    updateAppWidgetState(context, glanceId) { preferences ->
        if (snapshot != null) {
            preferences[BoxOfficeWidget.KEY_SNAPSHOT] = boxOfficeWidgetJson.encodeToString(snapshot)
        }
        preferences[BoxOfficeWidget.KEY_LAST_ERROR] = hasError
    }
}
