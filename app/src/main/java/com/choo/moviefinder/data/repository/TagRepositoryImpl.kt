package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.MovieTagDao
import com.choo.moviefinder.data.local.entity.MovieTagEntity
import com.choo.moviefinder.data.local.entity.toDomain
import com.choo.moviefinder.domain.model.FavoriteSortOrder
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieTag
import com.choo.moviefinder.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TagRepositoryImpl @Inject constructor(
    private val movieTagDao: MovieTagDao
) : TagRepository {

    override fun getTagsForMovie(movieId: Int): Flow<List<MovieTag>> =
        movieTagDao.getTagsByMovieId(movieId).map { entities ->
            entities.map { MovieTag(it.id, it.movieId, it.tagName, it.addedAt) }
        }

    override fun getAllTagNames(): Flow<List<String>> =
        movieTagDao.getAllDistinctTagNames()

    override suspend fun addTag(movieId: Int, tagName: String) {
        require(tagName.isNotBlank()) { "Tag name must not be blank" }
        movieTagDao.insertTag(MovieTagEntity(movieId = movieId, tagName = tagName.trim()))
    }

    override suspend fun removeTag(movieId: Int, tagName: String) {
        movieTagDao.deleteTag(movieId, tagName)
    }

    override fun getFavoritesByTag(tagName: String): Flow<List<Movie>> =
        movieTagDao.getFavoritesByTag(tagName).map { entities ->
            entities.map { it.toDomain() }
        }

    // 정렬 순서를 지정하여 태그 필터링 즐겨찾기 목록을 DB ORDER BY로 조회
    override fun getFavoritesByTagSorted(tagName: String, sortOrder: FavoriteSortOrder): Flow<List<Movie>> {
        val raw = when (sortOrder) {
            FavoriteSortOrder.ADDED_DATE -> movieTagDao.getFavoritesByTag(tagName)
            FavoriteSortOrder.TITLE -> movieTagDao.getFavoritesByTagSortedByTitle(tagName)
            FavoriteSortOrder.RATING -> movieTagDao.getFavoritesByTagSortedByRating(tagName)
        }
        return raw.map { entities -> entities.map { it.toDomain() } }
    }
}
