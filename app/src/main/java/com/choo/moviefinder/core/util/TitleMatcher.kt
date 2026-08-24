package com.choo.moviefinder.core.util

// 공백/구두점/대소문자 차이로 인한 오탐지를 줄이기 위한 제목 정규화
fun normalizeTitle(title: String): String =
    title.lowercase().filterNot { it.isWhitespace() || it in IGNORED_PUNCTUATION }

fun titleMatches(candidateTitle: String, targetTitle: String): Boolean =
    normalizeTitle(candidateTitle) == normalizeTitle(targetTitle)

private val IGNORED_PUNCTUATION = setOf(':', '-', '!', '?', '.', ',', '·', '\'', '"', '(', ')')
