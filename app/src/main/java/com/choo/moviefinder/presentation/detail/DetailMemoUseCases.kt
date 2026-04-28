package com.choo.moviefinder.presentation.detail

import com.choo.moviefinder.domain.usecase.DeleteMemoUseCase
import com.choo.moviefinder.domain.usecase.GetMemosUseCase
import com.choo.moviefinder.domain.usecase.SaveMemoUseCase
import com.choo.moviefinder.domain.usecase.UpdateMemoUseCase
import dagger.Reusable
import javax.inject.Inject

@Reusable
class DetailMemoUseCases @Inject constructor(
    val getMemos: GetMemosUseCase,
    val saveMemo: SaveMemoUseCase,
    val updateMemo: UpdateMemoUseCase,
    val deleteMemo: DeleteMemoUseCase
)
