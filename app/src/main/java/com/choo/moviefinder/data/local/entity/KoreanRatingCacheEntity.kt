package com.choo.moviefinder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.choo.moviefinder.domain.model.KoreanRating

@Entity(tableName = "korean_rating_cache")
data class KoreanRatingCacheEntity(
    @PrimaryKey val movieTitle: String,
    val found: Boolean,
    val gradeName: String?,
    val matchedTitle: String?,
    val productionCompany: String?,
    val directorName: String?,
    val productionYear: String?,
    val ratingReason: String?,
    val cachedAt: Long
)

fun KoreanRatingCacheEntity.toDomain(): KoreanRating? {
    if (!found) return null
    val fields = listOf(gradeName, matchedTitle, productionCompany, directorName, productionYear, ratingReason)
    if (fields.any { it == null }) return null
    return KoreanRating(
        gradeName = gradeName!!,
        title = matchedTitle!!,
        productionCompany = productionCompany!!,
        directorName = directorName!!,
        productionYear = productionYear!!,
        ratingReason = ratingReason!!
    )
}

fun KoreanRating?.toCacheEntity(movieTitle: String, cachedAt: Long): KoreanRatingCacheEntity =
    if (this == null) {
        KoreanRatingCacheEntity(
            movieTitle = movieTitle,
            found = false,
            gradeName = null,
            matchedTitle = null,
            productionCompany = null,
            directorName = null,
            productionYear = null,
            ratingReason = null,
            cachedAt = cachedAt
        )
    } else {
        KoreanRatingCacheEntity(
            movieTitle = movieTitle,
            found = true,
            gradeName = gradeName,
            matchedTitle = title,
            productionCompany = productionCompany,
            directorName = directorName,
            productionYear = productionYear,
            ratingReason = ratingReason,
            cachedAt = cachedAt
        )
    }
