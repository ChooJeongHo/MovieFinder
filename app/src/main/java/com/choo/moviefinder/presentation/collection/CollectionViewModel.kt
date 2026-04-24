package com.choo.moviefinder.presentation.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.domain.model.CollectionDetail
import com.choo.moviefinder.domain.usecase.GetCollectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CollectionUiState {
    object Loading : CollectionUiState()
    data class Success(val collection: CollectionDetail) : CollectionUiState()
    data class Error(val message: String) : CollectionUiState()
}

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val getCollectionUseCase: GetCollectionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val collectionId: Int = savedStateHandle["collectionId"] ?: 0

    private val _uiState = MutableStateFlow<CollectionUiState>(CollectionUiState.Loading)
    val uiState: StateFlow<CollectionUiState> = _uiState

    init {
        loadCollection()
    }

    fun loadCollection() {
        viewModelScope.launch {
            _uiState.value = CollectionUiState.Loading
            try {
                _uiState.value = CollectionUiState.Success(getCollectionUseCase(collectionId))
            } catch (e: Exception) {
                _uiState.value = CollectionUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
