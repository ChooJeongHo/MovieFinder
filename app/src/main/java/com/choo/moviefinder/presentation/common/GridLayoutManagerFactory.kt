package com.choo.moviefinder.presentation.common

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager

/**
 * LoadStateFooter가 전체 너비를 차지하도록 SpanSizeLookup이 적용된 GridLayoutManager 생성.
 * 영화 아이템은 1칸(= 2열 그리드), footer는 2칸(= 전체 너비).
 */
fun createMovieGridLayoutManager(
    context: Context,
    spanCount: Int = 2,
    adapterItemCount: () -> Int
): GridLayoutManager {
    return GridLayoutManager(context, spanCount).apply {
        spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position < adapterItemCount()) 1 else spanCount
            }
        }.apply { isSpanIndexCacheEnabled = true }
    }
}
