package com.choo.moviefinder.data.repository

import com.choo.moviefinder.data.local.dao.TrailerWatchDao
import com.choo.moviefinder.data.local.entity.TrailerWatchEntity
import com.choo.moviefinder.data.local.entity.toDomain
import com.choo.moviefinder.domain.model.TrailerWatch
import com.choo.moviefinder.domain.repository.TrailerWatchRepository
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class TrailerWatchRepositoryImpl @Inject constructor(
    private val trailerWatchDao: TrailerWatchDao
) : TrailerWatchRepository {

    // 특정 영화의 트레일러 시청 기록을 도메인 모델로 변환하여 조회
    override suspend fun getTrailerWatch(movieId: Int): TrailerWatch? {
        return trailerWatchDao.getTrailerWatch(movieId)?.toDomain()
    }

    // 영화의 트레일러 시청 기록을 Room DB에 저장
    @OptIn(ExperimentalTime::class)
    override suspend fun markTrailerWatched(movieId: Int, trailerKey: String) {
        trailerWatchDao.markWatched(
            TrailerWatchEntity(
                movieId = movieId,
                trailerKey = trailerKey,
                watchedAt = Clock.System.now().toEpochMilliseconds()
            )
        )
    }
}
