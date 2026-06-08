package com.choo.moviefinder.domain.usecase

import com.choo.moviefinder.domain.model.CollectionDetail
import com.choo.moviefinder.domain.repository.MovieDetailRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetCollectionUseCase @Inject constructor(
    private val repository: MovieDetailRepository
) {
    suspend operator fun invoke(collectionId: Int): CollectionDetail =
        repository.getCollection(collectionId)
}
