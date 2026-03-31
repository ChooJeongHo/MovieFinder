package com.choo.moviefinder.core.util

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(context: Context) : Timber.Tree() {

    private val logFile: File = File(context.cacheDir, "debug_log.txt")
    private val maxFileSize = 2L * 1024 * 1024 // 2MB
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        // 파일이 최대 크기를 초과하면 이전 파일로 교체한다
        if (logFile.exists() && logFile.length() > maxFileSize) {
            val backupFile = File(logFile.parent, "debug_log_prev.txt")
            backupFile.delete()
            logFile.renameTo(backupFile)
        }
    }

    // INFO 이상의 로그만 파일에 기록한다
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.INFO) return

        try {
            val timestamp = dateFormat.format(Date())
            val level = when (priority) {
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                else -> "D"
            }
            FileWriter(logFile, true).use { writer ->
                writer.appendLine("$timestamp $level/$tag: $message")
                if (t != null) {
                    val sw = StringWriter()
                    t.printStackTrace(PrintWriter(sw))
                    writer.appendLine(sw.toString())
                }
            }
        } catch (_: Exception) {
            // 파일 로깅 실패 시 앱 크래시를 방지한다
        }
    }

    companion object {
        fun getLogFile(context: Context): File = File(context.cacheDir, "debug_log.txt")
    }
}
