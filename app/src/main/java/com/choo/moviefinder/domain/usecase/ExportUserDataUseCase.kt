package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.UserDataBackup
import com.choo.moviefinder.domain.repository.MovieRepository
import javax.inject.Inject

class ExportUserDataUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    // 사용자 데이터(즐겨찾기, 워치리스트, 평점, 메모)를 백업 모델로 내보낸다
    suspend operator fun invoke(): UserDataBackup = repository.exportUserData()
}
