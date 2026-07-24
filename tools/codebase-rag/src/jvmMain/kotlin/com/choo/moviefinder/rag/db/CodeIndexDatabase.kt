package com.choo.moviefinder.rag.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import kotlinx.coroutines.Dispatchers

@Database(entities = [CodeChunk::class], version = 1, exportSchema = true)
@TypeConverters(FloatArrayCodec::class, ChunkTypeConverter::class)
abstract class CodeIndexDatabase : RoomDatabase() {
    abstract fun codeChunkDao(): CodeChunkDao
}

/** 프로젝트 루트 기준 `.rag/code_index.db` 파일에 저장 — git ignore 대상, 앱 DB(movie_finder_db)와 완전히 분리. */
fun openCodeIndexDatabase(projectRoot: File): CodeIndexDatabase {
    val dbDir = File(projectRoot, ".rag").apply { mkdirs() }
    val dbFile = File(dbDir, "code_index.db")
    return Room.databaseBuilder<CodeIndexDatabase>(name = dbFile.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
