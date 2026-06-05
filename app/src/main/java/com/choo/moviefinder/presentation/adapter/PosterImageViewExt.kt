package com.choo.moviefinder.presentation.adapter

import android.widget.ImageView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.size.ViewSizeResolver
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ImageUrlProvider

// 포스터 이미지를 공통 옵션(크로스페이드, 플레이스홀더, 실제 뷰 크기 다운샘플링)으로 로드
fun ImageView.loadPoster(posterPath: String?) {
    load(ImageUrlProvider.posterUrl(posterPath)) {
        crossfade(true)
        placeholder(R.drawable.bg_poster_placeholder)
        error(R.drawable.bg_poster_placeholder)
        size(ViewSizeResolver(this@loadPoster))
    }
}
