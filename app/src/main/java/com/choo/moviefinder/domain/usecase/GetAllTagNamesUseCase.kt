package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTagNamesUseCase @Inject constructor(
    private val repository: TagRepository
) {
    // 모든 고유 태그 이름 목록을 실시간 Flow로 반환한다
    operator fun invoke(): Flow<List<String>> =
        repository.getAllTagNames()
}
