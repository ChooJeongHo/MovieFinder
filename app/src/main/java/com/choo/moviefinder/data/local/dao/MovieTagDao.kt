package com.choo.moviefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.choo.moviefinder.data.local.entity.FavoriteMovieEntity
import com.choo.moviefinder.data.local.entity.MovieTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieTagDao {

    // 특정 영화의 태그 목록 실시간 관찰
    @Query("SELECT * FROM movie_tags WHERE movieId = :movieId ORDER BY addedAt ASC")
    fun getTagsByMovieId(movieId: Int): Flow<List<MovieTagEntity>>

    // 모든 고유 태그 이름 목록 (필터 칩 표시용)
    @Query("SELECT DISTINCT tagName FROM movie_tags ORDER BY tagName ASC")
    fun getAllDistinctTagNames(): Flow<List<String>>

    // 태그 삽입 (동일 영화+태그 조합 무시)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: MovieTagEntity)

    // 특정 태그 삭제
    @Query("DELETE FROM movie_tags WHERE movieId = :movieId AND tagName = :tagName")
    suspend fun deleteTag(movieId: Int, tagName: String)

    // 특정 영화의 모든 태그 삭제
    @Query("DELETE FROM movie_tags WHERE movieId = :movieId")
    suspend fun deleteAllTagsForMovie(movieId: Int)

    // 태그로 즐겨찾기 영화 필터링 - 추가일 역순
    @Query("""
        SELECT f.* FROM favorite_movies f
        INNER JOIN movie_tags t ON f.id = t.movieId
        WHERE t.tagName = :tagName
        ORDER BY f.addedAt DESC
    """)
    fun getFavoritesByTag(tagName: String): Flow<List<FavoriteMovieEntity>>

    // 태그로 즐겨찾기 영화 필터링 - 제목 오름차순
    @Query("""
        SELECT f.* FROM favorite_movies f
        INNER JOIN movie_tags t ON f.id = t.movieId
        WHERE t.tagName = :tagName
        ORDER BY f.title ASC
    """)
    fun getFavoritesByTagSortedByTitle(tagName: String): Flow<List<FavoriteMovieEntity>>

    // 태그로 즐겨찾기 영화 필터링 - 평점 내림차순
    @Query("""
        SELECT f.* FROM favorite_movies f
        INNER JOIN movie_tags t ON f.id = t.movieId
        WHERE t.tagName = :tagName
        ORDER BY f.voteAverage DESC
    """)
    fun getFavoritesByTagSortedByRating(tagName: String): Flow<List<FavoriteMovieEntity>>
}
