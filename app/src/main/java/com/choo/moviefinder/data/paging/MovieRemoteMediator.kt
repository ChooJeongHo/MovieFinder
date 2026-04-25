package com.choo.moviefinder.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.choo.moviefinder.core.util.NetworkMonitor
import com.choo.moviefinder.core.util.withExponentialBackoff
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.data.local.dao.CachedMovieDao
import com.choo.moviefinder.data.local.dao.RemoteKeyDao
import com.choo.moviefinder.data.local.entity.CachedMovieEntity
import com.choo.moviefinder.data.local.entity.RemoteKeyEntity
import com.choo.moviefinder.data.remote.api.MovieApiService
import com.choo.moviefinder.data.remote.dto.toDomain
import com.choo.moviefinder.data.local.entity.toCachedEntity
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalPagingApi::class)
class MovieRemoteMediator(
    private val apiService: MovieApiService,
    private val database: MovieDatabase,
    private val cachedMovieDao: CachedMovieDao,
    private val remoteKeyDao: RemoteKeyDao,
    private val category: String,
    private val networkMonitor: NetworkMonitor
) : RemoteMediator<Int, CachedMovieEntity>() {

    // 캐시 만료 여부를 확인하여 초기 새로고침 필요 여부 결정
    override suspend fun initialize(): InitializeAction {
        val remoteKey = remoteKeyDao.getRemoteKey(category)
        val lastUpdated = remoteKey?.lastUpdated ?: 0L
        val cacheAge = System.currentTimeMillis() - lastUpdated
        return if (cacheAge > CACHE_TIMEOUT_MS) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    // API에서 영화를 가져와 Room 캐시에 저장하는 페이징 로드 처리
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CachedMovieEntity>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                // 오프라인 상태면 Room 캐시를 즉시 표시하고 네트워크 호출 생략
                if (!networkMonitor.isConnected.value) {
                    return MediatorResult.Success(endOfPaginationReached = false)
                }
                1
            }
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKey = remoteKeyDao.getRemoteKey(category)
                remoteKey?.nextKey ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return try {
            val response = fetchMovies(page)

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
                val lastUpdated = if (loadType == LoadType.REFRESH) {
                    System.currentTimeMillis()
                } else {
                    remoteKeyDao.getRemoteKey(category)?.lastUpdated ?: System.currentTimeMillis()
                }
                remoteKeyDao.insert(
                    RemoteKeyEntity(
                        category = category,
                        nextKey = if (endOfPaginationReached) null else page + 1,
                        lastUpdated = lastUpdated
                    )
                )
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }

    // API 카테고리에 따라 적절한 엔드포인트를 호출한다
    private suspend fun fetchMovies(page: Int) = withExponentialBackoff {
        when (category) {
            CATEGORY_NOW_PLAYING -> apiService.getNowPlayingMovies(page)
            CATEGORY_POPULAR -> apiService.getPopularMovies(page)
            else -> throw IllegalArgumentException("Unknown category: $category")
        }
    }

    companion object {
        const val CATEGORY_NOW_PLAYING = "now_playing"
        const val CATEGORY_POPULAR = "popular"
        val CACHE_TIMEOUT_MS = TimeUnit.HOURS.toMillis(1)
    }
}
