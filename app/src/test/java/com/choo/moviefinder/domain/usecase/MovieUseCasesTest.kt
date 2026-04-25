package com.choo.moviefinder.domain.usecase

import androidx.paging.PagingData
import com.choo.moviefinder.domain.model.Cast
import com.choo.moviefinder.domain.model.Credits
import com.choo.moviefinder.domain.model.Genre
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.MovieDetail
import com.choo.moviefinder.domain.model.Review
import com.choo.moviefinder.domain.repository.MovieRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MovieUseCasesTest {

    private lateinit var movieRepository: MovieRepository

    private val testMovie = Movie(
        id = 1,
        title = "Test Movie",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        overview = "Overview",
        releaseDate = "2024-01-01",
        voteAverage = 7.5,
        voteCount = 1000
    )

    private val testMovieDetail = MovieDetail(
        id = 1,
        title = "Test Movie",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        overview = "Overview",
        releaseDate = "2024-01-01",
        voteAverage = 7.5,
        voteCount = 1000,
        runtime = 120,
        genres = listOf(Genre(28, "Action")),
        tagline = "Test tagline"
    )

    @Before
    fun setUp() {
        movieRepository = mockk()
    }

    // --- GetNowPlayingMoviesUseCase ---

    @Test
    fun `GetNowPlayingMoviesUseCase delegates to repository`() {
        val flow = mockk<Flow<PagingData<Movie>>>()
        every { movieRepository.getNowPlayingMovies() } returns flow
        val useCase = GetNowPlayingMoviesUseCase(movieRepository)

        val result = useCase()

        verify(exactly = 1) { movieRepository.getNowPlayingMovies() }
        assertEquals(flow, result)
    }

    // --- GetPopularMoviesUseCase ---

    @Test
    fun `GetPopularMoviesUseCase delegates to repository`() {
        val flow = mockk<Flow<PagingData<Movie>>>()
        every { movieRepository.getPopularMovies() } returns flow
        val useCase = GetPopularMoviesUseCase(movieRepository)

        val result = useCase()

        verify(exactly = 1) { movieRepository.getPopularMovies() }
        assertEquals(flow, result)
    }

    // --- GetTrendingMoviesUseCase ---

    @Test
    fun `GetTrendingMoviesUseCase delegates to repository`() {
        val flow = mockk<Flow<PagingData<Movie>>>()
        every { movieRepository.getTrendingMovies() } returns flow
        val useCase = GetTrendingMoviesUseCase(movieRepository)

        val result = useCase()

        verify(exactly = 1) { movieRepository.getTrendingMovies() }
        assertEquals(flow, result)
    }

    // --- SearchMoviesUseCase ---

    @Test
    fun `SearchMoviesUseCase passes query and year to repository`() {
        val flow = mockk<Flow<PagingData<Movie>>>()
        every { movieRepository.searchMovies("avengers", 2023) } returns flow
        val useCase = SearchMoviesUseCase(movieRepository)

        val result = useCase("avengers", 2023)

        verify(exactly = 1) { movieRepository.searchMovies("avengers", 2023) }
        assertEquals(flow, result)
    }

    @Test
    fun `SearchMoviesUseCase passes null year when not provided`() {
        val flow = mockk<Flow<PagingData<Movie>>>()
        every { movieRepository.searchMovies("batman", null) } returns flow
        val useCase = SearchMoviesUseCase(movieRepository)

        useCase("batman", null)

        verify(exactly = 1) { movieRepository.searchMovies("batman", null) }
    }

    // --- DiscoverMoviesUseCase ---

    @Test
    fun `DiscoverMoviesUseCase passes genres sortBy and year to repository`() {
        val flow = mockk<Flow<PagingData<Movie>>>()
        val genres = setOf(28, 12)
        every { movieRepository.discoverMovies(genres, "vote_average.desc", 2022) } returns flow
        val useCase = DiscoverMoviesUseCase(movieRepository)

        val result = useCase(genres, "vote_average.desc", 2022)

        verify(exactly = 1) { movieRepository.discoverMovies(genres, "vote_average.desc", 2022) }
        assertEquals(flow, result)
    }

    @Test
    fun `DiscoverMoviesUseCase uses default parameters when invoked without args`() {
        val flow = mockk<Flow<PagingData<Movie>>>()
        every { movieRepository.discoverMovies(emptySet(), "popularity.desc", null) } returns flow
        val useCase = DiscoverMoviesUseCase(movieRepository)

        useCase()

        verify(exactly = 1) { movieRepository.discoverMovies(emptySet(), "popularity.desc", null) }
    }

    // --- GetGenreListUseCase ---

    @Test
    fun `GetGenreListUseCase returns genre list from repository`() = runTest {
        val genres = listOf(Genre(28, "Action"), Genre(35, "Comedy"))
        coEvery { movieRepository.getGenreList() } returns genres
        val useCase = GetGenreListUseCase(movieRepository)

        val result = useCase()

        assertEquals(genres, result)
    }

    @Test
    fun `GetGenreListUseCase calls repository once`() = runTest {
        coEvery { movieRepository.getGenreList() } returns emptyList()
        val useCase = GetGenreListUseCase(movieRepository)

        useCase()

        coVerify(exactly = 1) { movieRepository.getGenreList() }
    }

    // --- GetMovieDetailUseCase ---

    @Test
    fun `GetMovieDetailUseCase returns movie detail from repository`() = runTest {
        coEvery { movieRepository.getMovieDetail(1) } returns testMovieDetail
        val useCase = GetMovieDetailUseCase(movieRepository)

        val result = useCase(1)

        assertEquals(testMovieDetail, result)
    }

    @Test
    fun `GetMovieDetailUseCase passes correct movieId to repository`() = runTest {
        coEvery { movieRepository.getMovieDetail(42) } returns testMovieDetail
        val useCase = GetMovieDetailUseCase(movieRepository)

        useCase(42)

        coVerify(exactly = 1) { movieRepository.getMovieDetail(42) }
    }

    // --- GetMovieCreditsUseCase ---

    @Test
    fun `GetMovieCreditsUseCase returns cast list from repository`() = runTest {
        val cast = listOf(Cast(1, "Actor Name", "Character", "/profile.jpg"))
        val credits = Credits(cast = cast, directors = emptyList())
        coEvery { movieRepository.getMovieCredits(1) } returns credits
        val useCase = GetMovieCreditsUseCase(movieRepository)

        val result = useCase(1)

        assertEquals(credits, result)
    }

    @Test
    fun `GetMovieCreditsUseCase passes correct movieId`() = runTest {
        coEvery { movieRepository.getMovieCredits(7) } returns Credits(cast = emptyList(), directors = emptyList())
        val useCase = GetMovieCreditsUseCase(movieRepository)

        useCase(7)

        coVerify(exactly = 1) { movieRepository.getMovieCredits(7) }
    }

    // --- GetSimilarMoviesUseCase ---

    @Test
    fun `GetSimilarMoviesUseCase returns similar movies from repository`() = runTest {
        coEvery { movieRepository.getSimilarMovies(1) } returns listOf(testMovie)
        val useCase = GetSimilarMoviesUseCase(movieRepository)

        val result = useCase(1)

        assertEquals(listOf(testMovie), result)
    }

    @Test
    fun `GetSimilarMoviesUseCase passes correct movieId`() = runTest {
        coEvery { movieRepository.getSimilarMovies(5) } returns emptyList()
        val useCase = GetSimilarMoviesUseCase(movieRepository)

        useCase(5)

        coVerify(exactly = 1) { movieRepository.getSimilarMovies(5) }
    }

    // --- GetMovieTrailerUseCase ---

    @Test
    fun `GetMovieTrailerUseCase returns trailer key from repository`() = runTest {
        coEvery { movieRepository.getMovieTrailerKey(1) } returns "abc123"
        val useCase = GetMovieTrailerUseCase(movieRepository)

        val result = useCase(1)

        assertEquals("abc123", result)
    }

    @Test
    fun `GetMovieTrailerUseCase returns null when no trailer`() = runTest {
        coEvery { movieRepository.getMovieTrailerKey(2) } returns null
        val useCase = GetMovieTrailerUseCase(movieRepository)

        val result = useCase(2)

        assertNull(result)
    }

    // --- GetMovieCertificationUseCase ---

    @Test
    fun `GetMovieCertificationUseCase returns certification from repository`() = runTest {
        coEvery { movieRepository.getMovieCertification(1) } returns "PG-13"
        val useCase = GetMovieCertificationUseCase(movieRepository)

        val result = useCase(1)

        assertEquals("PG-13", result)
    }

    @Test
    fun `GetMovieCertificationUseCase returns null when certification unavailable`() = runTest {
        coEvery { movieRepository.getMovieCertification(3) } returns null
        val useCase = GetMovieCertificationUseCase(movieRepository)

        val result = useCase(3)

        assertNull(result)
    }

    // --- GetMovieReviewsUseCase ---

    @Test
    fun `GetMovieReviewsUseCase returns reviews from repository`() = runTest {
        val reviews = listOf(
            Review("r1", "Author", null, 8.0, "Great movie!", "2024-01-01")
        )
        coEvery { movieRepository.getMovieReviews(1) } returns reviews
        val useCase = GetMovieReviewsUseCase(movieRepository)

        val result = useCase(1)

        assertEquals(reviews, result)
    }

    @Test
    fun `GetMovieReviewsUseCase passes correct movieId`() = runTest {
        coEvery { movieRepository.getMovieReviews(9) } returns emptyList()
        val useCase = GetMovieReviewsUseCase(movieRepository)

        useCase(9)

        coVerify(exactly = 1) { movieRepository.getMovieReviews(9) }
    }

    // --- GetMovieRecommendationsUseCase ---

    @Test
    fun `GetMovieRecommendationsUseCase returns recommended movies from repository`() = runTest {
        coEvery { movieRepository.getMovieRecommendations(1) } returns listOf(testMovie)
        val useCase = GetMovieRecommendationsUseCase(movieRepository)

        val result = useCase(1)

        assertEquals(listOf(testMovie), result)
    }

    @Test
    fun `GetMovieRecommendationsUseCase passes correct movieId`() = runTest {
        coEvery { movieRepository.getMovieRecommendations(11) } returns emptyList()
        val useCase = GetMovieRecommendationsUseCase(movieRepository)

        useCase(11)

        coVerify(exactly = 1) { movieRepository.getMovieRecommendations(11) }
    }
}
