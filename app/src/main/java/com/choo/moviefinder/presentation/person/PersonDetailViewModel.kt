package com.choo.moviefinder.presentation.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.domain.usecase.GetPersonCreditsUseCase
import com.choo.moviefinder.domain.usecase.GetPersonDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPersonDetailUseCase: GetPersonDetailUseCase,
    private val getPersonCreditsUseCase: GetPersonCreditsUseCase
) : ViewModel() {

    private val personId: Int = requireNotNull(savedStateHandle.get<Int>("personId")) {
        "personId argument is required for PersonDetailViewModel"
    }.also { require(it > 0) { "personId must be positive, got $it" } }

    private val _uiState = MutableStateFlow<PersonDetailUiState>(PersonDetailUiState.Loading)
    val uiState: StateFlow<PersonDetailUiState> = _uiState.asStateFlow()

    private val loadingMutex = Mutex()

    init {
        loadPersonDetail()
    }

    // 인물 상세 정보와 출연 영화 목록을 병렬로 조회한다
    fun loadPersonDetail() {
        viewModelScope.launch {
            if (!loadingMutex.tryLock()) return@launch
            try {
                _uiState.value = PersonDetailUiState.Loading
                coroutineScope {
                    val personDeferred = async { getPersonDetailUseCase(personId) }
                    val moviesDeferred = async {
                        try {
                            getPersonCreditsUseCase(personId)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                    _uiState.value = PersonDetailUiState.Success(
                        person = personDeferred.await(),
                        movies = moviesDeferred.await()
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = PersonDetailUiState.Error(ErrorMessageProvider.getErrorType(e))
            } finally {
                loadingMutex.unlock()
            }
        }
    }
}
