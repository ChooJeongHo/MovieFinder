package com.choo.moviefinder.presentation.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.core.util.launchWithErrorHandler
import com.choo.moviefinder.domain.model.CollectionDetail
import com.choo.moviefinder.domain.usecase.GetCollectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed class CollectionUiState {
    object Loading : CollectionUiState()
    data class Success(val collection: CollectionDetail) : CollectionUiState()
    data class Error(val errorType: ErrorType) : CollectionUiState()
}

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val getCollectionUseCase: GetCollectionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val collectionId: Int = requireNotNull(savedStateHandle.get<Int>("collectionId")) {
        "collectionId argument is required for CollectionViewModel"
    }.also { require(it > 0) { "collectionId must be positive, got $it" } }

    private val _uiState = MutableStateFlow<CollectionUiState>(CollectionUiState.Loading)
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init {
        loadCollection()
    }

    fun loadCollection() {
        viewModelScope.launchWithErrorHandler(
            onError = { _uiState.value = CollectionUiState.Error(it) }
        ) {
            _uiState.value = CollectionUiState.Loading
            _uiState.value = CollectionUiState.Success(getCollectionUseCase(collectionId))
        }
    }
}
