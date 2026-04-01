package com.choo.moviefinder.presentation.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.ChangeImageTransform
import android.transition.ChangeTransform
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.InputFilter
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
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
import com.choo.moviefinder.core.util.ErrorType
import com.choo.moviefinder.core.util.ImageUrlProvider
import com.choo.moviefinder.core.util.RateLimiter
import com.choo.moviefinder.databinding.FragmentDetailBinding
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.presentation.adapter.CastAdapter
import com.choo.moviefinder.presentation.adapter.HorizontalMovieAdapter
import com.choo.moviefinder.presentation.adapter.MemoAdapter
import com.choo.moviefinder.presentation.adapter.ReviewAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions")
@AndroidEntryPoint
class DetailFragment : Fragment() {

    private val retryRateLimiter = RateLimiter(2_000L)

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailViewModel by viewModels()

    private val args: DetailFragmentArgs by navArgs()

    private lateinit var castAdapter: CastAdapter
    private lateinit var similarMovieAdapter: HorizontalMovieAdapter
    private lateinit var recommendationAdapter: HorizontalMovieAdapter
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var memoAdapter: MemoAdapter
    private var memoEditDialog: android.app.Dialog? = null

    // Shared Element Transition 애니메이션 설정 (ChangeBounds + ChangeTransform + ChangeImageTransform)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedTransition = TransitionSet().apply {
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
            addTransition(ChangeImageTransform())
            duration = 300
        }
        sharedElementEnterTransition = sharedTransition
        sharedElementReturnTransition = sharedTransition
    }

    // 상세 화면 레이아웃을 인플레이트하고 바인딩 초기화
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 뷰 생성 후 툴바, RecyclerView, FAB, 상태 관찰 등 UI 초기화
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Shared Element Transition: 포스터 → 배경 이미지 전환
        ViewCompat.setTransitionName(binding.ivBackdrop, "poster_${args.movieId}")
        postponeEnterTransition(500, TimeUnit.MILLISECONDS)

        setupToolbar()
        setupRecyclerViews()
        setupFab()
        observeUiState()
        observeFavorite()
        observeWatchlist()
        observeUserRating()
        observeSnackbar()
        setupMemoInput()
        observeMemos()
    }

    // 툴바 뒤로가기 및 공유 메뉴 설정
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbar.inflateMenu(R.menu.menu_detail)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> {
                    shareMovie()
                    true
                }
                else -> false
            }
        }
    }

    // 영화 정보를 텍스트로 공유 (Intent.ACTION_SEND)
    private fun shareMovie() {
        val state = viewModel.uiState.value
        if (state !is DetailUiState.Success) return
        val detail = state.movieDetail
        val shareText = buildString {
            append(detail.title)
            if (detail.releaseDate.isNotBlank()) {
                append(" (${detail.releaseDate})")
            }
            append("\n")
            if (detail.overview.isNotBlank()) {
                append(detail.overview)
                append("\n\n")
            }
            append("https://www.themoviedb.org/movie/${detail.id}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)))
    }

    // 출연진, 비슷한 영화, 추천 영화, 리뷰 RecyclerView 초기화
    private fun setupRecyclerViews() {
        castAdapter = CastAdapter { personId ->
            findNavController().navigate(
                R.id.action_detailFragment_to_personDetailFragment,
                com.choo.moviefinder.presentation.person.PersonDetailFragmentArgs(personId).toBundle()
            )
        }
        binding.rvCast.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = castAdapter
        }

        // 비슷한 영화 클릭 시 같은 DetailFragment를 새 인스턴스로 재생성 (self-navigation)
        similarMovieAdapter = HorizontalMovieAdapter { movieId ->
            if (findNavController().currentDestination?.id == R.id.detailFragment) {
                findNavController().navigate(
                    R.id.action_detailFragment_self,
                    DetailFragmentArgs(movieId).toBundle()
                )
            }
        }
        binding.rvSimilar.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = similarMovieAdapter
        }

        // 추천 영화 클릭 시 같은 DetailFragment를 새 인스턴스로 재생성 (self-navigation)
        recommendationAdapter = HorizontalMovieAdapter { movieId ->
            if (findNavController().currentDestination?.id == R.id.detailFragment) {
                findNavController().navigate(
                    R.id.action_detailFragment_self,
                    DetailFragmentArgs(movieId).toBundle()
                )
            }
        }
        binding.rvRecommendations.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendationAdapter
        }

        reviewAdapter = ReviewAdapter()
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewAdapter
        }

        memoAdapter = MemoAdapter(
            onEditClick = { memo -> showEditMemoDialog(memo) },
            onDeleteClick = { memo ->
                viewModel.deleteMemo(memo.id)
                Snackbar.make(binding.root, R.string.memo_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) {
                        viewModel.saveMemo(memo.content)
                    }
                    .show()
            }
        )
        binding.rvMemos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memoAdapter
        }
    }

    // 즐겨찾기 및 워치리스트 FAB 클릭 리스너 설정
    private fun setupFab() {
        binding.fabFavorite.setOnClickListener {
            animateFabBounce(binding.fabFavorite)
            viewModel.toggleFavorite()
        }
        binding.fabWatchlist.setOnClickListener {
            animateFabBounce(binding.fabWatchlist)
            viewModel.toggleWatchlist()
        }
    }

    // FAB 토글 시 scale 바운스 애니메이션 실행
    private fun animateFabBounce(fab: View) {
        fab.animate().cancel()
        fab.scaleX = 1f
        fab.scaleY = 1f
        fab.animate()
            .scaleX(1.2f).scaleY(1.2f)
            .setDuration(150)
            .withEndAction {
                fab.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    // UI 상태(Loading/Success/Error) Flow를 수집하여 화면 전환
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DetailUiState.Loading -> showLoading()
                        is DetailUiState.Success -> showContent(state)
                        is DetailUiState.Error -> showError(state.errorType)
                    }
                }
            }
        }
    }

    // 즐겨찾기 상태를 수집하여 FAB 아이콘 갱신
    private fun observeFavorite() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isFavorite.collect { isFavorite ->
                    val icon = if (isFavorite) {
                        R.drawable.ic_favorite
                    } else {
                        R.drawable.ic_favorite_border
                    }
                    binding.fabFavorite.setImageResource(icon)
                    binding.fabFavorite.contentDescription = getString(
                        if (isFavorite) R.string.cd_fab_remove_favorite else R.string.cd_fab_add_favorite
                    )
                }
            }
        }
    }

    // 워치리스트 상태를 수집하여 FAB 아이콘 갱신
    private fun observeWatchlist() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isInWatchlist.collect { inWatchlist ->
                    val icon = if (inWatchlist) {
                        R.drawable.ic_watchlist
                    } else {
                        R.drawable.ic_watchlist_border
                    }
                    binding.fabWatchlist.setImageResource(icon)
                    binding.fabWatchlist.contentDescription = getString(
                        if (inWatchlist) R.string.cd_fab_remove_watchlist else R.string.cd_fab_add_watchlist
                    )
                }
            }
        }
    }

    // 사용자 평점 수집 및 RatingBar/삭제 버튼 리스너 설정
    private fun observeUserRating() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userRating.collect { rating ->
                    if (rating != null) {
                        binding.ratingBarUser.rating = rating
                        binding.btnClearRating.isVisible = true
                    } else {
                        binding.ratingBarUser.rating = 0f
                        binding.btnClearRating.isVisible = false
                    }
                }
            }
        }

        binding.ratingBarUser.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { _, rating, fromUser ->
                if (fromUser && rating > 0f) {
                    viewModel.setUserRating(rating)
                }
            }

        binding.btnClearRating.setOnClickListener {
            viewModel.deleteUserRating()
        }
    }

    // 에러 이벤트를 수집하여 Snackbar로 표시
    private fun observeSnackbar() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.snackbarEvent.collect { errorType ->
                    val message = ErrorMessageProvider.getMessage(requireContext(), errorType)
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 로딩 상태 표시 (ProgressBar 표시, 콘텐츠/FAB 숨김)
    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.contentLayout.isVisible = false
        binding.errorView.layoutError.isVisible = false
        binding.fabFavorite.isVisible = false
        binding.fabWatchlist.isVisible = false
    }

    // 성공 상태 표시 (영화 상세, 출연진, 비슷한 영화, 리뷰, 등급 바인딩)
    private fun showContent(state: DetailUiState.Success) {
        binding.progressBar.isVisible = false
        binding.contentLayout.isVisible = true
        binding.errorView.layoutError.isVisible = false
        binding.fabFavorite.isVisible = true
        binding.fabWatchlist.isVisible = true

        val detail = state.movieDetail
        bindMovieDetail(detail)
        bindTrailer(state.trailerKey)

        // Cast (self-navigation 시 이전 영화 데이터가 남지 않도록 항상 리스트를 갱신)
        castAdapter.submitList(state.credits)
        binding.tvCastLabel.isVisible = state.credits.isNotEmpty()
        binding.rvCast.isVisible = state.credits.isNotEmpty()

        // 비슷한 영화
        similarMovieAdapter.submitList(state.similarMovies)
        binding.tvSimilarLabel.isVisible = state.similarMovies.isNotEmpty()
        binding.rvSimilar.isVisible = state.similarMovies.isNotEmpty()

        // 추천 영화
        recommendationAdapter.submitList(state.recommendations)
        binding.tvRecommendationsLabel.isVisible = state.recommendations.isNotEmpty()
        binding.rvRecommendations.isVisible = state.recommendations.isNotEmpty()

        // 리뷰
        reviewAdapter.submitList(state.reviews)
        binding.tvReviewsLabel.isVisible = state.reviews.isNotEmpty()
        binding.rvReviews.isVisible = state.reviews.isNotEmpty()

        // 등급 배지
        bindCertification(state.certification)
    }

    // 영화 기본 정보 (제목, 배경, 평점, 개봉일, 장르 칩 등) 바인딩
    private fun bindMovieDetail(detail: MovieDetail) {
        binding.collapsingToolbar.title = detail.title

        binding.ivBackdrop.load(ImageUrlProvider.backdropUrl(detail.backdropPath)) {
            crossfade(true)
            placeholder(R.drawable.bg_poster_placeholder)
            error(R.drawable.bg_poster_placeholder)
            listener(
                onSuccess = { _, _ -> startPostponedEnterTransition() },
                onError = { _, _ -> startPostponedEnterTransition() }
            )
        }

        binding.tvTitle.text = detail.title
        binding.ratingView.setRating(detail.voteAverage)
        binding.tvReleaseDate.text = getString(R.string.release_date_format, detail.releaseDate)

        if (detail.runtime != null && detail.runtime > 0) {
            binding.tvRuntime.text = getString(R.string.runtime_format, detail.runtime)
            binding.tvRuntime.isVisible = true
        } else {
            binding.tvRuntime.isVisible = false
        }

        if (!detail.tagline.isNullOrBlank()) {
            binding.tvTagline.text = detail.tagline
            binding.tvTagline.isVisible = true
        } else {
            binding.tvTagline.isVisible = false
        }

        binding.tvOverview.text = detail.overview

        // 장르 칩 동적 추가
        binding.chipGroupGenres.removeAllViews()
        detail.genres.forEach { genre ->
            val chip = Chip(requireContext()).apply {
                text = genre.name
                isClickable = false
                isCheckable = false
            }
            binding.chipGroupGenres.addView(chip)
        }

        // 확장 정보
        bindExtendedInfo(detail)
    }

    // 확장 상세 정보 (제작비, 수익, 원어, 상태, IMDb 링크) 바인딩
    private fun bindExtendedInfo(detail: MovieDetail) {
        val hasInfo = detail.status.isNotBlank() || detail.originalLanguage.isNotBlank() ||
            detail.budget > 0 || detail.revenue > 0 || !detail.imdbId.isNullOrBlank()

        binding.tvMovieInfoLabel.isVisible = hasInfo
        binding.movieInfoSection.isVisible = hasInfo

        if (!hasInfo) return

        bindOptionalField(
            binding.tvStatus,
            detail.status.isNotBlank(),
            getString(R.string.status_format, detail.status)
        )
        bindOptionalField(
            binding.tvOriginalLanguage,
            detail.originalLanguage.isNotBlank(),
            getString(R.string.original_language_format, detail.originalLanguage.uppercase())
        )
        bindOptionalField(binding.tvBudget, detail.budget > 0, getString(R.string.budget_format, detail.budget))
        bindOptionalField(binding.tvRevenue, detail.revenue > 0, getString(R.string.revenue_format, detail.revenue))

        if (!detail.imdbId.isNullOrBlank()) {
            binding.btnImdb.isVisible = true
            binding.btnImdb.setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://www.imdb.com/title/${detail.imdbId}".toUri()
                )
                try {
                    startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    Snackbar.make(
                        binding.root,
                        R.string.error_no_browser,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            binding.btnImdb.isVisible = false
        }
    }

    // 조건부 텍스트 필드 표시/숨김 헬퍼
    private fun bindOptionalField(textView: TextView, show: Boolean, text: String) {
        textView.isVisible = show
        if (show) textView.text = text
    }

    // 콘텐츠 등급 배지 칩 표시 (KR/US 등급)
    private fun bindCertification(certification: String?) {
        if (!certification.isNullOrBlank()) {
            binding.chipCertification.text = certification
            binding.chipCertification.contentDescription =
                getString(R.string.cd_certification, certification)
            binding.chipCertification.isVisible = true
        } else {
            binding.chipCertification.isVisible = false
        }
    }

    // 예고편 버튼 표시 및 YouTube 연결 설정
    private fun bindTrailer(trailerKey: String?) {
        if (trailerKey != null) {
            binding.btnTrailer.isVisible = true
            binding.btnTrailer.setOnClickListener {
                // YouTube 앱 또는 웹으로 연결
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://www.youtube.com/watch?v=$trailerKey".toUri()
                )
                try {
                    startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    Snackbar.make(
                        binding.root,
                        R.string.error_no_browser,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            binding.btnTrailer.isVisible = false
        }
    }

    // 에러 상태 표시 (에러 메시지 및 재시도 버튼)
    private fun showError(errorType: ErrorType) {
        startPostponedEnterTransition()
        binding.progressBar.isVisible = false
        binding.contentLayout.isVisible = false
        binding.errorView.layoutError.isVisible = true
        binding.fabFavorite.isVisible = false
        binding.fabWatchlist.isVisible = false
        binding.errorView.btnRetry.isEnabled = true

        binding.errorView.tvErrorMessage.text =
            ErrorMessageProvider.getMessage(requireContext(), errorType)
        binding.errorView.btnRetry.setOnClickListener {
            if (retryRateLimiter.tryAcquire()) {
                binding.errorView.btnRetry.isEnabled = false
                viewModel.loadMovieDetail()
            }
        }
    }

    // 메모 입력 필드 및 저장 버튼 설정
    private fun setupMemoInput() {
        binding.etMemo.filters = arrayOf(InputFilter.LengthFilter(MAX_MEMO_LENGTH))
        binding.tilMemo.setEndIconOnClickListener {
            val content = binding.etMemo.text?.toString()?.trim().orEmpty()
            if (content.isNotBlank()) {
                viewModel.saveMemo(content)
                binding.etMemo.text?.clear()
            }
        }
    }

    // 메모 목록을 수집하여 RecyclerView 갱신
    private fun observeMemos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.memos.collect { memos ->
                    memoAdapter.submitList(memos)
                }
            }
        }
    }

    // 메모 수정 다이얼로그 표시
    private fun showEditMemoDialog(memo: com.choo.moviefinder.domain.model.Memo) {
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.dialog_padding_horizontal)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.dialog_padding_vertical)
        val editText = EditText(requireContext()).apply {
            setText(memo.content)
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 5
            filters = arrayOf(InputFilter.LengthFilter(MAX_MEMO_LENGTH))
        }
        memoEditDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.memo_edit_dialog_title)
            .setView(editText)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val newContent = editText.text?.toString()?.trim().orEmpty()
                if (newContent.isNotBlank()) {
                    viewModel.updateMemo(memo.id, newContent)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // 어댑터 해제 및 바인딩 null 처리로 메모리 누수 방지
    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvCast.adapter = null
        binding.rvSimilar.adapter = null
        binding.rvRecommendations.adapter = null
        binding.rvReviews.adapter = null
        memoEditDialog?.dismiss()
        memoEditDialog = null
        binding.rvMemos.adapter = null
        _binding = null
    }

    companion object {
        private const val MAX_MEMO_LENGTH = 500
    }
}
