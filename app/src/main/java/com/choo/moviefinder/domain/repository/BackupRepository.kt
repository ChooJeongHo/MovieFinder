package com.choo.moviefinder.domain.repository

import com.choo.moviefinder.domain.model.UserDataBackup

interface BackupRepository {

    // 사용자 데이터(즐겨찾기, 워치리스트, 평점, 메모)를 백업 모델로 내보낸다
    suspend fun exportUserData(): UserDataBackup

    // 백업 모델에서 사용자 데이터를 가져와 기존 데이터와 병합한다
    suspend fun importUserData(backup: UserDataBackup)
}
