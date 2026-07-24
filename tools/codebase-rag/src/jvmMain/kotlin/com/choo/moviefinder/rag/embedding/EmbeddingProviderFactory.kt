package com.choo.moviefinder.rag.embedding

import com.choo.moviefinder.rag.ProjectConfig

object EmbeddingProviderFactory {
    fun create(): EmbeddingProvider {
        val voyageKey = ProjectConfig.get("VOYAGE_API_KEY")
        return if (voyageKey != null) {
            VoyageEmbeddingProvider(apiKey = voyageKey)
        } else {
            LocalHashingEmbeddingProvider()
        }
    }
}
