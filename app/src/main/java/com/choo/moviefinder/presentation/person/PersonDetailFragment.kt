package com.choo.moviefinder.presentation.person

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.choo.moviefinder.R
import com.choo.moviefinder.core.util.ErrorMessageProvider
import com.choo.moviefinder.core.util.ImageUrlProvider
import com.choo.moviefinder.databinding.FragmentPersonDetailBinding
import com.choo.moviefinder.domain.model.PersonDetail
import com.choo.moviefinder.presentation.adapter.HorizontalMovieAdapter
import com.choo.moviefinder.presentation.detail.DetailFragmentArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PersonDetailFragment : Fragment() {

    private var _binding: FragmentPersonDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PersonDetailViewModel by viewModels()

    private val args: PersonDetailFragmentArgs by navArgs()

    private lateinit var filmographyAdapter: HorizontalMovieAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        observeUiState()
    }

    // 툴바 뒤로가기 설정
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    // 출연작 RecyclerView 초기화
    private fun setupRecyclerView() {
        filmographyAdapter = HorizontalMovieAdapter { movieId ->
            if (findNavController().currentDestination?.id == R.id.personDetailFragment) {
                findNavController().navigate(
                    R.id.action_personDetailFragment_to_detailFragment,
                    DetailFragmentArgs(movieId).toBundle()
                )
            }
        }
        binding.rvFilmography.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = filmographyAdapter
        }
    }

    // UI 상태(Loading/Success/Error) Flow를 수집하여 화면 전환
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PersonDetailUiState.Loading -> showLoading()
                        is PersonDetailUiState.Success -> showContent(state)
                        is PersonDetailUiState.Error -> showError(state)
                    }
                }
            }
        }
    }

    // 로딩 상태 표시
    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.contentLayout.isVisible = false
        binding.errorView.layoutError.isVisible = false
    }

    // 성공 상태: 인물 정보 바인딩
    private fun showContent(state: PersonDetailUiState.Success) {
        binding.progressBar.isVisible = false
        binding.contentLayout.isVisible = true
        binding.errorView.layoutError.isVisible = false

        bindPersonDetail(state.person)

        filmographyAdapter.submitList(state.movies)
        binding.tvFilmographyLabel.isVisible = state.movies.isNotEmpty()
        binding.rvFilmography.isVisible = state.movies.isNotEmpty()
    }

    // 인물 기본 정보 바인딩
    private fun bindPersonDetail(person: PersonDetail) {
        binding.collapsingToolbar.title = person.name
        binding.tvName.text = person.name

        binding.ivProfile.load(ImageUrlProvider.profileUrl(person.profilePath)) {
            crossfade(true)
            placeholder(R.drawable.bg_poster_placeholder)
            error(R.drawable.bg_poster_placeholder)
        }

        if (person.knownForDepartment.isNotBlank()) {
            binding.tvKnownFor.text = getString(R.string.person_known_for, person.knownForDepartment)
            binding.tvKnownFor.isVisible = true
        } else {
            binding.tvKnownFor.isVisible = false
        }

        if (!person.birthday.isNullOrBlank()) {
            binding.tvBirthday.text = getString(R.string.person_birthday_format, person.birthday)
            binding.tvBirthday.isVisible = true
        } else {
            binding.tvBirthday.isVisible = false
        }

        if (!person.placeOfBirth.isNullOrBlank()) {
            binding.tvBirthplace.text = getString(R.string.person_birthplace_format, person.placeOfBirth)
            binding.tvBirthplace.isVisible = true
        } else {
            binding.tvBirthplace.isVisible = false
        }

        val biography = person.biography.takeIf { it.isNotBlank() }
            ?: getString(R.string.person_no_biography)
        binding.tvBiography.text = biography
    }

    // 에러 상태 표시
    private fun showError(state: PersonDetailUiState.Error) {
        binding.progressBar.isVisible = false
        binding.contentLayout.isVisible = false
        binding.errorView.layoutError.isVisible = true
        binding.errorView.btnRetry.isEnabled = true

        binding.errorView.tvErrorMessage.text =
            ErrorMessageProvider.getMessage(requireContext(), state.errorType)
        binding.errorView.btnRetry.setOnClickListener {
            binding.errorView.btnRetry.isEnabled = false
            viewModel.loadPersonDetail()
        }
    }

    // 어댑터 해제 및 바인딩 null 처리
    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvFilmography.adapter = null
        _binding = null
    }
}
