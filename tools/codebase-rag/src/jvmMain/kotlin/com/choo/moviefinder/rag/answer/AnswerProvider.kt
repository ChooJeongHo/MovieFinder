package com.choo.moviefinder.rag.answer

import com.choo.moviefinder.rag.search.ScoredChunk

interface AnswerProvider {
    val name: String

    fun answer(question: String, context: List<ScoredChunk>): String
}
