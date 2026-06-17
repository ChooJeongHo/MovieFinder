package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.BackupRepository
import dagger.Reusable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Reusable
class ExportUserDataUseCase @Inject constructor(
    private val repository: BackupRepository,
    private val json: Json
) {
    suspend operator fun invoke(): String = json.encodeToString(repository.exportUserData())
}
