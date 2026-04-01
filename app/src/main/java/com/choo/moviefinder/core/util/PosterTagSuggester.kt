package com.choo.moviefinder.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class PosterTagSuggester @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
            .build()
    )

    // ML Kit이 반환하는 포스터 관련 무의미한 일반 레이블 제외
    private val blocklist = setOf(
        "Poster", "Font", "Text", "Rectangle", "Graphic design",
        "Stock photography", "Photo caption", "Logo", "Brand",
        "Photography", "Darkness", "Black-and-white", "Pattern",
        "Design", "Screenshot", "Picture frame", "Illustration",
        "Visual arts", "Art", "Advertising"
    )

    // 포스터 URL로 ML Kit 이미지 레이블링을 수행하고 추천 태그를 반환한다
    suspend fun suggestTags(posterPath: String?): List<String> {
        val url = ImageUrlProvider.posterUrl(posterPath) ?: return emptyList()
        return try {
            val bitmap = loadSoftwareBitmap(url) ?: return emptyList()
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            labeler.process(inputImage)
                .await()
                .filter { it.text !in blocklist }
                .take(MAX_SUGGESTIONS)
                .map { it.text }
        } catch (e: Exception) {
            Timber.w(e, "포스터 태그 추천 실패: %s", posterPath)
            emptyList()
        }
    }

    // Coil로 이미지를 로드하고 ML Kit 처리를 위해 소프트웨어 비트맵으로 반환한다
    private suspend fun loadSoftwareBitmap(url: String): Bitmap? =
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .build()
            val result = SingletonImageLoader.get(context).execute(request)
            val drawable = (result as? SuccessResult)?.image?.asDrawable(context.resources)
            val bitmap = (drawable as? BitmapDrawable)?.bitmap
            // Hardware bitmap은 ML Kit에서 사용 불가 → ARGB_8888 소프트웨어 비트맵으로 복사
            if (bitmap?.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Timber.w(e, "포스터 비트맵 로드 실패: %s", url)
            null
        }

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.70f
        private const val MAX_SUGGESTIONS = 5
    }
}

// Task<T>를 코루틴으로 변환하는 확장 함수
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
