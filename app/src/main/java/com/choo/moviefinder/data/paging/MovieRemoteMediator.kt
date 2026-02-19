package com.choo.moviefinder.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.RemoteKeyDao
import com.choo.moviefinder.data.local.entity.CachedMovieEntity
import com.choo.moviefinder.data.local.entity.RemoteKeyEntity
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.data.local.entity.toCachedEntity

@OptIn(ExperimentalPagingApi::class)
class MovieRemoteMediator(
    private val apiService: MovieApiService,
    private val database: MovieDatabase,
    private val cachedMovieDao: CachedMovieDao,
    private val remoteKeyDao: RemoteKeyDao,
    private val category: String
) : RemoteMediator<Int, CachedMovieEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CachedMovieEntity>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKey = remoteKeyDao.getRemoteKey(category)
                remoteKey?.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return try {
            val response = when (category) {
                CATEGORY_NOW_PLAYING -> apiService.getNowPlayingMovies(page)
                CATEGORY_POPULAR -> apiService.getPopularMovies(page)
                else -> throw IllegalArgumentException("Unknown category: $category")
            }

            val movies = response.results.map { dto ->
                dto.toDomain().toCachedEntity(category, page)
            }

            val endOfPaginationReached = page >= response.totalPages

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    cachedMovieDao.clearByCategory(category)
                    remoteKeyDao.clearByCategory(category)
                }

                cachedMovieDao.insertAll(movies)
                remoteKeyDao.insert(
                    RemoteKeyEntity(
                        category = category,
                        nextKey = if (endOfPaginationReached) null else page + 1
                    )
                )
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    companion object {
        const val CATEGORY_NOW_PLAYING = "now_playing"
        const val CATEGORY_POPULAR = "popular"
    }
}
