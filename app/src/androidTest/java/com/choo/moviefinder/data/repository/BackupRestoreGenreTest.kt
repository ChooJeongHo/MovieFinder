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

    // S1: 빈 DB 에 같은 백업을 두 번 가져온다
    @Test
    fun s1_sameBackupImportedTwice_storesEachEntryOnce() = runBlocking<Unit> {
        val repository = backupRepository()
        val backup = UserDataBackup(watchHistory = sampleItems())

        repository.importUserData(backup)
        repository.importUserData(backup)
        logState("s1")

        assertEachSampleStoredOnce()
    }

    // S2: 평소 저장 경로로 3건을 저장하고, 그 DB 에서 내보낸 백업을 같은 DB 에 가져온다
    @Test
    fun s2_ownExportImportedOverExistingHistory_addsNothing() = runBlocking<Unit> {
        val historyRepository = WatchHistoryRepositoryImpl(db.watchHistoryDao())
        samples.forEach { (movieId, genres) ->
            historyRepository.saveWatchHistoryWithGenres(
                Movie(
                    id = movieId, title = "Movie $movieId", posterPath = null, backdropPath = null,
                    overview = "", releaseDate = "", voteAverage = 0.0, voteCount = 0
                ),
                genres
            )
        }
        val repository = backupRepository()
        val exported = repository.exportUserData()
        logState("s2_before")
        log("s2_export_entries", exported.watchHistory.size)

        repository.importUserData(exported)
        logState("s2")

        assertEquals(3, exported.watchHistory.size)
        assertEachSampleStoredOnce()
    }

    // S3: 101 한 건을 먼저 가져온 뒤, 101·102·103 이 든 백업을 가져온다
    @Test
    fun s3_partlyOverlappingBackup_addsOnlyMissingEntries() = runBlocking<Unit> {
        val repository = backupRepository()
        repository.importUserData(UserDataBackup(watchHistory = sampleItems().take(1)))

        repository.importUserData(UserDataBackup(watchHistory = sampleItems()))
        logState("s3")

        assertEachSampleStoredOnce()
    }

    // S4: 101 항목이 모든 필드까지 같게 두 번 든 백업을 빈 DB 에 가져온다
    @Test
    fun s4_repeatedEntryInsideBackup_isStoredOnce() = runBlocking<Unit> {
        val items = sampleItems()

        backupRepository().importUserData(UserDataBackup(watchHistory = items + items.first().copy()))
        logState("s4")

        assertEachSampleStoredOnce()
    }

    // S5: 101 을 가져온 뒤, 영화 ID 와 시청 시각은 같고 제목만 다른 101 항목을 가져온다
    @Test
    fun s5_sameMovieAndTimeWithOtherTitle_isStoredSeparately() = runBlocking<Unit> {
        val repository = backupRepository()
        val original = sampleItems().first()
        repository.importUserData(UserDataBackup(watchHistory = listOf(original)))

        repository.importUserData(UserDataBackup(watchHistory = listOf(original.copy(title = "Movie 101 other"))))
        logState("s5")
        log("s5_titles", titlesText())

        assertEquals(2, countRows("watch_history"))
        assertEquals(4, countRows("watch_history_genre"))
        assertEquals("Movie 101;Movie 101 other", titlesText())
    }

    private fun backupRepository() = BackupRepositoryImpl(
        db, db.favoriteMovieDao(), db.watchlistDao(), db.userRatingDao(),
        db.memoDao(), db.movieTagDao(), db.watchHistoryDao()
    )

    private fun sampleItems(): List<BackupWatchHistory> = samples.map { (movieId, genres) ->
        BackupWatchHistory(
            movieId = movieId, title = "Movie $movieId",
            watchedAt = 1_700_000_000_000L + movieId, genres = genres
        )
    }

    // 단언보다 먼저 현재 상태를 RESULT 줄로 남긴다
    private suspend fun logState(prefix: String) {
        log("${prefix}_history_rows", countRows("watch_history"))
        log("${prefix}_genre_rows", countRows("watch_history_genre"))
        log("${prefix}_history_per_movie", historyPerMovieText())
        log("${prefix}_genre_counts", genreCountsText())
    }

    // 표본 3건이 각각 한 번씩, 장르 연결까지 그대로 저장돼 있는지 확인한다
    private suspend fun assertEachSampleStoredOnce() {
        assertEquals(3, countRows("watch_history"))
        assertEquals(6, countRows("watch_history_genre"))
        assertEquals("101:1;102:1;103:1", historyPerMovieText())
        assertEquals("Action:3;Adventure:2;Drama:1", genreCountsText())
        assertEquals("101:Action;101:Adventure;102:Action;103:Action;103:Adventure;103:Drama", genreLinksText())
    }

    private fun historyPerMovieText(): String =
        db.openHelper.readableDatabase.query(
            "SELECT movieId, COUNT(*) FROM watch_history GROUP BY movieId ORDER BY movieId"
        ).use { cursor ->
            generateSequence { if (cursor.moveToNext()) "${cursor.getInt(0)}:${cursor.getInt(1)}" else null }
                .joinToString(";")
        }

    private fun titlesText(): String =
        db.openHelper.readableDatabase.query("SELECT title FROM watch_history ORDER BY title").use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.joinToString(";")
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
