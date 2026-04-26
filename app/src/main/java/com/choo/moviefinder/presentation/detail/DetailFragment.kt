package com.choo.moviefinder.presentation.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.view.inputmethod.InputMethodManager
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.choo.moviefinder.domain.model.MemoConstants
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.presentation.adapter.CastAdapter
import com.choo.moviefinder.presentation.adapter.HorizontalMovieAdapter
import com.choo.moviefinder.presentation.adapter.MemoAdapter
import com.choo.moviefinder.presentation.adapter.ReviewAdapter
import com.choo.moviefinder.presentation.collection.CollectionFragmentArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Suppress("TooManyFunctions")
@AndroidEntryPoint
class DetailFragment : Fragment() {

    private val retryRateLimiter = RateLimiter()

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
        setupMemoInput()
        observeViewModelFlows()

        // edge-to-edge: 하단 네비게이션 바 인셋 처리 (상단은 fitsSystemWindows로 처리됨)
        ViewCompat.setOnApplyWindowInsetsListener(binding.nestedScroll) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = navBar.bottom)
            insets
        }
        val favOriginalBottom =
            (binding.fabFavorite.layoutParams as? android.view.ViewGroup.MarginLayoutParams)
                ?.bottomMargin ?: 0
        val watchOriginalBottom =
            (binding.fabWatchlist.layoutParams as? android.view.ViewGroup.MarginLayoutParams)
                ?.bottomMargin ?: 0
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabFavorite) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            (v.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.bottomMargin =
                favOriginalBottom + navBar.bottom
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabWatchlist) { v, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            (v.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.bottomMargin =
                watchOriginalBottom + navBar.bottom
            insets
        }
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
        val year = detail.releaseDate.take(4).takeIf { it.length == 4 }
        val shareText = buildString {
            append(detail.title)
            if (year != null) append(" ($year)")
            append("\n")
            if (detail.voteAverage > 0) {
                append("⭐ ${"%.1f".format(detail.voteAverage)}/10")
                if (state.certification != null) append("  |  ${state.certification}")
                append("\n")
            }
            if (detail.genres.isNotEmpty()) {
                append(detail.genres.joinToString(" · ") { it.name })
                append("\n")
            }
            if ((detail.runtime ?: 0) > 0) {
                append(getString(R.string.runtime_format, detail.runtime))
                append("\n")
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
            putExtra(Intent.EXTRA_SUBJECT, detail.title)
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
        binding.rvCast.setupHorizontal(castAdapter)

        // self-navigation 공통 핸들러 (비슷한 영화 / 추천 영화 공유)
        val onDetailSelfNavigate: (Int) -> Unit = { movieId ->
            if (findNavController().currentDestination?.id == R.id.detailFragment) {
                findNavController().navigate(
                    R.id.action_detailFragment_self,
                    DetailFragmentArgs(movieId).toBundle()
                )
            }
        }

        similarMovieAdapter = HorizontalMovieAdapter(onMovieClick = onDetailSelfNavigate)
        binding.rvSimilar.setupHorizontal(similarMovieAdapter)

        recommendationAdapter = HorizontalMovieAdapter(onMovieClick = onDetailSelfNavigate)
        binding.rvRecommendations.setupHorizontal(recommendationAdapter)

        reviewAdapter = ReviewAdapter()
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
            setHasFixedSize(true)
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
            itemAnimator = null
            setHasFixedSize(true)
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
        binding.btnSubmitTmdbRating.setOnClickListener { showTmdbRatingDialog() }
    }

    private fun showTmdbRatingDialog() {
        val ratingBar = RatingBar(requireContext()).apply {
            numStars = 5
            stepSize = 0.5f
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.tmdb_rating_dialog_title))
            .setView(ratingBar)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val rating = ratingBar.rating * 2f // TMDB: 1–10, RatingBar: 0.5–5
                if (rating > 0f) viewModel.submitTmdbRating(rating)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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

    private fun observeViewModelFlows() {
        setupRatingListeners()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { handleUiState(it) } }
                launch { viewModel.isFavorite.collect { updateFavoriteFab(it) } }
                launch { viewModel.isInWatchlist.collect { updateWatchlistFab(it) } }
                launch { viewModel.userRating.collect { updateRatingBar(it) } }
                launch { viewModel.snackbarEvent.collect { showFlowSnackbar(it) } }
                launch { viewModel.memos.collect { memoAdapter.submitList(it) } }
                launch { viewModel.isTmdbConnected.collect { binding.btnSubmitTmdbRating.isVisible = it } }
                launch {
                    viewModel.tmdbRatingResult.collect { success ->
                        val msg = if (success) R.string.tmdb_rating_success else R.string.tmdb_rating_failed
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupRatingListeners() {
        binding.ratingBarUser.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { _, rating, fromUser ->
                if (fromUser && rating > 0f) viewModel.setUserRating(rating)
            }
        binding.btnClearRating.setOnClickListener { viewModel.deleteUserRating() }
    }

    private fun handleUiState(state: DetailUiState) {
        when (state) {
            is DetailUiState.Loading -> showLoading()
            is DetailUiState.Success -> showContent(state)
            is DetailUiState.Error -> showError(state.errorType)
        }
    }

    private fun updateFavoriteFab(isFavorite: Boolean) {
        binding.fabFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )
        binding.fabFavorite.contentDescription = getString(
            if (isFavorite) R.string.cd_fab_remove_favorite else R.string.cd_fab_add_favorite
        )
    }

    private fun updateWatchlistFab(inWatchlist: Boolean) {
        binding.fabWatchlist.setImageResource(
            if (inWatchlist) R.drawable.ic_watchlist else R.drawable.ic_watchlist_border
        )
        binding.fabWatchlist.contentDescription = getString(
            if (inWatchlist) R.string.cd_fab_remove_watchlist else R.string.cd_fab_add_watchlist
        )
    }

    private fun updateRatingBar(rating: Float?) {
        binding.ratingBarUser.rating = rating ?: 0f
        binding.btnClearRating.isVisible = rating != null
    }

    private fun showFlowSnackbar(errorType: ErrorType) {
        val message = ErrorMessageProvider.getMessage(requireContext(), errorType)
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    // 로딩 상태 표시 (Shimmer 표시, 콘텐츠/FAB 숨김)
    private fun showLoading() {
        binding.shimmerDetail.shimmerLayout.isVisible = true
        binding.shimmerDetail.shimmerLayout.startShimmer()
        binding.contentLayout.isVisible = false
        binding.errorView.layoutError.isVisible = false
        binding.fabFavorite.isVisible = false
        binding.fabWatchlist.isVisible = false
    }

    // 성공 상태 표시 (영화 상세, 출연진, 비슷한 영화, 리뷰, 등급 바인딩)
    private fun showContent(state: DetailUiState.Success) {
        binding.shimmerDetail.shimmerLayout.stopShimmer()
        binding.shimmerDetail.shimmerLayout.isVisible = false
        binding.contentLayout.isVisible = true
        binding.errorView.layoutError.isVisible = false
        binding.fabFavorite.isVisible = true
        binding.fabWatchlist.isVisible = true

        val detail = state.movieDetail
        bindMovieDetail(detail)
        bindTrailer(state.trailerKey)

        // 감독
        val directors = state.credits?.directors.orEmpty()
        binding.tvDirectorLabel.isVisible = directors.isNotEmpty()
        binding.tvDirectorValue.isVisible = directors.isNotEmpty()
        binding.tvDirectorValue.text = directors.joinToString(", ")

        // 스트리밍 제공 정보
        bindWatchProviders(state.watchProviders)

        // Cast (null = 로딩 중 → 숨김, 빈 리스트 = 데이터 없음 → 숨김, 데이터 있음 → 표시)
        castAdapter.submitList(state.credits?.cast.orEmpty())
        bindOptionalSection(!state.credits?.cast.isNullOrEmpty(), binding.tvCastLabel, binding.rvCast)

        // 비슷한 영화
        similarMovieAdapter.submitList(state.similarMovies.orEmpty())
        bindOptionalSection(!state.similarMovies.isNullOrEmpty(), binding.tvSimilarLabel, binding.rvSimilar)

        // 추천 영화
        recommendationAdapter.submitList(state.recommendations.orEmpty())
        bindOptionalSection(
            !state.recommendations.isNullOrEmpty(), binding.tvRecommendationsLabel, binding.rvRecommendations
        )

        // 리뷰
        reviewAdapter.submitList(state.reviews.orEmpty())
        bindOptionalSection(!state.reviews.isNullOrEmpty(), binding.tvReviewsLabel, binding.rvReviews)

        // 등급 배지
        bindCertification(state.certification)
    }

    // 영화 기본 정보 (제목, 배경, 평점, 개봉일, 장르 칩 등) 바인딩
    private fun bindMovieDetail(detail: MovieDetail) {
        binding.collapsingToolbar.title = detail.title

        binding.ivBackdrop.load(ImageUrlProvider.backdropUrl(detail.backdropPath)) {
            crossfade(true)
            size(coil3.size.ViewSizeResolver(binding.ivBackdrop))
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

        val hasRuntime = detail.runtime != null && detail.runtime > 0
        binding.tvRuntime.isVisible = hasRuntime
        if (hasRuntime) binding.tvRuntime.text = getString(R.string.runtime_format, detail.runtime)

        val hasTagline = !detail.tagline.isNullOrBlank()
        binding.tvTagline.isVisible = hasTagline
        if (hasTagline) binding.tvTagline.text = detail.tagline

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

        // 시리즈 보기 버튼
        val collectionId = detail.belongsToCollectionId
        val collectionName = detail.belongsToCollectionName
        binding.btnViewCollection.isVisible = collectionId != null
        if (collectionId != null) {
            binding.btnViewCollection.text = collectionName ?: getString(R.string.collection_view_series)
            binding.btnViewCollection.setOnClickListener {
                if (findNavController().currentDestination?.id == R.id.detailFragment) {
                    findNavController().navigate(
                        R.id.action_detailFragment_to_collectionFragment,
                        CollectionFragmentArgs(collectionId).toBundle()
                    )
                }
            }
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

    // 스트리밍 제공 정보 섹션 바인딩 (로고 이미지 동적 추가)
    private fun bindWatchProviders(providers: List<com.choo.moviefinder.domain.model.WatchProvider>?) {
        val hasProviders = !providers.isNullOrEmpty()
        bindOptionalSection(hasProviders, binding.tvStreamingLabel, binding.scrollStreaming)
        if (!hasProviders) return
        binding.llStreamingProviders.removeAllViews()
        val sizePx = (56 * resources.displayMetrics.density).toInt()
        val marginPx = (8 * resources.displayMetrics.density).toInt()
        providers.orEmpty().forEach { provider ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    marginEnd = marginPx
                }
                contentDescription = provider.providerName
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            imageView.load(ImageUrlProvider.profileUrl(provider.logoPath)) {
                crossfade(true)
                size(sizePx, sizePx)
                placeholder(R.drawable.bg_poster_placeholder)
                error(R.drawable.bg_poster_placeholder)
            }
            binding.llStreamingProviders.addView(imageView)
        }
    }

    // 가로 스크롤 RecyclerView 공통 설정 헬퍼
    private fun RecyclerView.setupHorizontal(rvAdapter: RecyclerView.Adapter<*>) {
        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        setHasFixedSize(true)
        itemAnimator = null
        adapter = rvAdapter
    }

    // 조건부 텍스트 필드 표시/숨김 헬퍼
    private fun bindOptionalField(textView: TextView, show: Boolean, text: String) {
        textView.isVisible = show
        if (show) textView.text = text
    }

    // 선택적 섹션(라벨 + RecyclerView) 표시/숨김 헬퍼
    private fun bindOptionalSection(hasItems: Boolean, label: View, recyclerView: View) {
        label.isVisible = hasItems
        recyclerView.isVisible = hasItems
    }

    // 콘텐츠 등급 배지 칩 표시 (KR/US 등급)
    private fun bindCertification(certification: String?) {
        val hasCert = !certification.isNullOrBlank()
        binding.chipCertification.isVisible = hasCert
        if (hasCert) {
            binding.chipCertification.text = certification
            binding.chipCertification.contentDescription =
                getString(R.string.cd_certification, certification)
        }
    }

    // 예고편 인앱 재생 (YouTubePlayerView) 또는 외부 YouTube 연결 (fallback)
    private fun bindTrailer(trailerKey: String?) {
        binding.youtubePlayerView.isVisible = false
        if (trailerKey == null) {
            binding.btnTrailer.isVisible = false
            return
        }
        binding.btnTrailer.isVisible = true
        binding.btnTrailer.setOnClickListener { openYouTubeExternal(trailerKey) }
    }

    private fun openYouTubeExternal(trailerKey: String) {
        val appUri = "vnd.youtube:$trailerKey".toUri()
        val webUri = "https://www.youtube.com/watch?v=$trailerKey".toUri()
        try {
            startActivity(Intent(Intent.ACTION_VIEW, appUri))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    // 에러 상태 표시 (에러 메시지 및 재시도 버튼)
    private fun showError(errorType: ErrorType) {
        startPostponedEnterTransition()
        binding.shimmerDetail.shimmerLayout.stopShimmer()
        binding.shimmerDetail.shimmerLayout.isVisible = false
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
        binding.etMemo.filters = arrayOf(InputFilter.LengthFilter(MemoConstants.MAX_LENGTH))
        binding.tilMemo.setEndIconOnClickListener {
            val content = binding.etMemo.text?.toString()?.trim().orEmpty()
            if (content.isNotBlank()) {
                viewModel.saveMemo(content)
                binding.etMemo.text?.clear()
                binding.etMemo.clearFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.etMemo.windowToken, 0)
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
            filters = arrayOf(InputFilter.LengthFilter(MemoConstants.MAX_LENGTH))
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.memo_edit_dialog_title)
            .setView(editText)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val newContent = editText.text?.toString()?.trim().orEmpty()
                if (newContent.isNotBlank()) {
                    viewModel.updateMemo(memo.id, newContent)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()
        dialog.setOnDismissListener {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val token = dialog.window?.decorView?.windowToken
            if (token != null) imm.hideSoftInputFromWindow(token, 0)
        }
        memoEditDialog = dialog
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
        binding.youtubePlayerView.release()
        _binding = null
    }

}
