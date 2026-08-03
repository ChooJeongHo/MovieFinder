package com.choo.moviefinder.presentation.widget

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.choo.moviefinder.MainActivity
import com.choo.moviefinder.R
import java.text.NumberFormat
import java.util.Locale

// values/colors.xml + values-night/colors.xml의 icon_default / colorSecondary와 동일한 값.
// Glance는 XML 테마 대신 day/night ColorProvider로 다크 모드를 처리한다.
private val widgetTextColor = ColorProvider(day = Color(0xFF1C1B1F), night = Color(0xFFE6E1E5))
private val widgetSubTextColor = ColorProvider(day = Color(0xFF5F5F63), night = Color(0xFFB0ABAF))
private val widgetAccentColor = ColorProvider(day = Color(0xFF01B4E4), night = Color(0xFF01B4E4))

/**
 * 박스오피스 위젯 본문.
 *
 * @param snapshot Glance state에 저장된 마지막 스냅샷. null이면 아직 한 번도 갱신되지 않은 상태.
 * @param hasError 마지막 갱신이 실패했는지 여부. snapshot이 살아 있으면 (오래된) 데이터를 계속 보여준다.
 */
@Composable
internal fun BoxOfficeWidgetBody(
    snapshot: BoxOfficeWidgetSnapshot?,
    hasError: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(8.dp)
    ) {
        BoxOfficeWidgetHeader(onRefresh = actionRunCallback<BoxOfficeRefreshAction>())

        when {
            snapshot == null && hasError -> BoxOfficeWidgetMessage(R.string.box_office_error)
            snapshot == null -> BoxOfficeWidgetMessage(R.string.widget_loading)
            snapshot.items.isEmpty() -> BoxOfficeWidgetMessage(R.string.widget_empty)
            else -> snapshot.items.forEach { item -> BoxOfficeWidgetRow(item) }
        }
    }
}

@Composable
private fun BoxOfficeWidgetHeader(onRefresh: Action) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = LocalContext.current.getString(R.string.widget_box_office_title),
            style = TextStyle(
                color = widgetTextColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity<MainActivity>())
        )
        Image(
            provider = ImageProvider(R.drawable.ic_refresh),
            contentDescription = LocalContext.current.getString(R.string.cd_widget_refresh),
            modifier = GlanceModifier
                .padding(4.dp)
                .clickable(onRefresh)
        )
    }
}

@Composable
private fun BoxOfficeWidgetRow(item: BoxOfficeWidgetItem) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(rowAction(context, item.tmdbId)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = context.getString(R.string.widget_box_office_rank_format, item.rank),
            style = TextStyle(
                color = widgetAccentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = item.movieName,
                style = TextStyle(color = widgetTextColor, fontSize = 12.sp),
                maxLines = 1
            )
            Text(
                text = formatAudience(context, item.audienceCount),
                style = TextStyle(color = widgetSubTextColor, fontSize = 10.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BoxOfficeWidgetMessage(@StringRes messageRes: Int) {
    Text(
        text = LocalContext.current.getString(messageRes),
        style = TextStyle(color = widgetSubTextColor, fontSize = 12.sp),
        maxLines = 2,
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable(actionStartActivity<MainActivity>())
    )
}

/**
 * TMDB 매칭에 성공한 항목은 기존 `moviefinder://movie/{movieId}` 딥링크로 상세 화면을 연다.
 * 매칭 실패(tmdbId == null) 항목은 열 상세 화면이 없으므로 앱 메인 화면으로 폴백한다.
 */
private fun rowAction(context: Context, tmdbId: Int?): Action = if (tmdbId != null) {
    actionStartActivity(
        Intent(Intent.ACTION_VIEW, "moviefinder://movie/$tmdbId".toUri())
            .setPackage(context.packageName)
    )
} else {
    actionStartActivity<MainActivity>()
}

// Composable이 아닌 순수 포맷 함수. BoxOfficeAdapter와 동일한 표기(천 단위 구분 + "명")를 유지한다.
private fun formatAudience(context: Context, count: Long): String = context.getString(
    R.string.box_office_audience_format,
    NumberFormat.getNumberInstance(Locale.KOREA).format(count)
)
