package com.choo.moviefinder.rag

import java.io.File
import java.util.Properties

/** local.properties(앱과 동일한 파일, git 미추적) 또는 환경변수에서 시크릿을 읽는다. */
object ProjectConfig {
    private val localProperties: Properties by lazy {
        Properties().apply {
            val file = File("local.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }
    }

    fun get(key: String): String? =
        System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: localProperties.getProperty(key)?.takeIf { it.isNotBlank() }
}
