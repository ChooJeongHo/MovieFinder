package com.choo.moviefinder.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.choo.moviefinder.R

class FavoriteMoviesWidgetConfigureActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 기본 결과를 RESULT_CANCELED으로 설정 (사용자가 취소 시 위젯 추가 취소)
        setResult(RESULT_CANCELED)

        setContentView(R.layout.widget_configure)

        // Intent에서 appWidgetId 추출
        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        // 유효하지 않은 appWidgetId이면 종료
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // 기존 저장된 선택값으로 RadioButton 초기화
        val savedType = loadWidgetType(this, appWidgetId)
        val radioGroup = findViewById<RadioGroup>(R.id.radio_group_widget_type)
        val radioFavorites = findViewById<RadioButton>(R.id.radio_favorites)
        val radioWatchlist = findViewById<RadioButton>(R.id.radio_watchlist)

        if (savedType == WIDGET_TYPE_WATCHLIST) {
            radioWatchlist.isChecked = true
        } else {
            radioFavorites.isChecked = true
        }

        // 확인 버튼 클릭 처리
        findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            val selectedType = when (radioGroup.checkedRadioButtonId) {
                R.id.radio_watchlist -> WIDGET_TYPE_WATCHLIST
                else -> WIDGET_TYPE_FAVORITES
            }

            // SharedPreferences에 선택값 저장
            saveWidgetType(this, appWidgetId, selectedType)

            // 위젯 업데이트 트리거
            val appWidgetManager = AppWidgetManager.getInstance(this)
            FavoriteMoviesWidget.triggerUpdate(this, appWidgetManager, appWidgetId)

            // 성공 결과 반환
            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }

    companion object {
        const val PREFS_NAME = "widget_prefs"
        const val WIDGET_TYPE_FAVORITES = "favorites"
        const val WIDGET_TYPE_WATCHLIST = "watchlist"

        private fun prefKey(appWidgetId: Int) = "widget_type_$appWidgetId"

        fun saveWidgetType(context: Context, appWidgetId: Int, type: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(prefKey(appWidgetId), type)
                .apply()
        }

        fun loadWidgetType(context: Context, appWidgetId: Int): String {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(prefKey(appWidgetId), WIDGET_TYPE_FAVORITES)
                ?: WIDGET_TYPE_FAVORITES
        }

        fun deleteWidgetType(context: Context, appWidgetId: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(prefKey(appWidgetId))
                .apply()
        }
    }
}
