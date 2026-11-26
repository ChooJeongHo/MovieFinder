package com.choo.moviefinder.data.repository

import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.choo.moviefinder.data.local.MovieDatabase
import com.choo.moviefinder.domain.model.BackupWatchHistory
import com.choo.moviefinder.domain.model.Movie
import com.choo.moviefinder.domain.model.UserDataBackup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// 백업 복원 뒤의 장르 행·장르 통계가 평소 저장 경로와 일치하는지 실제 Room 메모리 DB로 확인한다
@RunWith(AndroidJUnit4::class)
class BackupRestoreGenreTest {

    private val db = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        MovieDatabase::class.java
    ).build()

    // 영화 3건, 장르 토큰 6개(2 + 1 + 3)
    private val samples = listOf(101 to "Action,Adventure", 102 to "Action", 103 to "Drama,Adventure,Action")

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun normalSavePath_createsGenreRows() = runBlocking<Unit> {
        val repository = WatchHistoryRepositoryImpl(db.watchHistoryDao())

        samples.forEach { (movieId, genres) ->
            repository.saveWatchHistoryWithGenres(
                Movie(
                    id = movieId, title = "Movie $movieId", posterPath = null, backdropPath = null,
                    overview = "", releaseDate = "", voteAverage = 0.0, voteCount = 0
                ),
                genres
            )
        }

        assertEquals(6, countRows("watch_history_genre"))
    }

    @Test
    fun restore_createsGenreRowsLikeNormalSave() = runBlocking<Unit> {
        val repository = BackupRepositoryImpl(
            db, db.favoriteMovieDao(), db.watchlistDao(), db.userRatingDao(),
            db.memoDao(), db.movieTagDao(), db.watchHistoryDao()
        )
        val backup = UserDataBackup(
            watchHistory = samples.map { (movieId, genres) ->
                BackupWatchHistory(
                    movieId = movieId, title = "Movie $movieId",
                    watchedAt = 1_700_000_000_000L + movieId, genres = genres
                )
            }
        )

        repository.importUserData(backup)
        val genreRowsAfterOnce = countRows("watch_history_genre")
        val genreCountsAfterOnce = genreCountsText()
        log("history_rows_after_1", countRows("watch_history"))
        log("genre_rows_after_1", genreRowsAfterOnce)
        log("genre_counts_after_1", genreCountsAfterOnce)
        val linkAfterOnce = genreLinksText()
        log("genre_links_after_1", linkAfterOnce)

        // 같은 백업을 한 번 더 가져온 결과는 출력만 하고 단언하지 않는다(중복 가져오기 동작은 이 테스트의 범위 밖)
        repository.importUserData(backup)
        log("history_rows_after_2", countRows("watch_history"))
        log("genre_rows_after_2", countRows("watch_history_genre"))

        assertEquals(6, genreRowsAfterOnce)
        assertEquals("Action:3;Adventure:2;Drama:1", genreCountsAfterOnce)
        assertEquals("101:Action;101:Adventure;102:Action;103:Action;103:Adventure;103:Drama", linkAfterOnce)
    }

    private fun countRows(table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private suspend fun genreCountsText(): String =
        db.watchHistoryDao().getGenreCounts().first().joinToString(";") { "${it.genre}:${it.count}" }

    // 어느 영화의 시청 기록에 어느 장르가 붙었는지 "movieId:장르" 목록으로 만든다
    private fun genreLinksText(): String =
        db.openHelper.readableDatabase.query(
            "SELECT h.movieId, g.genre_name FROM watch_history_genre g " +
                "INNER JOIN watch_history h ON h.rowId = g.watch_history_id ORDER BY h.movieId, g.genre_name"
        ).use { cursor ->
            generateSequence { if (cursor.moveToNext()) "${cursor.getInt(0)}:${cursor.getString(1)}" else null }
                .joinToString(";")
        }

    private fun log(key: String, value: Any) {
        Log.i("BackupRestoreGenreTest", "RESULT $key=$value")
    }
}
