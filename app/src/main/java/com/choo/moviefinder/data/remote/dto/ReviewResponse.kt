package com.choo.moviefinder.data.remote.dto

import com.choo.moviefinder.domain.model.Review
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewResponse(
    @SerialName("results") val results: List<ReviewDto> = emptyList()
)

@Serializable
data class AuthorDetailsDto(
    @SerialName("avatar_path") val avatarPath: String? = null,
    @SerialName("rating") val rating: Double? = null
)

@Serializable
data class ReviewDto(
    @SerialName("id") val id: String,
    @SerialName("author") val author: String = "",
    @SerialName("author_details") val authorDetails: AuthorDetailsDto = AuthorDetailsDto(),
    @SerialName("content") val content: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("url") val url: String = ""
)

// ReviewDto를 도메인 Review 모델로 변환
fun ReviewDto.toDomain() = Review(
    id = id,
    author = author,
    avatarPath = authorDetails.avatarPath,
    rating = authorDetails.rating,
    content = content,
    createdAt = createdAt
)
