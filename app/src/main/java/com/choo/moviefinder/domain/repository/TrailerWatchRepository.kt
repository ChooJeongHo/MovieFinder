package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.TrailerWatch

interface TrailerWatchRepository {

    // 특정 영화의 트레일러 시청 기록을 조회한다 (없으면 null)
    suspend fun getTrailerWatch(movieId: Int): TrailerWatch?

    // 영화의 트레일러를 시청했다고 기록한다
    suspend fun markTrailerWatched(movieId: Int, trailerKey: String)
}
