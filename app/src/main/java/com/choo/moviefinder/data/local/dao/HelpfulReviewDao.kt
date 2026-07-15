package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.HelpfulReviewEntity

@Dao
interface HelpfulReviewDao {

    // 특정 영화에서 도움이 됨으로 표시된 리뷰 ID 목록을 조회한다
    @Query("SELECT reviewId FROM helpful_reviews WHERE movieId = :movieId")
    suspend fun getHelpfulReviewIds(movieId: Int): List<String>

    // 리뷰를 도움이 됨으로 표시(또는 갱신)한다
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun mark(entity: HelpfulReviewEntity)

    // 리뷰의 도움이 됨 표시를 해제한다
    @Query("DELETE FROM helpful_reviews WHERE reviewId = :reviewId")
    suspend fun unmark(reviewId: String)
}
