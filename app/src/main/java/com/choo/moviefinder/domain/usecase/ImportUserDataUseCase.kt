package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.UserDataBackup
import com.choo.moviefinder.domain.repository.BackupRepository
import javax.inject.Inject

class ImportUserDataUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    // 백업 모델에서 사용자 데이터를 가져와 기존 데이터와 병합한다
    suspend operator fun invoke(backup: UserDataBackup) = repository.importUserData(backup)
}
