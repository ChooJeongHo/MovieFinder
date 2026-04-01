package com.choo.moviefinder.core.util

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
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

    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
    }

    private val handlerThread = HandlerThread("FileLogger").apply { start() }
    private val handler = Handler(handlerThread.looper)

    init {
        // 파일이 최대 크기를 초과하면 이전 파일로 교체한다
        if (logFile.exists() && logFile.length() > maxFileSize) {
            val backupFile = File(logFile.parent, "debug_log_prev.txt")
            backupFile.delete()
            logFile.renameTo(backupFile)
        }
    }

    // INFO 이상의 로그만 파일에 기록한다 (백그라운드 HandlerThread로 디스크 I/O 오프로드)
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.INFO) return

        val timestamp = dateFormat.get()!!.format(Date())
        val level = when (priority) {
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            else -> "D"
        }
        val logLine = "$timestamp $level/$tag: $message" +
            if (t != null) {
                val sw = StringWriter()
                t.printStackTrace(PrintWriter(sw))
                "\n$sw"
            } else {
                ""
            }

        handler.post {
            try {
                FileWriter(logFile, true).use { it.appendLine(logLine) }
            } catch (_: Exception) {
                // 파일 로깅 실패 시 앱 크래시를 방지한다
            }
        }
    }

    companion object {
        fun getLogFile(context: Context): File = File(context.cacheDir, "debug_log.txt")
    }
}
