package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.UserDataBackup
import com.choo.moviefinder.domain.repository.BackupRepository
import dagger.Reusable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Reusable
class ImportUserDataUseCase @Inject constructor(
    private val repository: BackupRepository,
    private val json: Json
) {
    suspend operator fun invoke(jsonString: String): Int {
        val backup = json.decodeFromString<UserDataBackup>(jsonString)
        repository.importUserData(backup)
        return backup.favorites.size + backup.watchlist.size + backup.ratings.size +
            backup.memos.size + backup.tags.size + backup.watchHistory.size
    }
}
